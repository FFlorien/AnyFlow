package be.florien.ampacheplayer.user

import android.content.Context
import be.florien.ampacheplayer.persistence.server.AmpacheConnection
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.player.PlayingQueue
import be.florien.ampacheplayer.player.ExoPlayerController
import be.florien.ampacheplayer.player.PlayerController
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient

/**
 * todo
 */
@Module
class UserModule {

    @Provides
    @UserScope
    fun providePlayerController(context: Context, playingQueue: PlayingQueue, ampacheConnection: AmpacheConnection, okHttpClient: OkHttpClient): PlayerController = ExoPlayerController(playingQueue, ampacheConnection, context, okHttpClient)
}