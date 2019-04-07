package be.florien.anyflow.user

import android.content.Context
import be.florien.anyflow.persistence.server.AmpacheConnection
import be.florien.anyflow.di.UserScope
import be.florien.anyflow.persistence.PersistenceManager
import be.florien.anyflow.player.PlayingQueue
import be.florien.anyflow.player.ExoPlayerController
import be.florien.anyflow.player.PlayerController
import dagger.Module
import dagger.Provides
import io.reactivex.Observable
import okhttp3.OkHttpClient

/**
 * Module for dependencies available only when a user is logged in.
 */
@Module
class UserModule {

    @Provides
    @UserScope
    fun providePlayerController(context: Context, playingQueue: PlayingQueue, ampacheConnection: AmpacheConnection, okHttpClient: OkHttpClient): PlayerController = ExoPlayerController(playingQueue, ampacheConnection, context, okHttpClient)

    @Provides
    @UserScope
    fun provideUpdateStatusUpdater(persistenceManager: PersistenceManager): Observable<PersistenceManager.UpdateStatus> = persistenceManager.updateStatusUpdater
}