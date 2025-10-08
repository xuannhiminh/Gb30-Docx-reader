package documents.office.docx.reader.viewer.editor.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.ezteam.baseproject.utils.IAPUtils
import com.ezteam.baseproject.utils.SystemUtils
import com.ezteam.baseproject.utils.TemporaryStorage
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.analytics.FirebaseAnalytics
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import documents.office.docx.reader.viewer.editor.R
import documents.office.docx.reader.viewer.editor.databinding.ExitAppDialogBinding
import documents.office.docx.reader.viewer.editor.databinding.SatisfactionDialogBinding
import documents.office.docx.reader.viewer.editor.screen.search.FeedBackActivity
import documents.office.docx.reader.viewer.editor.screen.setting.RateUsDialog

class ExitAppDialog : DialogFragment() {
    override fun getTheme(): Int {
        return R.style.DialogStyle
    }
    private var _binding: ExitAppDialogBinding? = null
    private val binding get() = _binding!!

    private var title: String = ""
    private var message: String = ""
    private var onConfirm: (() -> Unit)? = null
    private var isViewDestroyed = false
    private var isAdLoaded = false

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private fun logEvent(firebaseAnalytic: FirebaseAnalytics, event: String) {
        firebaseAnalytic.logEvent(event, Bundle().apply {
            putString("screen", "SatisficationDialog")
        })
    }

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
        _binding = ExitAppDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (TemporaryStorage.isLoadAds) {
            loadNativeNomedia()
        } else {
            Log.d("ExitAppDialog", "Not load Ads")
        }
        isViewDestroyed = false
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

            binding.btnBack.setOnClickListener {
                dismiss()
            }

            binding.btnExit.setOnClickListener {
                requireActivity().finishAffinity()
                dismiss()
            }

        } catch (e: Exception) {

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

    fun setTitle(title: String): ExitAppDialog {
        this.title = title
        return this
    }

    fun setMessage(message: String): ExitAppDialog {
        this.message = message
        return this
    }

    fun setOnConfirmListener(callback: () -> Unit): ExitAppDialog {
        this.onConfirm = callback
        return this
    }
}
