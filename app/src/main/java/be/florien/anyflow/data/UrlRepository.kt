package be.florien.anyflow.data

import be.florien.anyflow.feature.sync.SyncRepository
import be.florien.anyflow.data.server.di.ServerScope
import javax.inject.Inject
import javax.inject.Named

@ServerScope
class UrlRepository @Inject constructor(@Named("serverUrl") private val serverUrl: String) {

    fun getSongUrl(id: Long) = getMediaUrl(id, "song")
    fun getSongArtUrl(id: Long) = getArtUrl(SyncRepository.ART_TYPE_SONG, id)
    fun getAlbumArtUrl(id: Long) = getArtUrl(SyncRepository.ART_TYPE_ALBUM, id)
    fun getArtistArtUrl(id: Long) = getArtUrl(SyncRepository.ART_TYPE_ARTIST, id)
    fun getPlaylistArtUrl(id: Long) = getArtUrl(SyncRepository.ART_TYPE_PLAYLIST, id)
    fun getPodcastArtUrl(id: Long) = getArtUrl(SyncRepository.ART_TYPE_PODCAST, id)

    fun getArtUrl(type: String, id: Long) =
        "${serverUrl}server/json.server.php?action=get_art&type=$type&id=$id"

    fun getMediaUrl(id: Long, mediaType: String) =
        "${serverUrl}server/json.server.php?action=stream&type=$mediaType&id=$id&uid=1"
}