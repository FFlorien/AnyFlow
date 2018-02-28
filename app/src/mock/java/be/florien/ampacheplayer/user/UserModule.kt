package be.florien.ampacheplayer.user

import be.florien.ampacheplayer.api.MockUpAmpacheApi
import be.florien.ampacheplayer.api.AmpacheApi
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.player.AudioQueue
import be.florien.ampacheplayer.player.MockPlayerController
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
    fun provideAmpacheApi(url: String): AmpacheApi = MockUpAmpacheApi()

    @Provides
    @UserScope
    fun providePlayerController(audioQueueManager: AudioQueue): PlayerController = MockPlayerController(audioQueueManager)
}