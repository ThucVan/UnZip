package com.thaiduong.unzip.utils.customclass

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.thaiduong.unzip.App
import com.thaiduong.unzip.App.Companion.getDB
import com.thaiduong.unzip.R
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.activities.DropboxActivity
import com.thaiduong.unzip.ui.activities.ReadFilesActivity
import com.thaiduong.unzip.ui.adapters.CategoryAdapter
import com.thaiduong.unzip.ui.adapters.ExpandedFolderAdapter
import com.thaiduong.unzip.ui.adapters.SelectedItemAdapter
import com.thaiduong.unzip.utils.*
import com.thaiduong.unzip.utils.AppUtils.initProgressBarDialog
import com.thaiduong.unzip.utils.AppUtils.moveRecycleBin
import com.thaiduong.unzip.utils.AppUtils.myGetExternalStorageDir
import com.thaiduong.unzip.utils.AppUtils.restoreFile
import com.thaiduong.unzip.utils.interfaces.GetBottomSheetState
import com.thaiduong.unzip.utils.interfaces.IGetBottomSheetLayoutGravity
import com.thaiduong.unzip.utils.rxjava.CompressedFileRX
import java.io.File

class BottomSheetFunction(
    private var activity: Activity,
    private val folderName: String = ""
) {
    private lateinit var bottomView: View
    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var originalFileList: MutableList<FolderOrFile>
    private lateinit var mFolderOrFile: FolderOrFile
    private lateinit var mSelectedItemAdapter: SelectedItemAdapter
    private var mIGetBottomSheetLayoutGravity: IGetBottomSheetLayoutGravity? = null
    private var mExpandedFolderAdapter: ExpandedFolderAdapter? = null
    private var mCategoryAdapter: CategoryAdapter? = null
    private var selectedFileList = mutableListOf<FolderOrFile>()
    private lateinit var dialog: Dialog

    private var getBottomSheetState: GetBottomSheetState? = null

    fun initUi() {
        dialog = Dialog(activity, R.style.DialogStyle)
        dialog.setCancelable(false)

        bottomView = activity.findViewById(R.id.layout_function)
        if (folderName == activity.getString(R.string.recycle_bin)) {
            bottomView.findViewById<LinearLayout>(R.id.linear_recycle_bin).visibility = View.VISIBLE
            bottomView.findViewById<LinearLayout>(R.id.linear_folder_and_file).visibility =
                View.GONE
        } else {
            bottomView.findViewById<LinearLayout>(R.id.linear_recycle_bin).visibility = View.GONE
            bottomView.findViewById<LinearLayout>(R.id.linear_folder_and_file).visibility =
                View.VISIBLE
        }
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomView as LinearLayout)
        mSelectedItemAdapter = SelectedItemAdapter(activity, selectedFileList)

        mIGetBottomSheetLayoutGravity = activity as IGetBottomSheetLayoutGravity
        getBottomSheetState = activity as GetBottomSheetState
    }

    fun setData(
        originalFileList: MutableList<FolderOrFile>,
        mExpandedFolderAdapter: ExpandedFolderAdapter? = null,
        mCategoryAdapter: CategoryAdapter? = null
    ) {
        if (this.mExpandedFolderAdapter != null) this.mExpandedFolderAdapter = null
        if (this.mCategoryAdapter != null) this.mCategoryAdapter = null

        this.originalFileList = originalFileList
        this.mExpandedFolderAdapter = mExpandedFolderAdapter
        this.mCategoryAdapter = mCategoryAdapter
    }

    @SuppressLint("SetTextI18n")
    fun setOnSelectedItem(mFolderOrFile: FolderOrFile) {
        this.mFolderOrFile = mFolderOrFile
        if (mFolderOrFile.isSelected) {
            selectedFileList.add(mFolderOrFile)
        } else {
            selectedFileList.remove(mFolderOrFile)
        }

        setBottomSheetState()
        initView()
        setBottomSheetOnClickListener()
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        if (selectedFileList.size < 10) {
            bottomView.findViewById<TextView>(R.id.tv_amount_item_selected).text =
                "Select ${selectedFileList.size} item"
        } else {
            bottomView.findViewById<TextView>(R.id.tv_amount_item_selected).text =
                "Select ${selectedFileList.size} items"
        }

        bottomView.findViewById<ImageButton>(R.id.btn_rename)
            .setImageResource(R.drawable.ic_bottom_sheet_rename)
        bottomView.findViewById<ImageButton>(R.id.btn_rename).isEnabled = true
        bottomView.findViewById<TextView>(R.id.tvMenuRename)
            .setTextColor(Color.parseColor("#FFFFFFFF"))

        bottomView.findViewById<ImageButton>(R.id.btn_compress)
            .setImageResource(R.drawable.ic_bottom_sheet_compress)
        bottomView.findViewById<ImageButton>(R.id.btn_compress).isEnabled = true
        bottomView.findViewById<TextView>(R.id.tvMenuCompress)
            .setTextColor(Color.parseColor("#FFFFFFFF"))

        bottomView.findViewById<ImageButton>(R.id.btn_extracted)
            .setImageResource(R.drawable.ic_bottom_sheet_extracted)
        bottomView.findViewById<ImageButton>(R.id.btn_extracted).isEnabled = true
        bottomView.findViewById<TextView>(R.id.tvMenuExtracted)
            .setTextColor(Color.parseColor("#FFFFFFFF"))

        bottomView.findViewById<ImageButton>(R.id.btn_export)
            .setImageResource(R.drawable.ic_bottom_sheet_export)
        bottomView.findViewById<ImageButton>(R.id.btn_export).isEnabled = true
        bottomView.findViewById<TextView>(R.id.tvMenuExport)
            .setTextColor(Color.parseColor("#FFFFFFFF"))

        bottomView.findViewById<ImageButton>(R.id.btn_share)
            .setImageResource(R.drawable.ic_bottom_sheet_share)
        bottomView.findViewById<ImageButton>(R.id.btn_share).isEnabled = true
        bottomView.findViewById<TextView>(R.id.tvMenuShare)
            .setTextColor(Color.parseColor("#FFFFFFFF"))

        if (selectedFileList.size > 1) {
            bottomView.findViewById<ImageButton>(R.id.btn_rename)
                .setImageResource(R.drawable.ic_rename_invisible)
            bottomView.findViewById<ImageButton>(R.id.btn_rename).isEnabled = false
            bottomView.findViewById<TextView>(R.id.tvMenuRename)
                .setTextColor(Color.parseColor("#FFAEAEAE"))

            bottomView.findViewById<ImageButton>(R.id.btn_extracted)
                .setImageResource(R.drawable.ic_extracted_invisible)
            bottomView.findViewById<ImageButton>(R.id.btn_extracted).isEnabled = false
            bottomView.findViewById<TextView>(R.id.tvMenuExtracted)
                .setTextColor(Color.parseColor("#FFAEAEAE"))

        }
        for (file in selectedFileList) {
            if (file.mFile?.extension in listOf("zip", "rar", "7z", "tar")) {
                bottomView.findViewById<ImageButton>(R.id.btn_compress)
                    .setImageResource(R.drawable.ic_compress_invisible)
                bottomView.findViewById<ImageButton>(R.id.btn_compress).isEnabled = false
                bottomView.findViewById<TextView>(R.id.tvMenuCompress)
                    .setTextColor(Color.parseColor("#FFAEAEAE"))
            } else {
                bottomView.findViewById<ImageButton>(R.id.btn_extracted)
                    .setImageResource(R.drawable.ic_extracted_invisible)
                bottomView.findViewById<ImageButton>(R.id.btn_extracted).isEnabled = false
                bottomView.findViewById<TextView>(R.id.tvMenuExtracted)
                    .setTextColor(Color.parseColor("#FFAEAEAE"))
            }
            if (file.mFile?.isDirectory == true) {
                bottomView.findViewById<ImageButton>(R.id.btn_export)
                    .setImageResource(R.drawable.ic_export_invisible)
                bottomView.findViewById<ImageButton>(R.id.btn_export).isEnabled = false
                bottomView.findViewById<TextView>(R.id.tvMenuExport)
                    .setTextColor(Color.parseColor("#FFAEAEAE"))

                bottomView.findViewById<ImageButton>(R.id.btn_share)
                    .setImageResource(R.drawable.ic_share_invisible)
                bottomView.findViewById<ImageButton>(R.id.btn_share).isEnabled = false
                bottomView.findViewById<TextView>(R.id.tvMenuShare)
                    .setTextColor(Color.parseColor("#FFAEAEAE"))

                if (selectedFileList.size > 1) {
                    bottomView.findViewById<ImageButton>(R.id.btn_compress)
                        .setImageResource(R.drawable.ic_compress_invisible)
                    bottomView.findViewById<ImageButton>(R.id.btn_compress).isEnabled = false
                    bottomView.findViewById<TextView>(R.id.tvMenuCompress)
                        .setTextColor(Color.parseColor("#FFAEAEAE"))
                }
            }
        }

    }

    @SuppressLint("SetTextI18n", "InflateParams", "NotifyDataSetChanged", "LogNotTimber")
    private fun setBottomSheetOnClickListener() {
        bottomView.findViewById<ImageView>(R.id.imv_expanded).setOnClickListener {
            bottomView.findViewById<LinearLayout>(R.id.linear_title).visibility = View.GONE
            bottomView.findViewById<LinearLayout>(R.id.linear_recycle_bin).visibility = View.GONE
            bottomView.findViewById<LinearLayout>(R.id.linear_folder_and_file).visibility =
                View.GONE
            bottomView.findViewById<LinearLayout>(R.id.linear_expanded).visibility = View.VISIBLE
            bottomView.background = AppCompatResources.getDrawable(
                activity as Context,
                R.drawable.bg_bottom_sheet_visibility
            )
            mSelectedItemAdapter.setData(selectedFileList)
            bottomView.findViewById<RecyclerView>(R.id.rcv_file_selected).adapter =
                mSelectedItemAdapter
            getBottomSheetState?.getBottomSheetStateExpanded(true)
        }

        bottomView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            bottomView.findViewById<LinearLayout>(R.id.linear_title).visibility = View.VISIBLE
            if (folderName == activity.getString(R.string.recycle_bin)) {
                bottomView.findViewById<LinearLayout>(R.id.linear_recycle_bin).visibility =
                    View.VISIBLE
                bottomView.findViewById<LinearLayout>(R.id.linear_folder_and_file).visibility =
                    View.GONE
            } else {
                bottomView.findViewById<LinearLayout>(R.id.linear_recycle_bin).visibility =
                    View.GONE
                bottomView.findViewById<LinearLayout>(R.id.linear_folder_and_file).visibility =
                    View.VISIBLE
            }
            bottomView.findViewById<LinearLayout>(R.id.linear_expanded).visibility = View.GONE
            bottomView.background =
                AppCompatResources.getDrawable(activity as Context, R.drawable.bg_bottom_sheet)
            setBottomSheetState()
            for (file in originalFileList) {
                if (!selectedFileList.contains(file))
                    file.isSelected = false
            }

            initView()

            if (selectedFileList.isEmpty()) turnOffBottomSheet()
            changeData(originalFileList)
            getBottomSheetState?.getBottomSheetStateExpanded(false)
        }

        bottomView.findViewById<TextView>(R.id.btn_clearAll).setOnClickListener {
            selectedFileList.clear()
            setBottomSheetState()
            Log.e(
                "BottomSheetFunction",
                "btn_clearAll - originalFileList: ${originalFileList.size}"
            )
            for (file in originalFileList) {
                file.isSelected = false
            }
            changeData(originalFileList)
            mExpandedFolderAdapter?.clearSelectedFile()
            mCategoryAdapter?.clearSelectedFile()
        }

        bottomView.findViewById<ImageButton>(R.id.btn_delete).setOnClickListener {
            activity.moveRecycleBin(
                activity,
                originalFileList,
                selectedFileList,
                this
            )
        }

        bottomView.findViewById<ImageButton>(R.id.btn_rename).setOnClickListener {
            val view = LayoutInflater.from(activity).inflate(R.layout.rename_dialog, null, false)
            dialog.setContentView(view)
            dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }
            dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
                dialog.dismiss()
                val filenameNew = dialog.findViewById<EditText>(R.id.ed_fileName).text
                val currentFile = mFolderOrFile.mFile!!.absoluteFile
                if (FunctionApp().renameFile(
                        activity,
                        filenameNew.toString(),
                        currentFile
                    )
                ) {
                    val folderOrFileOld = FolderOrFile()
                    val folderOrFileNew = FolderOrFile()
                    folderOrFileOld.mFile = currentFile
                    folderOrFileNew.mFile = File(
                        currentFile.parentFile,
                        filenameNew.toString() + "." + currentFile.extension
                    )
                    originalFileList.remove(mFolderOrFile)
                    originalFileList.add(folderOrFileNew)
                    changeData(originalFileList)
                    selectedFileList.clear()
                    setBottomSheetState()
                }
                val imm =
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(bottomView.windowToken, 0)
            }
            dialog.show()
        }

        bottomView.findViewById<ImageButton>(R.id.btn_compress).setOnClickListener {
            compressFile()
        }

        bottomView.findViewById<ImageButton>(R.id.btn_extracted).setOnClickListener {
            val intent = Intent(activity as Context, ReadFilesActivity::class.java)
            val path = mFolderOrFile.mFile?.absolutePath
            SingletonLastSong.getInstance(activity).path = path
            intent.putExtra(PATH, path)
            intent.putExtra(EXTENSION_FILE, mFolderOrFile.mFile?.extension)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            activity.startActivity(intent)
        }

        bottomView.findViewById<ImageButton>(R.id.btn_export).setOnClickListener {
            getBottomSheetState?.getBottomSheetStateExpanded(true)
            bottomView.findViewById<LinearLayout>(R.id.linear_title).visibility = View.GONE
            bottomView.findViewById<LinearLayout>(R.id.linear_folder_and_file).visibility =
                View.GONE
            bottomView.findViewById<LinearLayout>(R.id.export).visibility = View.VISIBLE
            bottomView.background = AppCompatResources.getDrawable(activity as Context, R.drawable.bg_bottom_sheet_visibility)
            bottomView.findViewById<LinearLayout>(R.id.btn_exportDriver).setOnClickListener {
                val intent = Intent(activity, DropboxActivity::class.java)
                val fileSelect = Gson().toJson(selectedFileList)
                intent.putExtra("fileSelected", fileSelect)
                App.dataStore.putString("fileSelected", fileSelect)
                activity.startActivity(intent)
                selectedFileList.clear()
                turnOffBottomSheet()
            }
        }

        bottomView.findViewById<ImageButton>(R.id.btn_share).setOnClickListener {
            val pathList = mutableListOf<String>()
            for (file in selectedFileList) {
                pathList.add(file.mFile?.absolutePath.toString())
            }
            AppUtils.shareFile(activity, pathList, "*")
            selectedFileList.clear()
            for (file in originalFileList)
                file.isSelected = false
            setBottomSheetState()
            changeData(originalFileList)
        }

        bottomView.findViewById<ImageView>(R.id.imvDelete).setOnClickListener {
            dialog.setContentView(
                LayoutInflater.from(activity).inflate(R.layout.delete_dialog, null, false)
            )
            dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }
            dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
                dialog.dismiss()
                for (file in selectedFileList) {
                    getDB().fileDataDao().deleteFileDelete(
                        file.mFile?.name.toString(),
                        file.mFile?.absolutePath.toString()
                    )
                    if (file.mFile?.isDirectory == true) {
                        file.mFile?.deleteRecursively()
                    } else {
                        file.mFile?.delete()
                    }
                    originalFileList.remove(file)
                }
                selectedFileList.clear()
                setBottomSheetState()
                changeData(originalFileList)
                if (originalFileList.isEmpty()) {
                    activity.findViewById<ImageView>(R.id.imvNoFilesFound).visibility = View.VISIBLE
                    activity.findViewById<Button>(R.id.btn_clear_all).visibility = View.GONE
                } else {
                    activity.findViewById<ImageView>(R.id.imvNoFilesFound).visibility = View.GONE
                    activity.findViewById<Button>(R.id.btn_clear_all).visibility = View.VISIBLE
                }
            }
            dialog.show()
        }

        bottomView.findViewById<ImageView>(R.id.imvRestore).setOnClickListener {
            dialog.setContentView(
                LayoutInflater.from(activity).inflate(R.layout.restore_dialog, null, false)
            )
            dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }
            dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
                dialog.dismiss()
                val progressDialog = activity.initProgressBarDialog("Restore")
                progressDialog.show()
                Thread {
                    activity.runOnUiThread {
                        run {
                            for (file in selectedFileList) {
                                val fileName = file.mFile?.name
                                val input =
                                    "${activity.myGetExternalStorageDir()}/.Recycle Bin/$fileName"
                                val output = fileName?.let { it1 ->
                                    getDB().fileDataDao().getOriginalPath(
                                        it1
                                    )
                                }
                                if (output?.let { it1 -> File(it1).exists() } == true) {
                                    val mDialog = Dialog(activity, R.style.DialogStyle)
                                    mDialog.setContentView(
                                        LayoutInflater.from(activity)
                                            .inflate(R.layout.restore_file_dialog, null, false)
                                    )
                                    mDialog.setCancelable(false)
                                    mDialog.findViewById<TextView>(R.id.tv_folder_name).text =
                                        "Moving 1 item to ${File(output).parentFile?.name}"
                                    mDialog.findViewById<TextView>(R.id.tv_notify_content).text =
                                        "The destination already has a file name \"${fileName}\""
                                    mDialog.findViewById<LinearLayout>(R.id.linear_skip)
                                        .setOnClickListener {
                                            mDialog.dismiss()
                                        }
                                    mDialog.findViewById<LinearLayout>(R.id.linear_replace)
                                        .setOnClickListener {
                                            mDialog.dismiss()
                                            if (File(output).isDirectory) {
                                                File(output).deleteRecursively()
                                            } else {
                                                File(output).delete()
                                            }
                                            restoreFile(input, output)
                                            val folderOrFile = FolderOrFile()
                                            folderOrFile.mFile = File(output)
                                        }
                                    mDialog.show()
                                } else {
                                    output?.let { it1 -> restoreFile(input, it1) }
                                    val folderOrFile = FolderOrFile()
                                    folderOrFile.mFile = output?.let { it1 -> File(it1) }
                                }
                                originalFileList.remove(file)
                            }
                            selectedFileList.clear()
                            setBottomSheetState()
                            changeData(originalFileList)
                            if (originalFileList.isEmpty()) {
                                activity.findViewById<ImageView>(R.id.imvNoFilesFound).visibility =
                                    View.VISIBLE
                                activity.findViewById<Button>(R.id.btn_clear_all).visibility =
                                    View.GONE
                            } else {
                                activity.findViewById<ImageView>(R.id.imvNoFilesFound).visibility =
                                    View.GONE
                                activity.findViewById<Button>(R.id.btn_clear_all).visibility =
                                    View.VISIBLE
                            }
                            progressDialog.dismiss()
                        }
                    }
                }.start()
            }
            dialog.show()
        }

    }

    @SuppressLint("InflateParams")
    private fun compressFile() {
        var typeFile = ""
        dialog.setContentView(
            LayoutInflater.from(activity).inflate(R.layout.compress_dialog, null, false)
        )
        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<LinearLayout>(R.id.btn_rar).setOnClickListener {
            changeBgChoose(dialog.findViewById(R.id.btn_rar))
            dialog.findViewById<TextView>(R.id.tv_pass).visibility = View.GONE
            dialog.findViewById<TextInputLayout>(R.id.textField).visibility = View.GONE
            dialog.findViewById<RelativeLayout>(R.id.tv_alert).visibility = View.VISIBLE
            typeFile = "rar"
        }

        dialog.findViewById<LinearLayout>(R.id.btn_zip).setOnClickListener {
            changeBgChoose(dialog.findViewById(R.id.btn_zip))
            dialog.findViewById<TextView>(R.id.tv_pass).visibility = View.VISIBLE
            dialog.findViewById<TextInputLayout>(R.id.textField).visibility = View.VISIBLE
            dialog.findViewById<RelativeLayout>(R.id.tv_alert).visibility = View.GONE
            typeFile = "zip"
        }

        dialog.findViewById<LinearLayout>(R.id.btn_tar).setOnClickListener {
            changeBgChoose(dialog.findViewById(R.id.btn_tar))
            dialog.findViewById<TextView>(R.id.tv_pass).visibility = View.GONE
            dialog.findViewById<TextInputLayout>(R.id.textField).visibility = View.GONE
            dialog.findViewById<RelativeLayout>(R.id.tv_alert).visibility = View.VISIBLE
            typeFile = "tar"
        }

        dialog.findViewById<LinearLayout>(R.id.btn_7zip).setOnClickListener {
            changeBgChoose(dialog.findViewById(R.id.btn_7zip))
            dialog.findViewById<TextView>(R.id.tv_pass).visibility = View.GONE
            dialog.findViewById<TextInputLayout>(R.id.textField).visibility = View.GONE
            dialog.findViewById<RelativeLayout>(R.id.tv_alert).visibility = View.VISIBLE
            typeFile = "7z"
        }

        val fileName = dialog.findViewById<EditText>(R.id.ed_fileName).text
        val passWord = dialog.findViewById<TextInputEditText>(R.id.ed_pass).text

        dialog.findViewById<Button>(R.id.btnCompress).setOnClickListener {
            if (fileName.toString().trim() == "") {
                Toast.makeText(activity, "File name not null!", Toast.LENGTH_SHORT).show()
            } else if (typeFile == "") {
                Toast.makeText(activity, "Please select typeFile first!", Toast.LENGTH_SHORT).show()
            } else {
                //call compress
                dialog.dismiss()
                val compressedFileRX = CompressedFileRX(
                    activity,
                    selectedFileList,
                    fileName.toString(),
                    passWord.toString(),
                    typeFile
                )
                compressedFileRX.letSubscribe()
                for (file in originalFileList) {
                    file.isSelected = false
                }
                selectedFileList.clear()
                activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                val imm =
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(bottomView.windowToken, 0)
                turnOffBottomSheet()
            }
        }
        dialog.show()
    }

    private var lastChoice: LinearLayout? = null

    private fun changeBgChoose(frameViewSelected: LinearLayout?) {
        lastChoice?.setBackgroundResource(R.drawable.bg_fun2)
        frameViewSelected?.setBackgroundResource(R.drawable.bg_fun2_choose)
        lastChoice = frameViewSelected
    }

    fun turnOffBottomSheet() {
        selectedFileList.clear()
        for (file in originalFileList) {
            file.isSelected = false
        }
        mExpandedFolderAdapter?.clearSelectedFile()
        mCategoryAdapter?.clearSelectedFile()
        setBottomSheetState()

        bottomView.background = AppCompatResources.getDrawable(activity as Context, R.drawable.bg_bottom_sheet)
        bottomView.findViewById<LinearLayout>(R.id.linear_title).visibility = View.VISIBLE
        bottomView.findViewById<LinearLayout>(R.id.linear_folder_and_file).visibility =
            View.VISIBLE
        bottomView.findViewById<LinearLayout>(R.id.export).visibility = View.GONE
        getBottomSheetState?.getBottomSheetStateExpanded(false)
    }

    fun setBottomSheetState(selectedList: MutableList<FolderOrFile>? = null): Boolean {
        val isShow = selectedList?.isNotEmpty() ?: selectedFileList.isNotEmpty()
        mBottomSheetBehavior.isDraggable = !isShow
        mBottomSheetBehavior.state =
            if (isShow) BottomSheetBehavior.STATE_EXPANDED
            else BottomSheetBehavior.STATE_COLLAPSED
        if (!isShow) {
            mExpandedFolderAdapter?.clearSelectedFile()
            mCategoryAdapter?.clearSelectedFile()
        }
        mIGetBottomSheetLayoutGravity?.getLayoutGravity(isShow)
        return isShow
    }

    private fun changeData(originalList: MutableList<FolderOrFile>? = null) {
        mExpandedFolderAdapter?.submitList(originalList)
        mCategoryAdapter?.submitList(originalList)
    }
}