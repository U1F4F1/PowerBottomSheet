@file:Suppress("unused")

package com.u1f4f1.betterbottomsheet

import android.util.Log

val LOG_TAG = "POWERBOTTOMSHEET"

var logLevel: Int = 0

fun trace(message: String) {
    Log.v(LOG_TAG, message)
}

fun trace(message: String, vararg args: Any?) {
    Log.v(LOG_TAG, String.format(message, *args))
}

fun trace(throwable: Throwable, message: String) {
    Log.v(LOG_TAG, message, throwable)
}

fun debug(message: String) {
    Log.d(LOG_TAG, message)
}

fun debug(message: String, vararg args: Any?) {
    Log.d(LOG_TAG, String.format(message, *args))
}

fun debug(throwable: Throwable, message: String) {
    Log.d(LOG_TAG, message, throwable)
}

fun info(message: String) {
    Log.i(LOG_TAG, message)
}

fun info(message: String, vararg args: Any?) {
    Log.i(LOG_TAG, String.format(message, *args))
}

fun info(throwable: Throwable, message: String) {
    Log.i(LOG_TAG, message, throwable)
}

fun warn(message: String) {
    Log.w(LOG_TAG, message)
}

fun warn(message: String, vararg args: Any?) {
    Log.w(LOG_TAG, String.format(message, *args))
}

fun warn(throwable: Throwable, message: String) {
    Log.w(LOG_TAG, message, throwable)
}

fun error(message: String) {
    Log.e(LOG_TAG, message)
}

fun error(message: String, vararg args: Any?) {
    Log.e(LOG_TAG, String.format(message, *args))
}

fun error(throwable: Throwable, message: String) {
    Log.e(LOG_TAG, message, throwable)
}