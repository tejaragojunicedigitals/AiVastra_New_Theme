package aivastra.nice.interactive.viewmodel.category

import aivastra.nice.interactive.utils.PrefsManager
import aivastra.nice.interactive.utils.ViewControll
import aivastra.nice.interactive.viewmodel.Dress.CommonResponseModel
import aivastra.nice.interactive.viewmodel.Dress.DressTryOnResultModel
import aivastra.nice.interactive.viewmodel.Dress.DressesForDataModel
import aivastra.nice.interactive.viewmodel.Dress.DressesTypeDataModel
import aivastra.nice.interactive.viewmodel.Dress.ProductSearchDataModel
import aivastra.nice.interactive.viewmodel.Dress.UsetTryOnResultDataModel
import aivastra.nice.interactive.viewmodel.Login.UserLoginDataModel
import aivastra.nice.interactive.viewmodel.Qrcode.QrCodeLinkDataModel
import aivastra.nice.interactive.viewmodel.others.UploadImageModel
import android.app.Activity
import android.graphics.Color
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facewixlatest.ApiUtils.APICaller
import com.example.facewixlatest.ApiUtils.APIConstant
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody.Part.Companion.createFormData
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlin.random.Random

class SareecategoryDataViewModel () : ViewModel() {

    private val repository = SareeCategoryDataRepository
    private val _allSareeCategory = MutableLiveData<ArrayList<SareeCateDataModel.Data>>()
    val allSareeCategoryList: LiveData<ArrayList<SareeCateDataModel.Data>> get() = _allSareeCategory

    private val _dressesForData = MutableLiveData<ArrayList<DressesForDataModel.Data>>()
    val dressesForData: LiveData<ArrayList<DressesForDataModel.Data>> get() = _dressesForData

    private val _showTryOnSessionMsg = MutableLiveData<String>()
    val showTryOnSessionMessage: LiveData<String> get() = _showTryOnSessionMsg

    private val _tryOnResultDataList = MutableLiveData<ArrayList<UsetTryOnResultDataModel.Data>>()
    val allTryOnResultDataList: LiveData<ArrayList<UsetTryOnResultDataModel.Data>> get() = _tryOnResultDataList

    private val _dressesTypeData = MutableLiveData<ArrayList<DressesTypeDataModel.Data>>()
    val dressesTypeData: LiveData<ArrayList<DressesTypeDataModel.Data>> get() = _dressesTypeData

    private val _dressesItemsListData = MutableLiveData<ArrayList<DressesTypeDataModel.Data.Subcategory.Item>>()
    val dressesItemsListData: LiveData<ArrayList<DressesTypeDataModel.Data.Subcategory.Item>> get() = _dressesItemsListData

    private val _selectedCatItem = MutableLiveData<SareeCateDataModel.Data>()
    val selectedCatItem: LiveData<SareeCateDataModel.Data> get() = _selectedCatItem

    private val _selectedDressType = MutableLiveData<DressesTypeDataModel.Data>()
    val selectedDressType: LiveData<DressesTypeDataModel.Data> get() = _selectedDressType

    private val _dressTryOnResultData = MutableLiveData<DressTryOnResultModel?>()
    val dressTryOnResultData: LiveData<DressTryOnResultModel?> get() = _dressTryOnResultData

    private val _qrCodeLinkData = MutableLiveData<QrCodeLinkDataModel?>()
    val qrCodeLinkData: LiveData<QrCodeLinkDataModel?> get() = _qrCodeLinkData

    private val _userLoginData = MutableLiveData<UserLoginDataModel>()
    val userLoginInfo: LiveData<UserLoginDataModel> get() = _userLoginData

    private val _uploadUserImageData = MutableLiveData<UploadImageModel?>()
    val uploadUserImageData: LiveData<UploadImageModel?> get() = _uploadUserImageData

    private val _userOpenQrCodeLink = MutableLiveData<UploadImageModel?>()
    val userOpenQrCodeLink: LiveData<UploadImageModel?> get() = _userOpenQrCodeLink

