package be.florien.ampacheplayer.manager

import android.content.SharedPreferences
import be.florien.ampacheplayer.extension.applyPutString
import be.florien.ampacheplayer.model.local.*
import be.florien.ampacheplayer.model.realm.*
import io.reactivex.Observable
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Class managing all request for data, handling caching and updating the database in the process.
 */
class DataManager
@Inject constructor(
        var database: AmpacheDatabase,
        var connection: AmpacheConnection,
        var prefs: SharedPreferences) {

    /**
     * Constants
     */
    private val LAST_SONG_UPDATE_NAME = "lastSongUpdate"
    private val LAST_ARTIST_UPDATE_NAME = "lastArtistUpdate"
    private val LAST_ALBUM_UPDATE_NAME = "lastAlbumUpdate"
    private val LAST_PLAYLIST_UPDATE_NAME = "lastPlaylistUpdate"
    private val LAST_TAG_UPDATE_NAME = "lastTagUpdate"
    private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Fields
     */

    private var lastSongUpdate = "1970-01-01"
    private var lastArtistUpdate = "1970-01-01"
    private var lastAlbumUpdate = "1970-01-01"
    private var lastPlaylistUpdate = "1970-01-01"
    private var lastTagUpdate = "1970-01-01"


    /**
     * Constructor
     */
    init {
        lastSongUpdate = prefs.getString(LAST_SONG_UPDATE_NAME, lastSongUpdate)
        lastArtistUpdate = prefs.getString(LAST_ARTIST_UPDATE_NAME, lastArtistUpdate)
        lastAlbumUpdate = prefs.getString(LAST_ALBUM_UPDATE_NAME, lastAlbumUpdate)
        lastPlaylistUpdate = prefs.getString(LAST_PLAYLIST_UPDATE_NAME, lastPlaylistUpdate)
        lastTagUpdate = prefs.getString(LAST_TAG_UPDATE_NAME, lastTagUpdate)
    }

    /**
     * Getter
     */

    fun getSongs(onError: (returnCode: Int) -> Unit): Observable<List<Song>> = connection
            .getSongs(lastSongUpdate, onError)
            .flatMap {
                songs ->
                lastSongUpdate = DATE_FORMATTER.format(Date())
                prefs.applyPutString(LAST_SONG_UPDATE_NAME, lastSongUpdate)
                database.addSongs(songs.songs.map(::RealmSong))
            }
            .flatMap { database.getSongs() }
            .flatMap { songs -> Observable.just(songs.map(::Song)) }

    fun getArtists(): Observable<List<Artist>> = connection
            .getArtists(lastArtistUpdate)
            .flatMap {
                artists ->
                lastArtistUpdate = DATE_FORMATTER.format(Date())
                prefs.edit().putString(LAST_ARTIST_UPDATE_NAME, lastArtistUpdate).apply()
                database.addArtists(artists.artists.map(::RealmArtist))
            }
            .flatMap { database.getArtists() }
            .flatMap { artists -> Observable.just(artists.map(::Artist)) }

    fun getAlbums(): Observable<List<Album>> = connection
            .getAlbums(lastAlbumUpdate)
            .flatMap {
                albums ->
                lastAlbumUpdate = DATE_FORMATTER.format(Date())
                prefs.edit().putString(LAST_ALBUM_UPDATE_NAME, lastAlbumUpdate).apply()
                database.addAlbums(albums.albums.map(::RealmAlbum))
            }
            .flatMap { database.getAlbums() }
            .flatMap { albums -> Observable.just(albums.map(::Album)) }

    fun getPlayLists(): Observable<List<Playlist>> = connection
            .getPlaylists(lastPlaylistUpdate)
            .flatMap {
                playlist ->
                lastPlaylistUpdate = DATE_FORMATTER.format(Date())
                prefs.edit().putString(LAST_PLAYLIST_UPDATE_NAME, lastPlaylistUpdate).apply()
                database.addPlayLists(playlist.playlists.map(::RealmPlaylist))
            }
            .flatMap { database.getPlayLists() }
            .flatMap { playlists -> Observable.just(playlists.map(::Playlist)) }

    fun getTags(): Observable<List<Tag>> = connection
            .getTags(lastTagUpdate)
            .flatMap {
                tag ->
                lastTagUpdate = DATE_FORMATTER.format(Date())
                prefs.edit().putString(LAST_TAG_UPDATE_NAME, lastTagUpdate).apply()
                database.addTags(tag.tags.map(::RealmTag))
            }
            .flatMap { database.getTags() }
            .flatMap { tags -> Observable.just(tags.map(::Tag)) }

    fun getSong(id: Long): Observable<Song> = connection.getSong(id).flatMap { Observable.just(Song(it.songs[0])) }
}