package be.florien.anyflow.common.logging

import android.app.Application
import android.util.Log
import fr.bipi.treessence.file.FileLoggerTree
import org.jetbrains.annotations.NonNls
import timber.log.Timber
import java.io.File

fun Application.plantTimber() {
    Timber.plant(CrashReportingTree())
    val logDirectory = File(filesDir.absolutePath + "/logs")
    if (!logDirectory.exists()) {
        logDirectory.mkdir()
    }
    Timber.plant(
        FileLoggerTree
            .Builder()
            .withDir(logDirectory)
            .withFileName("anyflow_log_%g.log")
            .withFileLimit(5)
            .withMinPriority(Log.DEBUG)
            .appendToFile(true)
            .build()
    )
}

fun Any.vLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(getTag())
    Timber.v(message, *args)
}

fun Any.vLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(getTag())
    Timber.v(t, message, *args)
}

fun Any.vLog(t: Throwable) {
    Timber.tag(getTag())
    Timber.v(t)
}

fun Any.dLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(getTag())
    Timber.d(message, *args)
}

fun Any.dLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(getTag())
    Timber.d(t, message, *args)
}

fun Any.dLog(t: Throwable) {
    Timber.tag(getTag())
    Timber.d(t)
}

fun Any.iLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(getTag())
    Timber.i(message, *args)
}

fun Any.iLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(getTag())
    Timber.i(t, message, *args)
}

fun Any.iLog(t: Throwable) {
    Timber.tag(getTag())
    Timber.i(t)
}

fun Any.wLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(getTag())
    Timber.w(message, *args)
}

fun Any.wLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(getTag())
    Timber.w(t, message, *args)
}

fun Any.wLog(t: Throwable) {
    Timber.tag(getTag())
    Timber.w(t)
}

fun Any.eLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(getTag())
    Timber.e(message, *args)
}

fun Any.eLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(getTag())
    Timber.e(t, message, *args)
}

fun Any.eLog(t: Throwable) {
    Timber.tag(getTag())
    Timber.e(t)
}

private fun Any.getTag() = this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow"