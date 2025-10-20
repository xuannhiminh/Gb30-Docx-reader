package documents.office.docx.reader.viewer.editor.screen.start


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.ezteam.baseproject.iapLib.v3.BillingProcessor
import com.ezteam.baseproject.iapLib.v3.PurchaseInfo
import com.ezteam.baseproject.activity.BaseActivity
import com.ezteam.baseproject.utils.IAPUtils
import com.ezteam.baseproject.utils.PreferencesUtils
import com.ezteam.baseproject.utils.SystemUtils
import com.ezteam.baseproject.utils.TemporaryStorage
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.firebase.analytics.FirebaseAnalytics
import com.nlbn.ads.callback.AdCallback
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.AppOpenManager
import com.nlbn.ads.util.ConsentHelper
import documents.office.docx.reader.viewer.editor.R
import documents.office.docx.reader.viewer.editor.common.PresKey
import documents.office.docx.reader.viewer.editor.databinding.ActivitySplashBinding
import documents.office.docx.reader.viewer.editor.notification.NotificationDecider
import documents.office.docx.reader.viewer.editor.notification.NotificationManager
import documents.office.docx.reader.viewer.editor.screen.iap.IapActivity
import documents.office.docx.reader.viewer.editor.screen.iap.IapActivityV2
import documents.office.docx.reader.viewer.editor.screen.language.LanguageActivity
import documents.office.docx.reader.viewer.editor.screen.language.PreferencesHelper
import documents.office.docx.reader.viewer.editor.screen.main.MainActivity
import documents.office.docx.reader.viewer.editor.screen.main.MainViewModel
import documents.office.docx.reader.viewer.editor.service.NotificationForegroundService
import documents.office.docx.reader.viewer.editor.utils.AppUtils
import documents.office.docx.reader.viewer.editor.utils.FCMTopicHandler
import documents.office.docx.reader.viewer.editor.utils.FirebaseRemoteConfigUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import kotlin.coroutines.resume

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    private val viewModel by inject<MainViewModel>()

    companion object {
        const val FILE_PATH = "File_path"
        fun start(activity: FragmentActivity) {
            activity.intent.data?.let {
                activity.intent.apply {
                    setClass(activity, SplashActivity::class.java)
                }
                activity.startActivity(activity.intent)
            } ?: kotlin.run {
                val intent = Intent(activity, SplashActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }

    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    @Deprecated("Deprecated in Android 13, but still needed for API < 33")
    override fun onBackPressed() {
        // Block back button
        // If you want to allow in some cases, put condition here
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                // Block back button
            }
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                pendingPermissionContinuation?.invoke(granted)
                pendingPermissionContinuation = null
            }

        handleIntent()
        super.onCreate(savedInstanceState)
    }

    private var mCountDownTimer: CountDownTimer? = null

    override fun onResume() {
        super.onResume()
        mCountDownTimer?.cancel()
        mCountDownTimer = object : CountDownTimer(FirebaseRemoteConfigUtil.getInstance().getTimeoutLoadInterMillisecond()+10000L, FirebaseRemoteConfigUtil.getInstance().getTimeoutLoadInterMillisecond()) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                Log.e("SplashActivity", "Splash timeout reached, navigating to next screen")
                navigateToNextScreen()
            }
        }.start()

    }

    override fun onDestroy() {
        super.onDestroy()
        mCountDownTimer?.cancel()
    }

    override fun onStop() {
        super.onStop()
        try {
            Admob.getInstance().dismissLoadingDialog()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun initView() {

        try {
            startForegroundService( Intent(this, NotificationForegroundService::class.java))
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error starting service: ${e.message}")
        }

        binding.loadingTitle.text =  handleAppNameSpannable2()

    }
//    private var ads_inter_id = ""

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private fun logEvent(event: String) {
        firebaseAnalytics.logEvent(event, Bundle())
    }

    private fun showAdsInterstitial(interstitialAd: InterstitialAd?) {
        Log.i("SplashActivity", "showAdsInterstitial called with ad: $interstitialAd")
        Admob.getInstance().showInterAds(this@SplashActivity, interstitialAd, object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                navigateToNextScreen()
            }
        })
    }

    private fun showAdsOpen() {
        Log.i("SplashActivity", "show ads open")
        AppOpenManager.getInstance().showAppOpenSplash(this@SplashActivity, object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                navigateToNextScreen()
            }
        })
    }


    private suspend fun loadInterstitialAd(idAds: String): InterstitialAd? =
        withTimeoutOrNull(FirebaseRemoteConfigUtil.getInstance().getTimeoutLoadInterMillisecond()) {
            suspendCancellableCoroutine<InterstitialAd?> { cont ->
                val startTime = System.currentTimeMillis()
                Log.i("SplashActivity", "Loading interstitial started at $startTime")
                val consentHelper = ConsentHelper.getInstance(applicationContext)

                if (!consentHelper.canLoadAndShowAds()) consentHelper.reset()
                consentHelper.obtainConsentAndShow(this@SplashActivity) {
                    TemporaryStorage.isObtainConsent = true
                    if (!SystemUtils.isInternetAvailable(this@SplashActivity)) {
                        Log.e("SplashActivity", "No internet, skipping ad load")
                        if (cont.isActive) cont.resume(null)
                        return@obtainConsentAndShow
                    }

                    val interCallback = object : AdCallback() {
                        override fun onInterstitialLoad(ad: InterstitialAd?) {
                            Log.i("SplashActivity", "Inter Ad loaded in ${System.currentTimeMillis() - startTime} ms")
                            if (cont.isActive) cont.resume(ad)
                        }

                        override fun onInterstitialLoadFaild() {
                            Log.e("SplashActivity", "Inter Ad failed to load")
                            if (cont.isActive) cont.resume(null)
                        }
                        override fun onAdFailedToLoad(var1: LoadAdError?) {
                            Log.e("SplashActivity", "Inter Ad failed to load")
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                    Admob.getInstance().setOpenActivityAfterShowInterAds(false)
                    Admob.getInstance().loadInterAds(this@SplashActivity, idAds, interCallback)
                }

                // Cleanup if coroutine is cancelled by timeout or otherwise
                cont.invokeOnCancellation {
                    Log.w("SplashActivity", "Inter Ad load coroutine cancelled or timed out")
                    // If you have any explicit unregister API, call it here
                }
            }
        }?.also {
            // If we got here with non-null, it’s a real ad.
            Log.i("SplashActivity", "loadInterstitialAd returned a valid ad")
        } ?: run {
            // Timeout (or failure) branch
            Log.w("SplashActivity", "Inter Ad load timed out (15s) or failed")
            null
        }

    private suspend fun loadAppOpenAds() {
        withTimeoutOrNull(FirebaseRemoteConfigUtil.getInstance().getTimeoutLoadInterMillisecond()+5000L) {
            suspendCancellableCoroutine<Unit> { cont ->
                val startTime = System.currentTimeMillis()
                Log.i("SplashActivity", "Loading open ads started at $startTime")
                val consentHelper = ConsentHelper.getInstance(applicationContext)

                if (IAPUtils.isPremium()) {
                    Log.w("SplashActivity", "Skipping open ads load due to premium or no consent")

                    if (!consentHelper.canLoadAndShowAds()) {
                        consentHelper.reset()
                    }
                    consentHelper.obtainConsentAndShow(this@SplashActivity) {
                        TemporaryStorage.isObtainConsent = true
                    }
                    if (cont.isActive) cont.resume(Unit)
                    return@suspendCancellableCoroutine
                }

                if (!consentHelper.canLoadAndShowAds()) {
                    consentHelper.reset()
                }

                consentHelper.obtainConsentAndShow(this@SplashActivity) {
                    TemporaryStorage.isObtainConsent = true
                    if (!SystemUtils.isInternetAvailable(this@SplashActivity)) {
                        Log.e("SplashActivity", "No internet, skipping ad load")
                        if (cont.isActive) cont.resume(Unit)
                        //return@obtainConsentAndShow
                    }

                    AppOpenManager.getInstance().loadOpenAppAdSplash(
                        this@SplashActivity,
                        getString(R.string.open_splash),
                        100,
                        FirebaseRemoteConfigUtil.getInstance().getTimeoutLoadInterMillisecond(),
                        false,
                        object : AdCallback() {
                            override fun onAdSplashReady() {
                                Log.i("SplashActivity", "Open ad ready in ${System.currentTimeMillis() - startTime} ms")
                                if (cont.isActive) cont.resume(Unit)
                            }

                            override fun onAdFailedToLoad(var1: LoadAdError?) {
                                Log.e("SplashActivity", "Open ad failed to load")
                                if (cont.isActive) cont.resume(Unit)
                            }

                            override fun onNextAction() {
                                Log.e("SplashActivity", "Open ad failed to load onNextAction")
                                if (cont.isActive) cont.resume(Unit)
                            }
                        }
                    )
                }

                cont.invokeOnCancellation {
                    Log.w("SplashActivity", "Open ads load coroutine cancelled or timed out")
                }
            }
        } ?: Log.w("SplashActivity", "Open ads load timed out (35s) or failed")
    }



    private fun handleIntent() {
        if(intent.getBooleanExtra("${packageName}.isFromNotification", false)) {
            val notificationId = intent.getIntExtra("${packageName}.notificationID", -1)
            logEvent("notification_clicked_${if (notificationId != -1) notificationId else "_1"}")
            PreferencesUtils.putInteger("time_notification_clicked", PreferencesUtils.getInteger("time_notification_clicked", 0) + 1)
            if (notificationId != -1) {
                val notificationManager = NotificationManager(this)
                notificationManager.cancelNotification(notificationId)
                if (notificationId == NotificationManager.CALL_USE_APP_NOTIFICATION_ID) {
                    NotificationDecider.onNotificationClicked(this)
                } else {
                    NotificationDecider.onOrganicAppOpen(this) // other notification consider as organic open
                }
            }
        } else {
            if(intent.getBooleanExtra("${packageName}.isFromUninstall", false)) {
                NotificationDecider.onOpenAppFromUninstall(this)
            } else {
                NotificationDecider.onOrganicAppOpen(this)
            }
        }


//        ads_inter_id = if(intent.getBooleanExtra("${packageName}.isFromUninstall", false)) {
//            getString(R.string.inter_splash_uninstall)
//        } else {
//            getString(R.string.inter_splash)
//        }
    }

    private suspend fun initBillingAndAwait(): Unit = suspendCancellableCoroutine { cont ->
        val startTimeT = System.currentTimeMillis()
        Log.i("SplashActivity", "initAndRegister called at $startTimeT ms")

        // Keep a reference to the handler so we can unregister if cancelled
        val handler = object : BillingProcessor.IBillingHandler {
            override fun onProductPurchased(productId: String, details: PurchaseInfo?) { /*--*/ }
            override fun onPurchaseHistoryRestored() { /*--*/ }

            override fun onBillingError(errorCode: Int, error: Throwable?) {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onBillingInitialized() {
                val elapsed = System.currentTimeMillis() - startTimeT
                Log.i("SplashActivity", "Billing initialized in $elapsed ms")
                // Optionally load purchases
                IAPUtils.loadOwnedPurchasesFromGoogleAsync { success ->
                    Log.i("SplashActivity", "loadOwnedPurchasesFromGoogleAsync: $success")
                    // resume the coroutine
                    FCMTopicHandler.resetFCMTopic(this@SplashActivity)
                    if (cont.isActive) cont.resume(Unit)
                }
                // unregister listener right away
                IAPUtils.unregisterListener(this)
            }
        }

        // Kick off billing init
        IAPUtils.initAndRegister(this@SplashActivity, AppUtils.PUBLIC_LICENSE_KEY, handler)

        // Clean up if the coroutine is cancelled
        cont.invokeOnCancellation {
            Log.w("SplashActivity", "Billing init coroutine cancelled")
            IAPUtils.unregisterListener(handler)
        }
    }

    override fun initData() {
        // reset consent flag
        TemporaryStorage.isObtainConsent = false
        lifecycleScope.launch {
            try {
                coroutineScope {
                    val startTime = System.currentTimeMillis()
                    Log.i("SplashActivity", "Splash flow start at $startTime ms")
                    val loadFileDeferred = async { migrateFileDataAndHandleIntentOpeningFile() }
                 //   val loadInterAdDeferred   = async { loadInterstitialAd(ads_inter_id) }
                    val billingAndAdsDeferred = async {
                        initBillingAndAwait()
                        when (FirebaseRemoteConfigUtil.getInstance().getTypeOfStartUp()) {
                            FirebaseRemoteConfigUtil.Companion.StartUpType.ADS_OPEN_IAP_LANGUAGE.value -> {
                                loadAppOpenAds()
                            }
                            FirebaseRemoteConfigUtil.Companion.StartUpType.IAP_ADS_INTER_LANGUAGE.value -> {
                                TemporaryStorage.interAdPreloaded = loadInterstitialAd(getString(R.string.inter_splash))
                            }
                            FirebaseRemoteConfigUtil.Companion.StartUpType.ADS_OPEN_LANGUAGE.value -> {
                                loadAppOpenAds()
                            }
                            else -> {
                                loadAppOpenAds()
                            }
                        }
                    }

                    val granted: Boolean = requestNotificationPermissionSuspend()
                    if (granted) onNotificationPermissionGranted() else onNotificationPermissionDenied()


                    loadFileDeferred.await()
                    billingAndAdsDeferred.await()

                    Log.i("SplashActivity", "Splash flow completed in ${System.currentTimeMillis() - startTime} ms")
                    //showAdsInterstitial(interstitialAd)
                    when (FirebaseRemoteConfigUtil.getInstance().getTypeOfStartUp()) {
                        FirebaseRemoteConfigUtil.Companion.StartUpType.ADS_OPEN_IAP_LANGUAGE.value -> {
                            showAdsOpenAndPreLoadNativeAds()
                        }
                        FirebaseRemoteConfigUtil.Companion.StartUpType.IAP_ADS_INTER_LANGUAGE.value -> {
                            navigateToNextScreen()
                        }
                        FirebaseRemoteConfigUtil.Companion.StartUpType.ADS_OPEN_LANGUAGE.value -> {
                            showAdsOpenAndPreLoadNativeAds()
                        }
                        else -> {
                            showAdsOpenAndPreLoadNativeAds()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error in splash flow", e)
                // handle errors if either task failed
                navigateToNextScreen()
            }
        }
        if (isAcceptManagerStorage()) {
            viewModel.migrateFileDataViewModelScope()
        }
    }

    private fun showAdsOpenAndPreLoadNativeAds() {
        showAdsOpen()
        val isFistTimeOpenApp = PreferencesUtils.getBoolean(PresKey.GET_START, true)
        Log.i("SplashActivity", "isFistTimeOpenApp = $isFistTimeOpenApp")
        if (isFistTimeOpenApp) {
            // Preload native ad for language screen
            preloadLanguageNativeAd()
        }
    }

    private suspend fun  handleIntentOpeningFile() {
        if(intent.getBooleanExtra("${this.packageName}.isToSetDefaultReader", false)) {
            Log.i("MainActivity", "isToSetDefaultReader OK")
            intent.removeExtra("${this.packageName}.isToSetDefaultReader")
            intent?.data = null
            return
        }

        when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }

            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.data = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.get(0)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.get(0)
                })


            }
        }


        intent.data?.let {
            if (!isAcceptManagerStorage()) {
                this@SplashActivity.intent.putExtra("${this@SplashActivity.packageName}.uriToOpen",true);
                intent.data = null
                intent.removeExtra("${this@SplashActivity.packageName}.uriToOpen")
                toast(resources.getString(R.string.accept_all_file_permission_edit2))
                return
            }
            val path = viewModel.getPathFromUri(it)
            if (path.isNotEmpty()) {
                val checkIfFileExistInDb = viewModel.checkIfOurAppRecognizeThisFile(path)
                if (checkIfFileExistInDb) {
                    Log.d("File", "Path: From DB")
                    val fileModel = viewModel.getFileModelByPath(path)
                    this@SplashActivity.intent.putExtra("${this@SplashActivity.packageName}.fileModelToOpen",fileModel);
                } else {
                    val internalFile = viewModel.importUriToDownloadAllPDFTripSoft(it)
                    if (internalFile != null) {
                        this@SplashActivity.intent.putExtra("${this@SplashActivity.packageName}.fileModelToOpen",internalFile);
                    } else {
                        toast(resources.getString(R.string.cant_open_file))
                    }
                }
            } else {
             //   val pathFromDict = viewModel.searchFileFromDict(it)
                if (false) { // temporary comment searchFileFromDict because same file name are not trusted
                    Log.d("File", "Path: From dict")
//                    Log.d("File", "Path: From dict")
//                    val fileModel = viewModel.getFileModelByPath(pathFromDict)
//                    this@SplashActivity.intent.putExtra("${this@SplashActivity.packageName}.fileModelToOpen",fileModel);
                } else {
                    try {
                        val internalFile = viewModel.importUriToDownloadAllPDFTripSoft(it)
                        if (internalFile != null) {
                            this@SplashActivity.intent.putExtra("${this@SplashActivity.packageName}.fileModelToOpen",internalFile);
                        } else {
                            toast(resources.getString(R.string.cant_open_file))
                            // openFile(it)
                        }
                    } catch (_: SecurityException) {
                        toast(resources.getString(R.string.cant_open_file))
                    }
                }
            }
        } ?: run {
            Log.d("File", "No data in intent")
        }
    }


    private suspend fun migrateFileDataAndHandleIntentOpeningFile() {
        if (isAcceptManagerStorage()) {
//            viewModel.migrateFileData()
        } else {
            viewModel.addSameFilesInternal()
            Log.w("SplashActivity", "Skipping migration, no storage permission")
        }
        // Now switch back to Main for intent handling / toast / UI ops:
        withContext(Dispatchers.Main) {
            handleIntentOpeningFile()
        }
    }

    override fun initListener() {

    }

    private fun navigateToNextScreen() {

        if (FirebaseRemoteConfigUtil.getInstance().getTypeOfStartUp() == FirebaseRemoteConfigUtil.Companion.StartUpType.ADS_OPEN_IAP_LANGUAGE.value) {
            if (!IAPUtils.isPremium() && BillingProcessor.isIabServiceAvailable(this)) {
                intent.apply {
                    putExtra("${packageName}.isFromSplash", true)
                }
                when (FirebaseRemoteConfigUtil.getInstance().getIapScreenType()) {
                    0 -> IapActivityV2.start(this)
                    1 -> IapActivity.start(this)
                    else -> IapActivityV2.start(this)
                }
                finish()
                return
            }
        }

        if (PreferencesUtils.getBoolean(PresKey.GET_START, true)) {
            TemporaryStorage.shouldLoadAdsLanguageScreen = true
            LanguageActivity.start(this@SplashActivity)
            //            logEvent("splash_to_language")
            finish()
        } else {
            Log.d("SplashActivity", "isFromWidget = ${this@SplashActivity.intent.getBooleanExtra("${this@SplashActivity.packageName}.isFromWidget", false)}")
            Log.d("SplashActivity", "isFromNotification = ${this@SplashActivity.intent.getBooleanExtra("${this@SplashActivity.packageName}.isFromNotification", false)}")
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                FirebaseRemoteConfigUtil.getInstance().isAlwaysRequestNotiWhenEnterApp()
            ) {
                Log.i("SplashActivity", "Notification permission not granted, go to Noti request")
                RequestNotificationPermissionActivity.start(this)
        } else {
                MainActivity.start(this@SplashActivity)

            }
            finish()
        }
    }

