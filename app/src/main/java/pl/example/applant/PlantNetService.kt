package pl.example.applant

import android.content.Context
import android.graphics.Bitmap
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class PlantNetService(private val context: Context) {

    companion object {
        private const val PLANTNET_API_URL = "https://my-api.plantnet.org/v2/identify/all"
        private const val API_KEY = "2b10LXzlQJsLOo7Q3hV8ALE8W" // Twój klucz API
    }

    // Funkcja do identyfikacji rośliny
    fun identifyPlant(bitmap: Bitmap, onResult: (String?) -> Unit) {
        val client = OkHttpClient()

        // Przekształć Bitmapę na bajty
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val imageBytes = stream.toByteArray()

        // Przygotuj żądanie
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "images", // Klucz dla zdjęcia
                "plant.jpg", // Nazwa pliku
                imageBytes.toRequestBody("image/jpeg".toMediaType()) // Typ MIME
            )
            .addFormDataPart("organs", "auto") // Określ część rośliny (auto)
            .build()

        // Pobierz język aplikacji
        val appLanguage = getAppLanguage()

        // Zbuduj URL z parametrami
        val url = "$PLANTNET_API_URL?include-related-images=false&no-reject=false&lang=$appLanguage&type=kt&api-key=$API_KEY"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("accept", "application/json") // Dodaj nagłówek "accept"
            .build()

        // Wykonaj żądanie asynchronicznie
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Błąd podczas wysyłania żądania

                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {

                        // Przetwórz odpowiedź JSON
                        val json = JSONObject(responseBody)
                        val results = json.getJSONArray("results")
                        if (results.length() > 0) {
                            val bestMatch = results
                                .getJSONObject(0)
                                .getJSONObject("species")

                            val plantName = getPlantNameBasedOnLanguage(bestMatch, appLanguage)
                            onResult(plantName)
                        } else {
                            onResult(null)
                        }
                    } else {
                        onResult(null)
                    }
                } else {
                    val errorBody = response.body?.string()
                    onResult(null)
                }
            }
        })
    }

    // Funkcja do pobierania języka aplikacji
    private fun getAppLanguage(): String {
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return sharedPreferences.getString("app_language", "en") ?: "en"
    }

    // Funkcja do pobierania nazwy rośliny w zależności od języka aplikacji
    private fun getPlantNameBasedOnLanguage(species: JSONObject, appLanguage: String): String {
        // Sprawdź, czy istnieją commonNames w odpowiedzi API
        val commonNames = species.optJSONArray("commonNames")
        if (commonNames != null && commonNames.length() > 0) {
            // Jeśli język aplikacji to polski (pl), spróbuj znaleźć polską nazwę
            if (appLanguage == "pl") {
                for (i in 0 until commonNames.length()) {
                    val name = commonNames.getString(i)
                    // Załóżmy, że polskie nazwy zawierają znaki diakrytyczne
                    if (name.matches(Regex(".*[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ].*"))) {
                        return name
                    }
                }
            }
            // Jeśli nie znaleziono polskiej nazwy, użyj pierwszej dostępnej nazwy
            return commonNames.getString(0)
        }

        // Jeśli nie ma commonNames, użyj nazwy naukowej
        return species.getString("scientificName")
    }
}