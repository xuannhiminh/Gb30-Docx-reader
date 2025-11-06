package documents.office.docx.reader.viewer.editor.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class FullScreenNotificationReceiver : BroadcastReceiver() {
    private val TAG = "FullScreenReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Trigger full-screen notification at 11 AM")
        NotificationManager(context).showDailyFullScreenNotification()

        // Reschedule for next day to keep it daily
        NotificationScheduler(context).scheduleDailyFullScreen()
    }
}


