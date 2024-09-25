package be.florien.anyflow.management.playlist.di

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import be.florien.anyflow.management.playlist.work.PlaylistModificationWorkerFactory
import javax.inject.Inject

class PlaylistWorkerFactory @Inject constructor(
    private val workerFactories: Map<Class<out CoroutineWorker>, @JvmSuppressWildcards PlaylistModificationWorkerFactory>
): WorkerFactory(){
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return workerFactories
            .entries
            .find {
                Class.forName(workerClassName).isAssignableFrom(it.key)
            }
            ?.value
            ?.create(appContext, workerParameters)
    }

}