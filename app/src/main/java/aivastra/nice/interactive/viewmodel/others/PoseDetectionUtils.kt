package aivastra.nice.interactive.viewmodel.others

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.lang.Math.atan2

object PoseDetectionUtils {

    fun isHeadStraight(pose: Pose): Boolean {

        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (nose == null || leftShoulder == null || rightShoulder == null) return false

        if (nose.inFrameLikelihood < 0.6) return false

        val midX = (leftShoulder.position.x + rightShoulder.position.x) / 2
        val shoulderWidth = Math.abs(leftShoulder.position.x - rightShoulder.position.x)

        val diff = Math.abs(nose.position.x - midX)

        return diff < shoulderWidth * 0.25   // 🔥 dynamic tolerance
    }

    fun isUpperBodyCorrect(pose: Pose): Boolean {

        val headOk = isHeadStraight(pose)
        val shoulderOk = isShoulderStraight(pose)

        return headOk || shoulderOk   // 🔥 KEY CHANGE
    }

    fun isShoulderStraight(pose: Pose): Boolean {

        val left = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val right = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (left == null || right == null) return false

        // Ignore low confidence
        if (left.inFrameLikelihood < 0.6 || right.inFrameLikelihood < 0.6) return false

        val angle = Math.toDegrees(
            atan2(
                (right.position.y - left.position.y).toDouble(),
                (right.position.x - left.position.x).toDouble()
            )
        )

        return Math.abs(angle) < 15   // 🔥 increased tolerance (10 → 15)
    }

    fun isBodyStraight(pose: Pose): Boolean {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)

