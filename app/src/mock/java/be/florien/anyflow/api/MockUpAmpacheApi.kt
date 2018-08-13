package be.florien.anyflow.api

import android.content.Context
import be.florien.anyflow.api.model.*
import io.reactivex.Observable
import org.simpleframework.xml.core.Persister
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class MockUpAmpacheApi(val context: Context) : AmpacheApi {

    companion object {
        const val ACCOUNT_VALID_30_SECONDS = "30Seconds"
        const val ACCOUNT_VALID_1_HOUR = "1hour"
        const val ACCOUNT_VALID_30_YEAR = "30years"
        const val BAD_PASSWORD = "BadAuth"
        private const val MS_TO_S: Long = 1000L
        private const val MS_TO_M: Long = MS_TO_S * 60L
        private const val MS_TO_H: Long = MS_TO_M * 60L
        private const val MS_TO_D: Long = MS_TO_H * 24L
        private const val MS_TO_Y_ISH: Long = MS_TO_D * 365L
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", Locale.US)

    override fun authenticate(action: String, time: String, version: String, auth: String, user: String): Observable<AmpacheAuthentication> = when (user) {
        ACCOUNT_VALID_30_SECONDS -> Observable.just(AmpacheAuthentication().apply {
            sessionExpire = dateFormatter.format(Date(Date().time + (30 * MS_TO_S)))
            update = "3024-01-01T00:00:00GMT"
        })
        ACCOUNT_VALID_1_HOUR -> Observable.just(AmpacheAuthentication().apply {
            sessionExpire = dateFormatter.format(Date(Date().time + (1 * MS_TO_H)))
            update = "3024-01-01T00:00:00GMT"
        })
        ACCOUNT_VALID_30_YEAR -> Observable.just(AmpacheAuthentication().apply {
            sessionExpire = dateFormatter.format(Date(Date().time + (30 * MS_TO_Y_ISH)))
            update = "3024-01-01T00:00:00GMT"
        })
        BAD_PASSWORD -> Observable.just(AmpacheAuthentication().apply {
            error = AmpacheError().apply {
                code = 401
                errorText = "Bad user/password"
            }
        })
        else -> Observable.just(AmpacheAuthentication().apply {
            error = AmpacheError().apply {
                code = 401
                errorText = "Bad user/password"
            }
        })
    }

    override fun ping(action: String, auth: String): Observable<AmpachePing> = Observable.just(AmpachePing().apply {
        sessionExpire = "3024-01-01T00:00:00GMT"
    })

    override fun getSongs(action: String, update: String, auth: String): Observable<AmpacheSongList> {
        val reader = InputStreamReader(context.assets.open("mock_songs.xml"))
        return Observable.just(Persister().read(AmpacheSongList::class.java, reader))
    }

    override fun getArtists(action: String, update: String, auth: String): Observable<AmpacheArtistList> = Observable.just(AmpacheArtistList().apply {
        total_count = 2
        artists = listOf(
                AmpacheArtist().apply {
                    id = 200
                    name = "First Singer"
                },
                AmpacheArtist().apply {
                    id = 201
                    name = "Second Singer"
                }
        )
    })

    override fun getAlbums(action: String, update: String, auth: String): Observable<AmpacheAlbumList> = Observable.just(AmpacheAlbumList().apply {
        total_count = 2
        albums = listOf(
                AmpacheAlbum().apply {
                    id = 300
                    name = "First Album"
                },
                AmpacheAlbum().apply {
                    id = 301
                    name = "Second Album"
                }
        )
    })

    override fun getTags(action: String, update: String, auth: String): Observable<AmpacheTagList> = Observable.just(AmpacheTagList().apply {
        total_count = 1
        tags = listOf(
                AmpacheTag().apply {
                    id = 401
                    name = "Cool"
                }
        )
    })

    override fun getPlaylists(action: String, update: String, auth: String): Observable<AmpachePlayListList> = Observable.just(AmpachePlayListList().apply {
        total_count = 0
        playlists = listOf()
    })

    override fun getSong(action: String, uid: Long, auth: String): Observable<AmpacheSongList> = Observable.just(AmpacheSongList().apply {
        total_count = 1
        songs = listOf(
                AmpacheSong().apply {
                    id = 101
                    song = "Second Song"
                    title = "Second Song"
                    name = "Second Song"
                    artist = AmpacheArtistName().apply {
                        id = 201
                        name = "Second Singer"
                    }
                    album = AmpacheAlbumName().apply {
                        id = 301
                        name = "Second Album"
                    }
                    albumartist = AmpacheAlbumArtist().apply {
                        id = 201
                        name = "Second Singer"
                    }
                    tag = listOf(
                            AmpacheTagName().apply {
                                id = 401
                                this.value = "Cool"
                            }
                    )
                    filename = "Second Singer - Second Song"
                    track = 1
                    time = 100
                    url = "mock://secondsong"
                    art = "mock://secondsongart"
                    genre = "Pop"
                }
        )
    })
}