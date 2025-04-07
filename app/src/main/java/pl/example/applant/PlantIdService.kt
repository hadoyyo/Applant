package pl.example.applant

import android.content.Context
import android.graphics.Bitmap
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class PlantIdService(private val context: Context) {

    companion object {
        private const val PLANT_ID_API_URL = "https://api.plant.id/v2/health_assessment"
        private const val API_KEY_URL = "https://drive.google.com/uc?export=download&id=1TYUdxQgtGctZ-pyBh5PiZ_q3LFBHPaQo"
    }

    private fun getAppLanguage(): String {
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return sharedPreferences.getString("app_language", "en") ?: "en"
    }

    private fun getStringResource(key: String): String {
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        return if (resId != 0) context.getString(resId) else key
    }

    fun assessPlantHealth(bitmap: Bitmap, onResult: (SpannableStringBuilder) -> Unit) {
        fetchApiKey { apiKey ->
            if (apiKey.isNullOrEmpty()) {
                val error = SpannableString(getStringResource("api_key_error")).apply {
                    setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                }
                onResult(SpannableStringBuilder().append(error))
                return@fetchApiKey
            }

            performHealthAssessment(bitmap, apiKey, onResult)
        }
    }

    private fun fetchApiKey(onComplete: (String?) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(API_KEY_URL)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onComplete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.trim()?.let { key ->
                        onComplete(key)
                    } ?: run {
                        onComplete(null)
                    }
                } else {
                    onComplete(null)
                }
            }
        })
    }

    private fun performHealthAssessment(bitmap: Bitmap, apiKey: String, onResult: (SpannableStringBuilder) -> Unit) {
        val client = OkHttpClient()
        val appLanguage = getAppLanguage()

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val imageBytes = stream.toByteArray()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "images",
                "plant.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .addFormDataPart("language", appLanguage)
            .build()

        val request = Request.Builder()
            .url(PLANT_ID_API_URL)
            .post(requestBody)
            .addHeader("Api-Key", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val error = SpannableString(getStringResource("api_key_error")).apply {
                    setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                }
                onResult(SpannableStringBuilder().append(error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val result = parseHealthAssessmentResponse(responseBody)
                        onResult(result)
                    } ?: run {
                        val error = SpannableString(getStringResource("api_key_error")).apply {
                            setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                        }
                        onResult(SpannableStringBuilder().append(error))
                    }
                } else {
                    val error = SpannableString(getStringResource("api_key_error")).apply{
                        setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                    }
                    onResult(SpannableStringBuilder().append(error))
                }
            }
        })
    }

    private fun parseHealthAssessmentResponse(jsonResponse: String): SpannableStringBuilder {
        return try {
            val json = JSONObject(jsonResponse)
            val result = SpannableStringBuilder()
            val health = json.getJSONObject("health_assessment")
            val isHealthy = health.getBoolean("is_healthy")
            val healthProbability = health.getDouble("is_healthy_probability") * 100

            // Kolory
            val purple = ContextCompat.getColor(context, R.color.purple)
            val textColor = ContextCompat.getColor(context, R.color.text)
            val green = ContextCompat.getColor(context, android.R.color.holo_green_dark)
            val yellow = ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            val red = ContextCompat.getColor(context, android.R.color.holo_red_dark)

            // Nagłówek - stan rośliny
            val healthStatus = getStringResource(if (isHealthy) "plant_status_healthy" else "plant_status_diseased")
            val healthStatusText = SpannableString("$healthStatus\n").apply {
                setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                setSpan(android.text.style.RelativeSizeSpan(1.5f), 0, length, 0)
                setSpan(ForegroundColorSpan(if (isHealthy) green else red), 0, length, 0)
            }
            result.append(healthStatusText)

            if (isHealthy) {
                // Wiadomość dla zdrowej rośliny
                val healthyMessage = SpannableString("${getStringResource("healthy_message")}\n").apply {
                    setSpan(ForegroundColorSpan(purple), 0, length, 0)
                    setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                }
                result.append(healthyMessage)
            } else {
                val diseases = health.getJSONArray("diseases")
                if (diseases.length() > 0) {
                    val header = SpannableString("${getStringResource("top_diseases")}\n\n").apply {
                        setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                        setSpan(ForegroundColorSpan(purple), 0, length, 0)
                    }
                    result.append(header)

                    val filteredDiseases = mutableListOf<JSONObject>()
                    for (i in 0 until diseases.length()) {
                        val disease = diseases.getJSONObject(i)
                        if (!disease.optBoolean("redundant", false)) {
                            filteredDiseases.add(disease)
                        }
                    }

                    filteredDiseases.sortByDescending { it.getDouble("probability") }
                    val topDiseases = filteredDiseases.take(3)

                    topDiseases.forEachIndexed { index, disease ->
                        val name = disease.getString("name")
                        val probability = disease.getDouble("probability") * 100
                        var localName = name

                        if (disease.has("disease_details")) {
                            val details = disease.getJSONObject("disease_details")
                            if (details.has("local_name") && !details.isNull("local_name")) {
                                localName = details.getString("local_name")
                            }
                        }

                        val diseaseColor = when (index) {
                            0 -> green
                            1 -> yellow
                            else -> red
                        }

                        // Nazwa choroby
                        val diseaseText = SpannableString("${index + 1}. $localName\n").apply {
                            setSpan(ForegroundColorSpan(diseaseColor), 0, length, 0)
                            setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                        }
                        result.append(diseaseText)

                        // Prawdopodobieństwo
                        val probText = SpannableString("   ${getStringResource("probability")}: ${"%.1f".format(probability)}%\n\n").apply {
                            setSpan(ForegroundColorSpan(textColor), 0, length, 0)
                        }
                        result.append(probText)
                    }
                } else {
                    val noDiseases = SpannableString("${getStringResource("no_diseases")}\n").apply {
                        setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                        setSpan(ForegroundColorSpan(textColor), 0, length, 0)
                    }
                    result.append(noDiseases)
                }
            }

            result
        } catch (e: Exception) {
            SpannableStringBuilder(getStringResource("api_key_error")).apply {
                setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
            }
        }
    }
}