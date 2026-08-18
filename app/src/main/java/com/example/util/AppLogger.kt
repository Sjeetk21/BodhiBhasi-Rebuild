package com.example.util

import android.util.Log
import com.example.BuildConfig

object AppLogger {
    private const val TAG = "LexiUPSC"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun i(message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message)
        }
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.w(TAG, message, throwable)
            } else {
                Log.w(TAG, message)
            }
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        }
    }
}
