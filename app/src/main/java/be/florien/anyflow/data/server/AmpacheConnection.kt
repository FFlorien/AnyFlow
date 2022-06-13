package be.florien.anyflow.data.server

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.UserComponentContainer
import be.florien.anyflow.data.TimeOperations
import be.florien.anyflow.data.server.exception.NotAnAmpacheUrlException
import be.florien.anyflow.data.server.exception.SessionExpiredException
import be.florien.anyflow.data.server.exception.WrongFormatServerUrlException
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.data.server.model.*
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.extension.applyPutLong
import be.florien.anyflow.extension.eLog
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.HttpException
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for the ampache API server-side
 */
@Singleton
open class AmpacheConnection
@Inject constructor(
    private var authPersistence: AuthPersistence,
    private var userComponentContainer: UserComponentContainer,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val OFFSET_SONG = "songOffset"
        private const val OFFSET_GENRE = "genreOffset"
        private const val OFFSET_ARTIST = "artistOffset"
        private const val OFFSET_ALBUM = "albumOffset"
        private const val OFFSET_PLAYLIST = "playlistOffset"
        private const val OFFSET_PLAYLIST_SONGS = "playlistSongOffset"
        private const val RECONNECT_LIMIT = 3
        private const val COUNT_SONGS = "SONGS_COUNT"
        private const val COUNT_GENRES = "GENRES_COUNT"
        private const val COUNT_ALBUMS = "ALBUMS_COUNT"
        private const val COUNT_ARTIST = "ARTIST_COUNT"
        private const val COUNT_PLAYLIST = "PLAYLIST_COUNT"
    }

    private var ampacheApi: AmpacheApi = AmpacheApiDisconnected()

    private val oldestDateForRefresh = TimeOperations.getDateFromMillis(0L)

    private val itemLimit: Int = 150
    private var songOffset: Int = sharedPreferences.getLong(OFFSET_SONG, 0).toInt()
    private var genreOffset: Int = sharedPreferences.getLong(OFFSET_GENRE, 0).toInt()
    private var artistOffset: Int = sharedPreferences.getLong(OFFSET_ARTIST, 0).toInt()
    private var albumOffset: Int = sharedPreferences.getLong(OFFSET_ALBUM, 0).toInt()
    private var playlistOffset: Int = sharedPreferences.getLong(OFFSET_PLAYLIST, 0).toInt()
    private var reconnectByPing = 0
    private var reconnectByUserPassword = 0

    private var userComponent
        get() = userComponentContainer.userComponent
        set(value) {
            userComponentContainer.userComponent = value
        }

    val connectionStatusUpdater = MutableLiveData(ConnectionStatus.CONNEXION)

    val songsPercentageUpdater = MutableLiveData(-1)
    val genresPercentageUpdater = MutableLiveData(-1)
    val artistsPercentageUpdater = MutableLiveData(-1)
    val albumsPercentageUpdater = MutableLiveData(-1)
    val playlistsPercentageUpdater = MutableLiveData(-1)

    /**
     * Ampache connection handling
     */

    fun openConnection(serverUrl: String) {
        val url = try {
            serverUrl.toHttpUrl()
        } catch (exception: IllegalArgumentException) {
            throw WrongFormatServerUrlException(
                "The provided url was not correctly formed",
                exception
            )
        }
        ampacheApi = userComponentContainer.createUserScopeForServer(url.toString())
        authPersistence.saveServerInfo(url.toString())
    }

    fun ensureConnection() {
        if (userComponent == null) {
            val savedServerUrl = authPersistence.serverUrl.secret
            if (savedServerUrl.isNotBlank()) {
                openConnection(savedServerUrl)
            }
        }
    }

    fun resetReconnectionCount() {
        reconnectByUserPassword = 0
    }

    /**
     * API calls : connection
     */
    suspend fun authenticate(user: String, password: String): AmpacheAuthentication {
        val time = (TimeOperations.getCurrentDate().timeInMillis / 1000).toString()
        val encoder = MessageDigest.getInstance("SHA-256")
        encoder.reset()
        val passwordEncoded =
            binToHex(encoder.digest(password.toByteArray())).lowercase(Locale.ROOT)
        encoder.reset()
        val auth =
            binToHex(encoder.digest((time + passwordEncoded).toByteArray())).lowercase(Locale.ROOT)
        connectionStatusUpdater.postValue(ConnectionStatus.CONNEXION)
        try {
            val authentication = ampacheApi.authenticate(user = user, auth = auth, time = time)
            when (authentication.error.errorCode) {
                401 -> {
                    connectionStatusUpdater.postValue(ConnectionStatus.WRONG_ID_PAIR)
                    throw WrongIdentificationPairException(authentication.error.errorMessage)
                }
                0 -> {
                    authPersistence.saveConnectionInfo(
                        user,
                        password,
                        authentication.auth,
                        TimeOperations.getDateFromAmpacheComplete(authentication.session_expire).timeInMillis
                    )
                    saveDbCount(authentication.songs, authentication.albums, authentication.artists, authentication.playlists)
                }
            }
            connectionStatusUpdater.postValue(ConnectionStatus.CONNECTED)
            return authentication
        } catch (exception: HttpException) {
            connectionStatusUpdater.postValue(ConnectionStatus.WRONG_SERVER_URL)
            ampacheApi = AmpacheApiDisconnected()
            throw NotAnAmpacheUrlException("The ampache server couldn't be found at provided url")
        } catch (exception: Exception) {
            eLog(exception, "Unknown error while trying to login")
            throw exception
        }
    }

    suspend fun ping(authToken: String = authPersistence.authToken.secret): AmpachePing {
        if (authToken.isBlank()) {
            throw IllegalArgumentException("No token available !")
        }
        try {
            connectionStatusUpdater.postValue(ConnectionStatus.CONNEXION)
            val ping = ampacheApi.ping(auth = authToken)
            if (ping.session_expire.isEmpty()) {
                return ping
            }

            saveDbCount(ping.songs, ping.albums, ping.artists, ping.playlists)
            authPersistence.setNewAuthExpiration(TimeOperations.getDateFromAmpacheComplete(ping.session_expire).timeInMillis)
            connectionStatusUpdater.postValue(ConnectionStatus.CONNECTED)
            return ping
        } catch (exception: Exception) {
            eLog(exception)
            throw exception
        }
    }

    suspend fun <T> reconnect(request: suspend () -> T): T {
        if (!authPersistence.hasConnectionInfo()) {
            throw SessionExpiredException("Can't reconnect")
        } else {
            if (reconnectByPing >= RECONNECT_LIMIT) {
                reconnectByPing = 0
                authPersistence.revokeAuthToken()
            }
            return if (authPersistence.authToken.secret.isNotBlank()) {
                reconnectByPing(request)
            } else if (authPersistence.user.secret.isNotBlank() && authPersistence.password.secret.isNotBlank()) {
                reconnectByUsernamePassword(request)
            } else {
                throw SessionExpiredException("Can't reconnect")
            }
        }
    }

    private suspend fun <T> reconnectByPing(request: suspend () -> T): T {
        reconnectByPing++
        val ping = ping(authPersistence.authToken.secret)
        return if (ping.error.errorCode == 0) {
            saveDbCount(ping.songs, ping.albums, ping.artists, ping.playlists)
            request()
        } else {
            val auth = authenticate(authPersistence.user.secret, authPersistence.password.secret)
            if (auth.error.errorCode == 0) {
                request()
            } else {
                throw SessionExpiredException("Can't reconnect")
            }
        }
    }

    private suspend fun <T> reconnectByUsernamePassword(request: suspend () -> T): T {
        reconnectByUserPassword++
        val auth = authenticate(authPersistence.user.secret, authPersistence.password.secret)
        return if (auth.error.errorCode == 0) {
            saveDbCount(auth.songs, auth.albums, auth.artists, auth.playlists)
            request()
        } else {
            throw SessionExpiredException("Can't reconnect")
        }
    }

    private fun saveDbCount(songs: Int, albums: Int, artists: Int, playlists: Int) {
        val edit = sharedPreferences.edit()
        edit.putInt(COUNT_SONGS, songs)
        edit.putInt(COUNT_ALBUMS, albums)
        edit.putInt(COUNT_ARTIST, artists)
        edit.putInt(COUNT_PLAYLIST, playlists)
        edit.apply()
    }

    /**
     * API calls : data
     */

    suspend fun getSongs(from: Calendar = oldestDateForRefresh): List<AmpacheSong>? {
        try {
            val songList = if (from != oldestDateForRefresh) {
                ampacheApi.getSongs(
                    auth = authPersistence.authToken.secret,
                    update = TimeOperations.getAmpacheGetFormatted(from),
                    limit = itemLimit,
                    offset = songOffset
                ).song
            } else {
                ampacheApi.getSongsForFirstTime(
                    auth = authPersistence.authToken.secret,
                    limit = itemLimit,
                    offset = songOffset
                ).song
            }
            val totalSongs = sharedPreferences.getInt(COUNT_SONGS, 1)
            return if (songList.isEmpty()) {
                songsPercentageUpdater.postValue(-1)
                sharedPreferences.edit().remove(OFFSET_SONG).apply()
                null
            } else {
                val percentage = (songOffset * 100) / totalSongs
                songsPercentageUpdater.postValue(percentage)
                songOffset += songList.size
                sharedPreferences.applyPutLong(OFFSET_SONG, songOffset.toLong())
                songList
            }
        } catch (ex: Exception) {
            eLog(ex)
            throw ex
        }
    }

    suspend fun getGenres(from: Calendar = oldestDateForRefresh): List<AmpacheNameId>? {
        try {
            val genreList =  if (from != oldestDateForRefresh) {
                ampacheApi.getGenres(
                    auth = authPersistence.authToken.secret,
                    update = TimeOperations.getAmpacheGetFormatted(from),
                    limit = itemLimit,
                    offset = genreOffset
                ).genre
            } else {
                ampacheApi.getGenresForFirstTime(
                    auth = authPersistence.authToken.secret,
                    limit = itemLimit,
                    offset = genreOffset
                ).genre
            }
            val totalGenres = sharedPreferences.getInt(COUNT_GENRES, 1)
            return if (genreList.isEmpty()) {
                genresPercentageUpdater.postValue(-1)
                sharedPreferences.edit().remove(OFFSET_GENRE).apply()
                null
            } else {
                val percentage = (genreOffset * 100) / totalGenres
                genresPercentageUpdater.postValue(percentage)
                genreOffset += genreList.size
                sharedPreferences.applyPutLong(OFFSET_GENRE, genreOffset.toLong())
                genreList
            }
        } catch (ex: Exception) {
            eLog(ex)
            throw ex
        }
    }

    suspend fun getArtists(from: Calendar = oldestDateForRefresh): List<AmpacheArtist>? {
        try {
            val artistList =  if (from != oldestDateForRefresh) {
                ampacheApi.getArtists(
                    auth = authPersistence.authToken.secret,
                    update = TimeOperations.getAmpacheGetFormatted(from),
                    limit = itemLimit,
                    offset = artistOffset
                ).artist
            } else {
                ampacheApi.getArtistsForFirstTime(
                    auth = authPersistence.authToken.secret,
                    limit = itemLimit,
                    offset = artistOffset
                ).artist
            }
            val totalArtists = sharedPreferences.getInt(COUNT_ARTIST, 1)
            return if (artistList.isEmpty()) {
                artistsPercentageUpdater.postValue(-1)
                sharedPreferences.edit().remove(OFFSET_ARTIST).apply()
                null
            } else {
                val percentage = (artistOffset * 100) / totalArtists
                artistsPercentageUpdater.postValue(percentage)
                artistOffset += artistList.size
                sharedPreferences.applyPutLong(OFFSET_ARTIST, artistOffset.toLong())
                artistList
            }
        } catch (ex: Exception) {
            eLog(ex)
            throw ex
        }
    }

    suspend fun getAlbums(from: Calendar = oldestDateForRefresh): List<AmpacheAlbum>? {
        try {
            val albumList =  if (from != oldestDateForRefresh) {
                ampacheApi.getAlbums(
                    auth = authPersistence.authToken.secret,
                    update = TimeOperations.getAmpacheGetFormatted(from),
                    limit = itemLimit,
                    offset = albumOffset
                ).album
            } else {
                ampacheApi.getAlbumsForFirstTime(
                    auth = authPersistence.authToken.secret,
                    limit = itemLimit,
                    offset = albumOffset
                ).album
            }
            val totalAlbums = sharedPreferences.getInt(COUNT_ALBUMS, 1)
            return if (albumList.isEmpty()) {
                albumsPercentageUpdater.postValue(-1)
                sharedPreferences.edit().remove(OFFSET_ALBUM).apply()
                null
            } else {
                val percentage = (albumOffset * 100) / totalAlbums
                albumsPercentageUpdater.postValue(percentage)
                albumOffset += albumList.size
                sharedPreferences.applyPutLong(OFFSET_ALBUM, albumOffset.toLong())
                albumList
            }
        } catch (ex: java.lang.Exception) {
            eLog(ex)
            throw ex
        }
    }

    suspend fun getPlaylists(from: Calendar = oldestDateForRefresh): List<AmpachePlayList>? {
        val playlistList =  if (from != oldestDateForRefresh) {
            ampacheApi.getPlaylists(
                auth = authPersistence.authToken.secret,
                update = TimeOperations.getAmpacheGetFormatted(from),
                limit = itemLimit,
                offset = playlistOffset
            ).playlist
        } else {
            ampacheApi.getPlaylistsForFirstTime(
                auth = authPersistence.authToken.secret,
                limit = itemLimit,
                offset = playlistOffset
            ).playlist
        }
        val totalPlaylists = sharedPreferences.getInt(COUNT_PLAYLIST, 1)
        return if (playlistList.isEmpty()) {
            playlistsPercentageUpdater.postValue(-1)
            sharedPreferences.edit().remove(OFFSET_PLAYLIST).apply()
            null
        } else {
            val percentage = (playlistOffset * 100) / totalPlaylists
            playlistsPercentageUpdater.postValue(percentage)
            playlistOffset += playlistList.size
            sharedPreferences.applyPutLong(OFFSET_PLAYLIST, playlistOffset.toLong())
            playlistList
        }
    }

    suspend fun getPlaylistsSongs(playlistToQuery: Long): List<AmpacheSongId>? {
        val prefName = OFFSET_PLAYLIST_SONGS + playlistToQuery
        var currentPlaylistOffset = sharedPreferences.getLong(prefName, 0).toInt()
        val playlistSongList = ampacheApi.getPlaylistSongs(
            auth = authPersistence.authToken.secret,
            filter = playlistToQuery.toString(),
            limit = itemLimit,
            offset = currentPlaylistOffset
        ).song
        return if (playlistSongList.isEmpty()) {
            sharedPreferences.edit().remove(prefName).apply()
            null
        } else {
            currentPlaylistOffset += playlistSongList.size
            sharedPreferences.applyPutLong(prefName, currentPlaylistOffset.toLong())
            playlistSongList
        }
    }

    suspend fun createPlaylist(name: String) {
        ampacheApi.createPlaylist(auth = authPersistence.authToken.secret, name = name)
    }

    suspend fun addSongToPlaylist(songId: Long, playlistId: Long) {
        ampacheApi.addToPlaylist(
            auth = authPersistence.authToken.secret,
            filter = playlistId,
            songId = songId
        )
    }

    fun getSongUrl(id: Long): String {
        val serverUrl = authPersistence.serverUrl.secret
        val token = authPersistence.authToken.secret
        return "${serverUrl}play/index.php?ssid=$token&type=song&oid=$id&uid=1"
    }

    fun getAlbumArtUrl(id: Long): String {
        val serverUrl = authPersistence.serverUrl.secret
        val token = authPersistence.authToken.secret
        return "${serverUrl}image.php?auth=$token&object_type=album&object_id=$id"
    }

    fun getArtistArtUrl(id: Long): String {
        val serverUrl = authPersistence.serverUrl.secret
        val token = authPersistence.authToken.secret
        return "${serverUrl}image.php?auth=$token&object_type=artist&object_id=$id"
    }

    private fun binToHex(data: ByteArray): String =
        String.format("%0" + data.size * 2 + "X", BigInteger(1, data))

    enum class ConnectionStatus {
        WRONG_SERVER_URL,
        WRONG_ID_PAIR,
        CONNEXION,
        CONNECTED
    }
}
