package com.example.data.repository

import android.os.Build
import android.text.Html
import android.util.Log
import androidx.core.text.HtmlCompat
import com.example.data.api.JobApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class JobPosting(
    val id: Long,
    val title: String,
    val company: String,
    val location: String,
    val salary: String,
    val tags: List<String>,
    val description: String,
    val url: String,
    val publicationDate: String
)

class JobSearchRepository(
    private val apiService: JobApiService = JobApiService.create()
) {
    private val tag = "JobSearchRepository"

    suspend fun searchJobs(query: String?): Result<List<JobPosting>> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query?.trim()?.ifBlank { null }
            val response = apiService.searchJobs(search = cleanQuery, limit = 35)

            val postings = response.jobs.map { job ->
                val plainDescription = stripHtml(job.description)
                JobPosting(
                    id = job.id,
                    title = job.title.trim(),
                    company = job.companyName.trim(),
                    location = job.candidateRequiredLocation?.trim()?.ifBlank { "Remote" } ?: "Remote",
                    salary = job.salary?.trim()?.ifBlank { "Competitive" } ?: "Competitive",
                    tags = job.tags.filter { it.isNotBlank() }.take(5),
                    description = plainDescription,
                    url = job.url,
                    publicationDate = job.publicationDate?.take(10) ?: ""
                )
            }
            Result.success(postings)
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch jobs from API", e)
            Result.failure(e)
        }
    }

    private fun stripHtml(html: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
            } else {
                HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
            }
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), " ").trim()
        }
    }
}
