package be.florien.anyflow.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Player used to fill the implementation, but that play nothing
 */
class IdlePlayerController : PlayerController {

    override val stateChangeNotifier: LiveData<PlayerController.State> = MutableLiveData(PlayerController.State.NO_MEDIA)

    override val playTimeNotifier: LiveData<Long> = MutableLiveData(0)

    override fun isPlaying(): Boolean = false

    override fun play() {}

    override fun playForAlarm() {}

    override fun stop() {}

    override fun pause() {}

    override fun resume() {}

    override fun seekTo(duration: Long) {}

    override fun download(id: Long) {}

    override fun onDestroy() {}
}