package documents.office.docx.reader.viewer.editor.screen.search

//import com.google.android.gms.ads.ez.EzAdControl
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import documents.office.docx.reader.viewer.editor.R
import documents.office.docx.reader.viewer.editor.adapter.FileItemAdapter
import documents.office.docx.reader.viewer.editor.common.FileTab
import documents.office.docx.reader.viewer.editor.common.FunctionState
import documents.office.docx.reader.viewer.editor.databinding.ActivitySearchFileBinding
import documents.office.docx.reader.viewer.editor.model.FileModel
import documents.office.docx.reader.viewer.editor.screen.base.PdfBaseActivity
import documents.office.docx.reader.viewer.editor.screen.func.BottomSheetFileFunction
import documents.office.docx.reader.viewer.editor.screen.main.MainViewModel
import documents.office.docx.reader.viewer.editor.screen.start.SplashActivity
import com.ezteam.baseproject.utils.FirebaseRemoteConfigUtil
import kotlinx.coroutines.launch
import org.apache.commons.io.FilenameUtils
import org.koin.android.ext.android.inject
import java.util.Locale

class SearchFileActivity : PdfBaseActivity<ActivitySearchFileBinding>() {
    private val viewModel by inject<MainViewModel>()
    private lateinit var adapter: FileItemAdapter

    companion object {
        fun start(activity: FragmentActivity) {
            val intent = Intent(activity, SearchFileActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        EzAdControl.getInstance(this).showAds()
    }
    private fun showAdsOr(action: () -> Unit) {
        if (FirebaseRemoteConfigUtil.getInstance().isShowAdsMain()) {
            showAdsInterstitial(FirebaseRemoteConfigUtil.getInstance().getAdsConfigValue("inter_home")) {
                action()
            }
        } else {
            action()
        }
    }
    override fun initView() {
        if (Locale.getDefault().language == "ar") {
            binding.toolbar.ivBack.rotationY = 180f
        } else {
            binding.toolbar.ivBack.rotationY = 0f
        }
        adapter = FileItemAdapter(this, mutableListOf(), ::onItemClick, ::onSelectedFunc, ::onReactFavorite)
        binding.rcvListFile.adapter = adapter
        binding.toolbar.edtSearch.hint = Html.fromHtml("<i>${getString(R.string.search)}</i>", Html.FROM_HTML_MODE_LEGACY)
    }

    override fun initData() {
        lifecycleScope.launch {
            viewModel.getListSearchFile().observe(this@SearchFileActivity) {
                adapter.setList(it)
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
            if (!binding.toolbar.edtSearch.getText().isNullOrEmpty())
                binding.toolbar.edtSearch.setText("")
            showAdsOr {
                finish()
            }
        }

        binding.toolbar.ivClear.setOnClickListener {
            if (!binding.toolbar.edtSearch.getText().isNullOrEmpty())
            binding.toolbar.edtSearch.setText("")
        }

        binding.toolbar.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                viewModel.searchCharObservable.postValue(s.toString())
                if (s.isNullOrEmpty()) {
                    binding.toolbar.ivClear.setImageResource(R.drawable.find)
                } else {
                    binding.toolbar.ivClear.setImageResource(R.drawable.ic_cancel);
                }
            }

            override fun afterTextChanged(s: Editable?) {}

        })

        binding.toolbar.edtSearch.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.toolbar.edtSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun onItemClick(fileModel: FileModel) {
//        PdfViewerActivity.start(this, fileModel)
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

            FunctionState.RECENT -> {
                viewModel.reactRecentFile(fileModel, false)
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

            FunctionState.CREATE_SHORTCUT -> {
                val intent = Intent(this, SplashActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    type = "application/pdf"
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    putExtra(SplashActivity.FILE_PATH, fileModel.path)
                }
                val shortcut = ShortcutInfoCompat.Builder(this, fileModel.path)
                    .setShortLabel(FilenameUtils.getBaseName(fileModel.path))
                    .setLongLabel(FilenameUtils.getBaseName(fileModel.path))
                    .setIcon(
                        IconCompat.createWithResource(
                            this,
                            R.drawable.ic_pdf
                        )
                    )
                    .setIntent(
                        intent
                    )
                    .build()

                ShortcutManagerCompat.requestPinShortcut(this, shortcut, null)
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
                showDetailFile(fileModel, viewModel)
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

    override fun viewBinding(): ActivitySearchFileBinding {
        return ActivitySearchFileBinding.inflate(LayoutInflater.from(this))
    }
}