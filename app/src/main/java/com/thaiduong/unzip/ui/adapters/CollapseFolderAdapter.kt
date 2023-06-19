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
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ItemFileCollapseBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.ui.activities.FileListActivity
import com.thaiduong.unzip.ui.activities.ReadFilesActivity
import com.thaiduong.unzip.utils.*

class CollapseFolderAdapter(private var mContext: Context) :
    ListAdapter<FolderOrFile, CollapseFolderAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<FolderOrFile>() {
            override fun areItemsTheSame(oldItem: FolderOrFile, newItem: FolderOrFile): Boolean {
                return oldItem.mFile == newItem.mFile && oldItem.isSelected == newItem.isSelected
            }

            override fun areContentsTheSame(oldItem: FolderOrFile, newItem: FolderOrFile): Boolean {
                return oldItem.mFile == newItem.mFile && oldItem.isSelected == newItem.isSelected
            }

        }
    ) {

    inner class ViewHolder(itemView: ItemFileCollapseBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        private val binding = itemView

        @SuppressLint("SetTextI18n")
        fun onBind(item: FolderOrFile) {
            binding.tvTitleFolder.text = item.mFile?.name
            if (item.mFile?.isDirectory == true) {
                binding.imvThumbnails.setImageResource(R.drawable.ic_internal_folder_collapse)
                if (item.mFile?.listFiles()?.size == 0) {
                    binding.tvItemDetail.text = "0 item"
                } else
                    binding.tvItemDetail.text = "${item.mFile?.listFiles()?.size} item"
            } else {
                when (item.mFile?.extension) {
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
                            .load(item)
                            .centerCrop()
                            .into(binding.imvThumbnails)
                    }
                    else -> binding.imvThumbnails.setImageResource(R.drawable.ic_un_known)
                }
                binding.tvItemDetail.text = item.mFile?.length()?.let { FileFormat.sizeFormat(it) }
            }
            itemView.setOnClickListener {
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemFileCollapseBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder).onBind(getItem(position))
    }

}