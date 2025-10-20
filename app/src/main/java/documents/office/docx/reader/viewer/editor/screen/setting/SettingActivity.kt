package documents.office.docx.reader.viewer.editor.screen.search
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.SpannableString
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.ezteam.baseproject.extensions.launchActivity
import com.ezteam.baseproject.utils.IAPUtils
import com.ezteam.baseproject.utils.PathUtils
import com.ezteam.baseproject.utils.PreferencesUtils
import com.ezteam.baseproject.utils.TemporaryStorage
import com.nlbn.ads.util.AppOpenManager
import documents.office.docx.reader.viewer.editor.R
import documents.office.docx.reader.viewer.editor.common.FunctionState
import documents.office.docx.reader.viewer.editor.databinding.ActivitySettingsBinding
import documents.office.docx.reader.viewer.editor.dialog.AboutUsDialog
import documents.office.docx.reader.viewer.editor.dialog.AddToHomeDialog
import documents.office.docx.reader.viewer.editor.dialog.DefaultReaderRequestDialog
import documents.office.docx.reader.viewer.editor.dialog.DefaultReaderUninstallDialog
import documents.office.docx.reader.viewer.editor.screen.setting.RateUsDialog
import documents.office.docx.reader.viewer.editor.screen.PolicyActivity
import documents.office.docx.reader.viewer.editor.screen.TermAndConditionsActivity
import documents.office.docx.reader.viewer.editor.screen.base.PdfBaseActivity
import documents.office.docx.reader.viewer.editor.screen.create.BottomSheetCreatePdf
import documents.office.docx.reader.viewer.editor.screen.create.CreateSuccessActivity
import documents.office.docx.reader.viewer.editor.screen.iap.IapActivity
import documents.office.docx.reader.viewer.editor.screen.iap.IapActivityV2
import documents.office.docx.reader.viewer.editor.screen.language.LanguageActivity
import documents.office.docx.reader.viewer.editor.screen.main.MainActivity.Companion.CODE_ACTION_OPEN_DOCUMENT_FILE
import documents.office.docx.reader.viewer.editor.screen.main.MainActivity.Companion.CODE_CHOOSE_IMAGE
import documents.office.docx.reader.viewer.editor.screen.main.MainViewModel
import documents.office.docx.reader.viewer.editor.screen.overlay.ClearDefaultReaderOverlayActivity
import documents.office.docx.reader.viewer.editor.screen.reloadfile.FeatureRequestActivity
import documents.office.docx.reader.viewer.editor.utils.FileSaveManager
import documents.office.docx.reader.viewer.editor.utils.FirebaseRemoteConfigUtil
import documents.office.docx.reader.viewer.editor.utils.createPdf.OnPDFCreatedInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

class SettingActivity : PdfBaseActivity<ActivitySettingsBinding>() {
    private val viewModel by inject<MainViewModel>()