        if (leftShoulder != null && leftHip != null) {
            val dx = leftHip.position.x - leftShoulder.position.x
            val dy = leftHip.position.y - leftShoulder.position.y

            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))

            return Math.abs(angle - 90) < 10  // 90° = vertical
        }
        return false
    }

  /*  fun isLegBalanced(pose: Pose): Boolean {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)

        if (leftAnkle != null && rightAnkle != null && leftHip != null) {

            val legDistance = Math.abs(leftAnkle.position.x - rightAnkle.position.x)
            val bodyWidth = Math.abs(leftHip.position.x - rightAnkle.position.x)

            return legDistance > bodyWidth * 0.2   // ✅ relative check
        }
        return false
    }*/

    /*fun isLegBalanced(pose: Pose): Boolean {

        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        if (leftAnkle == null || rightAnkle == null || leftHip == null || rightHip == null) {
            return false
        }

        // 📏 body reference
        val bodyWidth = Math.abs(leftHip.position.x - rightHip.position.x)

        // ✅ 1. Side balance (X axis)
        val legDistanceX = Math.abs(leftAnkle.position.x - rightAnkle.position.x)
        val isSideBalanced = legDistanceX > bodyWidth * 0.15   // not too close

        // ❌ 2. Detect front/back (Y axis difference)
        val legDistanceY = Math.abs(leftAnkle.position.y - rightAnkle.position.y)
        val isSameLevel = legDistanceY < bodyWidth * 0.25   // 🔥 tolerance

        // ❌ 3. Detect extreme forward/back using knee alignment
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        var kneeBalanced = true
        if (leftKnee != null && rightKnee != null) {
            val kneeDiffY = Math.abs(leftKnee.position.y - rightKnee.position.y)
            kneeBalanced = kneeDiffY < bodyWidth * 0.25
        }
        return isSideBalanced && isSameLevel && kneeBalanced
    }*/

    fun isLegBalanced(pose: Pose): Boolean {

        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        if (leftAnkle == null || rightAnkle == null ||
            leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null
        ) return false

        val bodyWidth = Math.abs(leftHip.position.x - rightHip.position.x)

        // ✅ 1. Side distance (relaxed)
        val legDistanceX = Math.abs(leftAnkle.position.x - rightAnkle.position.x)
        val isSideBalanced = legDistanceX > bodyWidth * 0.12

        // ✅ 2. Cross check (mirror safe)
        val isNotCrossed =
            (leftAnkle.position.x < rightAnkle.position.x && leftHip.position.x < rightHip.position.x) ||
                    (leftAnkle.position.x > rightAnkle.position.x && leftHip.position.x > rightHip.position.x)

        // ✅ 3. Front/back (relaxed)
        val legDistanceY = Math.abs(leftAnkle.position.y - rightAnkle.position.y)
        val isSameLevel = legDistanceY < bodyWidth * 0.4

        var kneeBalanced = false
        val kneeDiffY = Math.abs(leftKnee.position.y - rightKnee.position.y)
        kneeBalanced = kneeDiffY < bodyWidth * 0.25

        // ✅ 4. Knee straight (FIXED)
        val leftAngle = getSafeAngle(leftHip, leftKnee, leftAnkle)
        val rightAngle = getSafeAngle(rightHip, rightKnee, rightAnkle)

        val isLeftStraight = leftAngle > 140   // 🔥 relaxed
        val isRightStraight = rightAngle > 140

        return isSideBalanced && isNotCrossed && isSameLevel && kneeBalanced && isLeftStraight && isRightStraight
    }

    fun getSafeAngle(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Double {

        val abX = a.position.x - b.position.x
        val abY = a.position.y - b.position.y
        val cbX = c.position.x - b.position.x
        val cbY = c.position.y - b.position.y

        val dot = abX * cbX + abY * cbY
        val magAB = Math.sqrt((abX * abX + abY * abY).toDouble())
        val magCB = Math.sqrt((cbX * cbX + cbY * cbY).toDouble())

        if (magAB == 0.0 || magCB == 0.0) return 180.0

        var cosValue = dot / (magAB * magCB)

        // 🔥 CLAMP to avoid NaN
        cosValue = cosValue.coerceIn(-1.0, 1.0)

        return Math.toDegrees(Math.acos(cosValue))
    }

    fun isHandVisible(pose: Pose): Boolean {

        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        // ❌ If any landmark missing → reject
        if (leftWrist == null || rightWrist == null || leftHip == null || rightHip == null) {
            return false
        }

        // 📏 Use body width as dynamic reference
        val bodyWidth = Math.abs(leftHip.position.x - rightHip.position.x)

        // ✅ 1. Allow hand around hip (not strict below)
        val leftNearHip = Math.abs(leftWrist.position.y - leftHip.position.y) < bodyWidth * 0.6
        val rightNearHip = Math.abs(rightWrist.position.y - rightHip.position.y) < bodyWidth * 0.6

        // ✅ 2. Allow slight inward/outward movement
        val leftAway = Math.abs(leftWrist.position.x - leftHip.position.x) > bodyWidth * 0.1
        val rightAway = Math.abs(rightWrist.position.x - rightHip.position.x) > bodyWidth * 0.1

        // ❌ 3. Reject if both hands too close to body (pocket case)
        val bothTooClose =
            Math.abs(leftWrist.position.x - leftHip.position.x) < bodyWidth * 0.05 &&
                    Math.abs(rightWrist.position.x - rightHip.position.x) < bodyWidth * 0.05

        if (bothTooClose) return false

        // ❌ 4. Reject if hands are together (folded/crossed)
        val handsDistance = Math.abs(leftWrist.position.x - rightWrist.position.x)
        val notTogether = handsDistance > bodyWidth * 0.25

        // ✅ 5. Individual hand validity
        val leftValid = leftNearHip && leftAway
        val rightValid = rightNearHip && rightAway

        // 🔥 FINAL: allow natural variation
        return leftValid && rightValid && notTogether
    }

    fun isFullBodyVisible(pose: Pose): Boolean {

        val requiredLandmarks = listOf(
            PoseLandmark.NOSE,
            PoseLandmark.LEFT_SHOULDER,
            PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_ANKLE
        )

        // ✅ Only check: all parts detected with good confidence
        for (type in requiredLandmarks) {
            val landmark = pose.getPoseLandmark(type)
            if (landmark == null || landmark.inFrameLikelihood < 0.5f) {
                return false
            }
        }
        return true
    }

    fun isValidPoseDetect(pose: Pose):Boolean {
        if(!isFullBodyVisible(pose)){
            return false
        }
        val checks = listOf(
            isUpperBodyCorrect(pose),
            isBodyStraight(pose),
            isLegBalanced(pose),
            isHandVisible(pose),
        )

        val passCount = checks.count { it }

        return passCount >= 4   // ✅ allow minor mistakes
    }

    fun getPoseError(pose: Pose): String {

        if(!isUpperBodyCorrect(pose)
            && !isHandVisible(pose)
            && !isBodyStraight(pose)
            && !isLegBalanced(pose)
        ){
            return "Invalid pose"
        }

        if (!isFullBodyVisible(pose)) return "Full body not detected"
        if (!isUpperBodyCorrect(pose)) return "Keep your head and shoulder straight"
        if (!isHandVisible(pose)) return "Keep both hands down relaxed"
        if (!isBodyStraight(pose)) return "Stand straight"
        if (!isLegBalanced(pose)) return "Balance your legs"

        return "Invalid pose"
    }

    fun isImageBlurred(bitmap: Bitmap, threshold: Double = 10.0): Boolean {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to grayscale
        val gray = DoubleArray(width * height)
        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xff
            val g = (pixels[i] shr 8) and 0xff
            val b = pixels[i] and 0xff

            gray[i] = (0.299 * r + 0.587 * g + 0.114 * b)
        }

        // Apply Laplacian (edge detection)
        val laplacian = DoubleArray(width * height)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {

                val index = y * width + x

                val center = gray[index]
                val top = gray[(y - 1) * width + x]
                val bottom = gray[(y + 1) * width + x]
                val left = gray[y * width + (x - 1)]
                val right = gray[y * width + (x + 1)]

                laplacian[index] = (4 * center - top - bottom - left - right)
            }
        }

        // Calculate variance
        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (value in laplacian) {
            sum += value
            sumSq += value * value
            count++
        }

        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)

        // Debug log
        Log.d("BlurCheck", "Variance: $variance")

        return variance < threshold
    }
}