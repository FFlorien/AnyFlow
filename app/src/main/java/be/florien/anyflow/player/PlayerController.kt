package be.florien.anyflow.player

import io.reactivex.Flowable
import io.reactivex.Observable

/**
 * Interface used to interact with the player currently used
 */
interface PlayerController {
    val playTimeNotifier: Observable<Long>
    val stateChangeNotifier: Flowable<State>

    fun initialize()
    fun isPlaying(): Boolean
    fun play()
    fun prepare(songsUrl: List<String>)
    fun stop()
    fun pause()
    fun resume()
    fun seekTo(duration: Long)
    fun release()

    enum class State {
        BUFFER,
        RECONNECT,
        PLAY,
        PAUSE,
        NO_MEDIA
    }

}