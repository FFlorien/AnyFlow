package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.exception.NoServerException
import be.florien.anyflow.data.server.model.*
import io.reactivex.Observable

/**
 * Responses for ampache calls when the server is not set
 */
class AmpacheApiDisconnected : AmpacheApi {
    override fun authenticate(action: String, time: String, version: String, auth: String, user: String): Observable<AmpacheAuthentication> =
            Observable.error(NoServerException())

    override fun ping(action: String, auth: String): Observable<AmpachePing> =
            Observable.error(NoServerException())

    override fun getSongs(action: String, update: String, auth: String, limit: Int, offset: Int): Observable<AmpacheSongList> =
            Observable.error(NoServerException())

    override fun getArtists(action: String, update: String, auth: String, limit: Int, offset: Int): Observable<AmpacheArtistList> =
            Observable.error(NoServerException())

    override fun getAlbums(action: String, update: String, auth: String, limit: Int, offset: Int): Observable<AmpacheAlbumList> =
            Observable.error(NoServerException())

    override fun getTags(action: String, update: String, auth: String, limit: Int, offset: Int): Observable<AmpacheTagList> =
            Observable.error(NoServerException())

    override fun getPlaylists(action: String, update: String, auth: String, limit: Int, offset: Int): Observable<AmpachePlayListList> =
            Observable.error(NoServerException())

    override fun getSong(action: String, uid: Long, auth: String): Observable<AmpacheSongList> =
            Observable.error(NoServerException())
}