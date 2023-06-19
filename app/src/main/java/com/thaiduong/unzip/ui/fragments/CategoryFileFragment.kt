package com.thaiduong.unzip.ui.fragments

import android.os.Bundle
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.FragmentCategoryFileBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.adapters.CategoryFileAdapter
import com.thaiduong.unzip.ui.bases.BaseFragment
import com.thaiduong.unzip.utils.AppUtils.myGetExternalStorageDir
import com.thaiduong.unzip.utils.FileManager
import java.io.File

class CategoryFileFragment(override val layoutId: Int = R.layout.fragment_category_file) :
    BaseFragment<FragmentCategoryFileBinding>() {

    private lateinit var mCategoryFileAdapter: CategoryFileAdapter
    private var path = ""
    private val fileList = mutableListOf<FolderOrFile>()
    private val folderList = mutableListOf<FolderOrFile>()
    private lateinit var fileRootList: Array<File>

    companion object {
        fun newInstance(type: String) =
            CategoryFileFragment().apply {
                arguments = Bundle().apply {
                    putString("type", type)
                }
            }
    }

    override fun initUi() {
        path = requireActivity().myGetExternalStorageDir().toString()
        fileRootList = File(path).listFiles()!!
    }

    override fun doWork() {
        val typeList =
            if (arguments?.getString("type") == requireActivity().getString(R.string.image)) listOf("jpg", "jpeg", "png", "webp")
            else listOf("mp4")
        for (file in fileRootList)
            FileManager.getFile(fileList, typeList, file)
        for (file in fileList) {
            val folderOrFile = FolderOrFile(file.mFile?.parentFile)
            if (!folderList.contains(folderOrFile))
                folderList.add(folderOrFile)
        }
        mCategoryFileAdapter = CategoryFileAdapter(requireContext(), typeList)
        mCategoryFileAdapter.submitList(folderList)
        binding.rcvCategoryFolder.adapter = mCategoryFileAdapter
    }

}