package aivastra.nice.interactive.activity.camera

import aivastra.nice.interactive.Loader.LoaderManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import aivastra.nice.interactive.R
import aivastra.nice.interactive.customview.MyDeviceAdminReceiver
import aivastra.nice.interactive.customview.UniversalRotationManager
import aivastra.nice.interactive.databinding.ActivityCamera2CaptureBinding
import aivastra.nice.interactive.utils.AppConstant
import aivastra.nice.interactive.utils.PrefsManager
import aivastra.nice.interactive.utils.ViewControll
import aivastra.nice.interactive.utils.ViewControll.setSafeOnClickListener
import aivastra.nice.interactive.viewmodel.Dress.DressesTypeDataModel
import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.camera2.CaptureRequest
import android.media.ExifInterface
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import com.yalantis.ucrop.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class Camera2CaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCamera2CaptureBinding
    private var vastraFor: String = "women"
    private var selectedVastraSubCategory : DressesTypeDataModel.Data.Subcategory? = null
    private var selectedVastraItem : DressesTypeDataModel.Data.Subcategory.Item? = null
    private val cameraPermissions = arrayOf(Manifest.permission.CAMERA)
    // Only request WRITE_EXTERNAL_STORAGE for Android 9 and below
    private val legacyStoragePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private var imageCapture: ImageCapture? = null
    private var handler: Handler? = null
    private var countdownRunnable: Runnable? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var activeCamera: Camera


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamera2CaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        initView()
    }

    private fun initView() {
        vastraFor = intent?.extras?.getString(AppConstant.VASTRA_FOR).toString()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            selectedVastraSubCategory = intent?.extras?.getSerializable(
                AppConstant.SELECTED_VASTRA_SUBCAT,
                DressesTypeDataModel.Data.Subcategory::class.java)!!
        }else{
            selectedVastraSubCategory= intent.extras?.getSerializable(AppConstant.SELECTED_VASTRA_SUBCAT) as DressesTypeDataModel.Data.Subcategory
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            selectedVastraItem = intent?.extras?.getSerializable(
                AppConstant.SELECTED_VASTRA_ITEM,
                DressesTypeDataModel.Data.Subcategory.Item::class.java)!!
        }else{
            selectedVastraItem= intent.extras?.getSerializable(AppConstant.SELECTED_VASTRA_ITEM) as DressesTypeDataModel.Data.Subcategory.Item
        }
        ViewControll.setCompanyLogoHorizontal(this,binding.llToolbar.appLogo)
        setKioskLockModeSetting()
        binding.buttonCapture.setSafeOnClickListener{
            stopCountdown()
            takePhoto()
        }
        binding.btnFlipCamera.setOnClickListener {
            onFlipCameraClicked()
        }
        binding.llToolbar.imgBack.setOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun requestCameraPermissions(isFromOpenDefault: Boolean) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9 or lower
            val allGranted = legacyStoragePermissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            } && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

            if (!allGranted) {
                permissionLauncher.launch(legacyStoragePermissions + cameraPermissions)
                return
            }
        }else{
            // Android 10+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(cameraPermissions)
                return
            }
        }
        // If permissions granted -> open camera
        openCameraInKioskMode(isFromOpenDefault)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {result ->
        if(result.values.all { it }){
            openCameraInKioskMode(true)
        }else{
            ViewControll.showMessage(this,"Permission required to use camera")
            finish()
        }
    }

    private fun enableCameraForKiosk() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, MyDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(packageName)) {
                dpm.setCameraDisabled(admin, false)

                // 🔴 REQUIRED for Android 9
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    dpm.setKeyguardDisabled(admin, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openCameraSafely(isFromOpenDefault: Boolean) {
        val delay = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) 800L else 0L

        Handler(Looper.getMainLooper()).postDelayed({
            setupCamera(isFromOpenDefault)
        }, delay)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun setupCamera(isFromOpenDefault:Boolean) {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
                )
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        AspectRatio.RATIO_4_3,
                        AspectRatioStrategy.FALLBACK_RULE_AUTO
                    )
                )
                .build()

            cameraProviderFuture.addListener({

                cameraProvider = cameraProviderFuture.get()

                if (!hasMultipleCameras(cameraProvider)) {
                    binding.btnFlipCamera.visibility = View.GONE
                }

                if (isFromOpenDefault) {
                    lensFacing =
                        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                }
//                val rotation = binding.previewView.display?.rotation ?: Surface.ROTATION_0
                val rotation = getCorrectPreviewRotation()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setTargetRotation(rotation)
                    .build()
                    .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

                val imageCaptureBuilder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setResolutionSelector(resolutionSelector)
                    .setTargetRotation(rotation)

                // ---- Camera2 Interop (SAFE) ----
                val extender = Camera2Interop.Extender(imageCaptureBuilder)

                extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )

                extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )

                extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO
                )

            /*    extender.setCaptureRequestOption(
                    CaptureRequest.NOISE_REDUCTION_MODE,
                    CaptureRequest.NOISE_REDUCTION_MODE_FAST
                )*/

              /*  val noiseMode = if (isLikelyDark())
                    CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
                else
                    CaptureRequest.NOISE_REDUCTION_MODE_FAST

                extender.setCaptureRequestOption(
                    CaptureRequest.NOISE_REDUCTION_MODE,
                    CaptureRequest.NOISE_REDUCTION_MODE_FAST
                )*/

                imageCapture = imageCaptureBuilder.build()


                // ---- HDR EXTENSION (CRASH SAFE) ----
                /*  val finalCameraSelector =
                if (cameraProvider.isExtensionAvailable(
                        cameraSelector,
                        ExtensionMode.HDR
                    )
                ) {
                    cameraProvider.getExtensionEnabledCameraSelector(
                        cameraSelector,
                        ExtensionMode.HDR
                    )
                } else {
                    cameraSelector
                }*/

                val extensionsManagerFuture =
                    ExtensionsManager.getInstanceAsync(this, cameraProvider)

                val extensionsManager = extensionsManagerFuture.get()

               /* val baseSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()*/

                val baseSelector = getVersionBasedCameraSelector(cameraProvider)
                if(baseSelector==null){
                    reportCameraFailure(
                        stage = "No camera available",
                        throwable = null
                    )
                    return@addListener
                }

              /*  val finalSelector =
                    extensionsManager?.let { manager ->
                        if (manager.isExtensionAvailable(
                                baseSelector,
                                ExtensionMode.HDR
                            )
                        ) {
                            manager.getExtensionEnabledCameraSelector(
                                baseSelector,
                                ExtensionMode.HDR
                            )
                        } else baseSelector
                    } ?: baseSelector*/

                bindCamera(cameraProvider, baseSelector, preview)

                /*   cameraProvider.unbindAll()

            val camera = cameraProvider.bindToLifecycle(
                this,
                finalSelector,
                preview,
                imageCapture
            )*/

            }, ContextCompat.getMainExecutor(this))
        }catch (e:Exception){
            reportCameraFailure(
                stage = "setupCamera",
                throwable = e
            )
        }
    }

    private fun bindCamera(
        cameraProvider: ProcessCameraProvider,
        cameraSelector: CameraSelector,
        preview: Preview,
    ) {
        try{
            cameraProvider.unbindAll()

            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
            )
            activeCamera = camera
            startCameraStateMonitoring(camera)
            // ---- Autofocus Metering ----
            startAutoFocus(camera)
            Handler(Looper.getMainLooper()).postDelayed({
                applySmartLightControl(camera)
            }, 1000)
            startCountdown()
        }catch (e:Exception){
            reportCameraFailure(
                stage = "bindCamera",
                throwable = e
            )
        }
    }

    private fun isKioskDevice(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(packageName)
    }

    private fun getCorrectPreviewRotation(): Int {
        return if (isKioskDevice()) {
            Surface.ROTATION_270   // ✅ FIX for most kiosk cameras
        } else {
            binding.previewView.display?.rotation ?: Surface.ROTATION_0
        }
    }

    private fun getVersionBasedCameraSelector(
        cameraProvider: ProcessCameraProvider
    ): CameraSelector? {
        // 🔴 Android 9 and below (API 28 and below)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {

            val availableCameraInfos = cameraProvider.availableCameraInfos

            if (availableCameraInfos.isNotEmpty()) {
                return CameraSelector.Builder()
                    .addCameraFilter { listOf(availableCameraInfos.first()) }
                    .build()
            }else{
                return null
            }
        }else{
            // 🟢 Android 10+
            return CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
        }
    }

    private fun startCameraStateMonitoring(camera: Camera) {
        camera.cameraInfo.cameraState.observe(this) { state ->

            val error = state.error
            if (error != null) {

                val exactReason = when (error.code) {
                    CameraState.ERROR_CAMERA_DISABLED ->
                        "ERROR_CAMERA_DISABLED (DevicePolicy / Kiosk)"

                    CameraState.ERROR_CAMERA_IN_USE ->
                        "ERROR_CAMERA_IN_USE (Another app is using camera)"

                    CameraState.ERROR_MAX_CAMERAS_IN_USE ->
                        "ERROR_MAX_CAMERAS_IN_USE"
                    else ->
                        "UNKNOWN_ERROR"
                }

                reportCameraFailure(
                    stage = "CameraState",
                    throwable = RuntimeException(exactReason)
                )
            }
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun applySmartLightControl(camera: Camera) {

        val isDark = isDarkScene(camera)

        // 🔦 TORCH (BEST LOW LIGHT FIX)
        if (camera.cameraInfo.hasFlashUnit()) {
            camera.cameraControl.enableTorch(isDark)
        }

        // 🌞 EXPOSURE COMPENSATION (SAFE)
        val exposureState = camera.cameraInfo.exposureState
        val range = exposureState.exposureCompensationRange

        if (range.upper > 0) {
            val value = if (isDark) {
                (range.upper * 0.4f).toInt() // gentle boost
            } else {
                0
            }
            camera.cameraControl.setExposureCompensationIndex(value)
        }
    }

    private fun isDarkScene(camera: Camera): Boolean {
        val exposureState = camera.cameraInfo.exposureState
        val range = exposureState.exposureCompensationRange

        if (range.upper <= 0) return false

        val current = exposureState.exposureCompensationIndex
        val threshold = (range.upper * 0.6f).toInt()

        return current >= threshold
    }

    private fun isLikelyDark(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    fun onFlipCameraClicked() {
        val newLensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                CameraSelector.LENS_FACING_BACK
            else
                CameraSelector.LENS_FACING_FRONT

        val newSelector = CameraSelector.Builder()
            .requireLensFacing(newLensFacing)
            .build()

        if (cameraProvider.hasCamera(newSelector)) {
            lensFacing = newLensFacing
            openCameraInKioskMode(false)
        } else {
            Toast.makeText(
                this,
                "Camera not available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun hasMultipleCameras(cameraProvider: ProcessCameraProvider): Boolean {
        return cameraProvider.availableCameraInfos.size > 1
    }

    private fun openCameraInKioskMode(isFromOpenDefault: Boolean){
        enableCameraForKiosk()
        openCameraSafely(isFromOpenDefault)
    }

    private fun getCameraSelector(cameraProvider: ProcessCameraProvider): CameraSelector {
        return when {
            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                CameraSelector.DEFAULT_BACK_CAMERA

            else -> throw IllegalStateException("No camera available")
        }
    }

    private fun startAutoFocus(camera: Camera) {
        val factory = binding.previewView.meteringPointFactory
        val point = factory.createPoint(
            binding.previewView.width / 2f,
            binding.previewView.height / 2f
        )

        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()

        camera.cameraControl.startFocusAndMetering(action)
    }


    private fun setKioskLockModeSetting(){
        try{
            if (Build.VERSION.SDK_INT >= 34) {
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val admin = ComponentName(this, MyDeviceAdminReceiver::class.java)
                if (dpm.isDeviceOwnerApp(packageName)) {
                    dpm.setCameraDisabled(admin, false)
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun takePhoto() {
        imageCapture ?: return
        // 🔥 UPDATE ROTATION JUST BEFORE CAPTURE
        imageCapture?.targetRotation = binding.previewView.display?.rotation ?: Surface.ROTATION_0
        val sound = MediaActionSound()
        sound.play(MediaActionSound.SHUTTER_CLICK)
        /* val photoFile = File(
              getExternalFilesDir(null),
             "IMG_${System.currentTimeMillis()}.jpg"
         )*/
        Handler(Looper.getMainLooper()).postDelayed({
            capturePhotoCallback()
        }, 500)
    }

    private fun lockFocusThenCapture() {
        if(!::activeCamera.isInitialized){
            return
        }

        val factory = binding.previewView.meteringPointFactory
        val point = factory.createPoint(
            binding.previewView.width / 2f,
            binding.previewView.height / 2f
        )

        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )
            .setAutoCancelDuration(2, TimeUnit.SECONDS)
            .build()

        activeCamera.cameraControl.startFocusAndMetering(action)

        Handler(Looper.getMainLooper()).postDelayed({
            capturePhotoCallback()
        }, 600)
    }

    private fun capturePhotoCallback(){
        LoaderManager.show(this@Camera2CaptureActivity,findViewById(android.R.id.content),true)
        LoaderManager.setMessage("\uD83D\uDCF8 Preparing photo preview…")
        val photoFile = createImageFile() ?: return
        val options = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture?.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    Log.d("Camera", "Saved: ${photoFile.absolutePath}")
                    logCapturePhotoResolution(photoFile)
                    val photoURI = FileProvider.getUriForFile(
                        this@Camera2CaptureActivity,
                        "${packageName}.provider",
                        photoFile
                    )
                    photoURI?.let { path ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            LoaderManager.setMessage("✨ Getting your photo ready…")
                            val rotationFixedUri = fixImageRotationFromUriNew(this@Camera2CaptureActivity,path)
                            withContext(Dispatchers.Main) {
                                LoaderManager.remove(this@Camera2CaptureActivity)
                                if (rotationFixedUri != null) {
                                    startUCropImage(rotationFixedUri)
                                }else{
                                    startUCropImage(path)
                                }
//                                startUCropImage(path)
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("Camera", "Capture failed", exception)
                }
            }
        )
    }

    private fun logCapturePhotoResolution(photoFile:File){
        lifecycleScope.launch(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeFile(photoFile.absolutePath, options)

            Log.d(
                "CameraResolution",
                "Captured image size = ${options.outWidth} x ${options.outHeight}"
            )
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

//        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val cameraDir = File(externalCacheDir, "AiVastraCamera")
        if (!cameraDir.exists()) cameraDir.mkdirs()
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", cameraDir)
    }

    fun fixImageRotationFromUri(context: Context, imageUri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return imageUri

            val tempFile = File(context.cacheDir, "fixed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
            inputStream.close()

            val exif = ExifInterface(tempFile.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

//            val isFrontCamera = true
//            val isFrontCamera = isFrontCameraImage(tempFile.absolutePath)
            val isFrontCamera = if (!hasMultipleCameras(cameraProvider)) {
                true
            }else{
                if (lensFacing == CameraSelector.LENS_FACING_BACK){
                    false
                }else{
                    true
                }
            }

            if (rotationDegrees == 0 && !isFrontCamera) {
                return FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
            }

            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            val matrix = Matrix()
            if (rotationDegrees != 0) matrix.postRotate(rotationDegrees.toFloat())
            if (isFrontCamera) matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)

            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            FileOutputStream(tempFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            val newExif = ExifInterface(tempFile.absolutePath)
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            newExif.saveAttributes()

            // ✅ Return FileProvider URI
            FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)

        } catch (e: Exception) {
            e.printStackTrace()
            imageUri // fallback
        }
    }

    fun fixImageRotationFromUriNew(context: Context, imageUri: Uri): Uri {

        // 1️⃣ Copy Uri → temp file (required for EXIF + stable decode)
        val input = context.contentResolver.openInputStream(imageUri)!!
        val tempFile = File(context.cacheDir, "rot_${System.currentTimeMillis()}.jpg")

        FileOutputStream(tempFile).use { out ->
            input.copyTo(out)
        }

        val isFrontCamera = if (!hasMultipleCameras(cameraProvider)) {
            true
        }else{
            if (lensFacing == CameraSelector.LENS_FACING_BACK){
                false
            }else{
                true
            }
        }

        // 2️⃣ Decode bitmap (SAFE)
        var bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)

        // 3️⃣ Process rotation (UNIVERSAL)
        bitmap = UniversalRotationManager.processBitmap(
            context = context,
            bitmap = bitmap,
            imageFile = tempFile
        )

        // 🪞 Un-mirror
        bitmap = unMirrorIfFrontCamera(bitmap, isFrontCamera)

        // 4️⃣ Overwrite file with rotated bitmap
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        // 5️⃣ Reset EXIF orientation
        try {
            val exif = ExifInterface(tempFile.absolutePath)
            exif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )
            exif.saveAttributes()
        } catch (_: Exception) {}

        // 6️⃣ Return new Uri
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )
    }

    private fun unMirrorIfFrontCamera(
        bitmap: Bitmap,
        isFrontCamera: Boolean
    ): Bitmap {
        if (!isFrontCamera) return bitmap

        val matrix = Matrix().apply {
            postScale(-1f, 1f) // horizontal flip
            postTranslate(bitmap.width.toFloat(), 0f)
        }

        return Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )
    }

    private fun startUCropImage(uri: Uri) {
        try {
            val uniqueFileName = "cropped_${UUID.randomUUID()}.jpg"
            val destinationUri = Uri.fromFile(File(cacheDir, uniqueFileName))

            val intent = UCrop.of(uri, destinationUri)
                .useSourceImageAspectRatio()
                .withOptions(getUCropOptions())
                .getIntent(this)

            uCropActivityResult.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getUCropOptions(): UCrop.Options {
        return UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(95)
        }
    }

    private val uCropActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val uri = UCrop.getOutput(result.data!!)
                    if (uri != null) {
                        val file = File(FileUtils.getPath(this, uri))
                        gotoNextScreen(file.absolutePath, AppConstant.ISFROM_CAMERA_PHOTO)
                    } else {
                        ViewControll.showMessage(this, "Capture failed. Please try again.")
                    }
                } else if (result.resultCode == UCrop.RESULT_ERROR) {
                    val cropError = UCrop.getError(result.data!!)
                    ViewControll.showMessage(this, "Crop failed: ${cropError?.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ViewControll.showMessage(this, "Capture failed. Please try again.")
            }
        }

    private fun gotoNextScreen(capturePhotoFilePath:String,isFrom:String){
        val intent = Intent(this@Camera2CaptureActivity, UploadPhotoActivity::class.java)
        intent.putExtra(AppConstant.CAPTURED_PHOTO,capturePhotoFilePath)
        intent.putExtra(AppConstant.VASTRA_FOR,vastraFor)
        intent.putExtra(AppConstant.IS_FROM,isFrom)
        intent.putExtra(AppConstant.SELECTED_VASTRA_SUBCAT,selectedVastraSubCategory)
        intent.putExtra(AppConstant.SELECTED_VASTRA_ITEM,selectedVastraItem)
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.fade_and_scale_in, R.anim.fade_and_scale_out)
    }


    private fun startCountdown() {
        val autoTimerSeconds = getTimerSeconds()
        if(autoTimerSeconds==0){
            return
        }
        if(countdownRunnable!=null){
            stopCountdown()
        }
        binding.countdownText.visibility = View.VISIBLE
        binding.capturText.isVisible = true
        var timeLeft = autoTimerSeconds
        handler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                if (timeLeft > 0) {
                    binding.countdownText.text = timeLeft.toString()
                    binding.countdownText.animate()
                        .scaleX(1.5f).scaleY(1.5f)
                        .setDuration(500)
                        .withEndAction {
                            binding.countdownText.animate().scaleX(1f).scaleY(1f).duration = 500
                        }
                        .start()
                    timeLeft--
                    handler?.postDelayed(this, 1000)
                } else {
                    stopCountdown()
                    takePhoto() // 🔥 Auto Capture Photo when countdown finishes
                }
            }
        }
        handler?.post(countdownRunnable as Runnable)
    }

    // 🚀 Call this when the button is clicked to stop the countdown
    private fun stopCountdown() {
        countdownRunnable?.let { handler?.removeCallbacks(it) }
        binding.countdownText.visibility = View.GONE
        binding.capturText.isVisible = false
        countdownRunnable = null
        handler = null
    }

    private fun getTimerSeconds(): Int {
        val timerValue = PrefsManager.getString(AppConstant.CAPTURE_TIMER,"10 sec")
        return when {
            timerValue.isNullOrBlank() -> 0
            timerValue.equals("None", true) -> 0
            else -> timerValue.substringBefore(" ").toIntOrNull() ?: 0
        }
    }

    private fun shutdownCamera() {
        stopCountdown()
        try {
            if (::cameraProvider.isInitialized) {
                cameraProvider.unbindAll()
            }
            imageCapture = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::cameraProvider.isInitialized) {
            requestCameraPermissions(false)
        }else{
            requestCameraPermissions(true)
        }
    }

    override fun onPause() {
        super.onPause()
        shutdownCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdownCamera()
    }

    private fun reportCameraFailure(
        stage: String,
        throwable: Throwable? = null
    ) {
        LoaderManager.remove(this)
        val message = buildString {
            append("Stage: $stage\n")

            throwable?.let {
                append("Exception: ${it::class.java.simpleName}\n")
                append("Message: ${it.message}\n")
                append("Cause: ${it.cause?.message}\n")
            }

            append("SDK: ${Build.VERSION.SDK_INT}\n")
            append("Kiosk(DeviceOwner): ${isDeviceOwner()}\n")
            append("Permission: ${isCameraPermissionGranted()}\n")
            append("LensFacing: $lensFacing\n")
        }

        Log.e("CAMERA_EXACT_ERROR", message, throwable)

        showSnackErrorMsg(message)
    }

    fun showSnackErrorMsg(erroMsg:String){
        if (isFinishing || isDestroyed) return

        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            erroMsg,
            Snackbar.LENGTH_INDEFINITE
        )
        val textView =
            snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.maxLines = Int.MAX_VALUE
        textView.ellipsize = null
        textView.setTextColor(Color.BLACK)

        snackbar.setBackgroundTint(Color.LTGRAY)
        snackbar.setActionTextColor(Color.BLACK)

        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()
    }

    private fun isDeviceOwner(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(packageName)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}