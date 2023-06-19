package com.thaiduong.unzip.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.thaiduong.unzip.databinding.ItemFolderBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.activities.FileListActivity
import com.thaiduong.unzip.utils.CATEGORY_NAME
import com.thaiduong.unzip.utils.FOLDER_NAME
import com.thaiduong.unzip.utils.PATH

class CategoryFileAdapter(
    private var mContext: Context,
    private var typeList: List<String>? = null
) : ListAdapter<FolderOrFile, CategoryFileAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<FolderOrFile>() {
            override fun areItemsTheSame(
                oldItem: FolderOrFile,
                newItem: FolderOrFile
            ): Boolean {
                return oldItem.mFile == newItem.mFile && oldItem.isSelected == newItem.isSelected
            }

            override fun areContentsTheSame(
                oldItem: FolderOrFile,
                newItem: FolderOrFile
            ): Boolean {
                return oldItem.mFile == newItem.mFile && oldItem.isSelected == newItem.isSelected
            }

        }
    ) {

    inner class ViewHolder(itemView: ItemFolderBinding) : RecyclerView.ViewHolder(itemView.root) {
        private val binding = itemView
        fun onBind(item: FolderOrFile) {

            val list = mutableListOf<FolderOrFile>()
            binding.tvTitle.text = item.mFile?.nameWithoutExtension
            for (file in item.mFile?.listFiles()!!) {
                if (typeList?.contains(file.extension) == true) {
                    list.add(FolderOrFile(file))
                }
            }
            Glide.with(mContext)
                .load(list[0].mFile)
                .centerCrop()
                .into(binding.imvThumbnails)
            binding.tvAmountItem.text = "${list.size}"

            itemView.setOnClickListener {
                val path = item.mFile?.path
                val intent = Intent(mContext, FileListActivity::class.java)
                intent.putExtra(FOLDER_NAME, item.mFile?.nameWithoutExtension)
                intent.putExtra(CATEGORY_NAME, Gson().toJson(typeList))
                intent.putExtra(PATH, path)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                mContext.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemFolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        (holder).onBind(item)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun submitList(list: MutableList<FolderOrFile>?) {
        super.submitList(list)
        notifyDataSetChanged()
    }

}