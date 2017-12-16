package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.business.realm.Song
import io.reactivex.Observable
import io.reactivex.subjects.Subject

/**
 * Created by florien on 6/08/17.
 */
interface PlayerController {
    val playTimeNotifier: Observable<Int>
    val songNotifier: Subject<Song>

    fun isPlaying() : Boolean
    fun play()
    fun stop()
    fun pause()
    fun resume()
}