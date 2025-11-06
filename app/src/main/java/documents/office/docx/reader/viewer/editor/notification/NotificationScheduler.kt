package documents.office.docx.reader.viewer.editor.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ezteam.baseproject.utils.FirebaseRemoteConfigUtil
import java.util.Calendar
import java.util.concurrent.TimeUnit


class NotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val TAG = "NotificationScheduler"
    private val calendarDailyCallOpenApp = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)

        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DATE, 1)
        }
    }

    private val calenderUnfinishedReading = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)

        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DATE, 1)
        }
    }

    private val calendarForgotten = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)

        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DATE, 1)
        }
    }

    private val calendarDailyFullScreen = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, FirebaseRemoteConfigUtil.getInstance().getTimeShowFullScreenNotification())
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)

        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DATE, 1)
        }
    }

    private fun canUseExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                alarmManager.canScheduleExactAlarms()
            } catch (_: Throwable) {
                false
            }
        } else {
            true
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleUnfinishedReadingNotification() {
        // Check if the user has granted notification permission
//        if (!checkNotificationPermission()) {
//            Log.d(TAG, "scheduleUnfinishedReadingNotification: Notification permission not granted")
//            return
//        }
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("request_code", NotificationReceiver.DAILY_CHECK_UNFINISHED_FILES_REQUEST_CODE)
        }
        // Only check if the PendingIntent exists if you're sure it would be still valid
        val existingIntent = PendingIntent.getBroadcast(
            context,
            NotificationManager.UNFINISHED_READING_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existingIntent != null) {
            // Alarm already scheduled; nothing to do
            Log.d(TAG, "scheduleUnfinishedReadingNotification: Alarm already scheduled; nothing to do")
            return
        }
        // Set the alarm time. Optionally include logic to adjust time for today or tomorrow.
        val calendar = calenderUnfinishedReading
        val newPendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationManager.UNFINISHED_READING_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (canUseExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                newPendingIntent
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, newPendingIntent)
        }
        Log.d(TAG, "scheduleUnfinishedReadingNotification: Alarm scheduled for ${calendar.timeInMillis}")
    }

    fun scheduleForgottenFileNotification() {
        // Check if the user has granted notification permission
//        if (!checkNotificationPermission()) {
//            Log.d(TAG, "scheduleForgottenFileNotification: Notification permission not granted")
//            return
//        }
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("request_code", NotificationReceiver.DAILY_CHECK_FORGOTTEN_FILES_REQUEST_CODE)
        }

        // Only check if the PendingIntent exists if you're sure it would be still valid
        val existingIntent = PendingIntent.getBroadcast(
            context,
            NotificationManager.FORGOTTEN_FILE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existingIntent != null) {
            // Alarm already scheduled; nothing to do
            Log.d(TAG, "scheduleForgottenFileNotification: Alarm already scheduled; nothing to do")
            return
        }
        // Set the alarm time. Optionally include logic to adjust time for today or tomorrow.

        val calendar = calendarForgotten

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationManager.FORGOTTEN_FILE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (canUseExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
        Log.d(TAG, "scheduleForgottenFileNotification: Alarm scheduled for ${calendar.timeInMillis}")
    }

    fun scheduleDailyCallOpenAppNotification() {
        return
        // temporary do not use this function
        // Check if the user has granted notification permission
//        if (!checkNotificationPermission()) {
//            Log.d(TAG, "scheduleDailyCallOpenAppNotification: Notification permission not granted")
//            return
//        }
        val calendar = calendarDailyCallOpenApp

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("request_code", NotificationReceiver.DAILY_CALL_OPEN_APP_REQUEST_CODE)
        }

        // Only check if the PendingIntent exists if you're sure it would be still valid
        val existingIntent = PendingIntent.getBroadcast(
            context,
            NotificationManager.DAILY_CALL_OPEN_APP_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existingIntent != null) {
            // Alarm already scheduled; nothing to do
            Log.d(TAG, "scheduleDailyCallOpenAppNotification: Alarm already scheduled; nothing to do")
            return
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationManager.DAILY_CALL_OPEN_APP_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (canUseExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
        Log.d(TAG, "scheduleDailyCallOpenAppNotification: Alarm scheduled for ${calendar.timeInMillis}")

    }

    fun scheduleDailyFullScreen() {
        val now = System.currentTimeMillis()
        var triggerAt = calendarDailyFullScreen.timeInMillis
        if (triggerAt <= now) {
            // Nếu đã qua giờ hôm nay, chuyển sang ngày mai cùng khung giờ
            calendarDailyFullScreen.add(Calendar.DATE, 1)
            triggerAt = calendarDailyFullScreen.timeInMillis
        }

        val delayMs = (triggerAt - now).coerceAtLeast(0)

        val request = OneTimeWorkRequestBuilder<FullScreenNotificationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag("DAILY_FULL_SCREEN")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "DAILY_FULL_SCREEN",
                ExistingWorkPolicy.KEEP,
                request
            )

        Log.d(TAG, "scheduleDailyFullScreen (WorkManager): scheduled after ${delayMs}ms for ${calendarDailyFullScreen.timeInMillis}")
    }
} 