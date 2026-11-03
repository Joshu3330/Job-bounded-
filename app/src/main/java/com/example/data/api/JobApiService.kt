package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class RemotiveResponse(
    @Json(name = "job-count") val jobCount: Int = 0,
    @Json(name = "jobs") val jobs: List<RemotiveJob> = emptyList()
)

data class RemotiveJob(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "url") val url: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "company_name") val companyName: String = "",
    @Json(name = "company_logo") val companyLogo: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "job_type") val jobType: String? = null,
    @Json(name = "publication_date") val publicationDate: String? = null,
    @Json(name = "candidate_required_location") val candidateRequiredLocation: String? = null,
    @Json(name = "salary") val salary: String? = null,
    @Json(name = "description") val description: String = ""
)

interface JobApiService {
    @GET("api/remote-jobs")
    suspend fun searchJobs(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 30
    ): RemotiveResponse

    companion object {
        private const val BASE_URL = "https://remotive.com/"

        fun create(): JobApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(JobApiService::class.java)
        }
    }
}
