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
import be.florien.anyflow.extension.applyPutInt
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
        private const val OFFSET_ADD_SONG = "OFFSET_ADD_SONG"
        private const val OFFSET_ADD_GENRE = "OFFSET_ADD_GENRE"
        private const val OFFSET_ADD_ARTIST = "OFFSET_ADD_ARTIST"
        private const val OFFSET_ADD_ALBUM = "OFFSET_ADD_ALBUM"
        private const val OFFSET_ADD_PLAYLIST = "OFFSET_ADD_PLAYLIST"
        private const val OFFSET_PLAYLIST_SONGS = "OFFSET_PLAYLIST_SONGS"
        private const val OFFSET_UPDATE_SONG = "OFFSET_UPDATE_SONG"
        private const val OFFSET_UPDATE_GENRE = "OFFSET_UPDATE_GENRE"
        private const val OFFSET_UPDATE_ARTIST = "OFFSET_UPDATE_ARTIST"
        private const val OFFSET_UPDATE_ALBUM = "OFFSET_UPDATE_ALBUM"
        private const val OFFSET_UPDATE_PLAYLIST = "OFFSET_UPDATE_PLAYLIST"
        private const val OFFSET_DELETED_SONGS = "OFFSET_DELETED_SONGS"
        private const val RECONNECT_LIMIT = 3
        private const val COUNT_SONGS = "COUNT_SONGS"
        private const val COUNT_GENRES = "COUNT_GENRES"
        private const val COUNT_ALBUMS = "COUNT_ALBUMS"
        private const val COUNT_ARTIST = "COUNT_ARTIST"
        private const val COUNT_PLAYLIST = "COUNT_PLAYLIST"
        const val SERVER_UPDATE = "SERVER_UPDATE"
        const val SERVER_ADD = "SERVER_ADD"
        const val SERVER_CLEAN = "SERVER_CLEAN"
    }

    private var ampacheApi: AmpacheApi = AmpacheApiDisconnected()

    private val itemLimit: Int = 150
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
                    saveDbCount(
                        authentication.songs,
                        authentication.albums,
                        authentication.artists,
                        authentication.playlists
                    )
                    saveServerDates(
                        TimeOperations.getDateFromAmpacheComplete(authentication.add),
                        TimeOperations.getDateFromAmpacheComplete(authentication.update),
                        TimeOperations.getDateFromAmpacheComplete(authentication.clean)
                    )
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
        edit.putInt(COUNT_GENRES, playlists)
        edit.apply()
    }

    private fun saveServerDates(add: Calendar, update: Calendar, clean: Calendar) {
        val edit = sharedPreferences.edit()
        edit.putLong(SERVER_ADD, add.timeInMillis)
        edit.putLong(SERVER_UPDATE, update.timeInMillis)
        edit.putLong(SERVER_CLEAN, clean.timeInMillis)
        edit.apply()
    }

    /**
     * API calls : data
     */

    suspend fun getNewSongs(from: Calendar): List<AmpacheSong>? {
        val songList = getItems(AmpacheApi::getNewSongs, OFFSET_ADD_SONG, from)
        return if (updateRetrievingData(
                songList?.song,
                OFFSET_ADD_SONG,
                COUNT_SONGS,
                songsPercentageUpdater
            )
        ) {
            songList?.song
        } else {
            null
        }
    }

    suspend fun getNewGenres(from: Calendar): List<AmpacheNameId>? {
        val list =
            getItems(AmpacheApi::getNewGenres, OFFSET_ADD_GENRE, from)
        return if (updateRetrievingData(
                list?.genre,
                OFFSET_ADD_GENRE,
                COUNT_GENRES,
                genresPercentageUpdater
            )
        ) {
            list?.genre
        } else {
            null
        }
    }

    suspend fun getNewArtists(from: Calendar): List<AmpacheArtist>? {
        val list =
            getItems(AmpacheApi::getNewArtists, OFFSET_ADD_ARTIST, from)
        return if (updateRetrievingData(
                list?.artist,
                OFFSET_ADD_ARTIST,
                COUNT_ARTIST,
                artistsPercentageUpdater
            )
        ) {
            list?.artist
        } else {
            null
        }
    }

    suspend fun getNewAlbums(from: Calendar): List<AmpacheAlbum>? {
        val list =
            getItems(AmpacheApi::getNewAlbums, OFFSET_ADD_ALBUM, from)
        return if (updateRetrievingData(
                list?.album,
                OFFSET_ADD_ALBUM,
                COUNT_ALBUMS,
                albumsPercentageUpdater
            )
        ) {
            list?.album
        } else {
            null
        }
    }

    suspend fun getNewPlaylists(from: Calendar): List<AmpachePlayList>? {
        val list =
            getItems(
                AmpacheApi::getNewPlaylists,
                OFFSET_ADD_PLAYLIST,
                from
            )
        return if (updateRetrievingData(
                list?.playlist,
                OFFSET_ADD_PLAYLIST,
                COUNT_PLAYLIST,
                playlistsPercentageUpdater
            )
        ) {
            list?.playlist
        } else {
            null
        }
    }

    suspend fun getPlaylistsSongs(playlistToQuery: Long): List<AmpacheSongId>? {
        val prefName = OFFSET_PLAYLIST_SONGS + playlistToQuery
        var currentPlaylistOffset = sharedPreferences.getInt(prefName, 0)
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
            sharedPreferences.applyPutInt(prefName, currentPlaylistOffset)
            playlistSongList
        }
    }

    /**
     * API calls : updated data
     */

    suspend fun getUpdatedSongs(from: Calendar): List<AmpacheSong>? {
        val songList = getItems(AmpacheApi::getUpdatedSongs, OFFSET_UPDATE_SONG, from)
        return if (updateRetrievingData(
                songList?.song,
                OFFSET_UPDATE_SONG,
                COUNT_SONGS,
                songsPercentageUpdater
            )
        ) {
            songList?.song
        } else {
            null
        }
    }

    suspend fun getUpdatedGenres(from: Calendar): List<AmpacheNameId>? {
        val list =
            getItems(AmpacheApi::getUpdatedGenres, OFFSET_UPDATE_GENRE, from)
        return if (updateRetrievingData(
                list?.genre,
                OFFSET_UPDATE_GENRE,
                COUNT_GENRES,
                genresPercentageUpdater
            )
        ) {
            list?.genre
        } else {
            null
        }
    }

    suspend fun getUpdatedArtists(from: Calendar): List<AmpacheArtist>? {
        val list =
            getItems(AmpacheApi::getUpdatedArtists, OFFSET_UPDATE_ARTIST, from)
        return if (updateRetrievingData(
                list?.artist,
                OFFSET_UPDATE_ARTIST,
                COUNT_ARTIST,
                artistsPercentageUpdater
            )
        ) {
            list?.artist
        } else {
            null
        }
    }

    suspend fun getUpdatedAlbums(from: Calendar): List<AmpacheAlbum>? {
        val list =
            getItems(AmpacheApi::getUpdatedAlbums, OFFSET_UPDATE_ALBUM, from)
        return if (updateRetrievingData(
                list?.album,
                OFFSET_UPDATE_ALBUM,
                COUNT_ALBUMS,
                albumsPercentageUpdater
            )
        ) {
            list?.album
        } else {
            null
        }
    }

    suspend fun getUpdatedPlaylists(from: Calendar): List<AmpachePlayList>? {
        val list =
            getItems(
                AmpacheApi::getUpdatedPlaylists,
                OFFSET_UPDATE_PLAYLIST,
                from
            )
        return if (updateRetrievingData(
                list?.playlist,
                OFFSET_UPDATE_PLAYLIST,
                COUNT_PLAYLIST,
                playlistsPercentageUpdater
            )
        ) {
            list?.playlist
        } else {
            null
        }
    }

    suspend fun getDeletedSongs(): List<AmpacheSongId>? {
        var currentOffset = sharedPreferences.getInt(OFFSET_DELETED_SONGS, 0)
        val deletedList =
            ampacheApi.getDeletedSongs(
                auth = authPersistence.authToken.secret,
                limit = itemLimit,
                offset = currentOffset
            ).deleted_song
        return if (deletedList.isEmpty()) {
            null
        } else {
            currentOffset += deletedList.size
            sharedPreferences.applyPutInt(OFFSET_DELETED_SONGS, currentOffset)
            deletedList
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

    private suspend fun <T> getItems(
        apiMethod: suspend AmpacheApi.(String, Int, Int, String) -> T?,
        offsetName: String,
        from: Calendar
    ): T? {
        try {
            val currentOffset = sharedPreferences.getInt(offsetName, 0)
            return ampacheApi.apiMethod(
                authPersistence.authToken.secret,
                itemLimit,
                currentOffset,
                TimeOperations.getAmpacheCompleteFormatted(from)
            )
        } catch (ex: Exception) {
            eLog(ex)
            throw ex
        }
    }

    private fun <T> updateRetrievingData(
        itemList: List<T>?,
        offsetName: String,
        countName: String,
        percentageUpdater: MutableLiveData<Int>
    ): Boolean {
        var currentOffset = sharedPreferences.getInt(offsetName, 0)
        val totalSongs = sharedPreferences.getInt(countName, 1)
        return if (itemList.isNullOrEmpty()) {
            percentageUpdater.postValue(-1)
            false
        } else {
            currentOffset += itemList.size
            val percentage = (currentOffset * 100) / totalSongs
            percentageUpdater.postValue(percentage)
            sharedPreferences.applyPutInt(offsetName, currentOffset)
            true
        }
    }

    private fun binToHex(data: ByteArray): String =
        String.format("%0" + data.size * 2 + "X", BigInteger(1, data))

    fun resetAddOffsets() {
        sharedPreferences.edit().apply {
            remove(OFFSET_ADD_SONG)
            remove(OFFSET_ADD_GENRE)
            remove(OFFSET_ADD_ARTIST)
            remove(OFFSET_ADD_ALBUM)
            remove(OFFSET_ADD_PLAYLIST)
        }.apply()
    }

    fun resetUpdateOffsets() {
        sharedPreferences.edit().apply {
            remove(OFFSET_UPDATE_SONG)
            remove(OFFSET_UPDATE_GENRE)
            remove(OFFSET_UPDATE_ARTIST)
            remove(OFFSET_UPDATE_ALBUM)
            remove(OFFSET_UPDATE_PLAYLIST)
        }.apply()
    }

    enum class ConnectionStatus {
        WRONG_SERVER_URL,
        WRONG_ID_PAIR,
        CONNEXION,
        CONNECTED
    }
}
