package be.florien.anyflow.management.queue

import androidx.core.text.HtmlCompat
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.FilterGroup
import be.florien.anyflow.management.filters.model.FilterType
import be.florien.anyflow.management.filters.model.PodcastFilterType
import be.florien.anyflow.management.filters.model.TagFilterType
import be.florien.anyflow.management.queue.model.Ordering
import be.florien.anyflow.management.queue.model.Ordering.Companion.SUBJECT_ALBUM
import be.florien.anyflow.management.queue.model.Ordering.Companion.SUBJECT_ALBUM_ARTIST
import be.florien.anyflow.management.queue.model.Ordering.Companion.SUBJECT_ALBUM_ID
import be.florien.anyflow.management.queue.model.Ordering.Companion.SUBJECT_ALL
import be.florien.anyflow.management.queue.model.Ordering.Companion.SUBJECT_ARTIST
import be.florien.anyflow.management.queue.model.Ordering.Companion.SUBJECT_DISC
import be.florien.anyflow.management.queue.model.Ordering.Companion.SUBJECT_GENRE
import be.florien.anyflow.management.queue.model.Ordering.Companion.SUBJECT_TITLE
import be.florien.anyflow.management.queue.model.Ordering.Companion.SUBJECT_TRACK
import be.florien.anyflow.management.queue.model.Ordering.Companion.SUBJECT_YEAR
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.QueueItemDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.local.model.DbFilter
import be.florien.anyflow.tags.local.model.DbFilterGroup
import be.florien.anyflow.tags.local.model.DbOrdering
import be.florien.anyflow.tags.local.model.DbQueueItemDisplay
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.tags.local.query.QueryOrdering
import java.util.Calendar


fun DbOrdering.toViewOrdering(): Ordering {
    return when (orderingType) {
        Ordering.ASCENDING -> Ordering.Ordered(priority, subject)
        Ordering.PRECISE_POSITION -> Ordering.Precise(orderingArgument, subject, priority)
        Ordering.RANDOM -> Ordering.Random(priority, subject, orderingArgument)
        else -> Ordering.Ordered(priority, subject)
    }
}

fun Ordering.toDbOrdering() = DbOrdering(
    priority = priority,
    subject = subject,
    orderingType = ordering,
    orderingArgument = argument
)

fun Ordering.toQueryOrdering() = when (ordering) {
    Ordering.PRECISE_POSITION -> QueryOrdering.Precise(
        priority = priority,
        subject = subject(),
        precisePosition = argument,
        songId = subject
    )

    Ordering.RANDOM -> QueryOrdering.Random(
        priority = priority,
        subject = subject(),
        randomSeed = argument
    )

    else -> QueryOrdering.Ordered(
        priority = priority,
        subject = subject()
    )
}

fun List<Ordering>.toQueryOrderings() = map { it.toQueryOrdering() }

private fun Ordering.subject() = when (subject) {
    SUBJECT_ALL -> QueryOrdering.Subject.ALL
    SUBJECT_ARTIST -> QueryOrdering.Subject.ARTIST
    SUBJECT_ALBUM_ARTIST -> QueryOrdering.Subject.ALBUM_ARTIST
    SUBJECT_ALBUM -> QueryOrdering.Subject.ALBUM
    SUBJECT_ALBUM_ID -> QueryOrdering.Subject.ALBUM_ID
    SUBJECT_DISC -> QueryOrdering.Subject.DISC
    SUBJECT_YEAR -> QueryOrdering.Subject.YEAR
    SUBJECT_GENRE -> QueryOrdering.Subject.GENRE
    SUBJECT_TRACK -> QueryOrdering.Subject.TRACK
    SUBJECT_TITLE -> QueryOrdering.Subject.TITLE
    else -> QueryOrdering.Subject.TRACK
}


fun DbFilter.toViewFilter(filterList: List<DbFilter>): Filter<*> =
    Filter(
        argument = if (type == DbFilter.TYPE_DOWNLOADED) argument.toBoolean() else argument.toLong(),
        type = when (type) {
            DbFilter.TYPE_GENRE -> TagFilterType.GENRE_IS
            DbFilter.TYPE_SONG -> TagFilterType.SONG_IS
            DbFilter.TYPE_ARTIST -> TagFilterType.ARTIST_IS
            DbFilter.TYPE_ALBUM_ARTIST -> TagFilterType.ALBUM_ARTIST_IS
            DbFilter.TYPE_ALBUM -> TagFilterType.ALBUM_IS
            DbFilter.TYPE_DISK -> TagFilterType.DISK_IS
            DbFilter.TYPE_PLAYLIST -> TagFilterType.PLAYLIST_IS
            DbFilter.TYPE_DOWNLOADED -> TagFilterType.DOWNLOADED_STATUS_IS
            DbFilter.TYPE_PODCAST_EPISODE -> PodcastFilterType.PODCAST_EPISODE_IS
            DbFilter.TYPE_PODCAST -> PodcastFilterType.PODCAST_IS
            else -> TagFilterType.SONG_IS
        },
        displayText = displayText,
        children = getChildrenFilters(this, filterList)
    )

