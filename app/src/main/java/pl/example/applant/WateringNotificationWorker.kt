//WateringNotificationWorker.kt
package pl.example.applant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Locale

class WateringNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        setAppLocale(applicationContext, "en")
        val plantName = inputData.getString("plantName") ?: return Result.failure()

        // Utwórz kanał powiadomień (tylko dla Android 8.0+)
        val channelId = "watering_notifications"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Powiadomienia o podlewaniu",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val largeIcon = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.can)

        // Utwórz powiadomienie
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(applicationContext.getString(R.string.notification_text, plantName))
            .setSmallIcon(R.drawable.vector_logo)
            .setLargeIcon(largeIcon)
            .setColor(ContextCompat.getColor(applicationContext, R.color.purple))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Wyświetl powiadomienie
        notificationManager.notify(plantName.hashCode(), notification)

        return Result.success()
    }

    fun setAppLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}