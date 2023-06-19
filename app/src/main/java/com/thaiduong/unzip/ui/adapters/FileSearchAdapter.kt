package com.thaiduong.unzip.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thaiduong.unzip.databinding.ItemFileSearchBinding
import com.thaiduong.unzip.models.database.FileDataSearch
import com.thaiduong.unzip.utils.interfaces.IItemPathSelected

class FileSearchAdapter(mActivity: Activity) :
    ListAdapter<FileDataSearch, FileSearchAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<FileDataSearch>() {
            override fun areItemsTheSame(
                oldItem: FileDataSearch,
                newItem: FileDataSearch
            ): Boolean {
                return oldItem.fileName == newItem.fileName && oldItem.filePath == newItem.filePath
            }

            override fun areContentsTheSame(
                oldItem: FileDataSearch,
                newItem: FileDataSearch
            ): Boolean {
                return oldItem.fileName == newItem.fileName && oldItem.filePath == newItem.filePath
            }

        }
    ) {

    private var mIItemPathSelected = mActivity as IItemPathSelected

    inner class ViewHolder(itemView: ItemFileSearchBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        private val binding = itemView
        fun onBind(item: FileDataSearch) {
            binding.tvFileName.text = item.fileName
            binding.tvPath.text = item.filePath
            itemView.setOnClickListener {
                mIItemPathSelected.itemPathSelected(item.filePath)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemFileSearchBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder).onBind(getItem(position))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun submitList(list: MutableList<FileDataSearch>?) {
        super.submitList(list)
        notifyDataSetChanged()
    }

}