package com.thaiduong.unzip.ui.fragments

import android.os.Bundle
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.FragmentAllFileBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.adapters.CategoryAdapter
import com.thaiduong.unzip.ui.bases.BaseFragment
import com.thaiduong.unzip.utils.AppUtils.myGetExternalStorageDir
import com.thaiduong.unzip.utils.FileManager
import com.thaiduong.unzip.utils.interfaces.IGetItemCategory
import java.io.File

class AllFileFragment(override val layoutId: Int = R.layout.fragment_all_file) :
    BaseFragment<FragmentAllFileBinding>() {

    private lateinit var mCategoryAdapter: CategoryAdapter
    private lateinit var mIGetItemCategory: IGetItemCategory
    private var path = ""
    private var fileList = mutableListOf<FolderOrFile>()
    private lateinit var fileRootList: Array<File>

    companion object {
        @JvmStatic
        fun newInstance(type: String) =
            AllFileFragment().apply {
                arguments = Bundle().apply {
                    putString("type", type)
                }
            }
    }

    override fun initUi() {
        mCategoryAdapter = CategoryAdapter(requireContext())
        mIGetItemCategory = mContext as IGetItemCategory
        path = requireActivity().myGetExternalStorageDir().toString()
        fileRootList = File(path).listFiles()!!
    }

    override fun doWork() {
        val typeList =
            if (arguments?.getString("type") == requireActivity().getString(R.string.image)) listOf("jpg", "jpeg", "png", "webp")
            else listOf("mp4")
        for (file in fileRootList)
            FileManager.getFile(fileList, typeList, file)
        path = "${requireActivity().myGetExternalStorageDir()}/.Extracted"
        if (File(path).listFiles()?.isNotEmpty() == true) {
            fileRootList = File(path).listFiles()!!
            for (file in fileRootList)
                FileManager.getFile(fileList, typeList, file)
        }
        mCategoryAdapter.submitList(fileList)
        binding.rcvCategoryFolder.adapter = mCategoryAdapter
        mIGetItemCategory.sentData(fileList, mCategoryAdapter)
    }

}