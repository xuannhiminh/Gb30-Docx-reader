package documents.office.docx.reader.viewer.editor.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class FullScreenNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            NotificationManager(applicationContext).showDailyFullScreenNotification()
        } catch (_: Throwable) {
            // Ignore and still reschedule
        }

        // Reschedule for the next day
        try {
            NotificationScheduler(applicationContext).scheduleDailyFullScreen()
        } catch (_: Throwable) {
        }

        return Result.success()
    }
}


