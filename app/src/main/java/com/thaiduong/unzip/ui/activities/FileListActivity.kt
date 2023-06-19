package com.thaiduong.unzip.ui.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thaiduong.unzip.App
import com.thaiduong.unzip.App.Companion.getDB
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ActivityFileListBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.adapters.CategoryAdapter
import com.thaiduong.unzip.ui.adapters.ExpandedFolderAdapter
import com.thaiduong.unzip.ui.adapters.ImageAndVideoAdapter
import com.thaiduong.unzip.ui.adapters.SelectedItemAdapter
import com.thaiduong.unzip.ui.bases.BaseActivity
import com.thaiduong.unzip.ui.fragments.ExplorerFragment
import com.thaiduong.unzip.ui.fragments.GoogleDriveFragment
import com.thaiduong.unzip.utils.*
import com.thaiduong.unzip.utils.AppUtils.getFile
import com.thaiduong.unzip.utils.AppUtils.myGetExternalStorageDir
import com.thaiduong.unzip.utils.customclass.BottomSheetFunction
import com.thaiduong.unzip.utils.interfaces.GetBottomSheetState
import com.thaiduong.unzip.utils.interfaces.IGetBottomSheetLayoutGravity
import com.thaiduong.unzip.utils.interfaces.IGetItemCategory
import com.thaiduong.unzip.utils.interfaces.IItemSelected
import java.io.File

