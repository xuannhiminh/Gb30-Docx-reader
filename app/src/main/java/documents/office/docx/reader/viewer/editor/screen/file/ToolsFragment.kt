package documents.office.docx.reader.viewer.editor.screen.file

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.ezteam.baseproject.utils.IAPUtils
import com.ezteam.baseproject.utils.PDFConstants.Companion.ADS_ITEM_INDEX
import com.google.android.gms.ads.nativead.NativeAd
import documents.office.docx.reader.viewer.editor.R
import documents.office.docx.reader.viewer.editor.adapter.FileItemAdapter
import documents.office.docx.reader.viewer.editor.common.FileTab
import documents.office.docx.reader.viewer.editor.common.FunctionState
import documents.office.docx.reader.viewer.editor.common.LoadingState
import documents.office.docx.reader.viewer.editor.databinding.FragmentToolsBinding
import documents.office.docx.reader.viewer.editor.model.FileModel
import documents.office.docx.reader.viewer.editor.screen.base.IAdsControl
import documents.office.docx.reader.viewer.editor.screen.base.PdfBaseActivity
import documents.office.docx.reader.viewer.editor.screen.base.PdfBaseFragment
import documents.office.docx.reader.viewer.editor.screen.func.BottomSheetFileFunction
import documents.office.docx.reader.viewer.editor.screen.main.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.io.FilenameUtils
import org.koin.android.ext.android.inject


open class ToolsFragment(private val filesLiveData: LiveData<List<FileModel>>) : PdfBaseFragment<FragmentToolsBinding>(),
    IAdsControl {
    protected val viewModel by inject<MainViewModel>()
    private lateinit var adapter: FileItemAdapter
  //  private val sharedViewModel by MainViewModel<SharedViewModel>()

    override fun initView() {

    }


    override fun initData() {

    }
    override fun initListener() {

    }
    protected open fun fromTab(): FileTab {
        return FileTab.ALL_FILE
    }

    private fun onItemClick(fileModel: FileModel) {
        openFile(fileModel)
    }

    private fun onSelectedFunc(fileModel: FileModel) {
        val bottomSheetFileFunction =
            BottomSheetFileFunction(fileModel, fromTab()) {
                onSelectedFunction(fileModel, it)
            }
        bottomSheetFileFunction.show(childFragmentManager, BottomSheetFileFunction::javaClass.name)
    }

    private fun onReactFavorite(fileModel: FileModel) {
        fileModel.isFavorite = !fileModel.isFavorite
        viewModel.reactFavorite(fileModel)
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

            FunctionState.RECENT -> {
                viewModel.reactRecentFile(fileModel, false)
            }

            FunctionState.RENAME -> {
                fileModel.name?.let {
                    showRenameFile(it) { newName ->
                        viewModel.renameFile(fileModel, newName, onFail = {
                            lifecycleScope.launch(Dispatchers.Main) {
                                toast(resources.getString(R.string.rename_unsuccessful))
                            }
                        })
                    }
                }
            }

            FunctionState.DELETE -> {
                showDialogConfirm(
                    resources.getString(R.string.delete),
                    String.format(resources.getString(R.string.del_message), fileModel.name)
                ) {
                    viewModel.deleteFile(fileModel)
                }
            }
            FunctionState.CLEAR_RECENT -> {
                showDialogRemove(
                    resources.getString(R.string.are_you_sure),
                    String.format(resources.getString(R.string.remove_recent_content))
                ) {
                    viewModel.removeRecentFile(fileModel)
                }
            }
            FunctionState.CLEAR_FAVORITE -> {
                showDialogRemove(
                    resources.getString(R.string.are_you_sure),
                    String.format(resources.getString(R.string.remove_favourite_content))
                ) {
                    viewModel.removeFavouriteFile(fileModel)
                }
            }

            FunctionState.DETAIL -> {
                showDetailFile(fileModel)
            }
            FunctionState.PDF_TO_WORD -> {
                openFile(fileModel)
            }
            FunctionState.WORD_TO_PDF -> {
                openFile(fileModel)
            }
            FunctionState.PPT_TO_PDF -> {
                openFile(fileModel)
            }
            FunctionState.PRINT -> {
                openFile(fileModel)
            }

            else -> {}
        }
    }
    fun showDetailFile(fileModel: FileModel) {
        (requireActivity() as PdfBaseActivity<*>).showDetailFile(fileModel, viewModel)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolsBinding {
        return FragmentToolsBinding.inflate(inflater, container, false)
    }

    override fun onNativeAdLoaded(nativeAd: NativeAd?) {
        if (::adapter.isInitialized) {
            adapter.nativeAd = nativeAd
            adapter.notifyItemChanged(ADS_ITEM_INDEX)
        }
    }

    override fun onAdFailedToLoad() {
        if (::adapter.isInitialized) {
            if (adapter.getList().size > ADS_ITEM_INDEX && adapter.getList()[ADS_ITEM_INDEX].isAds) {
                adapter.getList().removeAt(ADS_ITEM_INDEX)
                adapter.notifyItemRemoved(ADS_ITEM_INDEX)
            }
        }
    }
}