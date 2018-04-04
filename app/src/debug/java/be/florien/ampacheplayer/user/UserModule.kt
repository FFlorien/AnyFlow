package be.florien.ampacheplayer.user

import android.content.Context
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.player.AudioQueue
import be.florien.ampacheplayer.player.ExoPlayerController
import be.florien.ampacheplayer.player.PlayerController
import dagger.Module
import dagger.Provides

/**
 * todo
 */
@Module
class UserModule {

    @Provides
    @UserScope
    fun providePlayerController(context: Context, audioQueueManager: AudioQueue): PlayerController = ExoPlayerController(context, audioQueueManager)
}