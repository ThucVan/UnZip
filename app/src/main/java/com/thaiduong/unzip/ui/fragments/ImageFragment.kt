package com.thaiduong.unzip.ui.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.davemorrissey.labs.subscaleview.ImageSource
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.FragmentImageBinding
import com.thaiduong.unzip.ui.activities.ReadFilesActivity
import com.thaiduong.unzip.ui.bases.BaseFragment
import com.thaiduong.unzip.utils.customview.OnSwipeTouchListener
import com.thaiduong.unzip.utils.interfaces.IActionBarShow
import java.io.File

class ImageFragment(override val layoutId: Int = R.layout.fragment_image) :
    BaseFragment<FragmentImageBinding>() {

    private lateinit var path: String
    private lateinit var mIActionBarShow: IActionBarShow
    private var currentPosition = -1
    private var imageList = mutableListOf<File>()
    private var isShow = true

    companion object {
        fun newInstance(path: String): ImageFragment {
            val args = Bundle()
            args.putString("path", path)
            val fragment = ImageFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun initUi() {
        path = arguments?.getString("path").toString()
        mIActionBarShow = requireActivity() as ReadFilesActivity
        binding.imvScreen.setImage(ImageSource.uri(path))
        val folderName = File(path).parent!!
        for (mFile in File(folderName).listFiles()!!) {
            if (mFile.extension in listOf("jpg", "jpeg", "png", "webp")) {
                imageList.add(mFile)
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvIndex.visibility = View.GONE
        }, 5000)

        getIndex()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun doWork() {
        mContext?.let {
            binding.imvScreen.setOnTouchListener(object : OnSwipeTouchListener(it) {
                @SuppressLint("SetTextI18n")
                override fun onSwipeLeft() {
                    super.onSwipeLeft()
                    currentPosition++
                    binding.imvScreen.setImage(ImageSource.uri(imageList[currentPosition].path))
                    binding.tvIndex.text = "${currentPosition + 1} / ${imageList.size}"

                }

                @SuppressLint("SetTextI18n")
                override fun onSwipeRight() {
                    super.onSwipeRight()
                    currentPosition--
                    binding.imvScreen.setImage(ImageSource.uri(imageList[currentPosition].path))
                    binding.tvIndex.text = "${currentPosition + 1} / ${imageList.size}"
                }

                override fun onClick() {
                    super.onClick()
                    isShow = !isShow
                    if (isShow) {
                        binding.tvIndex.visibility = View.VISIBLE
                    } else {
                        binding.tvIndex.visibility = View.GONE
                    }
                    mIActionBarShow.isShowActionBar(isShow)
                }
            })
        }
    }


    @SuppressLint("SetTextI18n")
    private fun getIndex() {
        for (i in imageList.indices) {
            if (imageList[i].name == File(path).name) {
                imageList[i].path
                currentPosition = i
                binding.tvIndex.text = "${i + 1} / ${imageList.size}"
                break
            }
        }
    }

    fun imageRotating(degrees: Float) {
        val yourSelectedImage = BitmapFactory.decodeFile(path)
        val mat = Matrix()
        mat.postRotate(degrees) //degree how much you rotate i rotate degrees

        val bMapRotate = Bitmap.createBitmap(
            yourSelectedImage, 0, 0, yourSelectedImage.width, yourSelectedImage.height, mat, true
        )
        binding.imvScreen.setImage(ImageSource.bitmap(bMapRotate))
    }

}