    private val TAG = "SettingActivity"
    companion object {
        fun start(activity: FragmentActivity) {
            val intent = Intent(activity, SettingActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onStart() {
        super.onStart()
        applyKeepScreenOnState()
        //loadNativeNomedia()
        loadSavedSwitchState()
       // checkNotificationState()
    }

    private fun checkFeatureRequestToShowUI() {
        binding.funcFeatureRequest.visibility = View.VISIBLE
    }
    private fun showAdsOr(action: () -> Unit) {
        if (FirebaseRemoteConfigUtil.getInstance().isShowAdsMain()) {
            showAdsInterstitial(R.string.inter_home) {
                action()
            }
        } else {
            action()
        }
    }
    override fun initView() {
        val isNotificationEnabled = PreferencesUtils.getBoolean("NOTIFICATION", false)
        binding.switchNotifications.isChecked = isNotificationEnabled
        binding.funcNotification.visibility = View.GONE
        val DefaultWordViewerResolveInfo = getDefaultWordViewerClass()
        if (DefaultWordViewerResolveInfo?.activityInfo?.name?.contains(packageName) == true) {
            binding.funcSetDefault.visibility = View.GONE
        }
        binding.tvIapTitle.text = getString(R.string.uprange_to_premium)
        checkFeatureRequestToShowUI()
    }
    override fun initData() {
        val textView = findViewById<TextView>(R.id.tv_iap_offer)
        val text = getString(R.string.see_full_offer)
        val spannable = SpannableString("\uD83D\uDD25\u00A0$text")

//        val drawable = ContextCompat.getDrawable(this, R.drawable.icon_small_fire)
//        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
//
//        val imageSpan = drawable?.let { ImageSpan(it, ImageSpan.ALIGN_BOTTOM) }
//        spannable.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // đặt ImageSpan tại vị trí 0

        textView.text = spannable
    }
    private fun browserFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("*/*")
        intent.putExtra(
            Intent.EXTRA_MIME_TYPES, arrayOf( // open the mime-types we know about
                "application/pdf",
                "application/vnd.ms-xpsdocument",
                "application/oxps",
                "application/x-cbz",
                "application/vnd.comicbook+zip",
                "application/epub+zip",
                "application/x-fictionbook",
                "application/x-mobipocket-ebook",
                "application/octet-stream",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
                "application/msword", // .doc
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
                "application/vnd.ms-excel", // .xls
                "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
                "application/vnd.ms-powerpoint" // .ppt
            )
        )
        startActivityForResult(intent, CODE_ACTION_OPEN_DOCUMENT_FILE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == CODE_ACTION_OPEN_DOCUMENT_FILE) {
                data?.data?.let {
                    val path = viewModel.getPathFromUri(it)
                    if (path.isNotEmpty()) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val rs = viewModel.checkIfOurAppRecognizeThisFile(path)
                            if (rs) {
                                val fileModel = viewModel.getFileModelByPath(path)
                                openFileFromSplash(fileModel)
                            } else {
                                val fileModel = viewModel.importUriToDownloadAllPDFTripSoft(it)
                                if (fileModel != null) {
                                    openFileFromSplash(fileModel)
                                } else {
                                    toast(getString(R.string.cant_open_file))
                                }
                            }
                        }
                    } else {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val fileModel = viewModel.importUriToDownloadAllPDFTripSoft(it)
                            if (fileModel != null) {
                                openFileFromSplash(fileModel)
                            } else {
                                toast(getString(R.string.cant_open_file))
                            }
                        }
                    }
                }
            } else if (requestCode == CODE_CHOOSE_IMAGE) {
                data?.let {
                    val lstData = ArrayList<String>()
                    it.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            val item = clipData.getItemAt(i)
                            val realPath = PathUtils.getPath(this, item.uri)
                            realPath?.let {
                                if (realPath.isNotEmpty() && File(realPath).exists()) {
                                    lstData.add(item.uri.toString())
                                }
                            }
                        }
                    } ?: it.data?.let { data ->
                        if (File(PathUtils.getRealPathFromUri(this, data)).exists()) {
                            lstData.add(data.toString())
                        }
                    }

