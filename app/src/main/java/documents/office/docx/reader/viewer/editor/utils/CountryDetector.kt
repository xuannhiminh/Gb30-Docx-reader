package documents.office.docx.reader.viewer.editor.utils

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import documents.office.docx.reader.viewer.editor.screen.language.PreferencesHelper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object CountryDetector {
    const val KEY_IS_NOT_VN = "is_not_vn"

    suspend fun checkIfNotVN(context: Context): Boolean {
        Log.d("CountryDetector", "checkIfNotVN: called")
        val isNotVN = PreferencesHelper.getString(KEY_IS_NOT_VN, null)

        if (!isNotVN.isNullOrEmpty()) {
            return isNotVN == "true"
        }
        Log.d("CountryDetector", "checkIfNotVN: detecting country")
        val isNotVn = isNotVietnam(context)
        PreferencesHelper.putString(KEY_IS_NOT_VN, if(isNotVn) "true" else "false")
        Log.d("CountryDetector", "checkIfNotVN: detected country!")
        return isNotVn
    }

    suspend fun isNotVietnam(context: Context): Boolean {
        val ipCountry = getCountryFromIP()?.uppercase(Locale.US)
        val simCountry = getSimCountry(context)
        val localeCountry = Locale.getDefault().country.uppercase(Locale.US)
        val tzOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000

        // Step 1: if IP says VN → stop immediately
        if (ipCountry == "VN") return false

        // Step 2: Combine signals to detect strong Vietnam pattern
        val possibleVietnam = when {
            simCountry == "VN" -> true
            localeCountry == "VN" -> true
            tzOffset == 7 && (localeCountry != "ID" && localeCountry != "IDN")   -> true
            else -> false
        }

        return !possibleVietnam
    }

    private suspend fun getCountryFromIP(): String? {
        return try {
            val url = URL("https://ipinfo.io/json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.requestMethod = "GET"
            conn.inputStream.bufferedReader().use {
                val json = JSONObject(it.readText())
                json.optString("country", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getSimCountry(context: Context): String? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.simCountryIso?.uppercase(Locale.US)
        } catch (e: Exception) {
            null
        }
    }
}
