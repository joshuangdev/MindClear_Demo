package com.mang0.mindcleardemo

import android.util.Log

object LogUtils {
    fun d(tag: String, message: String) = Log.d(tag, message)
    fun w(tag: String, message: String) = Log.w(tag, message)
    fun e(tag: String, message: String, t: Throwable? = null) = Log.e(tag, message, t)
}
