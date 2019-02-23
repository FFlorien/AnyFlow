package be.florien.anyflow.player

import io.reactivex.Observable

/**
 * Interface used to interact with the player currently used
 */
interface PlayerController {
    val playTimeNotifier: Observable<Long>
    val stateChangeNotifier: Observable<State>

    fun isPlaying(): Boolean
    fun play()
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