package documents.office.docx.reader.viewer.editor.utils

import android.content.Context
import android.icu.util.TimeZone
import android.util.Log
import com.ezteam.baseproject.utils.IAPUtils
import com.google.firebase.messaging.FirebaseMessaging
import documents.office.docx.reader.viewer.editor.BuildConfig
import documents.office.docx.reader.viewer.editor.screen.language.PreferencesHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object FCMTopicHandler {
     const val TAG = "FCMTopicHandler"
     fun resetFCMTopic(context: Context) {
         GlobalScope.launch {
             Log.d(TAG, "resetFCMTopic: called")
             val newTopic = generateTopic(context)
             val currentTopic = PreferencesHelper.getString(PreferencesHelper.GLOBAL_FIREBASE_TOPIC, null)
             if (!currentTopic.isNullOrEmpty() && currentTopic == newTopic) {
                    Log.d(TAG, "resetFCMTopic: topic is the same, no need to reset")
                 return@launch
             } else  {
                 FirebaseMessaging.getInstance().unsubscribeFromTopic(currentTopic). addOnCompleteListener { task ->
                     if (!task.isSuccessful) {
                         FirebaseMessaging.getInstance().unsubscribeFromTopic(currentTopic)
                         Log.d(TAG, "resetFCMTopic: failed to unsubscribe from $currentTopic, retrying")
                     }
                 }
                 FirebaseMessaging.getInstance().subscribeToTopic(newTopic).addOnCompleteListener { task ->
                     if (task.isSuccessful) {
                         PreferencesHelper.putString(PreferencesHelper.GLOBAL_FIREBASE_TOPIC, newTopic)
                         Log.d(TAG, "resetFCMTopic: subscribed to $newTopic")
                     } else {
                         FirebaseMessaging.getInstance().subscribeToTopic(newTopic).addOnCompleteListener {
                             if (it.isSuccessful) {
                                 PreferencesHelper.putString(PreferencesHelper.GLOBAL_FIREBASE_TOPIC, newTopic)
                                    Log.d(TAG, "resetFCMTopic: subscribed to $newTopic on retry")
                             }
                         }
                     }
                 }
             }
         }
    }



     private suspend fun generateTopic(context: Context): String {
        val premium = if (IAPUtils.isPremium()) "prem" else "free"

        val isNotVn = CountryDetector.checkIfNotVN(context)

        val offsetHours = (TimeZone.getDefault().rawOffset + TimeZone.getDefault().dstSavings) / (1000 * 60 * 60)

        val version = BuildConfig.VERSION_NAME

         val lastEngageMillis = PreferencesHelper.getLong(PreferencesHelper.KEY_LAST_ENGAGE, -1L)

         val daysSinceEngage = if (lastEngageMillis == -1L) {
             0L
         } else {
             val zone = ZoneId.systemDefault()

             // SAFER: use Instant → atZone() → toLocalDate() instead of LocalDate.ofInstant()
             val lastDate = Instant.ofEpochMilli(lastEngageMillis).atZone(zone).toLocalDate()
             val nowDate = Instant.now().atZone(zone).toLocalDate()

             ChronoUnit.DAYS.between(lastDate, nowDate)
         }

         val engageBucket = when {
             daysSinceEngage == 0L -> "d0"   // same calendar day
             daysSinceEngage < 3L -> "d1"   // yesterday
             daysSinceEngage < 7L -> "d3"
             daysSinceEngage < 14 -> "d14"
             else -> "d30"
         }

        val topic = "${premium}__vn${!isNotVn}__v${version}__utc${offsetHours}__${engageBucket}__debug${BuildConfig.DEBUG}"
        PreferencesHelper.putString(PreferencesHelper.GLOBAL_FIREBASE_TOPIC, topic)
        Log.d(TAG, "generateTopic: $topic")
        return topic
    }

}