package com.neobili.checker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

class PediToolsThresholdRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    suspend fun getThresholds(ga: Int, ageHours: Int, hasAdditionalRisk: Boolean): Thresholds =
        withContext(Dispatchers.IO) {
            val risk = if (hasAdditionalRisk) "any" else "none"
            val url = "https://peditools.org/bili2022/api/?ga=$ga&age=$ageHours&risk=$risk"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NeoBiliChecker/0.1 Android clinical reference prototype")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Threshold service returned HTTP ${response.code}")
                val html = response.body?.string().orEmpty()
                if (html.isBlank()) throw IOException("Threshold service returned an empty response")
                parseThresholds(html, hasAdditionalRisk)
            }
        }

    internal fun parseThresholds(html: String, hasAdditionalRisk: Boolean): Thresholds {
        val document = Jsoup.parse(html)
        val text = document.text().replace('\u00A0', ' ')

        val label = if (hasAdditionalRisk) {
            "ANY neurotoxicity risk factors"
        } else {
            "No neurotoxicity risk factors"
        }

        val labelIndex = text.indexOf(label, ignoreCase = true)
        if (labelIndex < 0) {
            throw IOException("Could not find the requested risk row in the threshold response")
        }

        val segment = text.substring(labelIndex, minOf(text.length, labelIndex + 260))
        val numberRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*mg/dL", RegexOption.IGNORE_CASE)
        val values = numberRegex.findAll(segment).map { it.groupValues[1].toDouble() }.toList()

        if (values.size < 2) {
            throw IOException("Could not parse phototherapy and exchange thresholds")
        }

        return Thresholds(
            phototherapyMgDl = values[0],
            exchangeMgDl = values[1],
        )
    }
}
