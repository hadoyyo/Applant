// PlantStorage.kt
package pl.example.applant

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PlantStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("plants_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun savePlants(plants: List<Plant>) {
        val json = gson.toJson(plants)
        prefs.edit().putString("plants_list", json).apply()
    }

    fun getPlants(): List<Plant> {
        val json = prefs.getString("plants_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Plant>>() {}.type
        return gson.fromJson(json, type)
    }

    fun updatePlant(updatedPlant: Plant) {
        val plants = getPlants().toMutableList()
        val index = plants.indexOfFirst { it.id == updatedPlant.id }

        if (index != -1) {
            plants[index] = updatedPlant
            savePlants(plants)
        }
    }

    fun scheduleWateringNotification(context: Context, plant: Plant) {
        // Sprawdź, czy powiadomienia są włączone
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) {
            return // Powiadomienia są wyłączone
        }

        if (plant.wateringIntervalDays <= 0 || plant.wateringDates.isEmpty()) {
            return // Nie planuj powiadomień, jeśli nie ustawiono interwału lub nie ma dat podlania
        }

        val lastWateringDate = plant.wateringDates.last()
        val lastWateringCalendar = parseDate(lastWateringDate)

        val nextWateringCalendar = Calendar.getInstance().apply {
            timeInMillis = lastWateringCalendar.timeInMillis
            add(Calendar.DAY_OF_YEAR, plant.wateringIntervalDays) // Dodaj interwał do ostatniej daty podlania
        }

        // Ustaw godzinę powiadomienia
        val notificationTime = sharedPreferences.getString("notification_time", "08:00")
        val (hour, minute) = notificationTime?.split(":")?.map { it.toInt() } ?: listOf(8, 0)
        nextWateringCalendar.set(Calendar.HOUR_OF_DAY, hour)
        nextWateringCalendar.set(Calendar.MINUTE, minute)
        nextWateringCalendar.set(Calendar.SECOND, 0)

        val delay = nextWateringCalendar.timeInMillis - System.currentTimeMillis()

        if (delay > 0) {
            val workManager = WorkManager.getInstance(context)
            val data = Data.Builder().putString("plantName", plant.name).build()

            val notificationRequest = OneTimeWorkRequest.Builder(WateringNotificationWorker::class.java)
                .setInputData(data)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS) // Zaplanuj powiadomienie z opóźnieniem
                .build()

            workManager.enqueue(notificationRequest)
        }
    }

    private fun parseDate(dateString: String): Calendar {
        val parts = dateString.split("-")
        return Calendar.getInstance().apply {
            set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
        }
    }

    fun cancelAllNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWork()
    }
}