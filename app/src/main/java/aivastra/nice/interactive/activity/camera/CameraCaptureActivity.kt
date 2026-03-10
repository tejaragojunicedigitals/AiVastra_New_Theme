package aivastra.nice.interactive.activity.camera

import aivastra.nice.interactive.Loader.LoaderManager
import android.os.Bundle
import aivastra.nice.interactive.R
import aivastra.nice.interactive.activity.launch.BaseActivity
import aivastra.nice.interactive.databinding.ActivityCameraCaptureBinding
import aivastra.nice.interactive.gpufilters.BeautyFilter
import aivastra.nice.interactive.gpufilters.ContrastFilter
import aivastra.nice.interactive.gpufilters.CurveCraftFilter
import aivastra.nice.interactive.gpufilters.DreamBeautyFilter
import aivastra.nice.interactive.gpufilters.DreamGlowFilter
import aivastra.nice.interactive.gpufilters.EdgeDefineFilter
import aivastra.nice.interactive.gpufilters.ExposureFilter
import aivastra.nice.interactive.gpufilters.FocusHaloFilter
import aivastra.nice.interactive.gpufilters.GlowEnhanceFilter
import aivastra.nice.interactive.gpufilters.HDRContrastBoostFilter
import aivastra.nice.interactive.gpufilters.HDRToneFilter
import aivastra.nice.interactive.gpufilters.HueFilter
import aivastra.nice.interactive.gpufilters.LevelsFilter
import aivastra.nice.interactive.gpufilters.LightSculptFilter
import aivastra.nice.interactive.gpufilters.LumaGlowFilter
import aivastra.nice.interactive.gpufilters.SaturationFilter
import aivastra.nice.interactive.gpufilters.SilkSkinFilter
import aivastra.nice.interactive.gpufilters.SoftFocusFilter
import aivastra.nice.interactive.gpufilters.SoftSkinFilter
import aivastra.nice.interactive.gpufilters.ToneTunerFilter
import aivastra.nice.interactive.gpufilters.VelvetFilter
import aivastra.nice.interactive.gpufilters.WarmBloomFilter
import aivastra.nice.interactive.utils.AppConstant
import aivastra.nice.interactive.utils.PrefsManager
import aivastra.nice.interactive.utils.ViewControll
import aivastra.nice.interactive.viewmodel.Dress.DressesTypeDataModel
import android.Manifest
import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.controls.Audio
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Hdr
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.controls.PictureFormat
import com.otaliastudios.cameraview.controls.Preview
import com.otaliastudios.cameraview.filter.Filter
import com.otaliastudios.cameraview.filter.Filters
import com.otaliastudios.cameraview.filter.MultiFilter
import com.otaliastudios.cameraview.filter.NoFilter
import com.otaliastudios.cameraview.filters.BrightnessFilter
import com.otaliastudios.cameraview.filters.FillLightFilter
import com.otaliastudios.cameraview.filters.VignetteFilter
import com.otaliastudios.cameraview.gesture.Gesture
import com.otaliastudios.cameraview.gesture.GestureAction
import com.otaliastudios.cameraview.size.Size
import com.otaliastudios.cameraview.size.SizeSelector
import com.yalantis.ucrop.UCrop
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
import java.util.logging.Level

class CameraCaptureActivity : BaseActivity() {

