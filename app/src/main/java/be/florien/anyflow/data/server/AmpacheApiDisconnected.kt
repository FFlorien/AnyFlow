package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.exception.NoServerException
import be.florien.anyflow.data.server.model.*

/**
 * Responses for ampache calls when the server is not set
 */
class AmpacheApiDisconnected : AmpacheApi {
    override suspend fun authenticate(action: String, time: String, version: String, auth: String, user: String): AmpacheAuthentication =
            throw NoServerException()

    override suspend fun ping(action: String, auth: String): AmpachePing =
            throw NoServerException()

    override suspend fun getSongs(action: String, add: String, auth: String, limit: Int, offset: Int): List<AmpacheSong> =
            throw NoServerException()

    override suspend fun getArtists(action: String, add: String, auth: String, limit: Int, offset: Int): List<AmpacheArtist> =
            throw NoServerException()

    override suspend fun getAlbums(action: String, add: String, auth: String, limit: Int, offset: Int): List<AmpacheAlbum> =
            throw NoServerException()

    override suspend fun getTags(action: String, add: String, auth: String, limit: Int, offset: Int): List<AmpacheTag> =
            throw NoServerException()

    override suspend fun getPlaylists(action: String, auth: String, limit: Int, offset: Int, hideSearch: Int): List<AmpachePlayList> =
            throw NoServerException()

    override suspend fun getPlaylistSongs(action: String, filter: String, auth: String, limit: Int, offset: Int): List<AmpacheSongId> =
            throw NoServerException()

    override suspend fun createPlaylist(action: String, auth: String, name: String, type: String)  = throw NoServerException()

    override suspend fun addToPlaylist(action: String, filter: Long, auth: String, songId: Long, check: Int) = throw NoServerException()
}