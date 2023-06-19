package com.thaiduong.unzip.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ItemFileSelectedBinding
import com.thaiduong.unzip.models.FolderOrFile
import com.thaiduong.unzip.utils.FileFormat
import com.thaiduong.unzip.utils.customclass.MyDiffUtil

class SelectedItemAdapter(
    private var mContext: Context,
    private var folderList: MutableList<FolderOrFile>
) : RecyclerView.Adapter<SelectedItemAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: ItemFileSelectedBinding) :
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
                folderList.removeAt(layoutPosition)
                setData(folderList)
                notifyItemRemoved(layoutPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemFileSelectedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = folderList[position]
        (holder).onBind(item)
    }

    override fun getItemCount(): Int {
        return folderList.size
    }

    fun setData(folderList: MutableList<FolderOrFile>) {
        val diffCallback = MyDiffUtil(this.folderList, folderList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
        this.folderList = folderList
    }

}