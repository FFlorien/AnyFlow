package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.exception.NoServerException
import be.florien.anyflow.data.server.model.*

/**
 * Responses for ampache calls when the server is not set
 */
class AmpacheApiDisconnected : AmpacheAuthApi, AmpacheDataApi, AmpacheEditApi {
    override suspend fun authenticate(
        action: String,
        time: String,
        version: String,
        auth: String,
        user: String
    ): AmpacheAuthentication =
        throw NoServerException()

    override suspend fun authenticatedPing(
        action: String,
        auth: String
    ): AmpacheAuthenticatedStatus  =
        throw NoServerException()

    override suspend fun ping(action: String): AmpacheStatus =
        throw NoServerException()

    override suspend fun getNewSongs(
        limit: Int,
        offset: Int,
        action: String
    ): AmpacheSongResponse {
        throw NoServerException()
    }

    override suspend fun getNewGenres(
        limit: Int,
        offset: Int,
        action: String
    ): AmpacheGenreResponse {
        throw NoServerException()
    }

    override suspend fun getNewArtists(
        limit: Int,
        offset: Int,
        action: String
    ): AmpacheArtistResponse {
        throw NoServerException()
    }

    override suspend fun getNewAlbums(
        limit: Int,
        offset: Int,
        action: String
    ): AmpacheAlbumResponse {
        throw NoServerException()
    }

    override suspend fun getPlaylists(
        limit: Int,
        offset: Int,
        action: String,
        type: String,
        include: Int,
        hideSearch: Int
    ): AmpachePlaylistResponse {
        throw NoServerException()
    }

    override suspend fun getAddedSongs(
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheSongResponse {
        throw NoServerException()
    }

    override suspend fun getAddedGenres(
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheGenreResponse {
        throw NoServerException()
    }

    override suspend fun getAddedArtists(
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheArtistResponse {
        throw NoServerException()
    }

    override suspend fun getAddedAlbums(
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheAlbumResponse {
        throw NoServerException()
    }

    override suspend fun getUpdatedSongs(
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheSongResponse {
        throw NoServerException()
    }

    override suspend fun getUpdatedGenres(
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheGenreResponse {
        throw NoServerException()
    }

    override suspend fun getUpdatedArtists(
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheArtistResponse {
        throw NoServerException()
    }

    override suspend fun getUpdatedAlbums(
        limit: Int,
        offset: Int,
        update: String,
        action: String
    ): AmpacheAlbumResponse {
        throw NoServerException()
    }

    override suspend fun getDeletedSongs(
        action: String,
        limit: Int,
        offset: Int
    ): AmpacheDeletedSongIdResponse {
        throw NoServerException()
    }

    override suspend fun createPlaylist(action: String, name: String, type: String) =
        throw NoServerException()

    override suspend fun deletePlaylist(action: String, id: String) {
        throw NoServerException()
    }

    override suspend fun addToPlaylist(
        action: String,
        filter: Long,
        songId: Long,
        check: Int
    ) = throw NoServerException()

    override suspend fun removeFromPlaylist(
        action: String,
        filter: Long,
        song: Long
    ) {
        throw NoServerException()
    }

    override suspend fun streamError(
        action: String,
        type: String,
        songId: Long
    ): AmpacheErrorObject {
        throw NoServerException()
    }
}