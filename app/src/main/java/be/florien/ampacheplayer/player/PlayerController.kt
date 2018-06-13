package be.florien.ampacheplayer.player

import io.reactivex.Observable

/**
 * Interface used to interact with the player currently used
 */
interface PlayerController {
    val playTimeNotifier: Observable<Long>

    fun isPlaying() : Boolean
    fun play()
    fun stop()
    fun pause()
    fun resume()
    fun seekTo(duration: Int)
}