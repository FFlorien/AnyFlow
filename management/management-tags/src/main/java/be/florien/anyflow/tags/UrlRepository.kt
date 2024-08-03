package be.florien.anyflow.tags

import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.management.filters.model.Filter.Companion.ART_TYPE_ALBUM
import be.florien.anyflow.management.filters.model.Filter.Companion.ART_TYPE_ARTIST
import be.florien.anyflow.management.filters.model.Filter.Companion.ART_TYPE_PLAYLIST
import be.florien.anyflow.management.filters.model.Filter.Companion.ART_TYPE_PODCAST
import be.florien.anyflow.management.filters.model.Filter.Companion.ART_TYPE_SONG
import javax.inject.Inject
import javax.inject.Named

@ServerScope
class UrlRepository @Inject constructor(@Named("serverUrl") private val serverUrl: String) {

    fun getSongUrl(id: Long) = getMediaUrl(id, "song")
    fun getSongArtUrl(id: Long) = getArtUrl(ART_TYPE_SONG, id)
    fun getAlbumArtUrl(id: Long) = getArtUrl(ART_TYPE_ALBUM, id)
    fun getArtistArtUrl(id: Long) = getArtUrl(ART_TYPE_ARTIST, id)
    fun getPlaylistArtUrl(id: Long) = getArtUrl(ART_TYPE_PLAYLIST, id)
    fun getPodcastArtUrl(id: Long) = getArtUrl(ART_TYPE_PODCAST, id)

    fun getArtUrl(type: String, id: Long) =
        "${serverUrl}server/json.server.php?action=get_art&type=$type&id=$id"

    fun getMediaUrl(id: Long, mediaType: String) =
        "${serverUrl}server/json.server.php?action=stream&type=$mediaType&id=$id&uid=1"
}