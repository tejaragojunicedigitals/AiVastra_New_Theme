package aivastra.nice.interactive.viewmodel.category

import aivastra.nice.interactive.utils.PrefsManager
import aivastra.nice.interactive.viewmodel.Dress.CommonResponseModel
import aivastra.nice.interactive.viewmodel.Dress.DressTryOnResultModel
import aivastra.nice.interactive.viewmodel.Dress.DressesForDataModel
import aivastra.nice.interactive.viewmodel.Dress.DressesItemsDataModel
import aivastra.nice.interactive.viewmodel.Dress.DressesTypeDataModel
import aivastra.nice.interactive.viewmodel.Dress.ProductSearchDataModel
import aivastra.nice.interactive.viewmodel.Dress.UsetTryOnResultDataModel
import aivastra.nice.interactive.viewmodel.Login.UserLoginDataModel
import aivastra.nice.interactive.viewmodel.Qrcode.QrCodeLinkDataModel
import aivastra.nice.interactive.viewmodel.others.UploadImageModel
import com.example.facewixlatest.ApiUtils.APICaller
import com.example.facewixlatest.ApiUtils.APIConstant
import okhttp3.MultipartBody
import okhttp3.RequestBody


object SareeCategoryDataRepository {

    private var sareeCatData: SareeCateDataModel.Data = SareeCateDataModel.Data()
    private var selectedDressType: DressesTypeDataModel.Data = DressesTypeDataModel.Data()
    private var uploadedImageData: UploadImageModel = UploadImageModel()
    private var allcategoryList: ArrayList<SareeCateDataModel.Data> = arrayListOf()
    private var dressesForDataList: ArrayList<DressesForDataModel.Data> = arrayListOf()
    private var dressesTypeDataList: ArrayList<DressesTypeDataModel.Data> = arrayListOf()
    var startAutoTryOnProcess = false
    var isFromCameraSetting = false
    private var tryOnSessionMessage:String = ""


    fun checkUserUploadImgViaQrCode(securityCode:String,apiCallBack: APICaller.APICallBack) {
        APICaller.getRequest(APIConstant.API_ENDPOINTS.UPLOAD_IMAGE_VIA_SCANNER+securityCode, getHeaderData(),
            UploadImageModel::class.java, apiCallBack)
    }

    fun getUserAllVastraTryOnResultAPI(tryOnResultId: String, apiCallBack: APICaller.APICallBack) {
        val userId = PrefsManager.loginUserInfo.user.deviceId
        APICaller.getRequest(APIConstant.API_ENDPOINTS.TRYON_RESULT_LIST+userId+"/$tryOnResultId", getHeaderData(),
            UsetTryOnResultDataModel::class.java, apiCallBack)
    }

    fun getDressesForDataAPI(apiCallBack: APICaller.APICallBack) {
        APICaller.getRequest(APIConstant.API_ENDPOINTS.GET_DRESSES_FOR, getHeaderData(),
            DressesForDataModel::class.java, apiCallBack)
    }

    fun getDressesTypeAPI(cType:String,apiCallBack: APICaller.APICallBack) {
        val marchantId = PrefsManager.loginUserInfo.user.addedOn
        APICaller.getRequest(APIConstant.API_ENDPOINTS.GET_DRESSES_TYPE+cType+"/$marchantId", getHeaderData(),
            DressesTypeDataModel::class.java, apiCallBack)
    }

    fun getDressesItemsDataAPI(dressTypeId:String,apiCallBack: APICaller.APICallBack) {
        APICaller.getRequest(APIConstant.API_ENDPOINTS.GET_DRESSES_ITEMS+dressTypeId, getHeaderData(),
            DressesItemsDataModel::class.java, apiCallBack)
    }

    fun filterProductBySKUNo(hashMapData: HashMap<String?, RequestBody?>, apiCallBack: APICaller.APICallBackWithError) {
        val headerData : HashMap<String?, String?> = HashMap<String?, String?>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        APICaller.postRequest(APIConstant.API_ENDPOINTS.FILTER_PRODUCT_LIST, hashMapData, headerData, ProductSearchDataModel::class.java, apiCallBack)
    }

    fun uploadUserFaceSwapAPI(hashMapData: HashMap<String?, RequestBody?>, apiCallBack: APICaller.APICallBackWithError) {
        val headerData : HashMap<String?, String?> = HashMap<String?, String?>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        APICaller.postRequestTryOnAPI(APIConstant.API_ENDPOINTS.SAREE_FACE_SWAP_API, hashMapData, headerData, DressTryOnResultModel::class.java, apiCallBack)
    }

    fun getQrCodeLinkAPI(hashMapData: HashMap<String?, RequestBody?>, apiCallBack: APICaller.APICallBackWithError) {
        val headerData : HashMap<String?, String?> = HashMap<String?, String?>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        APICaller.postRequest(APIConstant.API_ENDPOINTS.GET_QRCODE_LINK, hashMapData, headerData, QrCodeLinkDataModel::class.java, apiCallBack)
    }

    fun promtVastraResultTryOnAPI(hashMapData: HashMap<String?, RequestBody?>, apiCallBack: APICaller.APICallBackWithError) {
        val headerData : HashMap<String?, String?> = HashMap<String?, String?>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        APICaller.postRequestTryOnAPI(APIConstant.API_ENDPOINTS.VASTRA_TRY_ON_RESULT, hashMapData, headerData, DressTryOnResultModel::class.java, apiCallBack)
    }

