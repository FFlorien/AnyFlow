package be.florien.ampacheplayer.model.manager

import android.content.SharedPreferences
import be.florien.ampacheplayer.App
import be.florien.ampacheplayer.model.realm.*
import io.reactivex.Observable
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Class managing all request for data, handling caching and updating the database in the process.
 */
class DataManager {
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
    @Inject
    lateinit var database: AmpacheDatabase
    @Inject
    lateinit var connection: AmpacheConnection
    @Inject
    lateinit var prefs: SharedPreferences

    private var lastSongUpdate = "1970-01-01"
    private var lastArtistUpdate = "1970-01-01"
    private var lastAlbumUpdate = "1970-01-01"
    private var lastPlaylistUpdate = "1970-01-01"
    private var lastTagUpdate = "1970-01-01"


    /**
     * Constructor
     */
    init {
        App.ampacheComponent.inject(this)
        lastSongUpdate = prefs.getString(LAST_SONG_UPDATE_NAME, lastSongUpdate)
        lastArtistUpdate = prefs.getString(LAST_ARTIST_UPDATE_NAME, lastArtistUpdate)
        lastAlbumUpdate = prefs.getString(LAST_ALBUM_UPDATE_NAME, lastAlbumUpdate)
        lastPlaylistUpdate= prefs.getString(LAST_PLAYLIST_UPDATE_NAME, lastPlaylistUpdate)
        lastTagUpdate = prefs.getString(LAST_TAG_UPDATE_NAME, lastTagUpdate)
    }

    /**
     * Getter
     */

    fun getSongs(): Observable<List<Song>> {
        return connection
                .getSongs(lastSongUpdate)
                .flatMap {
                    songs ->
                    lastSongUpdate = DATE_FORMATTER.format(Date())
                    prefs.edit().putString(LAST_SONG_UPDATE_NAME, lastSongUpdate).apply()
                    database.addSongs(songs.songs.map(::Song))
                }
                .flatMap {
                    database.getSongs()
                }
    }

    fun getArtists(): Observable<List<Artist>> {
        return connection
                .getArtists(lastArtistUpdate)
                .flatMap {
                    artists ->
                    lastArtistUpdate = DATE_FORMATTER.format(Date())
                    prefs.edit().putString(LAST_ARTIST_UPDATE_NAME, lastArtistUpdate).apply()
                    database.addArtists(artists.artists.map(::Artist))
                }
                .flatMap {
                    database.getArtists()
                }
    }

    fun getAlbums(): Observable<List<Album>> {
        return connection
                .getAlbums(lastAlbumUpdate)
                .flatMap {
                    albums ->
                    lastAlbumUpdate = DATE_FORMATTER.format(Date())
                    prefs.edit().putString(LAST_ALBUM_UPDATE_NAME, lastAlbumUpdate).apply()
                    database.addAlbums(albums.albums.map(::Album))
                }
                .flatMap {
                    database.getAlbums()
                }
    }

    fun getPlaylists(): Observable<List<Playlist>> {
        return connection
                .getPlaylists(lastPlaylistUpdate)
                .flatMap {
                    playlist ->
                    lastPlaylistUpdate = DATE_FORMATTER.format(Date())
                    prefs.edit().putString(LAST_PLAYLIST_UPDATE_NAME, lastPlaylistUpdate).apply()
                    database.addPlaylists(playlist.playlists.map(::Playlist))
                }
                .flatMap {
                    database.getPlaylists()
                }
    }

    fun getTags(): Observable<List<Tag>> {
        return connection
                .getTags(lastTagUpdate )
                .flatMap {
                    tag ->
                    lastTagUpdate = DATE_FORMATTER.format(Date())
                    prefs.edit().putString(LAST_TAG_UPDATE_NAME, lastTagUpdate).apply()
                    database.addTags(tag.tags.map(::Tag))
                }
                .flatMap {
                    database.getTags()
                }
    }
}