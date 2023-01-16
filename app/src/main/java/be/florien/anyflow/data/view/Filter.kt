package be.florien.anyflow.data.view

import android.os.Parcelable
import be.florien.anyflow.data.DataRepository
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

    suspend fun contains(song: SongInfo, dataRepository: DataRepository): Boolean {
        return when (this.type) {
            FilterType.ALBUM_ARTIST_IS -> song.albumArtistId == argument
            FilterType.ALBUM_IS -> song.albumId == argument
            FilterType.ARTIST_IS -> song.artistId == argument
            FilterType.GENRE_IS -> song.genreIds.any { it == argument }
            FilterType.SONG_IS -> song.id == argument
            FilterType.PLAYLIST_IS -> dataRepository.isPlaylistContainingSong(
                argument as Long,
                song.id
            )
            FilterType.DOWNLOADED_STATUS_IS -> dataRepository.hasDownloaded(song)
        }
    }

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
        SONG_IS(DataRepository.ART_TYPE_SONG),
        ARTIST_IS(DataRepository.ART_TYPE_ARTIST),
        ALBUM_ARTIST_IS(DataRepository.ART_TYPE_ARTIST),
        ALBUM_IS(DataRepository.ART_TYPE_ALBUM),
        GENRE_IS(null),
        PLAYLIST_IS(DataRepository.ART_TYPE_PLAYLIST),
        DOWNLOADED_STATUS_IS(null)
    }
}

data class FilterCount(
    val duration: Duration,
    val genres: Int,
    val albumArtists: Int,
    val albums: Int,
    val artists: Int,
    val songs: Int,
    val playlists: Int
)