package documents.office.docx.reader.viewer.editor.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import java.util.Currency
import java.util.Locale

class AppUtils {

     companion object {
         fun getCurrencySymbol(currencyCode: String, locale: Locale = Locale.getDefault()): String {
             return Currency.getInstance(currencyCode)
                 .getSymbol(locale)
         }
        fun isWidgetNotAdded(context: Context, widgetClass: Class<*>): Boolean {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, widgetClass))
            return appWidgetIds.isEmpty()
        }
         const val PDF_DETAIL_EZLIB = 0L // 0 mean use EZ lib pdf detail, 1 mean use SoLib PDF detail
         const val FOLDER_EXTERNAL_IN_DOWNLOADS = "AllPDFReaderTripSoft"
         const val PUBLIC_LICENSE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtIPuy3/IMkcxSqVkU/dAl5g2Zbaace9DvaTdDf4pyz+SeqGrlbdViwOtZVYhNnY4iDUgNDgVGl1RkE2nNsEz46TL7AIAJhqoXS/6erGzsM87XH2s2LyfjtctLMjDSvRkWtMwmD+3NuOMPn0BI5LHEa1F0nM8Md4S4KQNZFfZq+BVTdKSuePZEIu2a6PJv7wKrTDDTykra61APgPFXcMP/Piw0EBzCrHWDX2dOhLbU8NRnT4MN3pEsSHD8kbpBnFshOCoNWuU2XR0J5n6eK2vAFx4mgyBkED9bPItW8j4MThi8zsrtavz/+AWQ8Ss0IutwBno5cJp12bf7zxpUZdqNwIDAQAB"
     }
 }