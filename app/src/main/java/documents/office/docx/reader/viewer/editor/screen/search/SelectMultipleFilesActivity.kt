package documents.office.docx.reader.viewer.editor.screen.search

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.ezteam.baseproject.utils.IAPUtils
import com.ezteam.baseproject.utils.PDFConstants.Companion.ADS_ITEM_INDEX
import com.ezteam.baseproject.utils.SystemUtils
import com.ezteam.baseproject.utils.TemporaryStorage
import documents.office.docx.reader.viewer.editor.R
import documents.office.docx.reader.viewer.editor.adapter.FileItemAdapter
import documents.office.docx.reader.viewer.editor.common.FileTab
import documents.office.docx.reader.viewer.editor.common.FunctionState
import documents.office.docx.reader.viewer.editor.databinding.ActivityCheckFileBinding
import documents.office.docx.reader.viewer.editor.model.FileModel
import documents.office.docx.reader.viewer.editor.screen.base.PdfBaseActivity
import documents.office.docx.reader.viewer.editor.screen.func.BottomSheetFileFunction
import documents.office.docx.reader.viewer.editor.screen.main.MainViewModel
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import documents.office.docx.reader.viewer.editor.screen.base.CurrentStatusAdsFiles
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Locale

class SelectMultipleFilesActivity : PdfBaseActivity<ActivityCheckFileBinding>() {
    private val viewModel by inject<MainViewModel>()
    private lateinit var adapter: FileItemAdapter
    private var fileTab: FileTab = FileTab.ALL_FILE
    companion object {
        fun start(activity: FragmentActivity, fileTab: FileTab) {
            val intent = Intent(activity, SelectMultipleFilesActivity::class.java)
            intent.putExtra("FileTab", fileTab)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        EzAdControl.getInstance(this).showAds()
    }

    override fun onStart() {
        super.onStart()
        loadNativeNomedia()
        loadNativeAdsMiddleFiles()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    val callback = object : NativeCallback() {
        override fun onNativeAdLoaded(nativeAd: NativeAd?) {
            if (nativeAd != null) {
                this@SelectMultipleFilesActivity.onNativeAdLoaded(nativeAd)
            } else {
                this@SelectMultipleFilesActivity.onAdFailedToLoad()
            }
        }
        override fun onAdFailedToLoad() {
            this@SelectMultipleFilesActivity.onAdFailedToLoad()
        }
    }

    private fun loadNativeNomedia() {
        if (IAPUtils.isPremium()) {
            binding.layoutNative.visibility = View.GONE
            return
        }
        if (SystemUtils.isInternetAvailable(this)) {
            binding.layoutNative.visibility = View.VISIBLE
            val loadingView = LayoutInflater.from(this)
                .inflate(R.layout.ads_native_loading_short, null)
            binding.layoutNative.removeAllViews()
            binding.layoutNative.addView(loadingView)

            val callback = object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)

                    val layoutRes = R.layout.ads_native_bot_no_media_short
                    val adView = LayoutInflater.from(this@SelectMultipleFilesActivity)
                        .inflate(layoutRes, null) as NativeAdView

                    binding.layoutNative.removeAllViews()
                    binding.layoutNative.addView(adView)

                    // Gán dữ liệu quảng cáo vào view
                    Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    binding.layoutNative.visibility = View.GONE
                }
            }

            Admob.getInstance().loadNativeAd(
                applicationContext,
                getString(R.string.native_bot_selectfiles),
                callback
            )
        } else {
            binding.layoutNative.visibility = View.GONE
        }
    }
    private fun loadNativeAdsMiddleFiles() {
        if (!IAPUtils.isPremium() && SystemUtils.isInternetAvailable(this)) {
            viewModel.updateAdsFilesStatus(CurrentStatusAdsFiles(true, null))
            val callback = object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    if (nativeAd != null) {
                        viewModel.updateAdsFilesStatus(CurrentStatusAdsFiles(true, nativeAd))
                    } else {
                        onAdFailedToLoad()
                    }
                }

                override fun onAdFailedToLoad() {
                    viewModel.updateAdsFilesStatus(CurrentStatusAdsFiles(false, null))
                }
            }

            Admob.getInstance().loadNativeAd(
                applicationContext,
                getString(R.string.native_between_files_selectfiles),
                callback
            )
        } else {
            viewModel.updateAdsFilesStatus(CurrentStatusAdsFiles(false, null))
        }
    }
    override fun initView() {
        adapter = FileItemAdapter(this, mutableListOf(), ::onItemClick, ::onSelectedFunc, ::onReactFavorite)
        adapter.toggleCheckMode(true)
        binding.rcvListFile.adapter = adapter
        updateNavMenuState(false)
        adapter.onSelectedCountChangeListener = { count ->
            var message = String.format(resources.getString(R.string.files_selected), count)
            binding.toolbar.tvSelectedCount.text = message

            val enabled = count > 0
            updateNavMenuState(enabled)
            binding.toolbar.ivCheck.isSelected = count == adapter.itemCount
        }
        if (Locale.getDefault().language == "ar") {
            binding.toolbar.ivBack.rotationY = 180f
        } else {
            binding.toolbar.ivBack.rotationY = 0f
        }
    }

    override fun initData() {
        lifecycleScope.launch {
            fileTab = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra("FileTab", FileTab::class.java)!!
            } else {
                (intent.getSerializableExtra("FileTab") as? FileTab)!! // Add a safe cast for older versions
            }
            viewModel.getListFileBaseOnFileTab(fileTab).observe(this@SelectMultipleFilesActivity) {
                adapter.setList(it)
//                 if((it.size == 1 || (it.isNotEmpty() &&  !adapter.getList()[ADS_ITEM_INDEX].isAds))) // ads at index 0
//                     adapter.addAds( FileModel().apply { isAds = true }, ADS_ITEM_INDEX) // ads at index 0
                adapter.notifyDataSetChanged()

                if (it.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.animationView.playAnimation()
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.animationView.cancelAnimation()
                }
            }
        }
    }

    override fun initListener() {
        binding.toolbar.ivBack.setOnClickListener {
            finish()
        }

        binding.toolbar.ivCheck.setOnClickListener {
            it.isSelected = !it.isSelected
            if (it.isSelected) {
                adapter.selectAll()
            } else {
                adapter.deselectAll()
            }
        }

        binding.navView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.share -> {
                    val selectedFiles = adapter.getSelectedFiles()
                    if (selectedFiles.isNotEmpty()) {
                        shareFiles(selectedFiles)
                    } else {
                        Toast.makeText(this, getString(R.string.please_choose_file), Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.delete -> {
                    val selectedFiles = adapter.getSelectedFiles()
                    if (selectedFiles.isNotEmpty()) {

                        showDialogConfirm(
                            resources.getString(R.string.delete),
                            getString(R.string.delete_all)
                        ) {
                            viewModel.deleteFiles(selectedFiles) {
                                toast(resources.getString(R.string.delete_successfully))
                            }
                            adapter.deselectAll()
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.please_choose_file), Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }

    }
    private fun toggleSelectAll() {
        adapter.selectAll()
    }
    override fun onResume() {
        super.onResume()
        if (binding.layoutNative.childCount == 0) {
            loadNativeNomedia()
        }
    }
    private fun updateNavMenuState(enabled: Boolean) {
        val menu = binding.navView.menu
        val colorEnabled = resources.getColor(R.color.text, theme)
        val colorDisabled = resources.getColor(R.color.cancel, theme)

        val shareItem = menu.findItem(R.id.share)
        val deleteItem = menu.findItem(R.id.delete)

        shareItem.isEnabled = enabled
        deleteItem.isEnabled = enabled

        val shareIcon = shareItem.icon
        val deleteIcon = deleteItem.icon
        shareIcon?.setTint(if (enabled) colorEnabled else colorDisabled)
        deleteIcon?.setTint(if (enabled) colorEnabled else colorDisabled)
    }


    private fun onItemClick(fileModel: FileModel) {
//        PdfViewerActivity.start(this, fileModel)
//        PdfDetailActivity.start(this, fileModel.path, fileModel.isFavorite, fileModel.isReadDone)
        openFile(fileModel)
    }
    private fun onReactFavorite(fileModel: FileModel) {
        fileModel.isFavorite = !fileModel.isFavorite
        viewModel.reactFavorite(fileModel)
    }
    private fun onSelectedFunc(fileModel: FileModel) {
        val bottomSheetFileFunction =
            BottomSheetFileFunction(fileModel, FileTab.ALL_FILE) {
                onSelectedFunction(fileModel, it)
            }
        bottomSheetFileFunction.show(
            supportFragmentManager,
            BottomSheetFileFunction::javaClass.name
        )
    }

    private fun onSelectedFunction(fileModel: FileModel, state: FunctionState) {
        when (state) {
            FunctionState.SHARE -> {
                shareFile(fileModel)
            }

            FunctionState.FAVORITE -> {
                fileModel.isFavorite = !fileModel.isFavorite
                viewModel.reactFavorite(fileModel)
            }


            FunctionState.RENAME -> {
                fileModel.name?.let {
                    showRenameFile(it) { newName ->
                        viewModel.renameFile(fileModel, newName, onFail = {
                            toast(resources.getString(R.string.rename_unsuccessful))
                        })
                    }
                }
            }


            FunctionState.DELETE -> {
                showDialogConfirm(
                    resources.getString(R.string.delete),
                    String.format(resources.getString(R.string.del_message), fileModel.name)
                ) {
                    viewModel.deleteFile(fileModel) {
                        toast(resources.getString(R.string.delete_successfully))
                    }
                }
            }

            FunctionState.DETAIL -> {
                showDetailFile(fileModel)
            }

            else -> {}
        }
    }

    override fun viewBinding(): ActivityCheckFileBinding {
        return ActivityCheckFileBinding.inflate(LayoutInflater.from(this))
    }




     fun onNativeAdLoaded(nativeAd: NativeAd?) {
        if (::adapter.isInitialized) {
            adapter.nativeAd = nativeAd
            adapter.notifyItemChanged(ADS_ITEM_INDEX)
        }
    }

     fun onAdFailedToLoad() {
        if (::adapter.isInitialized) {
            if (adapter.getList().size > ADS_ITEM_INDEX && adapter.getList()[ADS_ITEM_INDEX].isAds) {
                adapter.getList().removeAt(ADS_ITEM_INDEX)
                adapter.notifyItemRemoved(ADS_ITEM_INDEX)
            }
        }
    }
}