package com.thaiduong.unzip.ui.bases.DropBox.internal.ui

import android.app.Activity
import android.database.Cursor
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dropbox.core.DbxDownloader
import com.thaiduong.unzip.ui.bases.DropBox.internal.ui.FilesAdapter.MetadataViewHolder
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.Metadata
import com.dropbox.core.v2.files.ThumbnailFormat
import com.dropbox.core.v2.files.ThumbnailSize
import com.thaiduong.unzip.R
import java.io.ByteArrayOutputStream
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min


/**
 * Adapter for file list
 */
class FilesAdapter(
    private val dbxClientV2: DbxClientV2,
    private val mCallback: Callback,
    private val scope: CoroutineScope,
    private val activity: Activity,
) :
    RecyclerView.Adapter<MetadataViewHolder>() {
    private var mFiles: List<Metadata>? = null
    fun setFiles(files: List<Metadata>?) {
        mFiles = Collections.unmodifiableList(ArrayList(files))
        notifyDataSetChanged()
    }

    interface Callback {
        fun onFolderClicked(folder: FolderMetadata?)
        fun onFileClicked(file: FileMetadata?)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MetadataViewHolder {
        val context = viewGroup.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_dropbox, viewGroup, false)
        return MetadataViewHolder(view)
    }

    override fun onBindViewHolder(metadataViewHolder: MetadataViewHolder, i: Int) {
        metadataViewHolder.bind(mFiles!![i])
    }

    override fun getItemId(position: Int): Long {
        return mFiles!![position].pathLower.hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return if (mFiles == null) 0 else mFiles!!.size
    }

    inner class MetadataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private var job: Job? = null
        private val mTextView: TextView
        private val mImageView: ImageView
        private val mBtn : ImageButton
        private var mItem: Metadata? = null

        init {
            mImageView = itemView.findViewById<View>(R.id.item_imgdropbox) as ImageView
            mTextView = itemView.findViewById<View>(R.id.tv_nameitemdropbox) as TextView
            mBtn = itemView.findViewById<ImageButton>(R.id.btn_downloadItem) as ImageButton
            mBtn.setOnClickListener{
                if (mItem is FolderMetadata) {
                    mCallback.onFolderClicked(mItem as FolderMetadata?)
                } else if (mItem is FileMetadata) {
                    mCallback.onFileClicked(mItem as FileMetadata?)
                }
            }
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            if(mItem is FolderMetadata){
                mCallback.onFolderClicked(mItem as FolderMetadata?)
            }
        }

        fun bind(item: Metadata) {
            job?.let {
                if (it.isActive) {
                    it.cancel()
                }
            }
            job = null
            mItem = item
            mTextView.text = mItem!!.name
            val applicationContext = mImageView.context.applicationContext

            //scanfile
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
            )

            val images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val cur: Cursor = activity.managedQuery(
                images,
                projection,  // Which columns to return
                null,  // Which rows to return (all rows)
                null,  // Selection arguments (none)
                null // Ordering
            )
            if (cur.moveToFirst()) {
                var bucket: String
                val bucketColumn: Int = cur.getColumnIndex(
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                )
                do {
                    // Get the field values
                    try {
                        bucket = cur.getString(bucketColumn)

                        val path : String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/$bucket/${mItem!!.name}"
                        }else{
                            "storage/emulated/0/$bucket/${mItem!!.name}"
                        }

                        val path2 : String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${mItem!!.name}"
                        }else{
                            "storage/emulated/0/Download/${mItem!!.name}"
                        }

                        if (File(path).exists() || File(path2).exists()){
                            mBtn.visibility = View.GONE
                        }else{
                            mBtn.visibility = View.VISIBLE
                        }

                        // Do something with the values.
                    }catch (e : Exception){
                        Log.e("TAG", "bucket null", )
                    }
                } while (cur.moveToNext())
            }

            // Load based on file path
            // Prepending a magic scheme to get it to
            if (item is FileMetadata) {
                val mime = MimeTypeMap.getSingleton()
                val ext = item.getName().substring(item.getName().indexOf(".") + 1)
                val type = mime.getMimeTypeFromExtension(ext)
                Log.e("TAG", "bind: $type", )
                if (type != null){
                    when{
                        type.startsWith("image/") -> {
                            job = scope.launch {
                                Glide.with(applicationContext)
                                    .load(android.R.drawable.ic_popup_sync)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(mImageView)

                                val byteBuffer = ByteArrayOutputStream()
                                withContext(Dispatchers.IO) {
                                    val downloader: DbxDownloader<FileMetadata> =
                                        dbxClientV2.files()
                                            .getThumbnailBuilder(item.getPathLower())
                                            .withFormat(ThumbnailFormat.JPEG)
                                            .withSize(ThumbnailSize.W1024H768)
                                            .start()
                                    downloader.download(byteBuffer)
                                }

                                Glide.with(applicationContext)
                                    .load(byteBuffer.toByteArray())
                                    .placeholder(android.R.drawable.ic_popup_sync)
                                    .error(android.R.drawable.sym_def_app_icon)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(mImageView)
                            }
                        }

                        type.startsWith("application/rar") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_rar)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        type.startsWith("application/zip") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_zip)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        type.startsWith("application/tar") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_tar)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        type.startsWith("application/7z") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_7z)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        type.startsWith("application/pdf") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_pdf)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        type.startsWith("application/txt") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_txt)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        type.startsWith("application/docx") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_docx)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        type.startsWith("application/xlsx") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_xlsx)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        type.startsWith("application/pptx") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_pptx)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        type.startsWith("audio/") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_mp3)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        type.startsWith("video/") -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.bg_video_folder)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }

                        else -> {
                            Glide.with(applicationContext)
                                .load(R.drawable.ic_un_known)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(mImageView)
                        }
                    }
                }

                else {
                    println(item.mediaInfo)
                    Glide.with(applicationContext)
                        .load(android.R.drawable.ic_popup_sync)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(mImageView)
                }
            } else if (item is FolderMetadata) {
                mBtn.visibility = View.GONE
                Glide.with(applicationContext)
                    .load(R.drawable.ic_internal_folder_expanded)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(mImageView)
            }
        }
    }
}