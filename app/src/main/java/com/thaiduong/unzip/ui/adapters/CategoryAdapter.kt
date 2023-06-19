package com.thaiduong.unzip.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ItemCategoryBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.activities.ReadFilesActivity
import com.thaiduong.unzip.utils.EXTENSION_FILE
import com.thaiduong.unzip.utils.FileFormat
import com.thaiduong.unzip.utils.PATH
import com.thaiduong.unzip.utils.customview.FileUtility.getMediaDuration
import com.thaiduong.unzip.utils.interfaces.IItemSelected

class CategoryAdapter(private var mContext: Context) :
    ListAdapter<FolderOrFile, CategoryAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<FolderOrFile>() {
            override fun areItemsTheSame(oldItem: FolderOrFile, newItem: FolderOrFile): Boolean {
                return oldItem.mFile == newItem.mFile && oldItem.isSelected == newItem.isSelected
            }

            override fun areContentsTheSame(oldItem: FolderOrFile, newItem: FolderOrFile): Boolean {
                return oldItem.mFile == newItem.mFile && oldItem.isSelected == newItem.isSelected
            }

        }
    ) {

    private var mIItemSelected = mContext as IItemSelected
    private var isOpenBottomMenu = false
    var isDisableClick = false

    inner class ViewHolder(itemView: ItemCategoryBinding) : RecyclerView.ViewHolder(itemView.root) {
        private val binding = itemView
        fun onBind(item: FolderOrFile) {
            Glide.with(mContext)
                .load(item.mFile)
                .centerCrop()
                .into(binding.imvThumbnails)
            when (item.mFile?.extension) {
                "mp4" -> {
                    binding.tvTimeCount.visibility = View.VISIBLE
                    binding.tvTimeCount.text = item.mFile?.getMediaDuration(mContext)
                        ?.let { FileFormat.timeFormat(it) }
                }
                else -> {
                    binding.tvTimeCount.visibility = View.GONE
                }
            }
            if (!isDisableClick) {
                binding.imvSelected.setOnClickListener {
                    if (item.isSelected) {
                        binding.imvSelected.setImageResource(R.drawable.ic_unselected)
                        item.isSelected = false
                    } else {
                        binding.imvSelected.setImageResource(R.drawable.ic_selected)
                        item.isSelected = true
                    }
                    //isOpenBottomMenu = item.isSelected
                    mIItemSelected.selectedItem(item)
                }

                itemView.setOnClickListener {
                    if (isOpenBottomMenu) {
                        if (item.isSelected) {
                            binding.imvSelected.setImageResource(R.drawable.ic_unselected)
                            item.isSelected = false
                        } else {
                            binding.imvSelected.setImageResource(R.drawable.ic_selected)
                            item.isSelected = true
                        }
                        mIItemSelected.selectedItem(item)
                    } else {
                        val path = item.mFile?.absolutePath
                        val intent = Intent(mContext, ReadFilesActivity::class.java)
                        intent.putExtra(PATH, path)
                        intent.putExtra(EXTENSION_FILE, item.mFile?.extension)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        mContext.startActivity(intent)
                    }
                }
            }
//            if (isOpenBottomMenu) {
//                binding.imvSelected.visibility = View.VISIBLE
//            } else {
//                binding.imvSelected.visibility = View.GONE
//            }
            if (item.isSelected) {
                binding.imvSelected.setImageResource(R.drawable.ic_selected)
            } else {
                binding.imvSelected.setImageResource(R.drawable.ic_unselected)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        (holder).onBind(item)
//        holder.itemView.setOnLongClickListener {
//            item.isSelected = true
//            isOpenBottomMenu = true
//            mIItemSelected.selectedItem(item)
//            submitList(currentList)
//            false
//        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun submitList(list: MutableList<FolderOrFile>?) {
        super.submitList(list)
        notifyDataSetChanged()
    }

    fun clearSelectedFile() {
        isOpenBottomMenu = false
        submitList(currentList)
    }

}