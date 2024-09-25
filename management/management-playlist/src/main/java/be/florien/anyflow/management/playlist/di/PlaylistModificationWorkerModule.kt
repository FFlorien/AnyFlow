package be.florien.anyflow.management.playlist.di

import androidx.work.CoroutineWorker
import be.florien.anyflow.management.playlist.work.PlaylistModificationWorker
import be.florien.anyflow.management.playlist.work.PlaylistModificationWorkerFactory
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class WorkerKey(val value: KClass<out CoroutineWorker>)

@Module
abstract class PlaylistModificationWorkerModule {

    @Binds
    @IntoMap
    @WorkerKey(PlaylistModificationWorker::class)
    internal abstract fun bindMyWorkerFactory(worker: PlaylistModificationWorker.Factory): PlaylistModificationWorkerFactory
}