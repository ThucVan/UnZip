package com.thaiduong.unzip.ui.bases

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.util.*

abstract class BaseFragment<V : ViewBinding> : Fragment() {
    protected lateinit var binding: V
    private var rootView: View? = null
    protected var mContext: Context? = null

    abstract val layoutId: Int
    abstract fun initUi()
    abstract fun doWork()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        try {
            binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        } catch (e: Exception) {
            Log.e("BaseFragment", "onCreateView: $binding")
        }
        rootView = binding.root
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        doWork()
    }

    fun setLanguages(lang: String, context: Context) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val resources: Resources = context.resources
        val config: Configuration = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        restartActivity(context as Activity)
    }

    fun restartActivity(act: Activity) {
        val intent = Intent()
        intent.setClass(act, act.javaClass)
        act.startActivity(intent)
        act.finish()
    }
}