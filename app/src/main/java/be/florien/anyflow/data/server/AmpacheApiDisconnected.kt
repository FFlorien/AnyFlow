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

    override suspend fun getNewSongs(
        auth: String,
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheSongResponse {
        throw NoServerException()
    }

    override suspend fun getNewGenres(
        auth: String,
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheGenreResponse {
        throw NoServerException()
    }

    override suspend fun getNewArtists(
        auth: String,
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheArtistResponse {
        throw NoServerException()
    }

    override suspend fun getNewAlbums(
        auth: String,
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheAlbumResponse {
        throw NoServerException()
    }

    override suspend fun getNewPlaylists(
        auth: String,
        limit: Int,
        offset: Int,
        update: String,
        hideSearch: Int,
        action: String
    ): AmpachePlaylistResponse {
        throw NoServerException()
    }

    override suspend fun getUpdatedSongs(
        auth: String,
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheSongResponse {
        throw NoServerException()
    }

    override suspend fun getUpdatedGenres(
        auth: String,
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheGenreResponse {
        throw NoServerException()
    }

    override suspend fun getUpdatedArtists(
        auth: String,
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheArtistResponse {
        throw NoServerException()
    }

    override suspend fun getUpdatedAlbums(
        auth: String,
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheAlbumResponse {
        throw NoServerException()
    }

    override suspend fun getUpdatedPlaylists(
        auth: String,
        limit: Int,
        offset: Int,
        update: String,
        hideSearch: Int,
        action: String
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

    override suspend fun getDeletedSongs(
        action: String,
        auth: String,
        limit: Int,
        offset: Int
    ): AmpacheDeletedSongIdResponse {
        throw NoServerException()
    }

    override suspend fun createPlaylist(action: String, auth: String, name: String, type: String) =
        throw NoServerException()

    override suspend fun addToPlaylist(
        action: String,
        filter: Long,
        auth: String,
        songId: Long,
        check: Int
    ) = throw NoServerException()
}