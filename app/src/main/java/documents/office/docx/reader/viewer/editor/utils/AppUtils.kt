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
         const val PUBLIC_LICENSE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3c34zMCv+1PknpBhIqPH0mBJj2qOnzfNdyxYLsA52LMXc83nOs4lo0uuoe9aquadGxHEeQ3g+OrUt5L8hJsY38Vc2+o4trUpdujcYrbtPEQD7CKxpbGH6gbr0RgBP5+b04bMbvWSwHeNh8l3BpiciDtLa69sZ6fT+39gm5mlAF4IIq2g/2SaZANWr+jmQFsTAJUXD+kVMlnwvH0VLhdcb0M67Qv94UPGBcPK/xzpV/0FJefjsEOokehrLBUfQIY8XcEZszW28v4zV7hm0N8LPoqBwzLqZOC+Yahpbcsl1hh1gsaaAG2tRQi1AglEgsEKT3416O55OmYM4CwgYeROjQIDAQAB"
     }
 }