package aivastra.nice.interactive.viewmodel.others

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2

class PoseValidator(private val context: Context) {

    lateinit var poseLandmarker: PoseLandmarker
    private var isInitialized = false

    // ✅ Result Callback
    interface PoseResultCallback {
        fun onResult(isValid: Boolean, message: String)
    }

    // ✅ Landmark Model
    data class Landmark(
        val x: Float,
        val y: Float,
        val visibility: Float
    )

    private suspend fun setupPoseLandmarker(): Boolean = withContext(Dispatchers.Default) {
        try {
            if (isInitialized) return@withContext true

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_lite.task")
                .setDelegate(Delegate.CPU)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(
                context.applicationContext,
                options
            )

            isInitialized = true
            true

        } catch (e: Exception) {
            Log.e("POSE", "Init failed: ${e.message}")
            false
        }
    }

    // 🔥 Public API
    fun validatePose(imageUri: Uri, callback: PoseResultCallback) {
        (context as? LifecycleOwner)?.lifecycleScope?.launch {

            try {
                // ✅ Step 1: Initialize model in background
                if (!isInitialized) {
                    val success = withContext(Dispatchers.Default) {
                        setupPoseLandmarker()
                    }
                    if (!success) {
                        sendResult(callback, false, "Initialization failed")
                        return@launch
                    }
                }

                if (!::poseLandmarker.isInitialized) {
                    sendResult(callback, false, "Pose model not ready")
                    return@launch
                }

                // ✅ Step 2: Decode bitmap in IO thread
                val bitmap = withContext(Dispatchers.IO) {
                    getBitmapFromUri(imageUri)
                }

                if (bitmap == null) {
                    sendResult(callback, false, "Image not found")
                    return@launch
                }

                val width = bitmap.width
                val height = bitmap.height

                // ✅ Step 3: Pose detection in background (IMPORTANT)
                val landmarks = withContext(Dispatchers.Default) {
                    detectPose(bitmap, width, height)
                }

                if (landmarks == null) {
                    sendResult(callback, false, "No person detected")
                    return@launch
                }

                // ✅ Step 4: Validation (light but still better off Main)
                val message = withContext(Dispatchers.Default) {
                    isPerfectPose(landmarks, width, height)
                }

                val isValid = message == "Perfect Pose ✅"

                sendResult(callback, isValid, message)

            } catch (e: Exception) {
                sendResult(callback, false, "Something went wrong: ${e.message}")
            }
        }
    }

