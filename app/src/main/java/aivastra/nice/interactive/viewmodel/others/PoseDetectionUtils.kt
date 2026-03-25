package aivastra.nice.interactive.viewmodel.others

import aivastra.nice.interactive.activity.camera.UniversalCameraActivity
import aivastra.nice.interactive.dialog.ShowErrorAlertDialog
import aivastra.nice.interactive.utils.AppConstant
import aivastra.nice.interactive.utils.PoseLandmarkCache
import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.lang.Math.atan2

object PoseDetectionUtils {

    private var landmarkCache: PoseLandmarkCache? = null

    fun isHeadStraight(): Boolean {

        val cache = landmarkCache ?: return false
        val nose = cache.nose
        val leftShoulder = cache.leftShoulder
        val rightShoulder = cache.rightShoulder

        if (nose == null || leftShoulder == null || rightShoulder == null) return false

        if (nose.inFrameLikelihood < 0.6) return false

        val midX = (leftShoulder.position.x + rightShoulder.position.x) / 2
        val shoulderWidth = Math.abs(leftShoulder.position.x - rightShoulder.position.x)

        val diff = Math.abs(nose.position.x - midX)

        return diff < shoulderWidth * 0.25   // 🔥 dynamic tolerance
    }

    fun isUpperBodyCorrect(): Boolean {

        val headOk = isHeadStraight()
        val shoulderOk = isShoulderStraight()

        return headOk || shoulderOk   // 🔥 KEY CHANGE
    }