private fun getChildrenFilters( //warning: this hasn't been tested (yet)
    filter: DbFilter,
    filterList: List<DbFilter>
): List<Filter<*>> = filterList.filter { dbFilter -> filter.id == dbFilter.parentFilter }
    .map { dbFilter -> dbFilter.toViewFilter(filterList) }

fun DbFilterGroup.toViewFilterGroup(): FilterGroup {
    val dateAddedNS = dateAdded
    val nameNS = name
    return when {
        dateAddedNS == null -> FilterGroup.CurrentFilterGroup(id)
        nameNS == null -> FilterGroup.HistoryFilterGroup(
            id,
            Calendar.getInstance().apply { timeInMillis = dateAddedNS })

        else -> FilterGroup.SavedFilterGroup(
            id,
            Calendar.getInstance().apply { timeInMillis = dateAddedNS },
            nameNS
        )
    }
}

fun Filter<*>.toDbFilter(groupId: Long, parentId: Long? = null) = DbFilter(
    id = null,
    type = this.type.toDbFilterType(),
    argument = argument.toString(),
    displayText = displayText,
    filterGroup = groupId,
    parentFilter = parentId
)

fun FilterType.toDbFilterType() = when (this) {
    TagFilterType.GENRE_IS -> DbFilter.TYPE_GENRE
    TagFilterType.SONG_IS -> DbFilter.TYPE_SONG
    TagFilterType.ARTIST_IS -> DbFilter.TYPE_ARTIST
    TagFilterType.ALBUM_ARTIST_IS -> DbFilter.TYPE_ALBUM_ARTIST
    TagFilterType.ALBUM_IS -> DbFilter.TYPE_ALBUM
    TagFilterType.DISK_IS -> DbFilter.TYPE_DISK
    TagFilterType.PLAYLIST_IS -> DbFilter.TYPE_PLAYLIST
    TagFilterType.DOWNLOADED_STATUS_IS -> DbFilter.TYPE_DOWNLOADED
    PodcastFilterType.PODCAST_EPISODE_IS -> DbFilter.TYPE_PODCAST_EPISODE
    PodcastFilterType.PODCAST_IS -> DbFilter.TYPE_PODCAST
    PodcastFilterType.STATE_IS -> DbFilter.TYPE_STATE
}

fun DbQueueItemDisplay.toViewQueueItemDisplay(): QueueItemDisplay {
    val songIdNS = songId
    val songTitleNS = songTitle
    val songArtistNameNS = songArtistName
    val songAlbumNameNS = songAlbumName
    val songAlbumIdNS = songAlbumId
    val songTimeNS = songTime
    val podcastEpisodeIdNS = podcastEpisodeId
    val podcastTitleNS = podcastTitle
    val podcastTimeNS = podcastTime
    val podcastIdNS = podcastId
    val podcastNameNS = podcastName
    val podcastDescriptionNS = podcastDescription
    return if (
        mediaType == SONG_MEDIA_TYPE &&
        songIdNS != null &&
        songTitleNS != null &&
        songArtistNameNS != null &&
        songAlbumNameNS != null &&
        songAlbumIdNS != null &&
        songTimeNS != null
    ) {
        SongDisplay(
            id = songIdNS,
            title = songTitleNS,
            artistName = songArtistNameNS,
            albumName = songAlbumNameNS,
            albumId = songAlbumIdNS,
            time = songTimeNS
        )
    } else if (
        mediaType == PODCAST_MEDIA_TYPE &&
        podcastEpisodeIdNS != null &&
        podcastTitleNS != null &&
        podcastTimeNS != null &&
        podcastNameNS != null &&
        podcastIdNS != null &&
        podcastDescriptionNS != null
    ) {
        val podcastDescriptionHtmlEscaped = HtmlCompat.fromHtml(
            podcastDescriptionNS,
            HtmlCompat.FROM_HTML_MODE_COMPACT
        ).toString()
        PodcastEpisodeDisplay(
            id = podcastEpisodeIdNS,
            title = podcastTitleNS,
            time = podcastTimeNS,
            podcast = podcastNameNS,
            podcastId = podcastIdNS,
            description = podcastDescriptionHtmlEscaped
        )
    } else {
        throw IllegalArgumentException("DbQueueItemDisplay is not a valid SongDisplay or PodcastEpisodeDisplay\n$this")
    }

}