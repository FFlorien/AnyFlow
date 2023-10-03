package be.florien.anyflow.data

import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.feature.sync.SyncRepository
import be.florien.anyflow.injection.ServerScope
import javax.inject.Inject

@ServerScope
class UrlRepository @Inject constructor(private val ampacheDataSource: AmpacheDataSource) {

    fun getSongUrl(id: Long) = ampacheDataSource.getSongUrl(id)
    fun getAlbumArtUrl(id: Long) = ampacheDataSource.getArtUrl(SyncRepository.ART_TYPE_ALBUM, id)
    fun getArtistArtUrl(id: Long) = ampacheDataSource.getArtUrl(SyncRepository.ART_TYPE_ARTIST, id)
    fun getArtUrl(type: String, id: Long) = ampacheDataSource.getArtUrl(type, id)

    fun getPlaylistArtUrl(id: Long) =
        ampacheDataSource.getArtUrl(SyncRepository.ART_TYPE_PLAYLIST, id)
}