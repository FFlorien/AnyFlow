package be.florien.anyflow.feature.player.services.controller

import androidx.lifecycle.LiveData

/**
 * Interface used to interact with the player currently used
 */
interface PlayerController {
    val playTimeNotifier: LiveData<Long>
    val stateChangeNotifier: LiveData<State>
    val internetChangeNotifier: LiveData<Boolean>

    fun isPlaying(): Boolean
    fun isSeekable(): Boolean
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