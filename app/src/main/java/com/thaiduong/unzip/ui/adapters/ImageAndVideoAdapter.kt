package com.thaiduong.unzip.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.thaiduong.unzip.ui.fragments.AllFileFragment
import com.thaiduong.unzip.ui.fragments.CategoryFileFragment

class ImageAndVideoAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val folderName: String
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CategoryFileFragment.newInstance(folderName)
            1 -> AllFileFragment.newInstance(folderName)
            else -> Fragment()
        }
    }

}