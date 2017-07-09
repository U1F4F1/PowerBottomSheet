@file:Suppress("unused")

package com.u1f4f1.betterbottomsheet

import android.util.Log

val callStackPosition: Int = 2

var logLevel: Int = 0

fun trace(message: String) {
    Log.v(Thread.currentThread().stackTrace[callStackPosition].className, message)
}

fun trace(message: String, vararg args: Any) {
    Log.v(Thread.currentThread().stackTrace[callStackPosition].className, String.format(message, args))
}

fun trace(throwable: Throwable, message: String) {
    Log.v(Thread.currentThread().stackTrace[callStackPosition].className, message, throwable)
}

fun debug(message: String) {
    Log.d(Thread.currentThread().stackTrace[callStackPosition].className, message)
}

fun debug(message: String, vararg args: Any) {
    Log.d(Thread.currentThread().stackTrace[callStackPosition].className, String.format(message, args))
}

fun debug(throwable: Throwable, message: String) {
    Log.d(Thread.currentThread().stackTrace[callStackPosition].className, message, throwable)
}

fun info(message: String) {
    Log.i(Thread.currentThread().stackTrace[callStackPosition].className, message)
}

fun info(message: String, vararg args: Any) {
    Log.i(Thread.currentThread().stackTrace[callStackPosition].className, String.format(message, args))
}

fun Log.info(throwable: Throwable, message: String) {
    Log.i(Thread.currentThread().stackTrace[callStackPosition].className, message, throwable)
}

fun Log.warn(message: String) {
    Log.w(Thread.currentThread().stackTrace[callStackPosition].className, message)
}

fun Log.warn(message: String, vararg args: Any) {
    Log.w(Thread.currentThread().stackTrace[callStackPosition].className, String.format(message, args))
}

fun Log.warn(throwable: Throwable, message: String) {
    Log.w(Thread.currentThread().stackTrace[callStackPosition].className, message, throwable)
}

fun Log.error(message: String) {
    Log.e(Thread.currentThread().stackTrace[callStackPosition].className, message)
}

fun Log.error(message: String, vararg args: Any) {
    Log.e(Thread.currentThread().stackTrace[callStackPosition].className, String.format(message, args))
}

fun Log.error(throwable: Throwable, message: String) {
    Log.e(Thread.currentThread().stackTrace[callStackPosition].className, message, throwable)
}