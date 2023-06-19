package com.thaiduong.unzip.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hzy.libp7zip.P7ZipApi
import com.thaiduong.unzip.App
import com.thaiduong.unzip.R
import com.thaiduong.unzip.databinding.FragmentExtractBinding
import com.thaiduong.unzip.ui.bases.BaseFragment
import com.thaiduong.unzip.utils.AppUtils.myGetExternalStorageDir
import com.thaiduong.unzip.utils.EXTRACTED_STATUS
import com.thaiduong.unzip.utils.customview.InstallApk
import com.thaiduong.unzip.utils.rxjava.ExtractedFileRx
import net.lingala.zip4j.ZipFile
import java.io.File

class ExtractFragment(override val layoutId: Int = R.layout.fragment_extract) :
    BaseFragment<FragmentExtractBinding>() {

    private lateinit var path: String
    private lateinit var extension: String

    private lateinit var handler: Handler
    private lateinit var alertDialog: AlertDialog
    private var progress = 0

    companion object {
        fun newInstance(path: String, extensionFile: String): ExtractFragment {
            val args = Bundle()
            args.putString("path", path)
            args.putString("extension", extensionFile)
            val fragment = ExtractFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun initUi() {
        path = arguments?.getString("path").toString()
        extension = arguments?.getString("extension").toString()
    }

    override fun doWork() {
        when (extension) {
            "apk" -> {
                binding.progressBarInstall.visibility = View.VISIBLE
                val install: InstallApk = ViewModelProvider(this)[InstallApk::class.java]
                install.install(Uri.fromFile(File(path)))
                Handler(Looper.getMainLooper()).postDelayed({
                    requireActivity().finish()
                }, 5000)
            }
            else -> {
                binding.progressBarInstall.visibility = View.GONE
                val builder = AlertDialog.Builder(requireContext(), R.style.DialogStyle)
                val view = LayoutInflater.from(requireContext()).inflate(
                    R.layout.extract_dialog, requireActivity().findViewById(R.id.linearDialog)
                )
                val tvName = view.findViewById<EditText>(R.id.editFileName)
                val tvPass = view.findViewById<TextInputEditText>(R.id.etPassword)
                if (extension == "zip" && ZipFile(path).isEncrypted) {
                    view.findViewById<TextView>(R.id.tv_title_pass).visibility = View.VISIBLE
                    view.findViewById<TextInputLayout>(R.id.etPasswordLayout).visibility =
                        View.VISIBLE
                } else {
                    view.findViewById<TextView>(R.id.tv_title_pass).visibility = View.GONE
                    view.findViewById<TextInputLayout>(R.id.etPasswordLayout).visibility = View.GONE
                }
                builder.setView(view)
                alertDialog = builder.create()
                alertDialog.setCancelable(false)
                view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    alertDialog.dismiss()
                    requireActivity().finish()
                }
                view.findViewById<Button>(R.id.btnOk).setOnClickListener {
                    if (tvName.text.toString().isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "Please name the file before extracting",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val extractedFileRx = ExtractedFileRx(
                            requireActivity(),
                            path,
                            tvPass.text.toString(),
                            tvName.text.toString()
                        )

                        Thread {
                            requireActivity().runOnUiThread {
                                run {
                                    if (extractedFileRx.letSubscribe()) {
                                        alertDialog.dismiss()
                                        runProgressBar()
                                        App.dataStore.putInt(EXTRACTED_STATUS, 1)
                                    } else {
                                        App.dataStore.putInt(EXTRACTED_STATUS, 0)
                                    }
                                }
                            }
                        }.start()
                    }
                    val imm =
                        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                }
                alertDialog.show()
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun runProgressBar() {
        binding.progressExtracted.setOnProgressListener { requireActivity().finish() }
        handler = @SuppressLint("HandlerLeak")
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == 0) {
                    if (progress < 100) {
                        progress++
                        binding.progressExtracted.progress = progress
                    }
                }
            }
        }
        Thread {
            for (i in 0 until 100) {
                try {
                    Thread.sleep(100)
                    handler.sendEmptyMessage(0)
                } catch (e: InterruptedException) {
                    Log.e("ExtractFragment", "run failed: ${e.message}")
                }
            }
        }.start()
    }

}