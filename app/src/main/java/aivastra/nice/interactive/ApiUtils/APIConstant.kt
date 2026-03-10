package com.example.facewixlatest.ApiUtils

object APIConstant {
    const val errorSomethingWrong = "Opps!!Something went wrong, Please try again!"
    const val fileNotSupported = "Selected File not supported,Please try with another image!"
    const val serverTimeOut = "Server time out"
    const val BASE_URL = "https://api.aivastra.com/"
    const val BASE_IMAGE_URL = "https://api.aivastra.com/uploads/"
    const val BASE_IMAGE_URL_TRYON = "https://api.aivastra.com/uploads/tryon/"
    const val BASE_IMAGE_URL_GARMENTS = "https://api.aivastra.com/uploads/garments/"

    object API_ENDPOINTS {
     /*   val GET_SAREE_CATEGORY_LIST: String = "app_garmentlist/"
        val SAREE_FACE_SWAP_API: String = "app_garments_tryonnew"
        val CUSTOM_SAREE_SWAP_API: String = "app_custome_tryonnew"
        val GET_DRESSES_FOR: String = "app_dressforsnew"
        val GET_DRESSES_TYPE: String = "app_fortypev1/"
        val GET_DRESSES_ITEMS: String = "app_dresslist/"
        val UPLOAD_USER_IMAGE: String = "appuser_image"
        val APP_LOGIN: String = "applogin"
        val VASTRA_TRY_ON_RESULT: String = "app_tryonresult"
        val TRYON_RESULT_LIKE: String = "app_like"
        val TRYON_RESULT_ADDTOCART: String = "app_cart"
        val DELETE_ALL_TRYON_RESULT: String = "app_resultdata_delete"
        val TRYON_RESULT_LIST: String = "app_results_by_user/"
        val GET_QRCODE_LINK: String = "saved_link"
        val UPLOAD_IMAGE_VIA_SCANNER: String = "user_checkimage/"
        val FILTER_PRODUCT_LIST: String = "app_searchproduct"*/
        val SAREE_FACE_SWAP_API: String = "app_garments_tryonnewv1"
        val GET_DRESSES_FOR: String = "app_themsefor"
        val GET_DRESSES_TYPE: String = "app_fordata/"
        val GET_DRESSES_ITEMS: String = "app_dresslistv2/"
        val UPLOAD_USER_IMAGE: String = "appuser_imagev1"
        val APP_LOGIN: String = "app_loginnew"
        val VERIFY_USER: String = "app_checkuserv1"
        val VASTRA_TRY_ON_RESULT: String = "app_tryonresultv1"
        val TRYON_RESULT_LIKE: String = "app_likev1"
        val TRYON_RESULT_ADDTOCART: String = "app_cartv1"
        val DELETE_ALL_TRYON_RESULT: String = "app_resultdata_deletev1"
        val TRYON_RESULT_LIST: String = "app_results_by_userv1/"
        val GET_QRCODE_LINK: String = "saved_linkv1"
        val UPLOAD_IMAGE_VIA_SCANNER: String = "user_checkimagev1/"
        val FILTER_PRODUCT_LIST: String = "app_searchproduct"
        val LOGOUT_USER: String = "logoutv1"
    }

    object Parameter {
        val WIX_USER: String = "wixuser"
        val GARMENT_ID: String = "garment_id"
        val AUTHORIZATION: String = "Authorization"
        val USER_ID: String = "user_id"
        val SKU_NUMBER: String = "sku_number"
        val HUMAN_ID: String = "human_id"
        val THEMES_FOR: String = "themesfor"
        val DRESS_TYPE: String = "dress_type"
        val USERNAME: String = "username"
        val PASSWORD: String = "password"
        val DEVICE_ID: String = "device_id"
        val USER_IMAGE_ID: String = "userimage_id"
        val PROMT_ID: String = "promt_id"
        val RESULT_ID: String = "result_id"
        val STATUS: String = "status"
        val MERCHANT_ID: String = "merchant_id"
        val UTC_TIME: String = "utc_time"
        val SECURITY_CODE: String = "securitycode"
        val API_KEY: String = "Authorization"
        val APP_TYPE: String = "app_type"
    }

    object KeyParameter {
        val USER_HEADER_AUTHORIZATION_KEY: String = "e5e8ec37762a7eadad3f85eacc171b6259cb623ea581c7a4512341f219a5e9c4"
    }
}

