package documents.office.docx.reader.viewer.editor.screen.func

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import documents.office.docx.reader.viewer.editor.R
import documents.office.docx.reader.viewer.editor.common.FileTab
import documents.office.docx.reader.viewer.editor.common.FunctionState
import documents.office.docx.reader.viewer.editor.databinding.SelectFileDialogBinding
import documents.office.docx.reader.viewer.editor.model.FileModel
import com.ezteam.baseproject.listener.EzItemListener
import com.ezteam.baseproject.utils.DateUtils
import com.ezteam.baseproject.utils.IAPUtils
import com.ezteam.baseproject.utils.SystemUtils
import com.ezteam.baseproject.utils.TemporaryStorage
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import java.util.Locale

class BottomSheetFileFunction(
    var fileModel: FileModel,
    var from: FileTab,
    var listener: EzItemListener<FunctionState>
) : DialogFragment() {
    private lateinit var binding: SelectFileDialogBinding
    private var isViewDestroyed = false
    private var isAdLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SelectFileDialogBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun getTheme(): Int {
        return R.style.DialogStyle
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initListener()
        isViewDestroyed = false
        if (TemporaryStorage.isLoadAds) {
            loadNativeNomedia()
        } else {
            Log.d("BottomSheetFileFunction", "Not load Ads")
        }
    }
    private fun loadNativeNomedia() {
        if (IAPUtils.isPremium()) {
            binding.layoutNative.visibility = View.GONE
            return
        }
        val safeContext = context ?: return
        if (SystemUtils.isInternetAvailable(safeContext)) {
            isAdLoaded = false // reset trạng thái

            binding.layoutNative.visibility = View.VISIBLE
            val loadingView = LayoutInflater.from(safeContext)
                .inflate(R.layout.ads_native_loading_short, null)
            binding.layoutNative.removeAllViews()
            binding.layoutNative.addView(loadingView)

            val callback = object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    if (isViewDestroyed || !isAdded || binding == null) return

                    // Inflate ad view
                    val adView = LayoutInflater.from(safeContext)
                        .inflate(R.layout.ads_native_bot_no_media_short, null) as NativeAdView
                    binding.layoutNative.removeAllViews()
                    binding.layoutNative.addView(adView)
                    Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)

                    // Cho phép đóng dialog ngoài khi ad đã load
                    isAdLoaded = true
                    dialog?.setCancelable(true)
                    dialog?.setCanceledOnTouchOutside(true)
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    if (isViewDestroyed || !isAdded || binding == null) return

                    // Ẩn layout ad, vẫn coi là "đã load" để không block user
                    binding.layoutNative.visibility = View.GONE

                    isAdLoaded = true
                    dialog?.setCancelable(true)
                    dialog?.setCanceledOnTouchOutside(true)
                }
            }

            Admob.getInstance().loadNativeAd(
                safeContext.applicationContext,
                getString(R.string.native_popup_all),
                callback
            )
        } else {
            // Nếu không có internet, hide ad và mở khóa dialog
            binding.layoutNative.visibility = View.GONE
            isAdLoaded = true
            dialog?.setCancelable(true)
            dialog?.setCanceledOnTouchOutside(true)
        }
    }
    override fun onCancel(dialog: DialogInterface) {
        // Chỉ cho cancel khi quảng cáo đã load
        if (!isAdLoaded) {

        } else {
            super.onCancel(dialog)
        }
    }

    private fun initViews() {
        binding.tvTitle.text = fileModel.name
        @SuppressLint("SetTextI18n")
        val sizeParts = fileModel.sizeString.split(" ")
        val sizeValue = sizeParts.getOrNull(0)?.toDoubleOrNull()
        val sizeUnit = sizeParts.getOrNull(1) ?: ""
        val roundedSize = if (sizeValue != null) {
            sizeValue.toInt().toString()
        } else {
            fileModel.sizeString
        }
        binding.tvFileInfo.text = "${DateUtils.longToDateString(fileModel.date, DateUtils.DATE_FORMAT_7)} | ${"$roundedSize $sizeUnit".uppercase(Locale.ROOT)}"
        if (!fileModel.isFavorite){
            binding.starIcon.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
        }
        if (Locale.getDefault().language == "ar") {
            binding.tvTitle.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        } else {
            binding.tvTitle.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        // Icon theo file
        val isPdf = fileModel.path.endsWith(".pdf", true)
        val isPpt = fileModel.path.endsWith(".ppt", true) || fileModel.path.endsWith(".pptx", true)
        val isWord = fileModel.path.endsWith(".doc", true) || fileModel.path.endsWith(".docx", true)
        val isExcel = fileModel.path.endsWith(".xls", true) || fileModel.path.endsWith(".xlsx", true) || fileModel.path.endsWith(".xlsm", true)

        val fileIconRes = when {
            isPdf -> R.drawable.icon_pdf
            isPpt -> R.drawable.icon_ppt
            isWord -> R.drawable.icon_word
            isExcel -> R.drawable.icon_excel
            else -> R.drawable.icon_pdf
        }
        binding.fileIcon.setImageResource(fileIconRes)

        //-------------------------------
        // ẨN/HIỆN CÁC BUTTON CHUYỂN ĐỔI
        //-------------------------------
        // Mặc định ẩn hết 3 button
        binding.funcPdfToWord.isVisible = false
        binding.funcWordToPdf.isVisible = false
        binding.funcPptToPdf.isVisible = false
        binding.funcPrint.isVisible = false

        when {
            isPdf -> {
                binding.funcPdfToWord.isVisible = true
                binding.funcPrint.isVisible = true
            }
            isWord -> {
                binding.funcWordToPdf.isVisible = true
                binding.funcPrint.isVisible = true
            }
            isPpt -> {
                binding.funcPptToPdf.isVisible = true
            }
            isExcel -> {

            }
        }
        binding.funcRename.isVisible = !fileModel.isSample
        binding.funcRemoveRecent.isVisible = fileModel.isRecent
        binding.funcRemoveFavourite.isVisible = fileModel.isFavorite
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setDimAmount(0.5f)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        isViewDestroyed = true
    }


    private fun initListener() {
        binding.funcShare.setOnClickListener {
            listener.onListener(FunctionState.SHARE)
            dismiss()
        }


        binding.funcRename.setOnClickListener {
            listener.onListener(FunctionState.RENAME)
            dismiss()
        }


        binding.funcDelete.setOnClickListener {
            listener.onListener(FunctionState.DELETE)
            dismiss()
        }

        binding.starIcon.setOnClickListener {
            Log.d("BottomSheetFileFunction", "binding.starIcon.setOnClickListener")
            listener.onListener(FunctionState.FAVORITE)
            val favoriteColor = if (fileModel.isFavorite) R.color.yellow else R.color.gray
            binding.starIcon.setColorFilter(
                binding.root.context.getColor(favoriteColor),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
        binding.funcPdfToWord.setOnClickListener {
            listener.onListener(FunctionState.PDF_TO_WORD)
            dismiss()
        }
        binding.funcWordToPdf.setOnClickListener {
            listener.onListener(FunctionState.WORD_TO_PDF)
            dismiss()
        }
        binding.funcPptToPdf.setOnClickListener {
            listener.onListener(FunctionState.PPT_TO_PDF)
            dismiss()
        }
        binding.funcPrint.setOnClickListener {
            listener.onListener(FunctionState.PRINT)
            dismiss()
        }
        binding.funcRemoveRecent.setOnClickListener {
            listener.onListener(FunctionState.CLEAR_RECENT)
            dismiss()
        }
        binding.funcRemoveFavourite.setOnClickListener {
            listener.onListener(FunctionState.CLEAR_FAVORITE)
            dismiss()
        }


    }
}