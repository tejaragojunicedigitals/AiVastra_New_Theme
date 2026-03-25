package aivastra.nice.interactive.utils

import com.google.mlkit.vision.pose.PoseLandmark

data class PoseLandmarkCache(
    val leftShoulder: PoseLandmark?,
    val rightShoulder: PoseLandmark?,
    val leftHip: PoseLandmark?,
    val rightHip: PoseLandmark?,
    val leftKnee: PoseLandmark?,
    val rightKnee: PoseLandmark?,
    val leftAnkle: PoseLandmark?,
    val rightAnkle: PoseLandmark?,
    val leftElbow: PoseLandmark?,
    val rightElbow: PoseLandmark?,
    val leftWrist: PoseLandmark?,
    val rightWrist: PoseLandmark?,
    val nose: PoseLandmark?
)