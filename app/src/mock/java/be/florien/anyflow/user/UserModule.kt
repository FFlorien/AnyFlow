package be.florien.anyflow.user

import be.florien.anyflow.api.AmpacheApi
import be.florien.anyflow.api.MockUpAmpacheApi
import be.florien.anyflow.di.UserScope
import be.florien.anyflow.player.AudioQueue
import be.florien.anyflow.player.MockPlayerController
import be.florien.anyflow.player.PlayerController
import dagger.Module
import dagger.Provides

/**
 * todo
 */
@Module
class UserModule {

    @Provides
    @UserScope
    fun providePlayerController(audioQueueManager: AudioQueue): PlayerController = MockPlayerController(audioQueueManager)
}