package com.example.facewixlatest.ApiUtils

import aivastra.nice.interactive.network.NetworkInterceptor
import aivastra.nice.interactive.network.NetworkMonitor
import aivastra.nice.interactive.network.NetworkState
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

@SuppressLint("StaticFieldLeak")
object APICaller {

    private lateinit var context: Context

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    val retrofitInstance: APIInterface
        get() {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY)
            val networkInterceptor = NetworkInterceptor(context)
            val gson = GsonBuilder()
                .setLenient()
                .create()
            val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(networkInterceptor)
                .addInterceptor(httpLoggingInterceptor)
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build()
            val retrofit = Retrofit.Builder().baseUrl(baseURL())
                .addConverterFactory(GsonConverterFactory.create(gson)).client(okHttpClient).build()
            return retrofit.create(APIInterface::class.java)
        }

    fun <T> getRequest(
        url: String?,
        headers: HashMap<String, String>,
        modelclass: Class<T>?,
        apiCallBack: APICallBack
    ): Class<T>? {
        retrofitInstance.requestWithGet(url, headers , HashMap<String, String>())!!
            .enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    if (response.body() == null) {
                        apiCallBack.onFailure()
                        return
                    }
                    val objectMapper = ObjectMapper()
                    objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                        true
                    )
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                        true
                    )
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                        true
                    )


                    try {
                        val responseData: ResponseBody? = response.body()
                        val model: T =
                            objectMapper.readValue(responseData?.string()?.trim(), modelclass)
                        Log.d("response===", "" + responseData?.string()?.trim())
                        apiCallBack.onSuccess(model)
                    } catch (e: IOException) {
                        apiCallBack.onFailure()
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    apiCallBack.onFailure()
                }
            })
        return null
    }

    fun <T> getRequestWithJSONARRAY(
        url: String?,
        params: HashMap<String, String>,
        modelclass: Class<T>?,
        apiCallBack: APICallBack
    ): Class<T>? {
        retrofitInstance.requestWithGet(url, HashMap<String, String>(), params)!!
            .enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    if (response.body() == null) {
                        apiCallBack.onFailure()
                        return
                    }
                    val objectMapper = ObjectMapper()
                    objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                        true
                    )
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                        true
                    )
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                        true
                    )

                    try {
                        val responseData: ResponseBody? = response.body()
                        val model: T? = objectMapper.readValue(
                            responseData?.string()?.trim(),
                            objectMapper.getTypeFactory()
                                .constructCollectionType(List::class.java, modelclass)
                        )

                        Log.d("response===", "" + responseData?.string()?.trim())
                        apiCallBack.onSuccess(model)
                    } catch (e: IOException) {
                        apiCallBack.onFailure()
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    apiCallBack.onFailure()
                }
            })
        return null
    }

    fun <T> postRequest(
        url: String?,
        params: HashMap<String?, RequestBody?>,
        headers: HashMap<String?, String?>,
        modelclass: Class<T>?,
        apiCallBack: APICallBackWithError
    ): Class<T>? {
        retrofitInstance.requestWithPost(url, headers, params)!!
            .enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    if (!response.isSuccessful) {
                        Log.e(
                            "API_ERROR",
                            "Code: ${response.code()}, Error: ${response.errorBody()?.string()}"
                        )
                        apiCallBack.onFailure(APIConstant.serverTimeOut)
                        return
                    }

                    if (response.body() == null) {
                        Log.e("API_ERROR", "Response body is null. Code: ${response.code()}")
                        apiCallBack.onFailure(APIConstant.serverTimeOut)
                        return
                    }
                    val objectMapper = ObjectMapper()
                    objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                        true
                    )
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                        true
                    )
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                        true
                    )

                    try {
                        val responseData: ResponseBody? = response.body()
                        val model: T =
                            objectMapper.readValue(responseData?.string()?.trim(), modelclass)
                        Log.d("response===", "" + responseData?.string()?.trim())
                        apiCallBack.onSuccess(model)
                    } catch (e: IOException) {
                        apiCallBack.onFailure("")
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    apiCallBack.onFailure(APIConstant.serverTimeOut)
                }
            })
        return null
    }

    fun <T> postRequestTryOnAPI(
        url: String?,
        params: HashMap<String?, RequestBody?>,
        headers: HashMap<String?, String?>,
        modelclass: Class<T>?,
        apiCallBack: APICallBackWithError
    ): Class<T>? {
        retrofitInstance.requestWithPost(url, headers, params)!!
            .enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    if (!response.isSuccessful) {
                        Log.e(
                            "API_ERROR",
                            "Code: ${response.code()}, Error: ${response.errorBody()?.string()}"
                        )
                        apiCallBack.onFailure(
                            "Code: ${response.code()}, Error: ${
                                response.errorBody()?.string()
                            }"
                        )
                        return
                    }

                    if (response.body() == null) {
                        Log.e("API_ERROR", "Response body is null. Code: ${response.code()}")
                        apiCallBack.onFailure("Response body is null. Code: ${response.code()}")
                        return
                    }


                    // 🔥 Read body ONCE
                    val rawResponse = response.body()?.string()?.trim()

                    if (rawResponse == null || rawResponse.isEmpty()) {
                        apiCallBack.onFailure("Invalid response format")
                        return
                    }

                    Log.d("RAW_RESPONSE", "$rawResponse")

                    // ✅ Remove non-JSON garbage (like ↩️ Falling back to node 11)
                    val cleanJson = when {
                        rawResponse.startsWith("{") -> rawResponse
                        rawResponse.contains("{") -> rawResponse.substring(rawResponse.indexOf("{"))
                        else -> {
                            apiCallBack.onFailure("Invalid response format")
                            return
                        }
                    }

                    Log.e("CLEAN_JSON", cleanJson)

                    val objectMapper = ObjectMapper()
                    objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                        true
                    )
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                        true
                    )
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                        true
                    )

                    try {
//                        val responseData: ResponseBody? = response.body()
                        val model: T =
                            objectMapper.readValue(cleanJson, modelclass)
                        Log.e("response===", cleanJson)
                        apiCallBack.onSuccess(model)
                    } catch (e: JsonParseException) {
                        apiCallBack.onFailure("Invalid JSON format\n${e.message}")
                    }
                    catch (e: MismatchedInputException) {
                        apiCallBack.onFailure("Model mismatch\n${e.message}")
                    }
                    catch (e: UnrecognizedPropertyException) {
                        apiCallBack.onFailure("Unexpected field in response\n${e.message}")
                    }
                    catch (e: Exception) {
                        apiCallBack.onFailure("Unknown parse error\n${e.stackTraceToString()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {

                    val reason = when (t) {
                        is SSLHandshakeException ->
                            "SSL Handshake failed (certificate / TLS issue)"

                        is UnknownHostException ->
                            "No internet or DNS blocked"

                        is SocketTimeoutException ->
                            "Server timeout (slow or blocked network)"

                        is ConnectException ->
                            "Connection refused (firewall / proxy)"

                        else ->
                            t.stackTraceToString()
                    }


                    val errorMessage = """
        URL: ${call.request().url}
        Error: ${t::class.java.simpleName}
        Reason: $reason
    """.trimIndent()

                    Log.e("API_FAILURE", errorMessage)
                    apiCallBack.onFailure(errorMessage)

                }
            })
        return null
    }

    fun <T> postMultipartRequest(
        url: String?, headers: HashMap<String, String>?,
        params: HashMap<String, RequestBody>?, image: MultipartBody.Part?,
        modelclass: Class<T>?, apiCallBack: APICallBack
    ): Class<T>? {
        retrofitInstance.requestWithPostImage(url, headers, params, image)
            ?.enqueue(object : Callback<ResponseBody?> {

                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    if (!response.isSuccessful) {
                        Log.e(
                            "API_ERROR",
                            "Code: ${response.code()}, Error: ${response.errorBody()?.string()}"
                        )
                        apiCallBack.onFailure()
                        return
                    }

                    if (response.body() == null) {
                        Log.e("API_ERROR", "Response body is null. Code: ${response.code()}")
                        apiCallBack.onFailure()
                        return
                    }

                    val objectMapper = ObjectMapper().apply {
                        configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                        configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                    }

                    try {
                        val responseString = response.body()?.string() ?: ""
                        Log.d("API_SUCCESS", "Response: $responseString")

                        val model: T = objectMapper.readValue(responseString, modelclass)
                        apiCallBack.onSuccess(model)
                    } catch (e: IOException) {
                        Log.e("JSON_PARSE_ERROR", "Failed to parse response: ${e.message}")
                        apiCallBack.onFailure()
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    Log.e("NETWORK_ERROR", "Request failed: ${t.message}", t)
                    apiCallBack.onFailure()
                }
            })
        return null
    }


    fun <T> postMultipleMultipartRequest(
        url: String?, headers: HashMap<String, String>?,
        params: HashMap<String, RequestBody>?, images: MutableList<MultipartBody.Part>,
        modelclass: Class<T>?, apiCallBack: APICallBack
    ): Class<T>? {
        retrofitInstance.requestWithPostMultipleImages(url, headers, params, images)!!
            .enqueue(object : Callback<ResponseBody?> {

                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    if (response.body() == null) {
                        apiCallBack.onFailure()
                        return
                    }

                    val objectMapper = ObjectMapper()
                    objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                        true
                    )
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                        true
                    )
                    objectMapper.configure(
                        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                        true
                    )

                    try {
                        val responseData: ResponseBody? = response.body()
                        val model: T =
                            objectMapper.readValue(responseData?.string()?.trim(), modelclass)
                        Log.d("response===", "" + responseData?.string()?.trim())
                        apiCallBack.onSuccess(model)
                    } catch (e: IOException) {
                        apiCallBack.onFailure()
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    apiCallBack.onFailure()
                }
            })
        return null
    }


    fun baseURL(): String {
        return APIConstant.BASE_URL
    }

    interface APICallBack {
        fun <T> onSuccess(modelclass: T): Class<T>?
        fun onFailure()
    }

    interface APICallBackWithError {
        fun <T> onSuccess(modelclass: T): Class<T>?
        fun onFailure(errorMsg: String)
    }

}
