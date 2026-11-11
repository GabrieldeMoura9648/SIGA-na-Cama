package com.example.fatecskip.data.remote

import com.example.fatecskip.data.remote.dto.HolidayDto
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface HolidayApiService {

    @GET("{year}")
    suspend fun getNationalHolidays(@Path("year") year: Int): List<HolidayDto>

    companion object {
        private const val BASE_URL = "https://brasilapi.com.br/api/feriados/v1/"

        fun create(): HolidayApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(HolidayApiService::class.java)
        }
    }
}
