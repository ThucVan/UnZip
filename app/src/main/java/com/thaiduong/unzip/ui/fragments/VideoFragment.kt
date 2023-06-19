package com.thaiduong.unzip.ui.fragments

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.FragmentVideoBinding
import com.thaiduong.unzip.ui.activities.ReadFilesActivity
import com.thaiduong.unzip.ui.bases.BaseFragment
import com.thaiduong.unzip.utils.interfaces.IActionBarShow
import java.io.File

class VideoFragment(override val layoutId: Int = R.layout.fragment_video) :
    BaseFragment<FragmentVideoBinding>() {

    private lateinit var path: String
    private lateinit var mExoPlayer: ExoPlayer
    private lateinit var mIActionBarShow: IActionBarShow
    private var isFullScreen = false

    companion object {
        fun newInstance(path: String): VideoFragment {
            val args = Bundle()
            args.putString("path", path)
            val fragment = VideoFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun initUi() {
        path = arguments?.getString("path").toString()

        val trackSelector = DefaultTrackSelector(requireContext())
        trackSelector.setParameters(
            trackSelector
                .buildUponParameters()
                .setMaxVideoSize(300, 300)
                .setForceHighestSupportedBitrate(true)
        )
        mExoPlayer = ExoPlayer.Builder(requireContext())
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .setTrackSelector(trackSelector)
            .build()

        loadingVideo()
        mIActionBarShow = requireActivity() as ReadFilesActivity

    }

    private fun playVideo(){
        binding.videoView.player = mExoPlayer
        binding.videoView.keepScreenOn = true
        mExoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
        mExoPlayer.prepare()
        mExoPlayer.play()
    }

    private fun loadingVideo() {
        if (File(path).extension == "mp4") {
            playVideo()
        } else{
            playGif()
        }
    }

    private fun playGif() {
        binding.videoView.visibility = View.GONE
        Glide.with(this)
            .load(Uri.fromFile(File(path)))
            .into(binding.imgExoplayer)
    }


    @SuppressLint("SourceLockedOrientationActivity", "ClickableViewAccessibility")
    override fun doWork() {
        binding.videoView.videoSurfaceView?.setOnTouchListener { _, _ ->
            mIActionBarShow.isShowActionBar(!binding.videoView.isControllerVisible)
            false
        }

        binding.videoView.findViewById<ImageView>(R.id.imvFullScreen).setOnClickListener {
            if (isFullScreen) {
                binding.videoView.findViewById<ImageView>(R.id.imvFullScreen).setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_fullscreen
                    )
                )
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                binding.videoView.findViewById<ImageView>(R.id.imvFullScreen).setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_fullscreen_exit
                    )
                )
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            isFullScreen = !isFullScreen
        }
    }

    override fun onPause() {
        super.onPause()
        mExoPlayer.pause()
        mExoPlayer.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        mExoPlayer.stop()
    }
}