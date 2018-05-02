package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.persistence.SongsDatabase
import be.florien.ampacheplayer.persistence.model.Song
import io.reactivex.subjects.PublishSubject
import io.realm.RealmResults
import javax.inject.Inject

const val NO_CURRENT_SONG = -13456

/**
 * Manager for the queue of accounts that are playing. It handle filters, random, repeat and addition to the queue
 */
@UserScope
class AudioQueue
@Inject constructor(private val songsDatabase: SongsDatabase) {

    /**
     * Fields
     */
    private var filters: MutableList<Filter<*>> = mutableListOf()
    val positionObservable: PublishSubject<Int> = PublishSubject.create()
    val itemsCount: Int
        get() = songsDatabase.getSongs(filters).size
    var listPosition: Int = 0
        set(value) {
            field = when {
                value in 0 until songsDatabase.getSongs(filters).size -> value
                value < 0 -> 0
                else -> songsDatabase.getSongs(filters).size -1
            }
            positionObservable.onNext(field)
        }


    /**
     * Methods
     */
    fun getCurrentSong(): Song {
        val songs = songsDatabase.getSongs(filters)
        return if (listPosition in 0 until songs.size) songs[listPosition] ?: Song() else Song()
    }

    fun getCurrentAudioQueue(): RealmResults<Song> = songsDatabase.getSongs(filters)

    fun addFilter(filter: Filter<*>) = filters.add(filter)

    fun resetFilters() {filters.clear()}

}