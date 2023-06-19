package com.thaiduong.unzip.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.FragmentExplorerBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.activities.FileListActivity
import com.thaiduong.unzip.ui.bases.BaseFragment
import com.thaiduong.unzip.utils.AppUtils.myGetExternalStorageDir
import com.thaiduong.unzip.utils.FOLDER_NAME
import com.thaiduong.unzip.utils.FileManager
import java.io.File

class ExplorerFragment(override val layoutId: Int = R.layout.fragment_explorer) :
    BaseFragment<FragmentExplorerBinding>() {

    @SuppressLint("SetTextI18n")
    override fun initUi() {
        val root = requireActivity().myGetExternalStorageDir()?.let { File(it) }
        val filesAndFolders = root?.listFiles()!!
        val imageFilesList = mutableListOf<FolderOrFile>()
        val videoFilesList = mutableListOf<FolderOrFile>()
        val audioFilesList = mutableListOf<FolderOrFile>()
        val documentFilesList = mutableListOf<FolderOrFile>()
        val apkFilesList = mutableListOf<FolderOrFile>()
        for (file in filesAndFolders) {
            FileManager.getFile(imageFilesList, listOf("jpg", "jpeg", "png", "webp"), file)
            FileManager.getFile(videoFilesList, listOf("mp4"), file)
            FileManager.getFile(audioFilesList, listOf("mp3"), file)
            FileManager.getFile(
                documentFilesList,
                listOf("xlsx", "docx", "pptx", "pdf", "txt"),
                file
            )
            FileManager.getFile(apkFilesList, listOf("apk"), file)
        }
        //Image
        if (imageFilesList.size > 10)
            binding.tvImageAmount.text = "${imageFilesList.size} ${getString(R.string.items)}"
        else
            binding.tvImageAmount.text = "${imageFilesList.size} ${getString(R.string.item)}"
        //Video
        if (videoFilesList.size > 10)
            binding.tvVideoAmount.text = "${videoFilesList.size} ${getString(R.string.items)}"
        else
            binding.tvVideoAmount.text = "${videoFilesList.size} ${getString(R.string.item)}"
        //Audio
        if (audioFilesList.size > 10)
            binding.tvAudioAmount.text = "${audioFilesList.size} ${getString(R.string.items)}"
        else
            binding.tvAudioAmount.text = "${audioFilesList.size} ${getString(R.string.item)}"
        //Document
        if (documentFilesList.size > 10)
            binding.tvDocumentAmount.text = "${documentFilesList.size} ${getString(R.string.items)}"
        else
            binding.tvDocumentAmount.text = "${documentFilesList.size} ${getString(R.string.item)}"
        //Download
        val fileList = File("${requireActivity().myGetExternalStorageDir()}/Download")
        if (fileList.listFiles()?.size!! > 10)
            binding.tvDownloadAmount.text = "${fileList.listFiles()?.size!!} ${getString(R.string.items)}"
        else
            binding.tvDownloadAmount.text = "${fileList.listFiles()?.size!!} ${getString(R.string.item)}"
        //Apk
        if (apkFilesList.size > 10)
            binding.tvApkAmount.text = "${apkFilesList.size} ${getString(R.string.items)}"
        else
            binding.tvApkAmount.text = "${apkFilesList.size} ${getString(R.string.item)}"
    }


    override fun doWork() {
        binding.linearImage.setOnClickListener { openFolderCategory(getString(R.string.image)) }
        binding.linearVideo.setOnClickListener { openFolderCategory(getString(R.string.video)) }
        binding.linearAudio.setOnClickListener { openFolderCategory(getString(R.string.audio)) }
        binding.linearDocument.setOnClickListener { openFolderCategory(getString(R.string.document)) }
        binding.linearDownload.setOnClickListener { openFolderCategory(getString(R.string.download)) }
        binding.linearApk.setOnClickListener { openFolderCategory(getString(R.string.apk)) }
    }

    private fun openFolderCategory(folderName: String) {
        val intent = Intent(mContext, FileListActivity::class.java)
        intent.putExtra(FOLDER_NAME, folderName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        mContext?.startActivity(intent)
    }

}