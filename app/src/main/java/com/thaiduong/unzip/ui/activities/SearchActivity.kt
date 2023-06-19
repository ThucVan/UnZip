package com.thaiduong.unzip.ui.activities

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.thaiduong.unzip.App.Companion.getDB
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.ActivitySearchBinding
import com.thaiduong.unzip.models.database.FileDataSearch
import com.thaiduong.unzip.ui.adapters.FileSearchAdapter
import com.thaiduong.unzip.ui.bases.BaseActivity
import com.thaiduong.unzip.utils.AppUtils.myGetExternalStorageDir
import com.thaiduong.unzip.utils.EXTENSION_FILE
import com.thaiduong.unzip.utils.FOLDER_NAME
import com.thaiduong.unzip.utils.PATH
import com.thaiduong.unzip.utils.SingletonLastSong
import com.thaiduong.unzip.utils.customclass.GetListFilePath
import com.thaiduong.unzip.utils.interfaces.IGetIListFilePath
import com.thaiduong.unzip.utils.interfaces.IItemPathSelected
import java.io.File

class SearchActivity(override val layoutId: Int = R.layout.activity_search) :
    BaseActivity<ActivitySearchBinding>(), IItemPathSelected, IGetIListFilePath {

    private var fileSearchList: MutableList<FileDataSearch>? = null
    private lateinit var mFileSearchAdapter: FileSearchAdapter

    override fun initUi() {
        binding.ivDelete.visibility = View.GONE
        val filesAndFolders = this.myGetExternalStorageDir()?.let { File(it).listFiles() }

        val itemDecoration: RecyclerView.ItemDecoration = DividerItemDecoration(
            this, DividerItemDecoration.VERTICAL
        )
        binding.rcvSearch.addItemDecoration(itemDecoration)
        fileSearchList = getDB().fileDataDao().getListSearch()
        mFileSearchAdapter = FileSearchAdapter(this)
        mFileSearchAdapter.submitList(fileSearchList)
        binding.rcvSearch.adapter = mFileSearchAdapter
        deleteData()
        Thread {
            run {
                val mGetListFilePath = filesAndFolders?.let { GetListFilePath(this, it) }
                mGetListFilePath?.letSubscribe()
            }
        }.start()

    }

    override fun doWork() {
        binding.ivBack.setOnClickListener { onBackPressed() }

        binding.ivDelete.setOnClickListener {
            binding.editSearch.text.clear()
        }

        binding.editSearch.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, _: Long ->
                val editArr = binding.editSearch.text.toString().split("\n")
                this.itemPathSelected(editArr[1])
                val methodManager =
                    this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                methodManager.hideSoftInputFromWindow(this.currentFocus!!.windowToken, 0)
            }
    }

    private fun deleteData() {
        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                fileSearchList?.get(position)
                    ?.let {
                        fileSearchList!![position].let { it1 ->
                            getDB().fileDataDao().deleteFileSearch(it.fileName, it1.filePath)
                        }
                    }
                fileSearchList?.removeAt(position)
                mFileSearchAdapter.submitList(fileSearchList)
            }
        }).attachToRecyclerView(binding.rcvSearch)
    }


    override fun itemPathSelected(path: String) {
        val intent: Intent
        val file = File(path)
        val fileDataSearch = FileDataSearch()
        fileDataSearch.fileName = file.name
        fileDataSearch.filePath = path
        if (!getDB().fileDataDao()
                .isExistsSearchFile(fileDataSearch.fileName, fileDataSearch.filePath)
        )
            getDB().fileDataDao().insertDataSearch(fileDataSearch)
        if (file.isDirectory) {
            intent = Intent(this, FileListActivity::class.java)
            intent.putExtra(FOLDER_NAME, file.name)
        } else {
            intent = Intent(this, ReadFilesActivity::class.java)
            intent.putExtra(EXTENSION_FILE, file.extension)
            SingletonLastSong.getInstance(this).path = path
        }

        intent.putExtra(PATH, path)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun getFilePathList(pathList: ArrayList<String>) {
        val arr = mutableListOf<String>()
        for (str in pathList) {
            val fileDataSearch = FileDataSearch()
            fileDataSearch.fileName = File(str).name
            fileDataSearch.filePath = File(str).absolutePath
            arr.add(fileDataSearch.toString())
        }
        val fileList: ArrayAdapter<String> = ArrayAdapter<String>(
            this, R.layout.item_list_search, arr
        )

        binding.editSearch.setAdapter(fileList)
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.ivDelete.visibility = View.VISIBLE
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

}