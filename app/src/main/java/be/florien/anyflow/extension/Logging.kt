package be.florien.anyflow.extension

import org.jetbrains.annotations.NonNls
import timber.log.Timber

fun Any.vLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName)
    Timber.v(message, *args)
}

fun Any.vLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName)
    Timber.v(t, message, *args)
}

fun Any.vLog(t: Throwable) {
    Timber.tag(this::class.java.simpleName)
    Timber.v(t)
}

fun Any.dLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName)
    Timber.d(message, *args)
}

fun Any.dLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName)
    Timber.d(t, message, *args)
}

fun Any.dLog(t: Throwable) {
    Timber.tag(this::class.java.simpleName)
    Timber.d(t)
}

fun Any.iLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName)
    Timber.i(message, *args)
}

fun Any.iLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName)
    Timber.i(t, message, *args)
}

fun Any.iLog(t: Throwable) {
    Timber.tag(this::class.java.simpleName)
    Timber.i(t)
}

fun Any.wLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName)
    Timber.w(message, *args)
}

fun Any.wLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName)
    Timber.w(t, message, *args)
}

fun Any.wLog(t: Throwable) {
    Timber.tag(this::class.java.simpleName)
    Timber.w(t)
}

fun Any.eLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName)
    Timber.e(message, *args)
}

fun Any.eLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName)
    Timber.e(t, message, *args)
}

fun Any.eLog(t: Throwable) {
    Timber.tag(this::class.java.simpleName)
    Timber.e(t)
}