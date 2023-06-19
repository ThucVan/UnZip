package com.thaiduong.unzip.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

class FileFormat {
    companion object {
        fun sizeFormat(size: Long): String {
            var result = size.toDouble() / 1024
            if (result < 1024) return "${result.roundToInt()} Kb"
            result /= 1024
            if (result < 1024) return String.format("%.2f Mb", result)
            result /= 1024
            return String.format("%.2f Gb", result)
        }

        @SuppressLint("SimpleDateFormat")
        fun dateFormat(milliseconds: Long): String {
            val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
            return simpleDateFormat.format(milliseconds)
        }

        fun timeFormat(duration: Int): String {
            val mDuration = duration.div(10)
            val minutes =
                if (mDuration / 60 < 10) "0${(mDuration / 60)}" else (mDuration / 60).toString()
            val seconds =
                if (mDuration % 60 < 10) "0${(mDuration % 60)}" else (mDuration % 60).toString()
            return "$minutes:$seconds"
        }

        fun timeFormat(duration: Long): String {
            var mDuration = duration
            val hours =
                if (mDuration / 3_600_000 < 10) "0${(mDuration / 3_600_000)}" else (mDuration / 3_600_000).toString()

            mDuration -= hours.toInt().times(3_600_000)
            val minutes =
                if (mDuration / 60_000 < 10) "0${(mDuration / 60_000)}" else (mDuration / 60_000).toString()

            mDuration -= minutes.toInt().times(60_000)
            val seconds =
                if (mDuration / 1000 < 10) "0${(mDuration / 1000)}" else (mDuration / 1000).toString()
            return "$hours:$minutes:$seconds"
        }
    }
}