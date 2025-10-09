package documents.office.docx.reader.viewer.editor.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.ezteam.baseproject.utils.IAPUtils
import com.ezteam.baseproject.utils.PreferencesUtils
import com.ezteam.baseproject.utils.SystemUtils
import com.ezteam.baseproject.utils.TemporaryStorage
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import documents.office.docx.reader.viewer.editor.R
import documents.office.docx.reader.viewer.editor.common.PresKey
import documents.office.docx.reader.viewer.editor.databinding.DefaultReaderSortDialog2Binding

class SortDialog2 : DialogFragment() {

    override fun getTheme(): Int = R.style.DialogStyle

    private var _binding: DefaultReaderSortDialog2Binding? = null
    private val binding get() = _binding!!

    private var callBack: ((Int) -> Unit)? = null
    private var isViewDestroyed = false
    private var isAdLoaded = false

    private var selectedCriteria: View? = null
    private var selectedOrder: View? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DefaultReaderSortDialog2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewDestroyed = false

        setupSortButtons()
        setupListeners()
        restoreSortState()

        if (TemporaryStorage.isLoadAds) loadNativeNomedia()
        else Log.d("SortDialog", "Not load Ads")
    }

    private fun setupSortButtons() {
        val criteriaButtons = listOf(
            binding.btnCriteriaName to "name",
            binding.btnCriteriaDate to "date",
            binding.btnCriteriaSize to "size"
        )
        val orderButtons = listOf(
            binding.btnOrderAsc to "asc",
            binding.btnOrderDesc to "desc"
        )

        // Khi chọn criteria
        criteriaButtons.forEach { (btn, _) ->
            btn.setOnClickListener {
                selectOne(criteriaButtons.map { it.first }, btn)
                selectedCriteria = btn
                updateApplyButtonState()
            }
        }

        // Khi chọn order
        orderButtons.forEach { (btn, _) ->
            btn.setOnClickListener {
                selectOne(orderButtons.map { it.first }, btn)
                selectedOrder = btn
                updateApplyButtonState()
            }
        }

        // Ban đầu disable apply
        binding.btnOk.isEnabled = false
        binding.btnOk.alpha = 0.5f
    }

    private fun updateApplyButtonState() {
        val enable = selectedCriteria != null && selectedOrder != null
        binding.btnOk.isEnabled = enable
        binding.btnOk.alpha = if (enable) 1f else 0.5f
    }

    private fun selectOne(buttons: List<View>, selected: View) {
        buttons.forEach { it.isSelected = it == selected }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnOk.setOnClickListener {
            val state = getSortState()
            PreferencesUtils.putInteger(PresKey.SORT_STATE, state)
            callBack?.invoke(state)
            dismiss()
        }
    }

    private fun getSortState(): Int {
        val criteria = when (selectedCriteria?.id) {
            R.id.btn_criteria_name -> "name"
            R.id.btn_criteria_date -> "date"
            R.id.btn_criteria_size -> "size"
            else -> "date"
        }

        val order = when (selectedOrder?.id) {
            R.id.btn_order_asc -> "asc"
            R.id.btn_order_desc -> "desc"
            else -> "asc"
        }

        return when {
            criteria == "name" && order == "asc" -> 1
            criteria == "name" && order == "desc" -> 2
            criteria == "date" && order == "asc" -> 3
            criteria == "date" && order == "desc" -> 4
            criteria == "size" && order == "asc" -> 5
            criteria == "size" && order == "desc" -> 6
            else -> 3
        }
    }

    private fun restoreSortState() {
        val sortState = PreferencesUtils.getInteger(PresKey.SORT_STATE, 4)

        val criteriaMap = mapOf(
            1 to binding.btnCriteriaName,
            2 to binding.btnCriteriaName,
            3 to binding.btnCriteriaDate,
            4 to binding.btnCriteriaDate,
            5 to binding.btnCriteriaSize,
            6 to binding.btnCriteriaSize
        )
        val orderMap = mapOf(
            1 to binding.btnOrderAsc,
            2 to binding.btnOrderDesc,
            3 to binding.btnOrderAsc,
            4 to binding.btnOrderDesc,
            5 to binding.btnOrderAsc,
            6 to binding.btnOrderDesc
        )

        criteriaMap[sortState]?.let { btn ->
            selectOne(
                listOf(binding.btnCriteriaName, binding.btnCriteriaDate, binding.btnCriteriaSize),
                btn
            )
            selectedCriteria = btn
        }

        orderMap[sortState]?.let { btn ->
            selectOne(listOf(binding.btnOrderAsc, binding.btnOrderDesc), btn)
            selectedOrder = btn
        }

        updateApplyButtonState()
    }

    private fun loadNativeNomedia() {
        if (IAPUtils.isPremium()) {
            binding.layoutNative.isVisible = false
            return
        }
        val safeContext = context ?: return
        if (SystemUtils.isInternetAvailable(safeContext)) {
            isAdLoaded = false

            binding.layoutNative.isVisible = true
            val loadingView = LayoutInflater.from(safeContext)
                .inflate(R.layout.ads_native_loading_short, null)
            binding.layoutNative.removeAllViews()
            binding.layoutNative.addView(loadingView)

            val callback = object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    if (isViewDestroyed || !isAdded || _binding == null) return

                    val adView = LayoutInflater.from(safeContext)
                        .inflate(R.layout.ads_native_bot_no_media_short, null) as NativeAdView
                    binding.layoutNative.removeAllViews()
                    binding.layoutNative.addView(adView)
                    Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)

                    isAdLoaded = true
                    dialog?.setCancelable(true)
                    dialog?.setCanceledOnTouchOutside(true)
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    if (isViewDestroyed || !isAdded || _binding == null) return
                    binding.layoutNative.isVisible = false
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
            binding.layoutNative.isVisible = false
            isAdLoaded = true
            dialog?.setCancelable(true)
            dialog?.setCanceledOnTouchOutside(true)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        if (isAdLoaded) super.onCancel(dialog)
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

    fun setOnSortSelectedListener(callback: (Int) -> Unit) {
        this.callBack = callback
    }
}
