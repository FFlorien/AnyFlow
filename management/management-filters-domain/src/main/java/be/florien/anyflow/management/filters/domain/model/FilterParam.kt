package be.florien.anyflow.management.filters.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class FilterParam<T>(
    val type: @RawValue FilterType,
    val argument: @RawValue T,
    val displayText: String
) : Parcelable {

    override fun equals(other: Any?) =
        other is FilterParam<*> && argument == other.argument && type == other.type

    override fun hashCode(): Int {
        var result = argument.hashCode() + javaClass.name.hashCode()
        result = 31 * result + (argument?.hashCode() ?: 0)
        return result
    }

    companion object { //todo put in common with SyncRepository
        const val ART_TYPE_SONG = "song"
        const val ART_TYPE_ALBUM = "album"
        const val ART_TYPE_ARTIST = "artist"
        const val ART_TYPE_PLAYLIST = "playlist"
        const val ART_TYPE_PODCAST = "podcast"
    }
}

data class FilterTagsCount( //todo move ?
    val duration: Int,
    val genres: Int,
    val albumArtists: Int,
    val albums: Int,
    val artists: Int,
    val songs: Int,
    val playlists: Int,
    val downloaded: Int
)


data class FilterPodcastCount(
    val podcasts: Int,
    val podcastEpisodes: Int
)