package com.example.familydocumentsapp.network
import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.familydocumentsapp.network.AuthInterceptor

object RetrofitInstance {
    private const val BASE_URL = "http://10.0.2.2:8000/docs/"

    private var retrofit: Retrofit? = null

    private fun getRetrofit(context: Context): Retrofit {
        if (retrofit == null) {
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context))
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
    fun getApi(context: Context): PersonalApiService {
        return getRetrofit(context).create(PersonalApiService::class.java)
    }

    fun getDataApi(context: Context): MainApiService {
        return getRetrofit(context).create(MainApiService::class.java)
    }


}