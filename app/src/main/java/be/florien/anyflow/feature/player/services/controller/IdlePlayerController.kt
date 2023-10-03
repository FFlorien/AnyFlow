package be.florien.anyflow.feature.player.services.controller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Player used to fill the implementation, but that play nothing
 */
class IdlePlayerController : PlayerController {

    override val stateChangeNotifier: LiveData<PlayerController.State> = MutableLiveData(
        PlayerController.State.NO_MEDIA
    )

    override val playTimeNotifier: LiveData<Long> = MutableLiveData(0)

    override fun isPlaying(): Boolean = false

    override fun isSeekable(): Boolean = false

    override fun playForAlarm() {}

    override fun stop() {}

    override fun pause() {}

    override fun resume() {}

    override fun seekTo(duration: Long) {}

    override fun onDestroy() {}
}