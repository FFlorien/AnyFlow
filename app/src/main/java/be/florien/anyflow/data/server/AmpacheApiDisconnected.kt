package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.exception.NoServerException
import be.florien.anyflow.data.server.model.*

/**
 * Responses for ampache calls when the server is not set
 */
class AmpacheApiDisconnected : AmpacheApi {
    override suspend fun authenticate(
        action: String,
        time: String,
        version: String,
        auth: String,
        user: String
    ): AmpacheAuthentication =
        throw NoServerException()

    override suspend fun ping(action: String, auth: String): AmpachePing =
        throw NoServerException()

    override suspend fun getSongsForFirstTime(
        action: String,
        auth: String,
        limit: Int,
        offset: Int
    ): AmpacheSongResponse {
        throw NoServerException()
    }

    override suspend fun getGenresForFirstTime(
        action: String,
        auth: String,
        limit: Int,
        offset: Int
    ): AmpacheGenreResponse {
        throw NoServerException()
    }

    override suspend fun getArtistsForFirstTime(
        action: String,
        auth: String,
        limit: Int,
        offset: Int
    ): AmpacheArtistResponse {
        throw NoServerException()
    }

    override suspend fun getAlbumsForFirstTime(
        action: String,
        auth: String,
        limit: Int,
        offset: Int
    ): AmpacheAlbumResponse {
        throw NoServerException()
    }

    override suspend fun getPlaylistsForFirstTime(
        action: String,
        auth: String,
        limit: Int,
        offset: Int,
        hideSearch: Int
    ): AmpachePlaylistResponse {
        throw NoServerException()
    }

    override suspend fun getSongs(
        action: String,
        update: String,
        auth: String,
        limit: Int,
        offset: Int
    ): AmpacheSongResponse {
        throw NoServerException()
    }

    override suspend fun getGenres(
        action: String,
        update: String,
        auth: String,
        limit: Int,
        offset: Int
    ): AmpacheGenreResponse {
        throw NoServerException()
    }

    override suspend fun getArtists(
        action: String,
        update: String,
        auth: String,
        limit: Int,
        offset: Int
    ): AmpacheArtistResponse {
        throw NoServerException()
    }

    override suspend fun getAlbums(
        action: String,
        update: String,
        auth: String,
        limit: Int,
        offset: Int
    ): AmpacheAlbumResponse {
        throw NoServerException()
    }

    override suspend fun getPlaylists(
        action: String,
        update: String,
        auth: String,
        limit: Int,
        offset: Int,
        hideSearch: Int
    ): AmpachePlaylistResponse {
        throw NoServerException()
    }

    override suspend fun getPlaylistSongs(
        action: String,
        filter: String,
        auth: String,
        limit: Int,
        offset: Int
    ): AmpacheSongIdResponse =
        throw NoServerException()

    override suspend fun createPlaylist(action: String, auth: String, name: String, type: String) =
        throw NoServerException()

    override suspend fun addToPlaylist(
        action: String,
        filter: Long,
        auth: String,
        songId: Long,
        check: Int
    ) = throw NoServerException()

    override suspend fun getArtistWithId(
        action: String,
        id: Long,
        auth: String
    ): AmpacheArtist {
        throw NoServerException()
    }
}