    private val _closeDialogCallback = MutableLiveData<Boolean>()
    val closeDialogCallback: LiveData<Boolean> get() = _closeDialogCallback

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error
    private var pollingJob: Job? = null
    private var tryOnCount = 0
   /* private val _closeDialog = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )

    val closeDialog = _closeDialog.asSharedFlow()*/

    fun requestCloseDialog() {
        _closeDialogCallback.postValue(true)
    }

    fun resetDialog() {
        _closeDialogCallback.postValue(false)
    }

    fun fetchUserAllTryOnResultListAPI(tryOnResultId: String) {
        viewModelScope.launch {
            try {
                repository.getUserAllVastraTryOnResultAPI(tryOnResultId,object : APICaller.APICallBack{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as UsetTryOnResultDataModel
                        _tryOnResultDataList.value = getModelClass.data
                        return null
                    }

                    override fun onFailure() {
                        _error.postValue("Error: ${APIConstant.errorSomethingWrong}")
                    }
                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun fetchDressesTypeData(cType:String) {
        viewModelScope.launch {
            try {
                repository.getDressesTypeAPI(cType,object : APICaller.APICallBack{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as DressesTypeDataModel
                        if (getModelClass.status==false) {
                            _error.postValue(getModelClass.message)
                            return null
                        }
                        _dressesTypeData.value = getModelClass.data
                        SareeCategoryDataRepository.savDressesTypeData(getModelClass.data)
                        return null
                    }

                    override fun onFailure() {
                        _error.postValue("Error: ${APIConstant.errorSomethingWrong}")
                    }
                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun filterProductBySKUNumber(searchBy:String) {
        viewModelScope.launch {
            try {
                val hashMapData : HashMap<String?, RequestBody?> = HashMap<String?, RequestBody?> ()
                hashMapData.put(APIConstant.Parameter.USER_ID,ViewControll.convertStringToRequestBody(PrefsManager.loginUserInfo.user.addedOn))
                hashMapData.put(APIConstant.Parameter.SKU_NUMBER, ViewControll.convertStringToRequestBody(searchBy))
                repository.filterProductBySKUNo(hashMapData,object : APICaller.APICallBackWithError{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as ProductSearchDataModel
                        if(getModelClass.status==false){
                            _error.postValue("No product found")
                            return null
                        }
                        _dressesItemsListData.value = getModelClass.data
                        return null
                    }

                    override fun onFailure(errorMsg: String) {
                        _error.postValue("Error: $errorMsg")
                    }
                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    // Call API to get all posts
    fun fetchDressesForAPI() {
        viewModelScope.launch {
            try {
                repository.getDressesForDataAPI(object : APICaller.APICallBack{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as DressesForDataModel
                        if (getModelClass.status==false) {
                            _error.postValue(getModelClass.message)
                            return null
                        }
                        _dressesForData.value = getModelClass.data
                        if(getModelClass.status==true){
                            _showTryOnSessionMsg.value = getModelClass.message
                        }
                        SareeCategoryDataRepository.savDressesForData(getModelClass.data)
                        SareeCategoryDataRepository.saveSessionMessage(getModelClass.message)
                        return null
                    }

                    override fun onFailure() {
                        _error.postValue("Error: ${APIConstant.errorSomethingWrong}")
                    }
                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun resetTryOnResultData() {
        _dressTryOnResultData.postValue(null)  // Clear the previous value before calling API
        _error.postValue(null)  // Clear the previous value before calling API
    }

    fun resetUploadImageData() {
        _uploadUserImageData.postValue(null)  // Clear the previous value before calling API
        _error.postValue(null)  // Clear the previous value before calling API
    }

    fun resetErrorData() {
        _error.postValue(null)  // Clear the previous value before calling API
    }

    fun resetSearchProductData() {
        _error.postValue(null)
        _dressesItemsListData.postValue(null)// Clear the previous value before calling API
    }

    fun resetAppLoginData() {
        _userLoginData.postValue(null)  // Clear the previous value before calling API
        _error.postValue(null)  // Clear the previous value before calling API
    }

    fun resetQrCodeLinkData() {
        _qrCodeLinkData.postValue(null)  // Clear the previous value before calling API
        _error.postValue(null)  // Clear the previous value before calling API
    }

    fun fetchDressTryOnAPI(activity: Activity,garmentId:String,deviceId:String) {
        viewModelScope.launch {
            try {
                val imageId  = PrefsManager.getImageID(activity)
                val hashMapData : HashMap<String?, RequestBody?> = HashMap<String?, RequestBody?> ()
                hashMapData.put(APIConstant.Parameter.HUMAN_ID,ViewControll.convertStringToRequestBody(imageId))
                hashMapData.put(APIConstant.Parameter.WIX_USER, ViewControll.convertStringToRequestBody(deviceId))
                hashMapData.put(APIConstant.Parameter.GARMENT_ID,ViewControll.convertStringToRequestBody(garmentId))
                repository.uploadUserFaceSwapAPI(hashMapData,object : APICaller.APICallBackWithError{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as DressTryOnResultModel
                        if (getModelClass.status.equals("error")) {
//                            _error.postValue(APIConstant.fileNotSupported)
                            fetchVastraTryOnResultAPI(activity,getModelClass.promt_data.garment_id
                                ,deviceId,getModelClass.promt_data.promt_id,getModelClass.promt_data.userimage_id)
                            return null
                        }
                        if (getModelClass.tryon_image.isEmpty()) {
                            _error.postValue(APIConstant.fileNotSupported)
                            return null
                        }
                        _dressTryOnResultData.value = getModelClass
                        return null
                    }

                    override fun onFailure(errorMsg: String) {
                        showSnackErrorMsg(activity,errorMsg)
                        if(errorMsg.equals(APIConstant.serverTimeOut)){
                            _error.postValue(APIConstant.serverTimeOut)
                        }else{
                            _error.postValue(APIConstant.fileNotSupported)
                        }
                    }
                })
            } catch (e: Exception) {
                val errorMsg = "Unknown parse error ${e.stackTraceToString()}"
                showSnackErrorMsg(activity,errorMsg)
                _error.postValue(errorMsg)
            }
        }
    }

    fun fetchVastraTryOnResultAPI(activity: Activity,garmentId:String,deviceId:String,promtId:String,imageId:String) {
        viewModelScope.launch {
            try {
                tryOnCount++
//                val imageId  = PrefsManager.getImageID(activity)
                val hashMapData : HashMap<String?, RequestBody?> = HashMap<String?, RequestBody?> ()
                hashMapData.put(APIConstant.Parameter.USER_IMAGE_ID,ViewControll.convertStringToRequestBody(imageId))
                hashMapData.put(APIConstant.Parameter.GARMENT_ID,ViewControll.convertStringToRequestBody(garmentId))
                hashMapData.put(APIConstant.Parameter.PROMT_ID,ViewControll.convertStringToRequestBody(promtId))
                hashMapData.put(APIConstant.Parameter.WIX_USER, ViewControll.convertStringToRequestBody(deviceId))
                Log.e("PROMT_ID",promtId)
                repository.promtVastraResultTryOnAPI(hashMapData,object : APICaller.APICallBackWithError{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as DressTryOnResultModel
                        if (getModelClass.status.equals("error")) {
                            if(tryOnCount>5){
                                tryOnCount = 0
                                _error.postValue(APIConstant.serverTimeOut)
                            }else{
                                fetchVastraTryOnResultAPI(activity,garmentId,deviceId,promtId,imageId)
                            }
//                            _error.postValue(APIConstant.serverTimeOut)
                            return null
                        }
                        if (getModelClass.tryon_image.isEmpty()) {
                            _error.postValue(APIConstant.fileNotSupported)
                            return null
                        }
                        _dressTryOnResultData.value = getModelClass
                        return null
                    }

                    override fun onFailure(errorMsg: String) {
                       /* if(errorMsg.equals(APIConstant.serverTimeOut)){
                            _error.postValue(APIConstant.serverTimeOut)
                        }else{
                            _error.postValue(APIConstant.serverTimeOut)
                        }*/
                        showSnackErrorMsg(activity,errorMsg)
                        if(tryOnCount>5){
                            tryOnCount = 0
                            _error.postValue(APIConstant.serverTimeOut)
                        }else{
                            fetchVastraTryOnResultAPI(activity,garmentId,deviceId,promtId,imageId)
                        }
                    }
                })
            }catch (e: Exception) {
                val errorMsg = "Unknown parse error ${e.stackTraceToString()}"
                showSnackErrorMsg(activity,errorMsg)
            }
        }
    }

    fun showSnackErrorMsg(activity: Activity,erroMsg:String){
      /*  if (activity.isFinishing || activity.isDestroyed) return

        val snackbar = Snackbar.make(
            activity.findViewById(android.R.id.content),
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
        snackbar.show()*/
    }

    fun getQrCodeLinkAPI(activity: Activity) {
        viewModelScope.launch {
            try {
                val hashMapData : HashMap<String?, RequestBody?> = HashMap<String?, RequestBody?> ()
                val deviceId = Settings.Secure.getString(activity.contentResolver, Settings.Secure.ANDROID_ID)
                val utcTime = (System.currentTimeMillis()/1000).toString()
                val randomNumber = (1..5).joinToString("") { Random.nextInt(0, 10).toString() }
                val securityCode = "aivastra_$randomNumber"
                hashMapData.put(APIConstant.Parameter.MERCHANT_ID,ViewControll.convertStringToRequestBody(PrefsManager.loginUserInfo.user.addedOn))
                hashMapData.put(APIConstant.Parameter.WIX_USER, ViewControll.convertStringToRequestBody(deviceId))
                hashMapData.put(APIConstant.Parameter.UTC_TIME, ViewControll.convertStringToRequestBody(utcTime))
                hashMapData.put(APIConstant.Parameter.SECURITY_CODE,ViewControll.convertStringToRequestBody(securityCode))
                repository.getQrCodeLinkAPI(hashMapData,object : APICaller.APICallBackWithError{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as QrCodeLinkDataModel
                        if (getModelClass.status==false) {
                            _error.postValue(getModelClass.message)
                            return null
                        }
                        _qrCodeLinkData.value = getModelClass
                        return null
                    }

                    override fun onFailure(errorMsg: String) {
                        _error.postValue(APIConstant.errorSomethingWrong)
                    }

                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

     fun cancelQrScanPhotoFetchApiJob(){
        pollingJob?.cancel()
    }

    fun startCheckOfUserImageUpload(securityCode: String) {
        // Cancel any existing polling before starting a new one
        pollingJob?.cancel()

        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val result = suspendCancellableCoroutine<UploadImageModel> { continuation ->
                        checkUserUploadImageAPI(securityCode) { uploadImageModel ->
                            if (continuation.isActive) {
                                continuation.resume(uploadImageModel) {}
                            }
                        }
                    }

                    _userOpenQrCodeLink.postValue(result)

                    if(result.status == true) {
                        _uploadUserImageData.postValue(result)
                        cancel()
                    }else{
                        if(result.is_session_expired){
                            cancel()
                        }else{
                            delay(10_000)
                        }
                    }
                }catch (e: Exception) {
                    e.printStackTrace()
                    delay(10_000)
                }
            }
        }
    }

    fun checkUserUploadImageAPI(securityCode:String,apiResponseCallback:(UploadImageModel)->Unit) {
        viewModelScope.launch {
            try {
                repository.checkUserUploadImgViaQrCode(securityCode,object : APICaller.APICallBack{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as UploadImageModel
                        apiResponseCallback(getModelClass)
                        return null
                    }

                    override fun onFailure() {
                        _error.postValue(APIConstant.errorSomethingWrong)
                    }
                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun likeVastraTryOnResultAPI(resultId:String,likeStatus:String) {
        viewModelScope.launch {
            try {
                val hashMapData : HashMap<String?, RequestBody?> = HashMap<String?, RequestBody?> ()
                hashMapData.put(APIConstant.Parameter.RESULT_ID,ViewControll.convertStringToRequestBody(resultId))
                hashMapData.put(APIConstant.Parameter.USER_ID,ViewControll.convertStringToRequestBody(PrefsManager.loginUserInfo.user.id))
                hashMapData.put(APIConstant.Parameter.STATUS,ViewControll.convertStringToRequestBody(likeStatus))
                repository.tryOnResultLikeAPI(hashMapData,object : APICaller.APICallBackWithError{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as CommonResponseModel
                        return null
                    }

                    override fun onFailure(errorMsg: String) {
                        _error.postValue(APIConstant.errorSomethingWrong)
                    }
                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun addToCartVastraTryOnResultAPI(resultId:String,cardStatus:String) {
        viewModelScope.launch {
            try {
                val hashMapData : HashMap<String?, RequestBody?> = HashMap<String?, RequestBody?> ()
                hashMapData.put(APIConstant.Parameter.RESULT_ID,ViewControll.convertStringToRequestBody(resultId))
                hashMapData.put(APIConstant.Parameter.USER_ID,ViewControll.convertStringToRequestBody(PrefsManager.loginUserInfo.user.id))
                hashMapData.put(APIConstant.Parameter.STATUS,ViewControll.convertStringToRequestBody(cardStatus))
                repository.tryOnResultAddToCartAPI(hashMapData,object : APICaller.APICallBackWithError{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as CommonResponseModel
                        return null
                    }

                    override fun onFailure(errorMsg: String) {
                        _error.postValue(APIConstant.errorSomethingWrong)
                    }
                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun deleteAllTryOnResultAPI(userImageId:String,deviceId:String,responseCallback:(Boolean,String)->Unit) {
        viewModelScope.launch {
            try {
                val hashMapData : HashMap<String?, RequestBody?> = HashMap<String?, RequestBody?> ()
                hashMapData.put(APIConstant.Parameter.DEVICE_ID,ViewControll.convertStringToRequestBody(deviceId))
                hashMapData.put(APIConstant.Parameter.USER_IMAGE_ID,ViewControll.convertStringToRequestBody(userImageId))
                repository.deleteAllTryOnResultAPI(hashMapData,object : APICaller.APICallBackWithError{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as CommonResponseModel
                        responseCallback.invoke(true,"Delete All TryOn Results Successfully")
                        return null
                    }
                    override fun onFailure(errorMsg: String) {
                        responseCallback.invoke(false,APIConstant.errorSomethingWrong)
                    }
                })
            } catch (e: Exception) {
                responseCallback.invoke(false,"Error: ${e.message}")
            }
        }
    }

    fun uploadCaptureImageAPI(activity: Activity,imgFile: File) {
        viewModelScope.launch {
            try {
                val requestBody = imgFile.asRequestBody("image".toMediaTypeOrNull())
                val imageMultiPart = requestBody.let {
                    createFormData("userfile", imgFile.name, it)
                }
                val deviceId = Settings.Secure.getString(activity.contentResolver, Settings.Secure.ANDROID_ID)
                val hashMapData : HashMap<String, RequestBody> = HashMap<String, RequestBody> ()
                hashMapData.put(APIConstant.Parameter.USER_ID,ViewControll.convertStringToRequestBody(deviceId))
                repository.uploadUserImageAPI(hashMapData,imageMultiPart,object : APICaller.APICallBack{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as UploadImageModel
                        if (getModelClass.status==false) {
                            _error.postValue(modelclass.message)
                            return null
                        }
                        _uploadUserImageData.value = getModelClass
                        PrefsManager.saveImageId(activity,getModelClass.id)
                        SareeCategoryDataRepository.saveUploadedImageData(getModelClass)
                        return null
                    }

                    override fun onFailure() {
                        _error.postValue("Error: ${APIConstant.errorSomethingWrong}")
                    }
                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }


    fun userAppLoginAPI(userName:String,password:String,deviceId:String) {
        viewModelScope.launch {
            try {
                val hashMapData : HashMap<String?, RequestBody?> = HashMap<String?, RequestBody?> ()
                hashMapData.put(APIConstant.Parameter.USERNAME,ViewControll.convertStringToRequestBody(userName))
                hashMapData.put(APIConstant.Parameter.PASSWORD, ViewControll.convertStringToRequestBody(password))
                hashMapData.put(APIConstant.Parameter.DEVICE_ID,ViewControll.convertStringToRequestBody(deviceId))
                hashMapData.put(APIConstant.Parameter.APP_TYPE,ViewControll.convertStringToRequestBody("tryon"))
                repository.userAppLoginAPI(hashMapData,object : APICaller.APICallBackWithError{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as UserLoginDataModel
                        if (getModelClass.status==false) {
                            _error.postValue(getModelClass.message)
                            return null
                        }
                        PrefsManager.saveLoginUserData(getModelClass)
                        userVerifyApi(deviceId)
                        return null
                    }

                    override fun onFailure(errorMsg: String) {
                        _error.postValue(APIConstant.errorSomethingWrong)
                    }

                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun userVerifyApi(deviceId:String) {
        viewModelScope.launch {
            try {
                val hashMapData : HashMap<String?, RequestBody?> = HashMap<String?, RequestBody?> ()
                hashMapData.put(APIConstant.Parameter.DEVICE_ID,ViewControll.convertStringToRequestBody(deviceId))
                repository.userVerifyAPI(hashMapData,object : APICaller.APICallBackWithError{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as UserLoginDataModel
                        if (getModelClass.status==false) {
                            _error.postValue(getModelClass.message)
                            return null
                        }
                        _userLoginData.value = getModelClass
                        return null
                    }

                    override fun onFailure(errorMsg: String) {
                        _error.postValue(APIConstant.errorSomethingWrong)
                    }

                })
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun userLogoutAPI(deviceId:String,onSuccessCallBack: (Boolean,String) -> Unit) {
        viewModelScope.launch {
            try {
                val hashMapData : HashMap<String?, RequestBody?> = HashMap<String?, RequestBody?> ()
                hashMapData.put(APIConstant.Parameter.DEVICE_ID,ViewControll.convertStringToRequestBody(deviceId))
                hashMapData.put(APIConstant.Parameter.APP_TYPE,ViewControll.convertStringToRequestBody("tryon"))
                repository.userLogoutAPI(hashMapData,object : APICaller.APICallBackWithError{
                    override fun <T> onSuccess(modelclass: T): Class<T>? {
                        val getModelClass = modelclass as CommonResponseModel
                        if (getModelClass.status==false) {
                            onSuccessCallBack(false,getModelClass.message)
                            return null
                        }
                        onSuccessCallBack(true,"")
                        return null
                    }
                    override fun onFailure(errorMsg: String) {
                        onSuccessCallBack(false,errorMsg)
                    }
                })
            } catch (e: Exception) {
                onSuccessCallBack(false,APIConstant.errorSomethingWrong)
            }
        }
    }

    fun getSelectedCatItem(){
        val selectedItem = SareeCategoryDataRepository.getSelectedSareeCatData()
        _selectedCatItem.postValue(selectedItem)
    }

    fun getSelectedDressType(){
        val selectedItem = SareeCategoryDataRepository.getSelectedDressTypeData()
        _selectedDressType.postValue(selectedItem)
    }

    fun getAllCatList(){
        val allCatList = SareeCategoryDataRepository.getAllSareeCatData()
        _allSareeCategory.postValue(allCatList)
    }

    fun getDressesForList(){
        val dressesForDataList = SareeCategoryDataRepository.getDressesForData()
        _dressesForData.postValue(dressesForDataList)
    }

    fun getSessionMessage(){
        val sessionMsg = SareeCategoryDataRepository.getSessionMessage()
        _showTryOnSessionMsg.postValue(sessionMsg)
    }

    fun getDressesTypeList(){
        val dressesTypeDataList = SareeCategoryDataRepository.getDressesTypeData()
        _dressesTypeData.postValue(dressesTypeDataList)
    }

    fun setAllCatList(list: ArrayList<SareeCateDataModel.Data>) {
        _allSareeCategory.value = list
    }

    fun setSelectedCatItem(selectedItem: SareeCateDataModel.Data) {
        _selectedCatItem.value = selectedItem
    }
}