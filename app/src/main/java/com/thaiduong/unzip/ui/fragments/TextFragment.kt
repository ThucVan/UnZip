package com.thaiduong.unzip.ui.fragments

import android.os.Bundle
import android.view.View
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.FragmentTextBinding
import com.thaiduong.unzip.ui.bases.BaseFragment
import java.io.BufferedReader
import java.io.File

class TextFragment(override val layoutId: Int = R.layout.fragment_text) :
    BaseFragment<FragmentTextBinding>() {

    private lateinit var path: String

    companion object {
        fun newInstance(path: String): TextFragment {
            val args = Bundle()
            args.putString("path", path)
            val fragment = TextFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun initUi() {
        path = arguments?.getString("path").toString()
    }

    override fun doWork() {
        if (File(path).extension == "pdf") {
            binding.pdfView.fromFile(File(path)).load()
        } else {
            binding.pdfView.visibility = View.GONE
            binding.scrollText.visibility = View.VISIBLE
            val bufferedReader: BufferedReader = File(path).bufferedReader()
            val inputString = bufferedReader.use { it.readText() }
            binding.tvText.text = inputString
        }
    }

}