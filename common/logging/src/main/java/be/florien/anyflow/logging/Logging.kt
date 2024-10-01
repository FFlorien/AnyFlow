package be.florien.anyflow.logging

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
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.v(message, *args)
}

fun Any.vLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.v(t, message, *args)
}

fun Any.vLog(t: Throwable) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.v(t)
}

fun Any.dLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.d(message, *args)
}

fun Any.dLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.d(t, message, *args)
}

fun Any.dLog(t: Throwable) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.d(t)
}

fun Any.iLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.i(message, *args)
}

fun Any.iLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.i(t, message, *args)
}

fun Any.iLog(t: Throwable) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.i(t)
}

fun Any.wLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.w(message, *args)
}

fun Any.wLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.w(t, message, *args)
}

fun Any.wLog(t: Throwable) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.w(t)
}

fun Any.eLog(@NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.e(message, *args)
}

fun Any.eLog(t: Throwable, @NonNls message: String, vararg args: Any) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.e(t, message, *args)
}

fun Any.eLog(t: Throwable) {
    Timber.tag(this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "Anyflow")
    Timber.e(t)
}