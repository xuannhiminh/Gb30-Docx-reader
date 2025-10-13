package documents.office.docx.reader.viewer.editor

//import com.google.android.gms.ads.ez.EzApplication
//import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.akexorcist.localizationactivity.core.LocalizationApplicationDelegate
import com.ezstudio.pdftoolmodule.di.toolModule
import com.ezteam.baseproject.BuildConfig
import com.ezteam.baseproject.di.baseModule
import com.ezteam.baseproject.utils.IAPUtils
import com.ezteam.baseproject.utils.PreferencesUtils
import com.facebook.ads.AdSettings
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.nlbn.ads.util.AppFlyer
import com.nlbn.ads.util.AppOpenManager
import documents.office.docx.reader.viewer.editor.common.LocaleManager
import documents.office.docx.reader.viewer.editor.di.appModule
import documents.office.docx.reader.viewer.editor.notification.NotificationManager
import documents.office.docx.reader.viewer.editor.screen.iap.IapActivity
import documents.office.docx.reader.viewer.editor.screen.iap.IapActivityV2
import documents.office.docx.reader.viewer.editor.screen.language.PreferencesHelper
import documents.office.docx.reader.viewer.editor.screen.start.SplashActivity
import documents.office.docx.reader.viewer.editor.utils.FCMTopicHandler
import documents.office.docx.reader.viewer.editor.utils.FirebaseRemoteConfigUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import office.file.ui.MyLibApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


class PdfApplication: MyLibApplication() {
    companion object {
        private const val TAG = "PdfApplication"
    }

    private val delegate = LocalizationApplicationDelegate()

    override fun onCreate() {
        super.onCreate()
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(IapActivityV2::class.java)
        AppFlyer.getInstance().initAppFlyer(this, getString(R.string.app_flyer_id), true, false, true) // thay
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        PreferencesUtils.init(this)
        setupKoin()
        PreferencesHelper.init(this)
        initLanguage()
        if (!BuildConfig.DEBUG) {
            Log.i(TAG, "onCreate: init Firebase")
            FirebaseApp.initializeApp(this)
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            FirebaseMessaging.getInstance()
                .subscribeToTopic("release_devices")
        } else {
//            FirebaseAppCheck.getInstance()
//                .installAppCheckProviderFactory(
//                    DebugAppCheckProviderFactory.getInstance()
//                )
            FirebaseMessaging.getInstance().subscribeToTopic("debug_device")
        }
        subscribeToTimezoneTopic()

        FCMTopicHandler.resetFCMTopic(this@PdfApplication)

        FirebaseRemoteConfigUtil.getInstance().fetchRemoteConfig { Log.d(TAG, "fetched") }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d("MyFirebaseMessagingService", "Token: $token")
            }
        FirebaseFirestore.getInstance()
        NotificationManager(this).createNotificationChannel()
        applyAdMobAPI35WorkaroundIfNeeded(this)
        AdSettings.addTestDevice("f8fb2c9d-1e1b-4f6d-b9c9-2f955bb05c2c")
        AdSettings.addTestDevice("2aa3aa2c-8872-40db-bb15-05c9a3b80a85")
    }

    override fun enableAdsResume(): Boolean {
        return !IAPUtils.isPremium()
    }

    override fun getKeyRemoteIntervalShowInterstitial(): String? {
        return null
    }

    override fun getListTestDeviceId(): MutableList<String>? {
        return listOf("9C9F13F96407265FD1AD8B7217DFBB4F", "19CF3EF910710E71DA25CC42219EC340", "21EA4B3FCEDFE792A5CE1655F1B92B1F", "6877F63B09F390A8A9C2FBF6C0A89106",
            "DB2BB2022DEE9BB6351662532A8F6F05","7BF8EBE42FEB24B9C0231207C37B53FD", "87BCCFEB97F85D17C4D63F0257B0CBE5") as MutableList<String>?
    }

    override fun getResumeAdId(): String {
        return getString(R.string.open_all)
    }


    override fun buildDebug(): Boolean {
        return BuildConfig.DEBUG
    }

    override fun isForceShowFullAdsTest(): Boolean {
        return true
    }

    override fun isPurchased(): Boolean {
        return IAPUtils.isPremium()
    }

    private fun initLanguage() {
        if (TextUtils.isEmpty(PreferencesHelper.getString(PreferencesHelper.KEY_LANGUAGE))) {
            documents.office.docx.reader.viewer.editor.screen.language.LocaleManager.getInstance(this).prefLanguage =
                documents.office.docx.reader.viewer.editor.screen.language.LocaleManager.LANGUAGE_DEFAULT
            val currentLanguage = resources.configuration.locales.get(0).language
            for (i in documents.office.docx.reader.viewer.editor.screen.language.LocaleManager.lstCodeLanguage.indices) {
                if (currentLanguage == documents.office.docx.reader.viewer.editor.screen.language.LocaleManager.lstCodeLanguage[i]) {
                    PreferencesHelper.putString(PreferencesHelper.KEY_LANGUAGE, currentLanguage)
                    delegate.setDefaultLanguage(this, resources.configuration.locales.get(0))
                    return
                }
            }
        }
    }
    
    override fun attachBaseContext(base: Context?) {
        base?.let {
            LocaleManager.init(it)
            super.attachBaseContext(LocaleManager.setLocale(it))
        } ?: kotlin.run {
            super.attachBaseContext(base)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }

    private fun setupKoin() {
        startKoin {
            androidContext(this@PdfApplication)
            modules(
                appModule,
                baseModule,
                toolModule
            )
        }
    }


    fun subscribeToTimezoneTopic() {
        val tz = TimeZone.getDefault()
        val offsetHours = (tz.rawOffset + tz.dstSavings) / (1000 * 60 * 60)

        val topic = "utc${offsetHours}" // Example: "utc7"

        FirebaseMessaging.getInstance()
            .subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG,"Subscribed to topic: $topic")
                }
            }
    }

    private fun applyAdMobAPI35WorkaroundIfNeeded(application: Application) {
        if (Build.VERSION.SDK_INT < 35) return

        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                applyAPI35WorkaroundToActivity(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                applyAPI35WorkaroundToActivity(activity)
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyAPI35WorkaroundToActivity(activity: Activity) {
        if (activity.javaClass.name != "com.google.android.gms.ads.AdActivity") return

        val controller = activity.window.insetsController
        controller?.hide(WindowInsets.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT

    }

}