//    private fun loadAdsAndGoToNextScreen() {
//        val timeStart = System.currentTimeMillis()
//        val consentHelper = ConsentHelper.getInstance(this.applicationContext)
//        if (!consentHelper.canLoadAndShowAds()) consentHelper.reset()
//
//        consentHelper.obtainConsentAndShow(this@SplashActivity) {
//            if (SystemUtils.isInternetAvailable(this)) {
//                interCallback = object : AdCallback() {
//                    override fun onAdLoaded() {
//                        super.onAdLoaded()
//                        Log.i("SplashActivity", "Interstitial ad loaded in ${System.currentTimeMillis() - timeStart} ms")
//                    }
//                    override fun onNextAction() {
//                        super.onNextAction()
//                        Admob.getInstance().setOpenActivityAfterShowInterAds(true)
//                        navigateToNextScreen()
//                    }
//                }
//                Admob.getInstance().setOpenActivityAfterShowInterAds(false)
//                Admob.getInstance().loadSplashInterAds2(this@SplashActivity, ads_inter_id, 100, interCallback)
//                Log.i("SplashActivity", "Interstitial ad loaded")
//            } else {
//                Log.e("SplashActivity", "No internet connection, skipping native ad preload for language screen")
//                navigateToNextScreen()
//            }
//        }
//    }

    private fun preloadLanguageNativeAd() {
        Log.i("SplashActivity", "starting native ad preload for language screen")
        if (IAPUtils.isPremium()) {
            Log.w("SplashActivity", "Skipping native ad preload for language screen due to premium")
            return
        }
        if (!FirebaseRemoteConfigUtil.getInstance().isPreloadNativeLanguage()) {
            Log.w("SplashActivity", "Skipping native ad preload for language screen due to remote config off")
            return
        }
        TemporaryStorage.nativeAdPreload = null
        TemporaryStorage.isLoadingNativeAdsLanguage = true
        val startupTime = System.currentTimeMillis()
        val callBack = object : NativeCallback() {
            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                super.onNativeAdLoaded(nativeAd)
                Log.i("SplashActivity", "Native ad loaded for language loaded in ${System.currentTimeMillis() - startupTime} ms")
                TemporaryStorage.isLoadingNativeAdsLanguage = false
                TemporaryStorage.nativeAdPreload = nativeAd
                TemporaryStorage.callbackNativeAdsLanguage.invoke(nativeAd)
            }

            override fun onAdFailedToLoad() {
                super.onAdFailedToLoad()
                Log.e("SplashActivity", "Failed to load native ad for language screen")
                TemporaryStorage.isLoadingNativeAdsLanguage = false
                TemporaryStorage.nativeAdPreload = null
                TemporaryStorage.callbackNativeAdsLanguage.invoke(null)
            }
        }

        Admob.getInstance().loadNativeAd(
            applicationContext,
            getString(R.string.native_language),
            callBack
        )
    }


    private suspend fun preloadLanguageNativeAdSuspend(): NativeAd? =
        withTimeoutOrNull(FirebaseRemoteConfigUtil.getInstance().getTimeoutLoadInterMillisecond()) {
            suspendCancellableCoroutine { cont ->
                val startupTime = System.currentTimeMillis()
                Log.i("SplashActivity", "Starting native ad preload for language screen")

                val callback = object : NativeCallback() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                        super.onNativeAdLoaded(nativeAd)
                        Log.i(
                            "SplashActivity",
                            "Native ad loaded in ${System.currentTimeMillis() - startupTime} ms"
                        )
                        if (cont.isActive) cont.resume(nativeAd)
                    }

                    override fun onAdFailedToLoad() {
                        super.onAdFailedToLoad()
                        Log.e("SplashActivity", "Failed to load native ad")
                        if (cont.isActive) cont.resume(null)
                    }
                }

                Admob.getInstance().loadNativeAd(
                    applicationContext,
                    getString(R.string.native_language),
                    callback
                )

                cont.invokeOnCancellation {
                    Log.w("SplashActivity", "Native ad preload coroutine cancelled or timed out")
                    // If you have any explicit cancel/unregister API, call it here
                    TemporaryStorage.nativeAdPreload = null
                }
            }
        }?.also {
            // If non-null, optionally store it:
            TemporaryStorage.nativeAdPreload = it
        } ?: run {
            Log.w("SplashActivity", "Native ad preload timed out or failed")
            TemporaryStorage.nativeAdPreload = null
            null
        }


    override fun viewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(LayoutInflater.from(this))
    }

    private var pendingPermissionContinuation: ((Boolean) -> Unit)? = null

    private suspend fun requestNotificationPermissionSuspend(): Boolean =
        suspendCancellableCoroutine { cont ->

            // Already granted?
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                cont.resume(true)
                return@suspendCancellableCoroutine
            }


            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                cont.resume(false)
                return@suspendCancellableCoroutine
            }


            // Otherwise, suspend and launch the system dialog
            pendingPermissionContinuation = { granted ->
                cont.resume(granted)
            }
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

            // Clean up if the coroutine is cancelled
            cont.invokeOnCancellation {
                pendingPermissionContinuation = null
            }
        }