                    if (lstData.size > 0) {
                        showPopupCreatePdf(lstData)
                    }
                }
            }
        }
    }
    private fun getDefaultWordViewerClass(): ResolveInfo? {
        val fileName = "file_example_DOCX.docx"
        val assetManager = assets
        val file = File(File(filesDir, "defaultFiles").apply { mkdirs() }, fileName)

        // Copy file từ assets nếu chưa tồn tại
        if (!file.exists()) {
            try {
                assetManager.open(fileName).use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }

        val uri = Uri.fromFile(file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        Log.d("DefaultReader", "resolveInfo: $resolveInfo")

        return resolveInfo
    }
    private fun showPopupCreatePdf(lstUri: ArrayList<String>) {
        val bottomSheetCreatePdf = BottomSheetCreatePdf(complete = { fileName, password,size ->
            showHideLoading(true)
            createPdf(lstUri, fileName, password,size, object : OnPDFCreatedInterface {
                override fun onPDFCreationStarted() {

                }

                override fun onPDFCreated(success: Boolean, path: String?) {
                    Log.d("File create", path ?: "")
                    path?.let {
                        val uriFile = FileSaveManager.saveFileStorage(
                            this@SettingActivity,
                            path
                        )
                        uriFile?.let {
                            val realPath =
                                PathUtils.getRealPathFromUri(this@SettingActivity, it)
                            viewModel.createFile(realPath) {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    CreateSuccessActivity.start(
                                        this@SettingActivity,
                                        it,
                                        lstUri.size,
                                        lstUri[0]
                                    )
                                }
                            }
                        } ?: {
                            toast(getString(R.string.app_error))
                        }
                    }

                    showHideLoading(false)
                }
            })
        })
        bottomSheetCreatePdf.show(supportFragmentManager, BottomSheetCreatePdf::javaClass.name)
    }
    private fun onSelectedFunction(state: FunctionState) {
        when (state) {
            FunctionState.BROWSE_FILE -> {
                AppOpenManager.getInstance().disableAppResume()
                browserFile()
            }

            FunctionState.RATE_US -> {
                AppOpenManager.getInstance().disableAppResume()
                val rateUsDialog = RateUsDialog();
                try {
                    rateUsDialog.show(this.supportFragmentManager, "RateUsDialog")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SettingActivity", "Error showing RateUsDialog: ${e.message}", e)
                }
            }

            FunctionState.FEEDBACK -> {
                launchActivity<FeedBackActivity> { }
            }

            FunctionState.SHARE_APP -> {
              // shareApp()
            }

            FunctionState.PRIVACY_POLICY -> {
                PolicyActivity.start(this)
            }

            FunctionState.CHANGE_LANGUAGE -> {
                changeLanguage()
            }
            else -> {
            }
        }
    }

    override fun initListener() {
        
        binding.layoutIap.setOnClickListener {
            when (FirebaseRemoteConfigUtil.getInstance().getIapScreenType()) {
                0 -> IapActivityV2.start(this)
                1 -> IapActivity.start(this)
                else -> IapActivityV2.start(this)
            }
        }
        checkFeatureRequestToShowUI()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                binding.funcBrowseMoreFiles.visibility = View.VISIBLE
            } else {
                binding.funcBrowseMoreFiles.visibility = View.GONE
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE )
                != PackageManager.PERMISSION_GRANTED) {
                binding.funcBrowseMoreFiles.visibility = View.GONE
            } else {
                binding.funcBrowseMoreFiles.visibility = View.VISIBLE
            }
        }

        binding.funcBrowseMoreFiles.setOnClickListener {
            onSelectedFunction(FunctionState.BROWSE_FILE)
        }

        binding.funcRateUs.setOnClickListener {
            onSelectedFunction(FunctionState.RATE_US)
        }
        binding.funcSendFeedback.setOnClickListener {
            onSelectedFunction(FunctionState.FEEDBACK)
        }

        binding.funcShare.setOnClickListener {
            onSelectedFunction(FunctionState.SHARE_APP)
        }

        binding.funcPrivacy.setOnClickListener {
            onSelectedFunction(FunctionState.PRIVACY_POLICY)
        }

        binding.funcChangeLanguage.setOnClickListener {
            onSelectedFunction(FunctionState.CHANGE_LANGUAGE)
            TemporaryStorage.shouldLoadAdsLanguageScreen = true
        }

        binding.funcTerms.setOnClickListener {
            TermAndConditionsActivity.start(this)
        }
        binding.funcFeatureRequest.setOnClickListener {
            FeatureRequestActivity.start(this)
        }

        binding.funcAboutUs.setOnClickListener {
            val dialog = AboutUsDialog();
            try {
                dialog.show(this.supportFragmentManager, "AboutUsDialog")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SettingActivity", "Error showing AboutUsDialog: ${e.message}", e)
            }
        }

        binding.funcAddWidget.setOnClickListener {
            TemporaryStorage.isShowedAddToHoneDialog = true
            val dialog = AddToHomeDialog();
            try {
                dialog.show(this.supportFragmentManager, "AddToHomeRequestDialog")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SettingActivity", "Error showing AddToHomeRequestDialog: ${e.message}", e)
            }
        }
        binding.funcSetDefault.setOnClickListener {
            val defaultWordViewerResolveInfo = getDefaultWordViewerClass()
            Log.i("DefaultReader", "DefaultWordViewer: $defaultWordViewerResolveInfo")
            if (defaultWordViewerResolveInfo?.activityInfo == null || defaultWordViewerResolveInfo.activityInfo.name.contains("internal.app.ResolverActivity")) { // default reader isn't set => show dialog to set default
                val dialog = DefaultReaderRequestDialog();
                try {
                    dialog.show(this.supportFragmentManager, "RequestDefaultReaderDialog")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SettingActivity", "Error showing RequestDefaultReaderDialog: ${e.message}", e)
                }
            } else if(!defaultWordViewerResolveInfo.activityInfo.name.contains(packageName) ) { // default reader is set but not our app => show dialog to clear default
                val fragmentManager = supportFragmentManager
                val existingDialog = fragmentManager.findFragmentByTag("DefaultReaderUninstallDialog")
                if (existingDialog == null) {
                    val dialog = DefaultReaderUninstallDialog()
                    dialog.defaultWordViewer = defaultWordViewerResolveInfo
                    dialog.listener = {
                        isGoingToSettingToClearDefault = true
                    }
                    try {
                        dialog.show(fragmentManager, "DefaultReaderUninstallDialog")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("SettingActivity", "Error showing DefaultReaderUninstallDialog: ${e.message}", e)
                    }
                }
            } else { // default reader is our app => do nothing
                Log.d("DefaultReader", "DefaultWordViewer: $defaultWordViewerResolveInfo")
            }
        }

        binding.switchKeepScreen.setOnCheckedChangeListener { _, isChecked ->
            TemporaryStorage.keepScreenOn = isChecked
            applyKeepScreenOnState()
        }

        // Switch: Push notifications
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val currentlyEnabled = PreferencesUtils.getBoolean("NOTIFICATION", false)
            val denialCount = PreferencesUtils.getInteger("NOTIF_DENIAL_COUNT", 0)

            if (!isChecked && currentlyEnabled) {
                // ON→OFF: simply disable
                PreferencesUtils.putBoolean("NOTIFICATION", false)

            } else if (isChecked && !currentlyEnabled) {
                // OFF→ON: if already denied twice, go to settings
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (denialCount >= 2) {
                        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                        waitingNotificationResult = true
                        AppOpenManager.getInstance().disableAppResume()
                    } else {
                        requestNotificationPermissionFlow()
                    }
                } else {
                    Log.d("SettingActivity", "Notification permission not required for SDK < 33")
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                        waitingNotificationResult = true
                        AppOpenManager.getInstance().disableAppResume()
                    } else {
                        onNotificationPermissionGranted()
                    }
                }
            }
        }

        // Switch: Night mode
        binding.switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            TemporaryStorage.isNightMode = isChecked
            nightModeState()
        }

        binding.ivBack.setOnClickListener {
            showAdsOr {
                finish()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermissionFlow() {
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Already have it
                PreferencesUtils.putBoolean("NOTIFICATION", true)
                PreferencesUtils.putInteger("NOTIF_DENIAL_COUNT", 0)
            }

            shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                // Show rationale, then request
                requestNotificationPermissionLauncher.launch(
                    android.Manifest.permission.POST_NOTIFICATIONS
                )

            }

            else -> {
                // First-time request
                requestNotificationPermissionLauncher.launch(
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun changeLanguage() {
        launchActivity<LanguageActivity> { }
    }

    private fun loadSavedSwitchState() {
        binding.switchKeepScreen.isChecked = TemporaryStorage.keepScreenOn
        binding.switchNightMode.isChecked = TemporaryStorage.isNightMode
    }
    private var waitingNotificationResult = false
    override fun onResume() {
        super.onResume()

        if (IAPUtils.isPremium()) {
            binding.layoutIap.visibility = View.GONE
        } else {
            binding.layoutIap.visibility = View.VISIBLE
        }
        checkFeatureRequestToShowUI()
        applyKeepScreenOnState()
        nightModeState()
        if (TemporaryStorage.isRateFullStar){
            binding.funcRateUs.visibility = View.INVISIBLE
        }
        if(waitingNotificationResult) {
            waitingNotificationResult = false
            val granted = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted && !PreferencesUtils.getBoolean("NOTIFICATION", false)) {
                // User enabled in Settings
                onNotificationPermissionGranted()
            } else if (!granted && PreferencesUtils.getBoolean("NOTIFICATION", false)) {
                // User disabled in Settings
                onNotificationPermissionDenied()
            }
        }
        val defaultWordViewerResolveInfo = getDefaultWordViewerClass()
        if(isGoingToSettingToClearDefault) {
            isGoingToSettingToClearDefault = false
            if (defaultWordViewerResolveInfo?.activityInfo == null || defaultWordViewerResolveInfo.activityInfo.name.contains("internal.app.ResolverActivity")) {// default reader isn't set => show dialog to set default
                val dialog = DefaultReaderRequestDialog();
                try {
                    dialog.show(this.supportFragmentManager, "RequestDefaultReaderDialog")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SettingActivity", "Error showing RequestDefaultReaderDialog: ${e.message}", e)
                }
            }else if(!defaultWordViewerResolveInfo.activityInfo.name.contains(packageName) ) { // default reader is set but not our app => show dialog to clear default
                val fragmentManager = supportFragmentManager
                val existingDialog = fragmentManager.findFragmentByTag("DefaultReaderUninstallDialog")
                if (existingDialog == null) {
                    val dialog = DefaultReaderUninstallDialog()
                    dialog.defaultWordViewer = defaultWordViewerResolveInfo
                    dialog.listener = {
                        isGoingToSettingToClearDefault = true
                    }
                    try {
                        dialog.show(fragmentManager, "DefaultReaderUninstallDialog")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("SettingActivity", "Error showing DefaultReaderUninstallDialog: ${e.message}", e)
                    }
                }
            } else { // default reader is our app => do nothing
                Log.d("DefaultReader", "DefaultWordViewer: $defaultWordViewerResolveInfo")
            }
        }

    }
    private fun applyKeepScreenOnState() {
        if (TemporaryStorage.keepScreenOn) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    private fun nightModeState() {
        if (TemporaryStorage.isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }


    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                onNotificationPermissionGranted()
            } else {
                onNotificationPermissionDenied()
            }
        }



    private fun onNotificationPermissionGranted() {
        Log.d("SettingActivity", "Notification permission granted")
        binding.switchNotifications.isChecked = true
        PreferencesUtils.putBoolean(
            "NOTIFICATION", true
        )
        PreferencesUtils.putInteger("NOTIF_DENIAL_COUNT", 0)
    }

    private fun onNotificationPermissionDenied() {
        binding.switchNotifications.isChecked = false
        Log.e("SettingActivity", "Notification permission denied")
        PreferencesUtils.putBoolean(
            "NOTIFICATION", false
        )
        PreferencesUtils.putInteger("NOTIF_DENIAL_COUNT",
            PreferencesUtils.getInteger("NOTIF_DENIAL_COUNT", 0) + 1
        )

    }

    override fun viewBinding(): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(LayoutInflater.from(this))
    }
    private var isGoingToSettingToClearDefault = false

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        if (isGoingToSettingToClearDefault){
            Log.i(TAG, "isGoingToSetting")
            startActivity(Intent(this@SettingActivity, ClearDefaultReaderOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_OPEN,
                    0,  // enter: instant
                    0   // exit: instant
                );
            } else {
                overridePendingTransition(0, 0);
            }
        }

    }
}
