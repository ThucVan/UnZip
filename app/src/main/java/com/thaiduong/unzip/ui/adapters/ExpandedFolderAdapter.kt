package com.thaiduong.unzip.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ItemFileExpandedBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.activities.FileListActivity
import com.thaiduong.unzip.ui.activities.ReadFilesActivity
import com.thaiduong.unzip.utils.*
import com.thaiduong.unzip.utils.interfaces.IItemSelected

class ExpandedFolderAdapter(private var mContext: Context) :
    ListAdapter<FolderOrFile, ExpandedFolderAdapter.ViewHolder>(
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

    inner class ViewHolder(itemView: ItemFileExpandedBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        private val binding = itemView

        @SuppressLint("SetTextI18n")
        fun onBind(item: FolderOrFile) {
            binding.tvTitleFolder.text = item.mFile?.name
            if (item.mFile?.isDirectory == true) {
                binding.imvThumbnails.setImageResource(R.drawable.ic_internal_folder_expanded)
                if (item.mFile?.listFiles()?.size == 0) {
                    binding.tvItemDetail.text = "0 item"
                } else
                    binding.tvItemDetail.text = "${item.mFile?.listFiles()?.size} item"
                binding.tvDate.visibility = View.GONE
            } else {
                when (item.mFile?.extension?.lowercase()) {
                    "mp3" -> binding.imvThumbnails.setImageResource(R.drawable.ic_mp3)
                    "xlsx" -> binding.imvThumbnails.setImageResource(R.drawable.ic_xlsx)
                    "tar" -> binding.imvThumbnails.setImageResource(R.drawable.ic_tar)
                    "zip" -> binding.imvThumbnails.setImageResource(R.drawable.ic_zip)
                    "rar" -> binding.imvThumbnails.setImageResource(R.drawable.ic_rar)
                    "7z" -> binding.imvThumbnails.setImageResource(R.drawable.ic_7z)
                    "docx" -> binding.imvThumbnails.setImageResource(R.drawable.ic_docx)
                    "pptx" -> binding.imvThumbnails.setImageResource(R.drawable.ic_pptx)
                    "txt" -> binding.imvThumbnails.setImageResource(R.drawable.ic_txt)
                    "pdf" -> binding.imvThumbnails.setImageResource(R.drawable.ic_pdf)
                    "apk" -> binding.imvThumbnails.setImageResource(R.drawable.ic_apk)
                    "jpg", "jpeg", "png", "gif", "mp4", "webp" -> {
                        Glide.with(mContext)
                            .load(item.mFile)
                            .centerCrop()
                            .into(binding.imvThumbnails)
                    }
                    else -> binding.imvThumbnails.setImageResource(R.drawable.ic_un_known)
                }
                binding.tvItemDetail.text = item.mFile?.length()?.let { FileFormat.sizeFormat(it) }
                binding.tvDate.text = item.mFile?.lastModified()?.let { FileFormat.dateFormat(it) }
            }
            binding.imvSelected.setOnClickListener {
                Log.e("ExpandedFolderAdapter", "isDisableClick: $isDisableClick")
                if (isDisableClick) return@setOnClickListener
//                if (item.isSelected || item.mFile?.parentFile?.name == ".Recycle Bin") {
//                    binding.imvSelected.setImageResource(R.drawable.ic_unselected)
//                    item.isSelected = false
//                } else {
//                    binding.imvSelected.setImageResource(R.drawable.ic_selected)
//                    item.isSelected = true
//                }
//                //isOpenBottomMenu = item.isSelected
//                mIItemSelected.selectedItem(item)
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
                if (isDisableClick) return@setOnClickListener
                if (isOpenBottomMenu || item.mFile?.parentFile?.name == ".Recycle Bin") {
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
                    val intent: Intent
                    if (item.mFile?.isDirectory == true) {
                        intent = Intent(mContext, FileListActivity::class.java)
                        intent.putExtra(FOLDER_NAME, item.mFile?.name)
                    } else {
                        intent = Intent(mContext, ReadFilesActivity::class.java)
                        intent.putExtra(EXTENSION_FILE, item.mFile?.extension)
                        SingletonLastSong.getInstance(mContext).path = path
                    }
                    intent.putExtra(PATH, path)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    mContext.startActivity(intent)
                }
            }
//            if (isOpenBottomMenu || item.mFile?.parentFile?.name == ".Recycle Bin") {
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
            ItemFileExpandedBinding.inflate(
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