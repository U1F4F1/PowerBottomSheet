@file:Suppress("unused")

package com.u1f4f1.powerbottomsheet

import android.util.Log

val LOG_TAG = "POWERBOTTOMSHEET"

var logLevel: Int = 0

fun trace(message: String) {
    if (shouldSkipMessage(Log.VERBOSE)) return
    Log.v(LOG_TAG, message)
}

fun trace(message: String, vararg args: Any?) {
    if (shouldSkipMessage(Log.VERBOSE)) return
    Log.v(LOG_TAG, String.format(message, *args))
}

fun trace(throwable: Throwable, message: String) {
    if (shouldSkipMessage(Log.VERBOSE)) return
    Log.v(LOG_TAG, message, throwable)
}

fun debug(message: String) {
    if (shouldSkipMessage(Log.DEBUG)) return
    Log.d(LOG_TAG, message)
}

fun debug(message: String, vararg args: Any?) {
    if (shouldSkipMessage(Log.DEBUG)) return
    Log.d(LOG_TAG, String.format(message, *args))
}

fun debug(throwable: Throwable, message: String) {
    if (shouldSkipMessage(Log.DEBUG)) return
    Log.d(LOG_TAG, message, throwable)
}

fun info(message: String) {
    if (shouldSkipMessage(Log.INFO)) return
    Log.i(LOG_TAG, message)
}

fun info(message: String, vararg args: Any?) {
    if (shouldSkipMessage(Log.INFO)) return
    Log.i(LOG_TAG, String.format(message, *args))
}

fun info(throwable: Throwable, message: String) {
    if (shouldSkipMessage(Log.INFO)) return
    Log.i(LOG_TAG, message, throwable)
}

fun warn(message: String) {
    if (shouldSkipMessage(Log.WARN)) return
    Log.w(LOG_TAG, message)
}

fun warn(message: String, vararg args: Any?) {
    if (shouldSkipMessage(Log.WARN)) return
    Log.w(LOG_TAG, String.format(message, *args))
}

fun warn(throwable: Throwable, message: String) {
    if (shouldSkipMessage(Log.WARN)) return
    Log.w(LOG_TAG, message, throwable)
}

fun error(message: String) {
    if (shouldSkipMessage(Log.ERROR)) return
    Log.e(LOG_TAG, message)
}

fun error(message: String, vararg args: Any?) {
    if (shouldSkipMessage(Log.ERROR)) return
    Log.e(LOG_TAG, String.format(message, *args))
}

fun error(throwable: Throwable, message: String) {
    if (shouldSkipMessage(Log.ERROR)) return
    Log.e(LOG_TAG, message, throwable)
}

private fun shouldSkipMessage(level: Int) : Boolean = logLevel > level