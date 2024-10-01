package be.florien.anyflow.management.queue

import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.FilterGroup
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
            DbFilter.TYPE_GENRE -> Filter.FilterType.GENRE_IS
            DbFilter.TYPE_SONG -> Filter.FilterType.SONG_IS
            DbFilter.TYPE_ARTIST -> Filter.FilterType.ARTIST_IS
            DbFilter.TYPE_ALBUM_ARTIST -> Filter.FilterType.ALBUM_ARTIST_IS
            DbFilter.TYPE_ALBUM -> Filter.FilterType.ALBUM_IS
            DbFilter.TYPE_DISK -> Filter.FilterType.DISK_IS
            DbFilter.TYPE_PLAYLIST -> Filter.FilterType.PLAYLIST_IS
            DbFilter.TYPE_DOWNLOADED -> Filter.FilterType.DOWNLOADED_STATUS_IS
            DbFilter.TYPE_PODCAST_EPISODE -> Filter.FilterType.PODCAST_EPISODE_IS
            else -> Filter.FilterType.SONG_IS
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

fun Filter.FilterType.toDbFilterType() = when (this) {
    Filter.FilterType.GENRE_IS -> DbFilter.TYPE_GENRE
    Filter.FilterType.SONG_IS -> DbFilter.TYPE_SONG
    Filter.FilterType.ARTIST_IS -> DbFilter.TYPE_ARTIST
    Filter.FilterType.ALBUM_ARTIST_IS -> DbFilter.TYPE_ALBUM_ARTIST
    Filter.FilterType.ALBUM_IS -> DbFilter.TYPE_ALBUM
    Filter.FilterType.DISK_IS -> DbFilter.TYPE_DISK
    Filter.FilterType.PLAYLIST_IS -> DbFilter.TYPE_PLAYLIST
    Filter.FilterType.DOWNLOADED_STATUS_IS -> DbFilter.TYPE_DOWNLOADED
    Filter.FilterType.PODCAST_EPISODE_IS -> DbFilter.TYPE_PODCAST_EPISODE
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
        podcastIdNS != null
    ) {
        PodcastEpisodeDisplay(
            id = podcastEpisodeIdNS,
            title = podcastTitleNS,
            artist = podcastAuthor ?: "",
            time = podcastTimeNS,
            album = podcastName ?: "",
            albumId = podcastIdNS
        )
    } else {
        throw IllegalArgumentException("DbQueueItemDisplay is not a valid SongDisplay or PodcastEpisodeDisplay\n$this")
    }

}