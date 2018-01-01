package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.persistence.model.Song
import io.realm.RealmQuery

/**
 * Filter to apply to the list of song to be retrieved in order to get a subset.
 */
sealed class Filter<T>(
        private var filterFunction: RealmQuery<Song>.(String, T) -> RealmQuery<Song>,
        private var fieldName: String,
        private var argument: T,
        var subFilter: List<Filter<*>> = mutableListOf()) {
    fun applyFilter(realmQuery: RealmQuery<Song>) {
        realmQuery.filterFunction(fieldName, argument)
    }

    /**
     * String filters
     */
    class TitleIs(argument: String) : Filter<String>(RealmQuery<Song>::equalTo, Song::title.name, argument)

    class TitleContain(argument: String) : Filter<String>(RealmQuery<Song>::contains, Song::title.name, argument)

    class GenreIs(argument: String) : Filter<String>(RealmQuery<Song>::equalTo, Song::genre.name, argument)

    /**
     * Long filters
     */

    class SongIs(argument: Long) : Filter<Long>(RealmQuery<Song>::equalTo, Song::id.name, argument)

    class ArtistIs(argument: Long) : Filter<Long>(RealmQuery<Song>::equalTo, Song::artistId.name, argument)

    class AlbumArtistIs(argument: Long) : Filter<Long>(RealmQuery<Song>::equalTo, Song::albumArtistId.name, argument)

    class AlbumIs(argument: Long) : Filter<Long>(RealmQuery<Song>::equalTo, Song::albumId.name, argument)
}