package documents.office.docx.reader.viewer.editor.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class FcmTopicWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        FCMTopicHandler.resetFCMTopic(applicationContext)
        return Result.success()
    }
}

// Schedule once per day
val request = PeriodicWorkRequestBuilder<FcmTopicWorker>(1, TimeUnit.DAYS)
    .setInitialDelay(1, TimeUnit.HOURS)
    .build()

