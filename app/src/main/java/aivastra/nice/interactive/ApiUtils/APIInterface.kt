package com.example.facewixlatest.ApiUtils

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.*

interface APIInterface {

    @GET
    fun requestWithGet(@Url url: String?,
                       @HeaderMap header: HashMap<String, String>,
                       @QueryMap param: HashMap<String, String>
    ): Call<ResponseBody?>?


    @Multipart
    @POST
    fun requestWithPost(@Url url: String?,
                        @HeaderMap header: HashMap<String?, String?>,
                        @PartMap params: HashMap<String?, RequestBody?>
    ): Call<ResponseBody?>?

    @Multipart
    @POST
    fun requestWithPostImage(@Url url: String?,
                             @HeaderMap header: HashMap<String, String>?,
                             @PartMap params: HashMap<String, RequestBody>?,
                             @Part profile_pic: MultipartBody.Part?): Call<ResponseBody?>?


    @Multipart
    @POST
    fun requestWithPostImageWithRawJson(@Url url: String?,
                             @HeaderMap header: HashMap<String, String>?,
                             @Part jsonData: JSONObject,
                             @Part image: MultipartBody.Part?): Call<ResponseBody?>?

    @Multipart
    @POST
    fun requestWithPostMultipleImages(@Url url: String?,
                                      @HeaderMap header: HashMap<String, String>?,
                                      @PartMap params: HashMap<String, RequestBody>?,
                                      @Part imageList: MutableList<MultipartBody.Part>?): Call<ResponseBody?>?

}