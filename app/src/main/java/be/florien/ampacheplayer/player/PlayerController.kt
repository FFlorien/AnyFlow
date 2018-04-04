package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.persistence.model.Song
import io.reactivex.Observable
import io.reactivex.subjects.Subject

/**
 * Interface used to interact with the player currently used
 */
interface PlayerController {
    val playTimeNotifier: Observable<Long>
    val songNotifier: Subject<Song>

    fun isPlaying() : Boolean
    fun play()
    fun stop()
    fun pause()
    fun resume()
    fun seekTo(duration: Int)
}