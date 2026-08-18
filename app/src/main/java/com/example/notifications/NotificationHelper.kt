package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.MainActivity
import com.example.R
import com.example.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "lexi_upsc_reminders"
        const val WORK_NAME = "lexi_upsc_reminder_work"
        
        // This is called from the Worker to actually show the notification
        fun showNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Revision Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminds you to complete your daily LexiUPSC vocabulary revision."
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this exists, or use android.R.drawable.ic_dialog_info
                .setContentTitle("Time for Revision!")
                .setContentText("It's your usual time to revise vocabulary. Keep your streak alive!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
        }
        
        fun scheduleNextRevisionNotification(context: Context, averageTimeOfDayMs: Long) {
            // Calculate delay until the next occurrence of averageTimeOfDayMs
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MILLISECOND, averageTimeOfDayMs.toInt())
            }

            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }

            val delay = target.timeInMillis - now.timeInMillis

            val workRequest = OneTimeWorkRequestBuilder<RevisionReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}

class RevisionReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        NotificationHelper.showNotification(context)
        
        // After showing, schedule the next one for exactly 24 hours later
        // or recalculate if the user had a new revision.
        val settingsRepo = SettingsRepository(context)
        val historyStr = settingsRepo.lastRevisionTimesFlow.first()
        val averageMs = calculateAverageTime(historyStr)
        
        NotificationHelper.scheduleNextRevisionNotification(context, averageMs)
        return Result.success()
    }
    
    private fun calculateAverageTime(historyStr: String): Long {
        if (historyStr.isBlank()) {
             return 20L * 60 * 60 * 1000 // 8 PM default
        }
        val times = historyStr.split(",").mapNotNull { it.toLongOrNull() }
        if (times.isEmpty()) {
             return 20L * 60 * 60 * 1000
        }
        val sum = times.sum()
        return sum / times.size
    }
}