    private lateinit var binding:ActivityCameraCaptureBinding
    private var vastraFor: String = "women"
    private var selectedVastraSubCategory : DressesTypeDataModel.Data.Subcategory? = null
    private var selectedVastraItem : DressesTypeDataModel.Data.Subcategory.Item? = null
    private val cameraPermissions = arrayOf(
        Manifest.permission.CAMERA
    )
    // Only request WRITE_EXTERNAL_STORAGE for Android 9 and below
    private val legacyStoragePermissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private var filters: List<Filter> = listOf(NoFilter(),FillLightFilter(),VignetteFilter())
    private var cameraViewFilterNames = listOf("None","Fill Light", "Vignette")
    private var filterIndex = 0
    private var startX = 0f
    private var handler: Handler? = null
    private var countdownRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraCaptureBinding.inflate(layoutInflater)
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
        binding.camera.apply {
            setLifecycleOwner(this@CameraCaptureActivity)
            mode = Mode.PICTURE
            audio = Audio.OFF
            useDeviceOrientation = false
            facing = Facing.BACK
            pictureFormat = PictureFormat.JPEG
            hdr = Hdr.ON
            flash = Flash.ON
            preview = Preview.TEXTURE
            rotation = 180f
//            mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS)
        }
        addFocusRing()
//        applyFilterOnCameraViewListener()
//        if(PrefsManager.getBooleanTrue(AppConstant.IS_FIRST_TIME)){
//            showSwipeHint()
//        }
        // 2) request a large picture size (choose biggest available)
        binding.camera.setPictureSize(object : SizeSelector {
            override fun select(source: MutableList<Size>): MutableList<Size> {
                // sort by area desc and return the first (largest)
                source.sortWith(Comparator { a, b ->
                    val areaA = a.width.toLong() * a.height
                    val areaB = b.width.toLong() * b.height
                    (areaB - areaA).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
                })
                // return the list as-is; CameraView will pick the first supported
                return source
            }
        })
        // 3) make sure GL preview is used (glSurface) — filters require it. Usually set via XML,
        // but you can double-check with controls (not required here).
        // 4) add camera listener to receive picture result
        binding.camera.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: com.otaliastudios.cameraview.PictureResult) {
                // Save to a file (background handled by toFile callback)
                val file = createImageFile()?: return
                result.toFile(file) { savedFile ->
                    // savedFile is the File containing the JPEG with rotation fixed, filter applied.
                    val photoURI: Uri? = savedFile?.let {
                        FileProvider.getUriForFile(
                            this@CameraCaptureActivity,
                            "${packageName}.provider",
                            it
                        )
                    }
                    photoURI?.let { path ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val rotationFixedUri = fixImageRotationFromUri(this@CameraCaptureActivity,path)
                            withContext(Dispatchers.Main) {
                                if (rotationFixedUri != null) {
                                    startUCropImage(rotationFixedUri)
                                }else{
                                    startUCropImage(path)
                                }
                            }
                        }
                    }
                }
            }
        })
        // Capture button
        binding.buttonCapture.setOnClickListener {
            // This triggers the high-quality capture (respecting picture-size selector).
            stopCountdown()
            binding.camera.takePicture()
        }
        binding.llToolbar.imgBack.setOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnFlipCamera.setOnClickListener {
            binding.camera.toggleFacing()
        }
        binding.swipeAnimation.llMainRoot.setOnClickListener {
            binding.swipeAnimation.llMainRoot.isVisible = false
        }
        addGpuFilters()
