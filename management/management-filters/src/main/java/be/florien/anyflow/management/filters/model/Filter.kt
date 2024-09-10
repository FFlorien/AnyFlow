package be.florien.anyflow.management.filters.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlin.time.Duration

@Parcelize
data class Filter<T>(
    val type: FilterType,
    val argument: @RawValue T,
    val displayText: String,
    var children: List<Filter<*>> = emptyList()
) : Parcelable {

    fun getFullDisplay(): String =
        displayText + (children.firstOrNull()?.getFullDisplay()?.let { " > $it" } ?: "")

    fun getFilterIfTypePresent(filterType: FilterType): Filter<*>? =
        this.takeIf { it.type == filterType }
            ?: children.firstNotNullOfOrNull { it.getFilterIfTypePresent(filterType) }

    fun equalsIgnoreChildren(other: Filter<*>) = argument == other.argument && type == other.type

    override fun equals(other: Any?) =
        other is Filter<*>
                && equalsIgnoreChildren(other)
                && children.all { other.children.contains(it) }
                && other.children.all { children.contains(it) }

    override fun hashCode(): Int {
        var result = argument.hashCode() + javaClass.name.hashCode()
        result = 31 * result + (argument?.hashCode() ?: 0)
        return result
    }

    fun deepCopy(): Filter<T> {
        return copy(children = children.map { it.deepCopy() })
    }

    fun addToDeepestChild(filter: Filter<*>) {
        var candidateParent = this as Filter<*>
        while (candidateParent.children.isNotEmpty()) {
            candidateParent = candidateParent.children.first()
        }

        candidateParent.children = listOf(filter)
    }

    fun traversal(action: (Filter<*>) -> Unit) {
        action(this)
        children.forEach { it.traversal(action) }
    }

    enum class FilterType(val artType: String?) {
        SONG_IS(ART_TYPE_SONG),
        ARTIST_IS(ART_TYPE_ARTIST),
        ALBUM_ARTIST_IS(ART_TYPE_ARTIST),
        ALBUM_IS(ART_TYPE_ALBUM),
        GENRE_IS(null),
        PLAYLIST_IS(ART_TYPE_PLAYLIST),
        DOWNLOADED_STATUS_IS(null),
        PODCAST_EPISODE_IS(ART_TYPE_PODCAST),
        DISK_IS(ART_TYPE_ALBUM)
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
    val duration: Duration,
    val genres: Int,
    val albumArtists: Int,
    val albums: Int,
    val artists: Int,
    val songs: Int,
    val playlists: Int,
    val downloaded: Int
)


data class FilterPodcastCount(
    val podcastEpisodes: Int
)