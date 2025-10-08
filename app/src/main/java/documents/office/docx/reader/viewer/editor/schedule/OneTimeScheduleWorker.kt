package documents.office.docx.reader.viewer.editor.schedule

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import documents.office.docx.reader.viewer.editor.R
import documents.office.docx.reader.viewer.editor.screen.start.SplashActivity
import kotlin.random.Random

class OneTimeScheduleWorker(
    val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    companion object {
        private const val CHANNEL_ID = "schedule_channel"
    }

    override fun doWork(): Result {
        Log.e("FLAG_IMMUTABLE", "show notification")
        val notification = createNotification()
        notification.flags = Notification.FLAG_AUTO_CANCEL

        with(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {

            notify(Random.nextInt(), notification)
        }

        return Result.success()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notificationIntent = Intent(context, SplashActivity::class.java)
//        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val intent = PendingIntent.getActivity(context, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.cover)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentIntent(intent)
            .setColor(ContextCompat.getColor(context, android.R.color.background_dark))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap).setSummaryText(context.getString(R.string.notification_content)))
            .setContentText(context.getString(R.string.notification_content))
            .setContentTitle(context.getString(R.string.notification_title))
            .build()
    }
}