package be.florien.anyflow.player

import be.florien.anyflow.persistence.local.model.Song
import io.reactivex.Flowable
import io.reactivex.Observable

/**
 * Interface used to interact with the player currently used
 */
interface PlayerController {
    val playTimeNotifier: Observable<Long>
    val stateChangeNotifier: Flowable<State>

    fun isPlaying() : Boolean
    fun play()
    fun prepare(song: Song)
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