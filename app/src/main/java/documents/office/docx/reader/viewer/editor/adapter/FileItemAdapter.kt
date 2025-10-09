package documents.office.docx.reader.viewer.editor.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import documents.office.docx.reader.viewer.editor.databinding.ItemPdfFileBinding
import documents.office.docx.reader.viewer.editor.model.FileModel
import com.ezteam.baseproject.adapter.BaseRecyclerAdapter
import com.ezteam.baseproject.listener.EzItemListener
import com.ezteam.baseproject.utils.DateUtils
import com.ezteam.baseproject.utils.IAPUtils
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import documents.office.docx.reader.viewer.editor.R
import java.util.Locale

class FileItemAdapter(
    context: Context,
    list: List<FileModel>,
    var onClickListener: EzItemListener<FileModel>,
    var onSelectedFuncListener: EzItemListener<FileModel>,
    var listener: EzItemListener<FileModel>
) : BaseRecyclerAdapter<FileModel, FileItemAdapter.ViewHolder>(context, list) {


    private var isCheckMode = false
    var onSelectedCountChangeListener: ((Int) -> Unit)? = null


    inner class ViewHolder(
        var binding: ItemPdfFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindData(model: FileModel) {

            if (model.isAds) {
                binding.content.visibility = View.GONE
                this.binding.layoutNative.visibility = View.VISIBLE
                val loadingView = LayoutInflater.from(mContext)
                    .inflate(R.layout.ads_native_loading_middle, null)
                binding.layoutNative.removeAllViews()
                binding.layoutNative.addView(loadingView)

                nativeAd?.let {
                    val layoutRes = R.layout.ads_native_middle_files
                    val adView = LayoutInflater.from(mContext)
                        .inflate(layoutRes, null) as NativeAdView

                    binding.layoutNative.removeAllViews()
                    binding.layoutNative.addView(adView)

                    // Gán dữ liệu quảng cáo vào view
                    Admob.getInstance().pushAdsToViewCustom(it, adView)
                    } ?: kotlin.run {
                        binding.layoutNative.removeAllViews()
                        binding.layoutNative.addView(loadingView)
                    }
                    return
                }

            binding.layoutNative.visibility = View.GONE
            binding.content.visibility = View.VISIBLE


            val fileIconRes = when {
                model.path.lowercase().endsWith(".pdf") -> R.drawable.icon_pdf

                model.path.lowercase().endsWith(".ppt") || model.path.lowercase().endsWith(".pptx") -> R.drawable.icon_ppt

                model.path.lowercase().endsWith(".doc") || model.path.lowercase().endsWith(".docx") -> R.drawable.icon_word

                model.path.lowercase().endsWith(".xls") || model.path.lowercase().endsWith(".xlsx") || model.path.lowercase().endsWith(".xlsm") -> R.drawable.icon_excel
                else -> R.drawable.icon_pdf
            }
            binding.fileIcon.setImageResource(fileIconRes)
            binding.tvTitle.text = model.name
            val sizeParts = model.sizeString.split(" ")
            val sizeValue = sizeParts.getOrNull(0)?.toDoubleOrNull()
            val sizeUnit = sizeParts.getOrNull(1) ?: ""
            val roundedSize = if (sizeValue != null) {
                sizeValue.toInt().toString()
            } else {
                model.sizeString
            }
            binding.tvCreateDate.text =
                "${DateUtils.longToDateString(model.date, DateUtils.DATE_FORMAT_7)} | ${"$roundedSize $sizeUnit".uppercase(Locale.ROOT)}"

            binding.parent.setOnClickListener {
                onClickListener.onListener(model)
            }

            binding.icFunc.setOnClickListener {
                onSelectedFuncListener.onListener(model)
            }

            val favoriteIcon = if (model.isFavorite) {
                R.drawable.icon_favourite
            } else {
                R.drawable.icon_not_favourite
            }
            binding.ivFavorite.setImageResource(favoriteIcon)


            binding.ivFavorite.setOnClickListener {
                listener.onListener(model)
                notifyItemChanged(adapterPosition)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPdfFileBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }
    fun toggleCheckMode(isCheckMode: Boolean) {
        this.isCheckMode = isCheckMode
//        notifyDataSetChanged()
    }
    private val chosenPositions = mutableSetOf<Int>()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = list[position]

        holder.bindData(file)
        holder.binding.selectCheckbox.visibility = if (isCheckMode) View.VISIBLE else View.GONE
    //    holder.binding.ivFavorite.visibility = View.GONE
    //    holder.binding.icFunc.visibility = View.GONE

        // Cập nhật trạng thái checkbox dựa trên danh sách chọn
        holder.binding.selectCheckbox.isSelected = chosenPositions.contains(position)

        holder.binding.selectCheckbox.setOnClickListener {
            it.isSelected = !it.isSelected
            if (it.isSelected) {
                chosenPositions.add(position)
            } else {
                chosenPositions.remove(position)
            }
            onSelectedCountChangeListener?.invoke(chosenPositions.size)
        }


    }
    fun getSelectedFiles(): List<FileModel> {
        return chosenPositions.map { list[it] }
    }
    // Hàm chọn tất cả
    fun selectAll() {
        chosenPositions.clear()
        for (i in list.indices) {
            chosenPositions.add(i)
        }
        onSelectedCountChangeListener?.invoke(chosenPositions.size)
        notifyDataSetChanged()
    }

    // Hàm bỏ chọn tất cả
    fun deselectAll() {
        chosenPositions.clear()
        onSelectedCountChangeListener?.invoke(chosenPositions.size)
        notifyDataSetChanged()
    }
}
