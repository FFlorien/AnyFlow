package be.florien.ampacheplayer.api

import be.florien.ampacheplayer.api.model.*
import io.reactivex.Observable

/**
 * Created by FlamentF on 17-Jan-18.
 */
class MockUpAmpacheApi : AmpacheApi {
    companion object {
        const val ACCOUNT_VALID_1_HOUR = "OneHour"
        const val ACCOUNT_VALID_30_SECONDS = "ThirtySeconds"
        const val BAD_PASSWORD = "BadPassword"
        const val CANNOT_REACH = "CannotReach"  //todo
    }

    override fun authenticate(action: String, time: String, version: String, auth: String, user: String): Observable<AmpacheAuthentication> = when (user) {
        ACCOUNT_VALID_1_HOUR -> Observable.just(AmpacheAuthentication().apply {
            sessionExpire = "3024-01-01T00:00:00GMT"//todo
            update = "3024-01-01T00:00:00GMT"
        })
        ACCOUNT_VALID_30_SECONDS -> Observable.just(AmpacheAuthentication().apply {
            sessionExpire = "3024-01-01T00:00:00GMT"//todo
            update = "3024-01-01T00:00:00GMT"
        })
        BAD_PASSWORD -> Observable.just(AmpacheAuthentication().apply {
            error = AmpacheError().apply {
                code = 401
                errorText = "Bad user/password"
            }
        })
        else -> Observable.just(AmpacheAuthentication().apply {
            sessionExpire = "3024-01-01T00:00:00GMT"//todo
            update = "3024-01-01T00:00:00GMT"
        })
    }

    override fun ping(action: String, auth: String): Observable<AmpachePing> = Observable.just(AmpachePing().apply {
        sessionExpire = "3024-01-01T00:00:00GMT"
    })

    override fun getSongs(action: String, update: String, auth: String): Observable<AmpacheSongList> = Observable.just(AmpacheSongList().apply {
        total_count = 2
        songs = listOf(
                AmpacheSong().apply {
                    id = 100
                    song = "First Song"
                    title = "First Song"
                    name = "First Song"
                    artist = AmpacheArtistName().apply {
                        id = 200
                        name = "First Singer"
                    }
                    album = AmpacheAlbumName().apply {
                        id = 300
                        name = "First Album"
                    }
                    albumartist = AmpacheAlbumArtist().apply {
                        id = 200
                        name = "First Singer"
                    }
                    tag = listOf(
                            AmpacheTagName().apply {
                                id = 400
                                this.value = "Cool"
                            }
                    )
                    filename = "First Singer - First Song"
                    track = 1
                    time = 150
                    url = "mock://firstsong"
                    art = "mock://firstsongart"
                    genre = "Pop"
                },
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