class FileListActivity(override val layoutId: Int = R.layout.activity_file_list) :
    BaseActivity<ActivityFileListBinding>(), IItemSelected, IGetBottomSheetLayoutGravity,
    IGetItemCategory, GetBottomSheetState {

    private lateinit var mExpandedFolderAdapter: ExpandedFolderAdapter
    private lateinit var mCategoryAdapter: CategoryAdapter
    private lateinit var mSelectedItemAdapter: SelectedItemAdapter
    private lateinit var filesAndFolders: Array<File>
    private var originalFileList = mutableListOf<FolderOrFile>()
    private var selectedFileList = mutableListOf<FolderOrFile>()
    private var folderName = ""
    private var path = ""
    private var categoryName = ""
    private lateinit var dialog: AlertDialog

    private var mBottomSheetFunction: BottomSheetFunction? = null
    private lateinit var params: ConstraintLayout.LayoutParams

    override fun initUi() {

        path = intent.getStringExtra(PATH).toString()
        folderName = intent.getStringExtra(FOLDER_NAME).toString()
        categoryName = intent.getStringExtra(CATEGORY_NAME).toString()

        binding.tvTitleFolder.text = folderName

        mExpandedFolderAdapter = ExpandedFolderAdapter(this)
        mCategoryAdapter = CategoryAdapter(this)
        mSelectedItemAdapter = SelectedItemAdapter(this, selectedFileList)

        mBottomSheetFunction = BottomSheetFunction(this, folderName)
        mBottomSheetFunction?.initUi()
        mBottomSheetFunction?.setData(originalFileList, mExpandedFolderAdapter, mCategoryAdapter)

        params = binding.h2.layoutParams as ConstraintLayout.LayoutParams

        if (folderName in arrayOf("Compressed", "Document")) {
            binding.imvFilter.visibility = View.VISIBLE
        } else {
            binding.imvFilter.visibility = View.GONE
        }

    }

    @SuppressLint("InflateParams")
    override fun doWork() {
        binding.btnClearAll.setOnClickListener {
            val dialogDelete = Dialog(this, R.style.DialogStyle)
            dialogDelete.setContentView(
                LayoutInflater.from(this).inflate(R.layout.delete_dialog, null, false)
            )
            dialogDelete.setCancelable(false)
            dialogDelete.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialogDelete.dismiss()
            }
            dialogDelete.findViewById<Button>(R.id.btnOk).setOnClickListener {
                dialogDelete.dismiss()
                for (file in originalFileList) {
                    if (file.mFile?.isDirectory == true) {
                        file.mFile?.deleteRecursively()
                    } else {
                        file.mFile?.delete()
                    }
                }
                originalFileList.clear()
                getDB().fileDataDao().cleanRecycleBin()
                mExpandedFolderAdapter.submitList(originalFileList)
                binding.imvNoFilesFound.visibility = View.VISIBLE
                binding.btnClearAll.visibility = View.GONE
            }
            dialogDelete.show()
        }
        binding.imvBack.setOnClickListener {
            onBackPressed()
        }
        binding.imvFilter.setOnClickListener {
            filterFileShow()
        }
    }

    @SuppressLint("LogNotTimber")
    private fun initView() {
        when (path) {
            MENU_SETTINGS -> {
                binding.scrollContentSetting.visibility = View.VISIBLE
                if (folderName == getString(R.string.privacy_policy)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        binding.tvContentSetting.text =
                            Html.fromHtml(
                                getString(R.string.policy_content),
                                HtmlCompat.FROM_HTML_MODE_LEGACY
                            )
                    } else {
                        @Suppress("DEPRECATION")
                        binding.tvContentSetting.text =
                            Html.fromHtml(getString(R.string.policy_content))
                    }
                }
                if (folderName == getString(R.string.term)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        binding.tvContentSetting.text =
                            Html.fromHtml(
                                getString(R.string.term_content),
                                HtmlCompat.FROM_HTML_MODE_LEGACY
                            )
                    } else {
                        @Suppress("DEPRECATION")
                        binding.tvContentSetting.text =
                            Html.fromHtml(getString(R.string.term_content))
                    }
                }
            }
            CATEGORY_NAME -> {
                when (folderName) {
                    getString(R.string.file_explorer) -> replaceScreen(ExplorerFragment())
                    "Google Driver" -> replaceScreen(GoogleDriveFragment())
                    getString(R.string.recently) -> createRecentlyList()
                }
            }
            "null" -> {
                when (folderName) {
                    getString(R.string.image) -> {
                        setCategoryAdapter(false)
                    }
                    getString(R.string.video) -> {
                        setCategoryAdapter(false)
                    }
                    getString(R.string.audio) -> {
                        setCategoryAdapter(true)
                    }
                    getString(R.string.document) -> {
                        setCategoryAdapter(true)
                    }
                    getString(R.string.download) -> {
                        val fileList = File("${this.myGetExternalStorageDir()}/Download")
                        fileList.listFiles()?.sort()
                        for (f in fileList.listFiles()!!) {
                            originalFileList.add(FolderOrFile(f))
                        }
                        mExpandedFolderAdapter.submitList(originalFileList)
                        binding.rcvInternalFolder.adapter = mExpandedFolderAdapter
                    }
                    getString(R.string.apk) -> {
                        setCategoryAdapter(true)
                    }
                }
            }
            else -> {
                try {
                    when (folderName) {
                        getString(R.string.Compressed) -> createCompressedList()
                        getString(R.string.extracted) -> createExtractedList()
                        else -> {
                            Log.e("FileListActivity", categoryName)
                            if (categoryName != "null")
                                crateItemForFolder()
                            else
                                createFolderList()
                        }
                    }
                } catch (ex: Exception) {
                    binding.imvNoFilesFound.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun crateItemForFolder() {
        val filesList = File(path).listFiles()
        val typeToken = object : TypeToken<List<String>>() {}.type
        val list = Gson().fromJson<List<String>>(categoryName, typeToken)
        for (file in filesList!!) {
            if (file.extension in list) {
                if (!originalFileList.contains(FolderOrFile(file)))
                    originalFileList.add(FolderOrFile(file))
            }
        }
        mCategoryAdapter.submitList(originalFileList)
        binding.rcvInternalFolder.layoutManager = GridLayoutManager(this, 3)
        binding.rcvInternalFolder.adapter = mCategoryAdapter
        binding.rcvInternalFolder.visibility = View.VISIBLE
        binding.tabLayout.visibility = View.GONE
        binding.viewPager.visibility = View.GONE

    }

    private fun createCompressedList() {
        val rootFile = path.let { File(it).listFiles() }
        if (rootFile != null) {
            for (file in rootFile)
                FileManager.getFile(originalFileList, listOf("zip", "rar", "7z", "tar"), file)
        }
        path = "${this.myGetExternalStorageDir()}/.Compressed"
        for (file in File(path).listFiles()!!) {
            if (!originalFileList.contains(FolderOrFile(file)))
                originalFileList.add(FolderOrFile(file))
        }
        mExpandedFolderAdapter.submitList(originalFileList)
        binding.rcvInternalFolder.adapter = mExpandedFolderAdapter
    }

    private fun replaceScreen(mFragment: Fragment) {
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(R.id.frameList, mFragment)
        fragmentTransition.commitAllowingStateLoss()
    }

    private fun createRecentlyList() {
        val root = this.myGetExternalStorageDir()
        val fileList = root?.let { File(it).listFiles() }!!
        fileList.sortByDescending { it.lastModified() }
        this.getFile(fileList, originalFileList, 15)
        mExpandedFolderAdapter.submitList(originalFileList)
        binding.rcvInternalFolder.adapter = mExpandedFolderAdapter
    }

    @SuppressLint("SuspiciousIndentation")
    private fun setCategoryAdapter(isExplorerCategory: Boolean) {
        if (isExplorerCategory) {
            binding.rcvInternalFolder.visibility = View.VISIBLE
            binding.tabLayout.visibility = View.GONE
            binding.viewPager.visibility = View.GONE
            mExpandedFolderAdapter.submitList(originalFileList)
            path = this.myGetExternalStorageDir().toString()
            val rootFile = path.let { File(it).listFiles() }
            val typeList = when (folderName) {
                getString(R.string.audio) -> listOf("mp3")
                getString(R.string.document) -> listOf("xlsx", "docx", "pptx", "pdf", "txt")
                getString(R.string.apk) -> listOf("apk")
                else -> null
            }
            if (rootFile != null) {
                for (file in rootFile)
                    typeList?.let { FileManager.getFile(originalFileList, it, file) }
            }
            mExpandedFolderAdapter.submitList(originalFileList)
            binding.rcvInternalFolder.adapter = mExpandedFolderAdapter
            path = "null"

        } else {
            binding.rcvInternalFolder.visibility = View.GONE
            binding.tabLayout.visibility = View.VISIBLE
            binding.viewPager.visibility = View.VISIBLE
            binding.viewPager.offscreenPageLimit = 2
            binding.viewPager.setPageTransformer(DepthPageTransformer())
            binding.viewPager.isUserInputEnabled = false
            val mImageAndVideoAdapter =
                ImageAndVideoAdapter(supportFragmentManager, lifecycle, folderName)
            binding.viewPager.adapter = mImageAndVideoAdapter
            TabLayoutMediator(
                binding.tabLayout,
                binding.viewPager
            ) { tab: TabLayout.Tab, position: Int ->
                if (folderName == getString(R.string.image)) {
                    if (position == 0) {
                        tab.text = "Albums"
                    } else {
                        tab.text = "Images"
                    }
                }
                if (folderName == "Video") {
                    if (position == 0) {
                        tab.text = "Folders"
                    } else {
                        tab.text = "Videos"
                    }
                }
            }.attach()
        }
    }

    private fun createExtractedList() {
        val root = File("${this.myGetExternalStorageDir()}/.Extracted")
        val filesList = root.listFiles()!!
        if (filesList.isEmpty()) {
            binding.imvNoFilesFound.visibility = View.VISIBLE
            return
        }
        for (f in filesList) {
            if (!originalFileList.contains(FolderOrFile(f)))
                originalFileList.add(FolderOrFile(f))
        }
        mExpandedFolderAdapter.submitList(originalFileList)
        binding.rcvInternalFolder.adapter = mExpandedFolderAdapter
    }

    private fun createFolderList() {
        filesAndFolders = path.let { File(it).listFiles() }!!
        if (filesAndFolders.isEmpty()) {
            binding.imvNoFilesFound.visibility = View.VISIBLE
            binding.btnClearAll.visibility = View.GONE
            return
        }
        filesAndFolders.sort()
        for (mFile in filesAndFolders) {
            if ((mFile.name.first() != '.') && !originalFileList.contains(FolderOrFile(mFile))) {
                originalFileList.add(FolderOrFile(mFile))
            }
        }
        mExpandedFolderAdapter.submitList(originalFileList)
        binding.rcvInternalFolder.adapter = mExpandedFolderAdapter
        if (folderName == getString(R.string.recycle_bin) && originalFileList.isNotEmpty()) {
            binding.btnClearAll.visibility = View.VISIBLE
        } else {
            binding.btnClearAll.visibility = View.GONE
        }
    }

    private fun filterFileShow() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        val view = LayoutInflater.from(this).inflate(
            R.layout.filter_dialog, this.findViewById(R.id.linear_filter)
        )
        builder.setView(view)
        dialog = builder.create()
        dialog.setCancelable(true)
        if (folderName == getString(R.string.document)) {
            view.findViewById<LinearLayout>(R.id.linear_file_doc).visibility = View.VISIBLE
            view.findViewById<LinearLayout>(R.id.linear_file_pdf).visibility = View.VISIBLE
            view.findViewById<LinearLayout>(R.id.linear_file_pptx).visibility = View.VISIBLE
            view.findViewById<LinearLayout>(R.id.linear_file_excel).visibility = View.VISIBLE
            view.findViewById<LinearLayout>(R.id.linear_file_txt).visibility = View.VISIBLE
        }
        if (folderName == getString(R.string.Compressed)) {
            view.findViewById<LinearLayout>(R.id.linear_file_zip).visibility = View.VISIBLE
            view.findViewById<LinearLayout>(R.id.linear_file_rar).visibility = View.VISIBLE
            view.findViewById<LinearLayout>(R.id.linear_file_7z).visibility = View.VISIBLE
            view.findViewById<LinearLayout>(R.id.linear_file_tar).visibility = View.VISIBLE
        }
        view.findViewById<LinearLayout>(R.id.linear_file_all)
            .setOnClickListener { filterFile("all") }
        view.findViewById<LinearLayout>(R.id.linear_file_zip)
            .setOnClickListener { filterFile("zip") }
        view.findViewById<LinearLayout>(R.id.linear_file_rar)
            .setOnClickListener { filterFile("rar") }
        view.findViewById<LinearLayout>(R.id.linear_file_7z)
            .setOnClickListener { filterFile("7z") }
        view.findViewById<LinearLayout>(R.id.linear_file_tar)
            .setOnClickListener { filterFile("tar") }
        view.findViewById<LinearLayout>(R.id.linear_file_doc)
            .setOnClickListener { filterFile("docx") }
        view.findViewById<LinearLayout>(R.id.linear_file_pdf)
            .setOnClickListener { filterFile("pdf") }
        view.findViewById<LinearLayout>(R.id.linear_file_pptx)
            .setOnClickListener { filterFile("pptx") }
        view.findViewById<LinearLayout>(R.id.linear_file_excel)
            .setOnClickListener { filterFile("xlsx") }
        view.findViewById<LinearLayout>(R.id.linear_file_txt)
            .setOnClickListener { filterFile("txt") }

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val wmlp: WindowManager.LayoutParams = dialog.window!!.attributes
        wmlp.gravity = Gravity.TOP or Gravity.END
        wmlp.x = 100 //x position
        wmlp.y = 100 //y position

        dialog.show()
    }

    private fun filterFile(extension: String) {
        selectedFileList.clear()
        dialog.dismiss()
        if (extension == "all") {
            binding.imvNoFilesFound.visibility = View.GONE
            mExpandedFolderAdapter.submitList(originalFileList)
            binding.rcvInternalFolder.adapter = mExpandedFolderAdapter
        } else {
            for (f in originalFileList) {
                if (f.mFile?.extension == extension) {
                    selectedFileList.add(f)
                }
            }
            if (selectedFileList.isEmpty()) {
                binding.imvNoFilesFound.visibility = View.VISIBLE
            } else {
                binding.imvNoFilesFound.visibility = View.GONE
                mExpandedFolderAdapter.submitList(selectedFileList)
                binding.rcvInternalFolder.adapter = mExpandedFolderAdapter
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (mBottomSheetFunction?.setBottomSheetState() == false)
            initView()
        when (App.dataStore.getInt(EXTRACTED_STATUS, 2)) {
            0 -> {
                binding.imvExtractedFailed.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.imvExtractedFailed.visibility = View.GONE
                }, 2000)
                App.dataStore.putInt(EXTRACTED_STATUS, 2)
            }
            1 -> {
                binding.imvExtractedSuccess.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.imvExtractedSuccess.visibility = View.GONE
                }, 2000)
                App.dataStore.putInt(EXTRACTED_STATUS, 2)
            }
            else -> {
                binding.imvExtractedFailed.visibility = View.GONE
                binding.imvExtractedSuccess.visibility = View.GONE
            }
        }
    }

    override fun selectedItem(mFolderOrFile: FolderOrFile) {
        mBottomSheetFunction?.setOnSelectedItem(mFolderOrFile)
    }

    override fun getLayoutGravity(isShow: Boolean) {
        params.guidePercent = if (isShow) 0.9f else 1f
        if (folderName == getString(R.string.recycle_bin) && originalFileList.isNotEmpty()) {
            binding.btnClearAll.visibility = if (isShow) View.GONE else View.VISIBLE
        }
        binding.h2.layoutParams = params
    }

    override fun sentData(list: MutableList<FolderOrFile>, categoryAdapter: CategoryAdapter) {
        mBottomSheetFunction?.setData(list, mExpandedFolderAdapter, categoryAdapter)
    }

    override fun getBottomSheetStateExpanded(isExpanded: Boolean) {
        mExpandedFolderAdapter.isDisableClick = isExpanded
        mCategoryAdapter.isDisableClick = isExpanded
    }

    override fun onBackPressed() {
        if (mBottomSheetFunction?.setBottomSheetState() == true) {
            mBottomSheetFunction?.turnOffBottomSheet()
            return
        }
        super.onBackPressed()
    }

}