//    private val requestNotificationPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
//            if (isGranted) {
//                onNotificationPermissionGranted()
//            } else {
//                onNotificationPermissionDenied()
//            }
//        }

//    private fun requestNotificationPermission() {
//        Log.e("GetStartActivity", "Request notification permission SDK: ${Build.VERSION.SDK_INT}")
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ContextCompat.checkSelfPermission(this,
//                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
//            } else {
//                Log.d("MainActivity", "Notification permission granted, no need to request")
//                onNotificationPermissionGranted()
//            }
//        } else {
//            Log.d("MainActivity", "Notification permission not required for SDK < 33")
//            if (ContextCompat.checkSelfPermission(this,
//                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                onNotificationPermissionDenied()
//            } else {
//                onNotificationPermissionGranted()
//            }
//        }
//    }

    private fun onNotificationPermissionGranted() {
        PreferencesUtils.putBoolean("NOTIFICATION", true)
        Log.d("MainActivity", "Notification permission granted")
            try {
                startForegroundService( Intent(this, NotificationForegroundService::class.java))
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error starting service: ${e.message}")
            }
//        loadAdsAndGoToNextScreen()
//        val isFistTimeOpenApp = PreferencesUtils.getBoolean(PresKey.GET_START, true)
//        Log.i("SplashActivity", "Notification permission granted, isFistTimeOpenApp = $isFistTimeOpenApp")
//        if (isFistTimeOpenApp) {
//            preloadLanguageNativeAd()
//        }
    }

    private fun onNotificationPermissionDenied() {
        // Show a message or guide the user to settings
        //Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        PreferencesUtils.putBoolean("NOTIFICATION", false)
        PreferencesUtils.putInteger("NOTIF_DENIAL_COUNT",
            PreferencesUtils.getInteger("NOTIF_DENIAL_COUNT", 0) + 1
        )
//        if (!isServiceRunning(this, NotificationForegroundService::class.java))
            try {
                startForegroundService( Intent(this, NotificationForegroundService::class.java))
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error starting service: ${e.message}")
            }
        Log.e("MainActivity", "Notification permission denied")
//        loadAdsAndGoToNextScreen()
    }
}