package be.florien.ampacheplayer.api

import be.florien.ampacheplayer.api.model.*
import be.florien.ampacheplayer.exception.NoServerException
import io.reactivex.Observable

/**
 * Created by florien on 20/01/18.
 */
class AmpacheApiDisconnected: AmpacheApi {
    override fun authenticate(action: String, time: String, version: String, auth: String, user: String): Observable<AmpacheAuthentication> = Observable.error(NoServerException())

    override fun ping(action: String, auth: String): Observable<AmpachePing> = Observable.error(NoServerException())

    override fun getSongs(action: String, update: String, auth: String): Observable<AmpacheSongList> = Observable.error(NoServerException())

    override fun getArtists(action: String, update: String, auth: String): Observable<AmpacheArtistList> = Observable.error(NoServerException())

    override fun getAlbums(action: String, update: String, auth: String): Observable<AmpacheAlbumList> = Observable.error(NoServerException())

    override fun getTags(action: String, update: String, auth: String): Observable<AmpacheTagList> = Observable.error(NoServerException())

    override fun getPlaylists(action: String, update: String, auth: String): Observable<AmpachePlayListList> = Observable.error(NoServerException())

    override fun getSong(action: String, uid: Long, auth: String): Observable<AmpacheSongList> = Observable.error(NoServerException())

}