    // 📸 URI → Bitmap
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
    }

    // 🧠 MediaPipe Detection
    private fun detectPose(
        bitmap: Bitmap,
        width: Int,
        height: Int
    ): Map<Int, Landmark>? {

        val image = BitmapImageBuilder(bitmap).build()
        val result = poseLandmarker.detect(image)

        val pose = result.landmarks().firstOrNull() ?: return null

        val map = mutableMapOf<Int, Landmark>()

        pose.forEachIndexed { index, lm ->
            map[index] = Landmark(
                x = lm.x() * width,
                y = lm.y() * height,
                visibility = lm.visibility().orElse(1f)
            )
        }

        return map
    }

    // 🎯 Pose Validation Logic
    private fun isPerfectPose(
        landmarks: Map<Int, Landmark>,
        width: Int,
        height: Int
    ): String {

        fun p(i: Int) = landmarks[i]

        val leftShoulder = p(11)
        val rightShoulder = p(12)
        val leftHip = p(23)
        val rightHip = p(24)
        val leftKnee = p(25)
        val leftAnkle = p(27)
        val rightKnee = p(26)
        val rightAnkle = p(28)
        val leftElbow = p(13)
        val rightElbow = p(14)
        val leftWrist = p(15)
        val rightWrist = p(16)

        val nose = p(0)

        if (listOf(leftShoulder, rightShoulder, leftHip, rightHip,
            leftKnee,rightKnee,leftAnkle,rightAnkle).any { it == null }) {
            return "Body not fully visible"
        }

        // ✅ Visibility check
        val minVisibility = 0.5f

        val importantPoints = listOf(
            leftShoulder, rightShoulder,
            leftHip, rightHip,
            leftKnee,rightKnee,leftAnkle,rightAnkle
        )

        if (importantPoints.any { it == null || it.visibility < minVisibility }) {
            return "Some body parts not visible clearly"
        }

        // ✅ Center check
        val centerX = (leftShoulder!!.x + rightShoulder!!.x) / 2
        if (kotlin.math.abs(centerX - width / 2f) > width * 0.15f) {
            return "Move to center"
        }

        // ✅ Shoulder level
        val shoulderDiff = kotlin.math.abs(leftShoulder.y - rightShoulder!!.y)
        if (shoulderDiff > height * 0.03f) {
            return "Keep shoulders straight"
        }

        // ✅ Spine straight
        val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2
        val midHipX = (leftHip!!.x + rightHip!!.x) / 2

        if (kotlin.math.abs(midShoulderX - midHipX) > width * 0.05f) {
            return "Stand straight"
        }

        // ✅ Head alignment
        if (nose != null) {
            if (kotlin.math.abs(nose.x - midShoulderX) > width * 0.07f) {
                return "Keep head straight"
            }
        }

        // ✅ Left Leg straight
        if (leftKnee != null && leftAnkle != null) {
            val angle = calculateAngle(leftHip!!, leftKnee, leftAnkle)
            if (angle < 140) {
                return "Keep legs straight"
            }
        }

        // ✅ Right leg check
        if (rightKnee != null && rightAnkle != null) {
            val rightAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
            if (rightAngle < 140) {
                return "Keep legs straight"
            }
        }

        // ✅ Both leg check
        if (leftKnee != null && leftAnkle != null && rightKnee != null && rightAnkle != null) {

            val leftAngle = calculateAngle(leftHip!!, leftKnee, leftAnkle)
            val rightAngle = calculateAngle(rightHip!!, rightKnee, rightAnkle)

            val leftAligned =
                abs(leftHip.x - leftKnee.x) < width * 0.05f &&
                        abs(leftKnee.x - leftAnkle.x) < width * 0.05f

            val rightAligned =
                abs(rightHip.x - rightKnee.x) < width * 0.05f &&
                        abs(rightKnee.x - rightAnkle.x) < width * 0.05f

            val ankleDistance = abs(leftAnkle.x - rightAnkle.x)

            if (leftAngle < 150 || rightAngle < 150) {
                return "Keep both legs straight"
            }

            // ankle/toe alignment
            if (!leftAligned || !rightAligned || ankleDistance < width * 0.04f) {
                return "Legs straight but feet/toes crossed"
            }

            val minVisibility = 0.6f

            val isLeftLegVisible = leftHip.visibility > minVisibility &&
                    leftKnee.visibility > minVisibility &&
                    leftAnkle.visibility > minVisibility

            val isRightLegVisible = rightHip.visibility > minVisibility &&
                    rightKnee.visibility > minVisibility &&
                    rightAnkle.visibility > minVisibility

              // ✅ Only check legs if both knees and ankles are visible
              if (isLeftLegVisible && isRightLegVisible) {
                if (leftAngle < 140 || rightAngle < 140){
                    return "Keep both legs straight"
                }
            }
        }

        // ✅ Hand check

        val errorMessage = "Keep both hands relaxed down"

       //  Missing landmarks
        if (
            leftWrist == null || rightWrist == null ||
            leftElbow == null || rightElbow == null) {
            return "Missing landmarks $errorMessage"
        }

       //  Visibility
//        if (leftWrist.visibility < 0.6f || rightWrist.visibility < 0.6f) {
//            return "Both hand not visible properly"
//        }

        //  Not down
        val leftHandDown = leftWrist.y > leftElbow.y && leftElbow.y > leftShoulder.y
        val rightHandDown = rightWrist.y > rightElbow.y && rightElbow.y > rightShoulder.y

        if (!leftHandDown || !rightHandDown) {
            return "Not down $errorMessage"
        }

      /*  //  Near waist
        val leftNearHip =
            abs(leftWrist.x - leftHip.x) < width * 0.12f &&
                    abs(leftWrist.y - leftHip.y) < height * 0.10f

        val rightNearHip =
            abs(rightWrist.x - rightHip.x) < width * 0.12f &&
                    abs(rightWrist.y - rightHip.y) < height * 0.10f

        if (leftNearHip || rightNearHip) {
            return errorMessage
        }*/

       //  Hands together
        val wristDistance = abs(leftWrist.x - rightWrist.x)
        if (wristDistance < width * 0.08f) {
            return "Hands together  $errorMessage"
        }

//        //  Cross hands
//        if (leftWrist.x > rightShoulder.x || rightWrist.x < leftShoulder.x) {
//            return errorMessage
//        }

        //  Pocket
        val nearHipThresholdX = width * 0.10f
        val nearHipThresholdY = height * 0.10f

        val leftNearHip =
            abs(leftWrist.x - leftHip.x) < nearHipThresholdX &&
                    abs(leftWrist.y - leftHip.y) < nearHipThresholdY

        val rightNearHip =
            abs(rightWrist.x - rightHip.x) < nearHipThresholdX &&
                    abs(rightWrist.y - rightHip.y) < nearHipThresholdY

       // Angle check (slight bend, not fully straight)
        val leftAngle = calculateAngle(leftShoulder!!, leftElbow!!, leftWrist!!)
        val rightAngle = calculateAngle(rightShoulder!!, rightElbow!!, rightWrist!!)

        val leftSlightBend = leftAngle < 165
        val rightSlightBend = rightAngle < 165

       // Not fully hanging down (important for tall people)
        val leftNotTooDown = leftWrist.y < leftHip.y + height * 0.05f
        val rightNotTooDown = rightWrist.y < rightHip.y + height * 0.05f

        val leftInPocket = leftNearHip && leftSlightBend && leftNotTooDown
        val rightInPocket = rightNearHip && rightSlightBend && rightNotTooDown

        if (leftInPocket || rightInPocket) {
            return "pocket  $errorMessage"
        }

        return "Perfect Pose ✅"
    }

    // 📐 Angle Calculation
    private fun calculateAngle(a: Landmark, b: Landmark, c: Landmark): Double {
        val ab = atan2(a.y - b.y, a.x - b.x)
        val cb = atan2(c.y - b.y, c.x - b.x)

        var angle = Math.toDegrees(abs(ab - cb).toDouble())
        if (angle > 180) angle = 360 - angle

        return angle
    }

    // 📤 Send result on Main Thread
    private suspend fun sendResult(
        callback: PoseResultCallback,
        isValid: Boolean,
        message: String
    ) {
        withContext(Dispatchers.Main) {
            callback.onResult(isValid, message)
        }
    }
}