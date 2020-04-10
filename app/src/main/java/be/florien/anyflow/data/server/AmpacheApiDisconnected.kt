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

    override suspend fun getSongs(action: String, add: String, auth: String, limit: Int, offset: Int): AmpacheSongList =
            throw NoServerException()

    override suspend fun getArtists(action: String, add: String, auth: String, limit: Int, offset: Int): AmpacheArtistList =
            throw NoServerException()

    override suspend fun getAlbums(action: String, add: String, auth: String, limit: Int, offset: Int): AmpacheAlbumList =
            throw NoServerException()

    override suspend fun getTags(action: String, add: String, auth: String, limit: Int, offset: Int): AmpacheTagList =
            throw NoServerException()

    override suspend fun getPlaylists(action: String, add: String, auth: String, limit: Int, offset: Int): AmpachePlayListList =
            throw NoServerException()
}