//        addFilterProgressChangeListener()
    }

    private fun requestCameraPermissions(): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9 or lower
            val allGranted = legacyStoragePermissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            } && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

            if (!allGranted) {
                permissionLauncher.launch(legacyStoragePermissions + cameraPermissions)
                return false
            }
        }else{
            // Android 10+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(cameraPermissions)
                return false
            }
        }
        // If permissions granted -> open camera
        return true
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if(result.values.all { it }) {
            binding.camera.open()
            startCountdown()
        }else{
            ViewControll.showMessage(this,"Permission required to use camera")
        }
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
            setCompressionQuality(100)
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
            }catch (e: Exception) {
                e.printStackTrace()
                ViewControll.showMessage(this, "Capture failed. Please try again.")
            }
        }

    fun fixImageRotationFromUri(context: Context, imageUri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return imageUri

            val tempFile = File(context.cacheDir, "fixed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
            inputStream.close()

            val exif = ExifInterface(tempFile.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)

            var rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotationDegrees == 0 && bitmap.width > bitmap.height) {
                rotationDegrees = -90
                Log.e("Camera2", "Image is sideways, fixing rotation: $rotationDegrees")
            }

            val isFrontCamera = if(binding.camera.facing==Facing.FRONT){
                true
            }else{
                false
            }

            //for kiosk
//            val isFrontCamera = true

            //for mobile/tab
//            val isFrontCamera = isFrontCameraImage(tempFile.absolutePath)

            if (rotationDegrees == 0 && !isFrontCamera) {
                return FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
            }

            val matrix = Matrix()
            if (rotationDegrees != 0) matrix.postRotate(rotationDegrees.toFloat())
            if (isFrontCamera) matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)

            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            FileOutputStream(tempFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
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

    private fun isFrontCameraImage(imagePath: String): Boolean {
        return try {
            val exif = ExifInterface(imagePath)

            // Some devices store front camera as orientation 270 or 90
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

            // Check camera lens information (some devices include it)
            val lensMake = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
            val lensModel = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""

            Log.e("Camera2", "Orientation: $orientation, LensMake: $lensMake, LensModel: $lensModel")

            // If orientation is 270 or 90, likely a front camera (some devices)
            return orientation == ExifInterface.ORIENTATION_ROTATE_270 || orientation == ExifInterface.ORIENTATION_ROTATE_90

        }catch (e: Exception) {
            e.printStackTrace()
            false // Assume back camera if unknown
        }
    }

    override fun onStart() {
        super.onStart()
        // check permissions
        if(requestCameraPermissions()){
            binding.camera.open()
            startCountdown()
        }
    }

    override fun onStop() {
        binding.camera.close()
        super.onStop()
    }

    private fun addFocusRing(){
        binding.camera.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                showFocusAnimation(event.x, event.y)
                binding.camera.startAutoFocus(event.x,event.y)
            }
            return@setOnTouchListener false
        }
    }

    private fun showSwipeHint() {
        PrefsManager.putBoolean(AppConstant.IS_FIRST_TIME,false)
        binding.swipeAnimation.llMainRoot.isVisible = true
        val lottieDrawable = LottieDrawable()
        val composition = LottieCompositionFactory.fromRawResSync(
            this,
            R.raw.swipe_hand_animation
        ).value
        lottieDrawable.composition = composition
        lottieDrawable.repeatMode = LottieDrawable.REVERSE
        lottieDrawable.repeatCount = 10
        binding.swipeAnimation.lottieView.setImageDrawable(lottieDrawable)
        lottieDrawable.playAnimation()
        lottieDrawable.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                // Hide the view after animation completes
                binding.swipeAnimation.llMainRoot.visibility = View.GONE
            }

            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    private fun applyFilterOnCameraViewListener(){
        binding.camera.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startX = event.x
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.x - startX

                    if (deltaX > 120) {
                        applyPreviousFilter()
                    } else if (deltaX < -120) {
                        applyNextFilter()
                    } else {
                        // Clamp the coordinates inside view bounds
                        val focusX = event.x.coerceIn(0f, v.width.toFloat())
                        val focusY = event.y.coerceIn(0f, v.height.toFloat())
                        showFocusAnimation(focusX, focusY)
                        binding.camera.startAutoFocus(focusX, focusY)
                    }
                }
            }
            true
        }
    }

    private fun getCurrentFilterName(): String {
        /*  return Filters.values()[filterIndex].name
              .replace("_", " ")       // replace underscores
              .lowercase()             // make lowercase
              .replaceFirstChar { it.uppercase() }  // capitalize first letter*/
        if (filterIndex in cameraViewFilterNames.indices) {
            return cameraViewFilterNames[filterIndex]
        }
        return "Unknown"
    }

    fun getCurrentFilter(): Filter? {
        if (filterIndex in filters.indices) {
            return filters[filterIndex]
        }
        return null
    }

    private fun addFilterProgressChangeListener(){
        binding.verticalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // update filter / effect here
                val currentFilter = getCurrentFilter()
                if(currentFilter is ContrastFilter){
                    currentFilter.setContrast(progress.toFloat())
                }else if(currentFilter is HueFilter){
                    currentFilter.setHue(progress.toFloat())
                }else if(currentFilter is ExposureFilter){
                    currentFilter.setExposure(progress.toFloat())
                }else if(currentFilter is LevelsFilter){
                    currentFilter.setBlackLevel(0f)
                    currentFilter.setWhiteLevel(progress.toFloat())
                    currentFilter.setMidLevel(progress.toFloat())
                }else if(currentFilter is SaturationFilter){
                    currentFilter.setSaturation(progress.toFloat())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

    }

    private fun addGpuFilters(){
        val gpuFilterList:ArrayList<Filter> = arrayListOf()
        val gpuFilterNameList:ArrayList<String> = arrayListOf()

        val saturationFilter = SaturationFilter()
        saturationFilter.setSaturation(3f)
        gpuFilterList.add(saturationFilter)

        gpuFilterNameList.add(saturationFilter.getName())
        val contrastFilter = ContrastFilter()
        contrastFilter.setContrast(1.5f)
        gpuFilterList.add(contrastFilter)
        gpuFilterNameList.add(contrastFilter.getName())

        val levalsFilter = LevelsFilter()
        levalsFilter.setBlackLevel(0f)
        levalsFilter.setMidLevel(2f)
        levalsFilter.setWhiteLevel(2f)
        gpuFilterList.add(levalsFilter)
        gpuFilterNameList.add(levalsFilter.getName())

        val exposureFilter = ExposureFilter()
        exposureFilter.setExposure(0.2f)
        gpuFilterList.add(exposureFilter)
        gpuFilterNameList.add(exposureFilter.getName())

        val beautyFilter = BeautyFilter()
        gpuFilterList.add(beautyFilter)
        gpuFilterNameList.add(beautyFilter.getName())

        val velvetFilter = VelvetFilter()
        gpuFilterList.add(velvetFilter)
        gpuFilterNameList.add(velvetFilter.getName())

        val warmBloomFilter = WarmBloomFilter()
        gpuFilterList.add(warmBloomFilter)
        gpuFilterNameList.add(warmBloomFilter.getName())

        val dreamBeautyFilter = DreamBeautyFilter()
        gpuFilterList.add(dreamBeautyFilter)
        gpuFilterNameList.add(dreamBeautyFilter.getName())

        val softSkinFilter = SoftSkinFilter()
        gpuFilterList.add(softSkinFilter)
        gpuFilterNameList.add(softSkinFilter.getName())

        val silkSkinFilter = SilkSkinFilter()
        gpuFilterList.add(silkSkinFilter)
        gpuFilterNameList.add(silkSkinFilter.getName())

        val lumaGlowFilter = LumaGlowFilter()
        gpuFilterList.add(lumaGlowFilter)
        gpuFilterNameList.add(lumaGlowFilter.getName())

        val toneTunerFilter = ToneTunerFilter()
        gpuFilterList.add(toneTunerFilter)
        gpuFilterNameList.add(toneTunerFilter.getName())

        val lightSculptFilter = LightSculptFilter()
        gpuFilterList.add(lightSculptFilter)
        gpuFilterNameList.add(lightSculptFilter.getName())

        val focusHaloFilter = FocusHaloFilter()
        gpuFilterList.add(focusHaloFilter)
        gpuFilterNameList.add(focusHaloFilter.getName())

        val edgeDefineFilter = EdgeDefineFilter()
        gpuFilterList.add(edgeDefineFilter)
        gpuFilterNameList.add(edgeDefineFilter.getName())

        val curveCraftFilter = CurveCraftFilter()
        gpuFilterList.add(curveCraftFilter)
        gpuFilterNameList.add(curveCraftFilter.getName())

        val hdrToneFilter = HDRToneFilter()
        gpuFilterList.add(hdrToneFilter)
        gpuFilterNameList.add(hdrToneFilter.getName())

        val softFocusFilter = SoftFocusFilter()
        gpuFilterList.add(softFocusFilter)
        gpuFilterNameList.add(softFocusFilter.getName())

        val glowEnhanceFilter = GlowEnhanceFilter()
        gpuFilterList.add(glowEnhanceFilter)
        gpuFilterNameList.add(glowEnhanceFilter.getName())

        val dreamGlowFilter = DreamGlowFilter()
        gpuFilterList.add(dreamGlowFilter)
        gpuFilterNameList.add(dreamGlowFilter.getName())

        val filterPipeline = listOf(
            GlowEnhanceFilter(),
            SoftFocusFilter(),
            SoftSkinFilter(),
            LightSculptFilter()
        )
        gpuFilterList.add(MultiFilter(filterPipeline))
        gpuFilterNameList.add("Crystal4K")

        filters = filters+gpuFilterList
        cameraViewFilterNames = cameraViewFilterNames+gpuFilterNameList
    }

    private fun applyNextFilter() {
        filterIndex = (filterIndex + 1) % filters.size
        binding.camera.filter = filters[filterIndex]
        showFilterName()
    }

    private fun applyPreviousFilter() {
        filterIndex = (filterIndex - 1 + filters.size) % filters.size
        binding.camera.filter = filters[filterIndex]
        showFilterName()
    }

    private fun showFilterName(){
        binding.txtFilterName.isVisible = true
        val filterName = getCurrentFilterName()
        binding.txtFilterName.text = filterName
        // Cancel previously scheduled hide
        binding.txtFilterName.removeCallbacks(hideFilterRunnable)
        // Schedule new hide
        binding.txtFilterName.postDelayed(hideFilterRunnable, 3000) // 3 sec
    }

    private val hideFilterRunnable = Runnable {
        binding.txtFilterName.isVisible = false
    }

    private fun showFocusAnimation(touchX: Float, touchY: Float) {
        val ring = binding.focusRing

        val x = touchX - ring.width / 2
        val y = touchY - ring.height / 2

        ring.apply {
            this.x = x
            this.y = y

            visibility = View.VISIBLE
            scaleX = 1.4f
            scaleY = 1.4f
            alpha = 1f

            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(120)
                .withEndAction {
                    animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction { visibility = View.GONE }
                        .start()
                }
                .start()
        }
    }

    private fun createImageFileInDCIM(): File {
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), getString(R.string.app_name))
        if (!folder.exists()){
            folder.mkdirs()
        }
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        return File(folder, fileName)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

//        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val cameraDir = File(externalCacheDir, "AiVastraCamera")
        if (!cameraDir.exists()) cameraDir.mkdirs()
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", cameraDir)
    }

    private fun gotoNextScreen(capturePhotoFilePath:String,isFrom:String){
        val intent = Intent(this@CameraCaptureActivity, UploadPhotoActivity::class.java)
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
                    binding.camera.takePicture() // 🔥 Auto Capture Photo when countdown finishes
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

    override fun onResume() {
        super.onResume()
        if (binding.camera.isActivated && binding.camera.isOpened) {
            startCountdown()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCountdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdown()
        binding.camera.close()
    }
}