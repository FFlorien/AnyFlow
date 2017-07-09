package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.business.realm.RealmSong
import io.realm.RealmQuery

/**
 * Filter to apply to the list of song to be retrieved in order to get a subset.
 */
sealed class Filter<T>(
        var filterFunction: RealmQuery<RealmSong>.(String, T) -> RealmQuery<RealmSong>,
        var fieldName: String,
        var argument: T,
        var subFilter: List<Filter<*>> = mutableListOf()) {
    fun applyFilter(realmQuery: RealmQuery<RealmSong>) {
        realmQuery.filterFunction(fieldName, argument)
    }

    /**
     * String filters
     */
    class TitleIs(argument: String) : Filter<String>(RealmQuery<RealmSong>::equalTo, RealmSong::title.name, argument)

    class TitleContain(argument: String) : Filter<String>(RealmQuery<RealmSong>::contains, RealmSong::title.name, argument)

    class GenreIs(argument: String) : Filter<String>(RealmQuery<RealmSong>::equalTo, RealmSong::genre.name, argument)

    /**
     * Long filters
     */

    class SongIs(argument: Long) : Filter<Long>(RealmQuery<RealmSong>::equalTo, RealmSong::id.name, argument)

    class ArtistIs(argument: Long) : Filter<Long>(RealmQuery<RealmSong>::equalTo, RealmSong::artistId.name, argument)

    class AlbumArtistIs(argument: Long) : Filter<Long>(RealmQuery<RealmSong>::equalTo, RealmSong::albumArtistId.name, argument)

    class AlbumIs(argument: Long) : Filter<Long>(RealmQuery<RealmSong>::equalTo, RealmSong::albumId.name, argument)
}