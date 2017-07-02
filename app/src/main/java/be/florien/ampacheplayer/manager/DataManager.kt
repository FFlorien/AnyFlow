package be.florien.ampacheplayer.manager

import be.florien.ampacheplayer.model.local.*
import be.florien.ampacheplayer.model.queue.Filter
import be.florien.ampacheplayer.model.realm.*
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Class managing all request for data, handling caching and updating the database in the process.
 */
class DataManager
@Inject constructor(
        var database: AmpacheDatabase,
        var connection: AmpacheConnection) {

    /**
     * Getter
     */

    fun getSongs(filters: List<Filter<RealmSong, Any>> = emptyList()): Observable<List<Song>> = connection
            .getSongs()
            .flatMap {
                result ->
                when (result.error.code) {
                    401 -> connection.reconnect(connection.getSongs())
                    else -> Observable.just(result)
                }
            }
            .flatMap {
                songs ->
                database.addSongs(songs.songs.map(::RealmSong))
                val dbSongs = database.getSongs(filters)
                Observable.just(dbSongs.map(::Song))
            }

    fun getArtists(filters: List<Filter<RealmArtist, Any>> = emptyList()): Observable<List<Artist>> = connection
            .getArtists()
            .flatMap {
                artists ->
                database.addArtists(artists.artists.map(::RealmArtist))
                val dbArtists = database.getArtists(filters)

                Observable.just(dbArtists.map(::Artist))
            }

    fun getAlbums(filters: List<Filter<RealmAlbum, Any>> = emptyList()): Observable<List<Album>> = connection
            .getAlbums()
            .flatMap {
                albums ->
                database.addAlbums(albums.albums.map(::RealmAlbum))
                val dbAlbums = database.getAlbums(filters)
                Observable.just(dbAlbums.map(::Album))
            }

    fun getPlayLists(filters: List<Filter<RealmPlaylist, Any>> = emptyList()): Observable<List<Playlist>> = connection
            .getPlaylists()
            .flatMap {
                playlist ->
                database.addPlayLists(playlist.playlists.map(::RealmPlaylist))
                val dbPlayLists = database.getPlayLists(filters)
                Observable.just(dbPlayLists.map(::Playlist))
            }

    fun getTags(filters: List<Filter<RealmTag, Any>> = emptyList()): Observable<List<Tag>> = connection
            .getTags()
            .flatMap {
                tag ->
                database.addTags(tag.tags.map(::RealmTag))
                val dbTags = database.getTags(filters)
                Observable.just(dbTags.map(::Tag))
            }

    fun getSong(id: Long): Observable<Song> = connection.getSong(id).flatMap { Observable.just(Song(it.songs[0])) }
}