    fun isShoulderStraight(): Boolean {

        val cache = landmarkCache ?: return false
        val left = cache.leftShoulder
        val right = cache.rightShoulder

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

    fun isBodyStraight(): Boolean {

        val cache = landmarkCache ?: return false
        val leftShoulder = cache.leftShoulder
        val leftHip = cache.leftHip

        val rightShoulder = cache.rightShoulder
        val rightHip = cache.rightHip

        val leftStraight = if (leftShoulder != null && leftHip != null) {
            val dx = leftHip.position.x - leftShoulder.position.x
            val dy = leftHip.position.y - leftShoulder.position.y
            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
            Math.abs(angle - 90) < 10
        } else false

        val rightStraight = if (rightShoulder != null && rightHip != null) {
            val dx = rightHip.position.x - rightShoulder.position.x
            val dy = rightHip.position.y - rightShoulder.position.y
            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
            Math.abs(angle - 90) < 10
        } else false

        return leftStraight || rightStraight   // ✅ flexible
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

    fun isLegBalanced(): Boolean {
        val cache = landmarkCache ?: return false
        val leftAnkle = cache.leftAnkle
        val rightAnkle = cache.rightAnkle
        val leftHip = cache.leftHip
        val rightHip = cache.rightHip
        val leftKnee = cache.leftKnee
        val rightKnee =cache.rightKnee

        if (leftAnkle == null || rightAnkle == null ||
            leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null
        ) return false

        val bodyWidth = Math.abs(leftHip.position.x - rightHip.position.x)

        // ✅ 1. Side distance (relaxed)
        val legDistanceX = Math.abs(leftAnkle.position.x - rightAnkle.position.x)
        val isSideBalanced = legDistanceX > bodyWidth * 0.18

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

        val isLeftStraight = leftAngle > 160   // 🔥 relaxed
        val isRightStraight = rightAngle > 160

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

    fun isHandVisible(): Boolean {

        val cache = landmarkCache ?: return false

        val lw = cache.leftWrist
        val rw = cache.rightWrist
        val le = cache.leftElbow
        val re = cache.rightElbow
        val ls = cache.leftShoulder
        val rs = cache.rightShoulder
        val lh = cache.leftHip
        val rh = cache.rightHip

        if (lw == null || rw == null || le == null || re == null ||
            ls == null || rs == null || lh == null || rh == null
        ) return false

        val bodyWidth = Math.abs(lh.position.x - rh.position.x)

        // BOTH HANDS MUST BE VISIBLE
        val leftVisible = lw.inFrameLikelihood > 0.5
        val rightVisible = rw.inFrameLikelihood > 0.5
        if (!leftVisible || !rightVisible) return false

        // POCKET DETECTION
       /* val leftAngle = getSafeAngle(ls, le, lw)
        val rightAngle = getSafeAngle(rs, re, rw)

        val leftPocket =
            leftAngle < 150 &&
                    Math.abs(le.position.x - lh.position.x) < bodyWidth * 0.30

        val rightPocket =
            rightAngle < 150 &&
                    Math.abs(re.position.x - rh.position.x) < bodyWidth * 0.30

        if (leftPocket || rightPocket) return false*/
        val bodyHeight = Math.abs(ls.position.y - lh.position.y)

        val leftWristHipX = Math.abs(lw.position.x - lh.position.x) / bodyWidth
        val rightWristHipX = Math.abs(rw.position.x - rh.position.x) / bodyWidth

        val leftWristHipY = Math.abs(lw.position.y - lh.position.y) / bodyHeight
        val rightWristHipY = Math.abs(rw.position.y - rh.position.y) / bodyHeight

        val leftElbowHipX = Math.abs(le.position.x - lh.position.x) / bodyWidth
        val rightElbowHipX = Math.abs(re.position.x - rh.position.x) / bodyWidth

        val leftAngle = getSafeAngle(ls, le, lw)
        val rightAngle = getSafeAngle(rs, re, rw)

        val leftPocket =
            (leftWristHipX < 0.35) &&
                    (leftWristHipY < 0.6) &&
                    (leftElbowHipX < 0.35) &&
                    (leftAngle < 160)

        val rightPocket =
            (rightWristHipX < 0.35) &&
                    (rightWristHipY < 0.6) &&
                    (rightElbowHipX < 0.35) &&
                    (rightAngle < 160)

        if (leftPocket || rightPocket) return false

        //  CROSS HAND DETECTION
        val isCrossed =
            (lw.position.x > rw.position.x && ls.position.x < rs.position.x) ||
                    (lw.position.x < rw.position.x && ls.position.x > rs.position.x)

        if (isCrossed) return false

        val handsDistance = Math.abs(lw.position.x - rw.position.x)
        if (handsDistance < bodyWidth * 0.12) return false

        //  HANDS SHOULD NOT BE RAISED
        val leftDown = lw.position.y > ls.position.y
        val rightDown = rw.position.y > rs.position.y
        if (!leftDown || !rightDown) return false

        // FOLDED HAND DETECTION
        if (leftAngle < 100 || rightAngle < 100) return false

        val msg = buildString {
            append("📏 BW=${bodyWidth.toInt()}\n")
            append("👁 VIS L=$leftVisible R=$rightVisible\n")
            append("🧥 POCKET L=$leftPocket R=$rightPocket\n")
            append("🔀 CROSS=$isCrossed D=${handsDistance.toInt()}\n")
            append("⬇ DOWN L=$leftDown R=$rightDown\n")
            append("📐 ANG L=${"%.1f".format(leftAngle)} R=${"%.1f".format(rightAngle)}\n")
        }
        Log.d("Hand Landmark",msg)
        return true
    }

    fun isValidSinglePerson(): Boolean {

        val cache = landmarkCache ?: return false

        val ls = cache.leftShoulder
        val rs = cache.rightShoulder
        val lh = cache.leftHip
        val rh = cache.rightHip
        val nose = cache.nose
        val la = cache.leftAnkle
        val ra = cache.rightAnkle

        val basicValid = listOf(ls, rs, lh, rh, nose, la, ra).all {
            it != null && it.inFrameLikelihood > 0.6
        }

        if (!basicValid) return false

        // 🔥 Extra checks
        val shoulderWidth = Math.abs(ls!!.position.x - rs!!.position.x)
        if (shoulderWidth < 40f) return false

        val leftBody = Math.abs(ls.position.x - lh!!.position.x)
        val rightBody = Math.abs(rs.position.x - rh!!.position.x)

        if (Math.abs(leftBody - rightBody) > 80f) return false

         return true
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
            if (landmark == null || landmark.inFrameLikelihood < 0.6f) {
                return false
            }
        }
        return true
    }

    fun isValidPoseDetect(pose: Pose):Boolean {
        prepareLandmarks(pose)
        if(!isFullBodyVisible(pose)){
            return false
        }
//        if (!isValidSinglePerson()) return false
        if (!isLegBalanced()) return false
        if (!isHandVisible()) return false
        if (!isUpperBodyCorrect()) return false
        if (!isBodyStraight()) return false

        return true
    }

    fun prepareLandmarks(pose: Pose) {
        landmarkCache = PoseLandmarkCache(
            pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER),
            pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER),
            pose.getPoseLandmark(PoseLandmark.LEFT_HIP),
            pose.getPoseLandmark(PoseLandmark.RIGHT_HIP),
            pose.getPoseLandmark(PoseLandmark.LEFT_KNEE),
            pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE),
            pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE),
            pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE),
            pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW),
            pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW),
            pose.getPoseLandmark(PoseLandmark.LEFT_WRIST),
            pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST),
            pose.getPoseLandmark(PoseLandmark.NOSE)
        )
    }

    fun getPoseError(pose: Pose): String {

        if(!isUpperBodyCorrect()
            && !isHandVisible()
            && !isBodyStraight()
            && !isLegBalanced()
        ){
            return "Invalid pose"
        }

        if (!isFullBodyVisible(pose)) return "Full body not detected"
        if (!isUpperBodyCorrect()) return "Keep your head and shoulder straight"
        if (!isHandVisible()) return "Keep both hands down relaxed"
//        if (!isValidSinglePerson()) return "Multiple person detected. Only one person allowed"
        if (!isBodyStraight()) return "Stand straight"
        if (!isLegBalanced()) return "Balance your legs"

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