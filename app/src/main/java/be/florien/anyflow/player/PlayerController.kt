package be.florien.anyflow.player

import androidx.lifecycle.LiveData

/**
 * Interface used to interact with the player currently used
 */
interface PlayerController {
    val playTimeNotifier: LiveData<Long>
    val stateChangeNotifier: LiveData<State>

    fun isPlaying(): Boolean
    fun playForAlarm()
    fun stop()
    fun pause()
    fun resume()
    fun seekTo(duration: Long)
    fun onDestroy()

    enum class State {
        BUFFER,
        RECONNECT,
        PLAY,
        PAUSE,
        NO_MEDIA
    }

}