    fun userAppLoginAPI(hashMapData: HashMap<String?, RequestBody?>, apiCallBack: APICaller.APICallBackWithError) {
        val headerData : HashMap<String?, String?> = HashMap<String?, String?>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        APICaller.postRequest(APIConstant.API_ENDPOINTS.APP_LOGIN, hashMapData, headerData, UserLoginDataModel::class.java, apiCallBack)
    }

    fun userLogoutAPI(hashMapData: HashMap<String?, RequestBody?>, apiCallBack: APICaller.APICallBackWithError) {
        val headerData : HashMap<String?, String?> = HashMap<String?, String?>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        APICaller.postRequest(APIConstant.API_ENDPOINTS.LOGOUT_USER, hashMapData, headerData, CommonResponseModel::class.java, apiCallBack)
    }

    fun userVerifyAPI(hashMapData: HashMap<String?, RequestBody?>, apiCallBack: APICaller.APICallBackWithError) {
        val headerData : HashMap<String?, String?> = HashMap<String?, String?>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        APICaller.postRequest(APIConstant.API_ENDPOINTS.VERIFY_USER, hashMapData, headerData, UserLoginDataModel::class.java, apiCallBack)
    }

    fun tryOnResultLikeAPI(hashMapData: HashMap<String?, RequestBody?>, apiCallBack: APICaller.APICallBackWithError) {
        val headerData : HashMap<String?, String?>
                = HashMap<String?, String?>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        APICaller.postRequest(APIConstant.API_ENDPOINTS.TRYON_RESULT_LIKE, hashMapData, headerData, CommonResponseModel::class.java, apiCallBack)
    }

    fun tryOnResultAddToCartAPI(hashMapData: HashMap<String?, RequestBody?>, apiCallBack: APICaller.APICallBackWithError) {
        val headerData : HashMap<String?, String?>
                = HashMap<String?, String?>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        APICaller.postRequest(APIConstant.API_ENDPOINTS.TRYON_RESULT_ADDTOCART, hashMapData, headerData, CommonResponseModel::class.java, apiCallBack)
    }

    fun deleteAllTryOnResultAPI(hashMapData: HashMap<String?, RequestBody?>, apiCallBack: APICaller.APICallBackWithError) {
        val headerData : HashMap<String?, String?>
                = HashMap<String?, String?>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        APICaller.postRequest(APIConstant.API_ENDPOINTS.DELETE_ALL_TRYON_RESULT, hashMapData, headerData, CommonResponseModel::class.java, apiCallBack)
    }

    fun uploadUserImageAPI(hashMapData: HashMap<String, RequestBody>, image : MultipartBody.Part, apiCallBack: APICaller.APICallBack) {
        val headerData : HashMap<String, String> = HashMap<String, String>()
        headerData.put(APIConstant.Parameter.AUTHORIZATION,APIConstant.KeyParameter.USER_HEADER_AUTHORIZATION_KEY)
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        APICaller.postMultipartRequest(APIConstant.API_ENDPOINTS.UPLOAD_USER_IMAGE, headerData, hashMapData,
            image, UploadImageModel::class.java, apiCallBack)
    }

    fun setSelectedSareeCatData(selectedCatData: SareeCateDataModel.Data){
        sareeCatData = selectedCatData
    }

    fun getSelectedSareeCatData(): SareeCateDataModel.Data {
        return sareeCatData
    }

    fun setSelectedDressTypeData(selectedType:DressesTypeDataModel.Data){
        selectedDressType = selectedType
    }

    fun getSelectedDressTypeData(): DressesTypeDataModel.Data {
        return selectedDressType
    }

    fun saveUploadedImageData(imageData:UploadImageModel){
        uploadedImageData = imageData
    }

    fun getUploadedImageData(): UploadImageModel {
        return uploadedImageData
    }

    fun saveAllSareeCatData(allCatDataList:ArrayList<SareeCateDataModel.Data>){
        allcategoryList = allCatDataList
    }

    fun savDressesForData(dressesForList:ArrayList<DressesForDataModel.Data>){
        dressesForDataList = dressesForList
    }

    fun getDressesForData(): ArrayList<DressesForDataModel.Data> {
        return dressesForDataList
    }

    fun saveSessionMessage(message:String){
        tryOnSessionMessage = message
    }

    fun getSessionMessage(): String {
        return tryOnSessionMessage
    }

    fun savDressesTypeData(dressesTypeList:ArrayList<DressesTypeDataModel.Data>){
        dressesTypeDataList = dressesTypeList
    }

    fun getDressesTypeData(): ArrayList<DressesTypeDataModel.Data> {
        return dressesTypeDataList
    }

    fun getAllSareeCatData(): ArrayList<SareeCateDataModel.Data> {
        return allcategoryList
    }

    fun getHeaderData(): HashMap<String, String> {
        val headerData : HashMap<String, String> = HashMap<String, String>()
        headerData.put(APIConstant.Parameter.API_KEY,"Bearer ${PrefsManager.loginUserInfo.user.apiKey}")
        return headerData
    }
}