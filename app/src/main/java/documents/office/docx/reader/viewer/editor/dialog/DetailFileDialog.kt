package documents.office.docx.reader.viewer.editor.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.DialogFragment
import com.ezteam.baseproject.utils.DateUtils
import com.ezteam.baseproject.utils.IAPUtils
import com.ezteam.baseproject.utils.SystemUtils
import com.ezteam.baseproject.utils.TemporaryStorage
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import documents.office.docx.reader.viewer.editor.R
import documents.office.docx.reader.viewer.editor.databinding.DetailPageDialogBinding
import documents.office.docx.reader.viewer.editor.model.FileModel
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.util.Date

class DetailFileDialog(private val fileModel: FileModel) : DialogFragment() {
    override fun getTheme(): Int {
        return R.style.DialogStyle
    }
    private var _binding: DetailPageDialogBinding? = null
    private val binding get() = _binding!!

    private var title: String = ""
    private var message: String = ""
    private var onConfirm: (() -> Unit)? = null
    private var isViewDestroyed = false
    private var isAdLoaded = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DetailPageDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewDestroyed = false
        binding.btnOk.setOnClickListener {
            onConfirm?.invoke()
            dismiss()
        }
        if (TemporaryStorage.isLoadAds) {
            loadNativeNomedia()
        } else {
            Log.d("DetailFileDialog", "Not load Ads")
        }
        binding.tvFilename.text = fileModel.name
        binding.tvPath.text = fileModel.path
        binding.tvViewed.text = fileModel.timeRecent ?: "-"
        binding.tvSize.text = fileModel.sizeString
        binding.tvModified.text =  DateUtils.longToDateString(
            fileModel.date, DateUtils.DATE_FORMAT_5
        )
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
                    if (isViewDestroyed || !isAdded || _binding == null) return

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
                    if (isViewDestroyed || !isAdded || _binding == null) return

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
        _binding = null
    }



    fun setOnConfirmListener(callback: () -> Unit): DetailFileDialog {
        this.onConfirm = callback
        return this
    }
}
