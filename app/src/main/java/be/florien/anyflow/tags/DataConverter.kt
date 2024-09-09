package be.florien.anyflow.tags

import be.florien.anyflow.tags.local.model.DbAlarm
import be.florien.anyflow.tags.local.model.DbAlbum
import be.florien.anyflow.tags.local.model.DbArtist
import be.florien.anyflow.tags.local.model.DbFilter
import be.florien.anyflow.tags.local.model.DbFilterGroup
import be.florien.anyflow.tags.local.model.DbGenre
import be.florien.anyflow.tags.local.model.DbOrdering
import be.florien.anyflow.tags.local.model.DbPlaylist
import be.florien.anyflow.tags.local.model.DbPlaylistSongs
import be.florien.anyflow.tags.local.model.DbPodcast
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.tags.local.model.DbQueueItemDisplay
import be.florien.anyflow.tags.local.model.DbSong
import be.florien.anyflow.tags.local.model.DbSongDisplay
import be.florien.anyflow.tags.local.model.DbSongGenre
import be.florien.anyflow.tags.local.model.DbSongId
import be.florien.anyflow.tags.local.model.DbSongInfo
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.tags.local.query.QueryFilter
import be.florien.anyflow.tags.local.query.QueryOrdering
import be.florien.anyflow.tags.model.SongInfo
import be.florien.anyflow.data.server.model.AmpacheAlbum
import be.florien.anyflow.data.server.model.AmpacheArtist
import be.florien.anyflow.data.server.model.AmpacheNameId
import be.florien.anyflow.data.server.model.AmpachePlayList
import be.florien.anyflow.data.server.model.AmpachePlaylistObject
import be.florien.anyflow.data.server.model.AmpachePlaylistsWithSongs
import be.florien.anyflow.data.server.model.AmpachePodcast
import be.florien.anyflow.data.server.model.AmpachePodcastEpisode
import be.florien.anyflow.data.server.model.AmpacheSong
import be.florien.anyflow.data.server.model.AmpacheSongId
import be.florien.anyflow.tags.view.Alarm
import be.florien.anyflow.tags.view.Ordering
import be.florien.anyflow.tags.view.Ordering.Companion.SUBJECT_ALBUM
import be.florien.anyflow.tags.view.Ordering.Companion.SUBJECT_ALBUM_ARTIST
import be.florien.anyflow.tags.view.Ordering.Companion.SUBJECT_ALBUM_ID
import be.florien.anyflow.tags.view.Ordering.Companion.SUBJECT_ALL
import be.florien.anyflow.tags.view.Ordering.Companion.SUBJECT_ARTIST
import be.florien.anyflow.tags.view.Ordering.Companion.SUBJECT_DISC
import be.florien.anyflow.tags.view.Ordering.Companion.SUBJECT_GENRE
import be.florien.anyflow.tags.view.Ordering.Companion.SUBJECT_TITLE
import be.florien.anyflow.tags.view.Ordering.Companion.SUBJECT_TRACK
import be.florien.anyflow.tags.view.Ordering.Companion.SUBJECT_YEAR
import be.florien.anyflow.tags.view.QueueItemDisplay
import be.florien.anyflow.tags.view.SongDisplay
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions
import be.florien.anyflow.feature.playlist.selection.domain.TagType
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.FilterGroup
import be.florien.anyflow.tags.view.PodcastEpisodeDisplay
import be.florien.anyflow.utils.TimeOperations
import java.util.Calendar


//region Server to Database
fun AmpacheSong.toDbSong(local: String? = null) = DbSong(
    id = id,
    title = title,
    titleForSort = title.transformToSortFriendly(),
    artistId = artist.id,
    albumId = album.id,
    track = track,
    time = time,
    year = year,
    composer = composer ?: "",
    disk = disk,
    size = size,
    local = local,
    waveForm = ""
)

fun AmpacheSong.toDbSongGenres(): List<DbSongGenre> {
    val songId = id
    return genre.map { DbSongGenre(songId = songId, it.id) }
}

fun AmpacheSongId.toDbSongId() = DbSongId(id)

fun AmpacheArtist.toDbArtist() = DbArtist(
    id = id,
    name = name,
    prefix = prefix,
    basename = basename.transformToSortFriendly(),
    summary = summary
)

fun AmpacheNameId.toDbGenre() = DbGenre(
    id = id,
    name = name
)

fun AmpacheAlbum.toDbAlbum() = DbAlbum(
    id = id,
    name = name,
    prefix = prefix,
    basename = basename.transformToSortFriendly(),
    artistId = artist.id,
    year = year,
    diskcount = diskcount
)


fun AmpachePlayList.toDbPlaylist() = DbPlaylist(
    id = id,
    name = name,
    owner = owner
)

fun AmpachePlaylistsWithSongs.toDbPlaylistSongs() = playlists.flatMap { playlist ->
    val playlistId = playlist.key.toLongOrNull() ?: return@flatMap emptyList()
    playlist.value.mapIndexed { index: Int, playlistObject: AmpachePlaylistObject ->
        DbPlaylistSongs(index, playlistObject.id, playlistId)
    }
}

fun AmpachePodcast.toDbPodcast() = DbPodcast(
    id = id.toLong(),
    name = name,
    description = description,
    language = language,
    feedUrl = feed_url,
    website = website,
    buildDate = TimeOperations.getDateFromAmpacheComplete(build_date).timeInMillis,
    syncDate = TimeOperations.getDateFromAmpacheComplete(sync_date).timeInMillis
)

fun AmpachePodcastEpisode.toDbPodcastEpisode() = DbPodcastEpisode(
    id = id.toLong(),
    title = title,
    podcastId = podcast.id,
    description = description,
    category = category,
    authorFull = author_full,
    website = website,
    publicationDate = TimeOperations.getDateFromAmpacheComplete(pubdate).timeInMillis,
    state = state,
    time = time,
    size = size,
    playCount = playcount,
    played = played
)

//endregion

//region Database to view

fun DbSongDisplay.toViewSongDisplay() = SongDisplay(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumId = albumId,
    time = time
)

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

fun DbSongInfo.toViewSongInfo() = SongInfo(
    id = song.id,
    track = song.track,
    title = song.title,
    artistName = artist.name,
    artistId = song.artistId,
    albumName = album.album.name,
    albumId = song.albumId,
    disk = song.disk,
    albumArtistName = album.artist.name,
    albumArtistId = album.artist.id,
    genreNames = genres.map { it.name },
    genreIds = genres.map { it.id },
    playlistNames = playlists.map { it.name },
    playlistIds = playlists.map { it.id },
    time = song.time,
    year = song.year,
    size = song.size,
    local = song.local
)

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

fun DbOrdering.toViewOrdering(): Ordering {
    return when (orderingType) {
        Ordering.ASCENDING -> Ordering.Ordered(priority, subject)
        Ordering.PRECISE_POSITION -> Ordering.Precise(orderingArgument, subject, priority)
        Ordering.RANDOM -> Ordering.Random(priority, subject, orderingArgument)
        else -> Ordering.Ordered(priority, subject)
    }
}

fun DbAlarm.toViewAlarm() = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    isRepeating = monday || tuesday || wednesday || thursday || friday || saturday || sunday,
    daysToTrigger = listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday),
    active = active
)

fun DbPodcastEpisode.toViewPodcastEpisodeDisplay() = PodcastEpisodeDisplay(
    id = id,
    title = title,
    time = time,
    artist = authorFull,
    album = authorFull,
    albumId = podcastId
)

//endregion

//region Views to Database

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

fun Filter<*>.toDbFilter(groupId: Long, parentId: Long? = null) = DbFilter(
    id = null,
    type = this.type.toDbFilterType(),
    argument = argument.toString(),
    displayText = displayText,
    filterGroup = groupId,
    parentFilter = parentId
)

fun Ordering.toDbOrdering() = DbOrdering(
    priority = priority,
    subject = subject,
    orderingType = ordering,
    orderingArgument = argument
)

fun Alarm.toDbAlarm() = DbAlarm(
    id = id,
    hour = hour,
    minute = minute,
    active = active,
    monday = daysToTrigger[0],
    tuesday = daysToTrigger[1],
    wednesday = daysToTrigger[2],
    thursday = daysToTrigger[3],
    friday = daysToTrigger[4],
    saturday = daysToTrigger[5],
    sunday = daysToTrigger[6]
)

//endregion

//region View to view

fun SongInfo.toViewDisplay() = SongDisplay(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumId = albumId,
    time = time
)

fun SongInfoActions.SongFieldType.toTagType() = when (this) {
    SongInfoActions.SongFieldType.Title -> TagType.Title
    SongInfoActions.SongFieldType.Artist -> TagType.Artist
    SongInfoActions.SongFieldType.Album -> TagType.Album
    SongInfoActions.SongFieldType.Disk -> TagType.Disk
    SongInfoActions.SongFieldType.AlbumArtist -> TagType.AlbumArtist
    SongInfoActions.SongFieldType.Genre -> TagType.Genre
    SongInfoActions.SongFieldType.Playlist -> TagType.Playlist
    SongInfoActions.SongFieldType.Year,
    SongInfoActions.SongFieldType.Duration,
    SongInfoActions.SongFieldType.PodcastEpisode,
    SongInfoActions.SongFieldType.Track -> throw UnsupportedOperationException()
}

//endregion

// region view to utilities

fun Filter<*>.toQueryFilter(level: Int = 0): QueryFilter {
    val arg = argument
    return QueryFilter(
        type = when (type) {
            Filter.FilterType.GENRE_IS -> QueryFilter.FilterType.GENRE_IS
            Filter.FilterType.SONG_IS -> QueryFilter.FilterType.SONG_IS
            Filter.FilterType.ARTIST_IS -> QueryFilter.FilterType.ARTIST_IS
            Filter.FilterType.ALBUM_ARTIST_IS -> QueryFilter.FilterType.ALBUM_ARTIST_IS
            Filter.FilterType.ALBUM_IS -> QueryFilter.FilterType.ALBUM_IS
            Filter.FilterType.DISK_IS -> QueryFilter.FilterType.DISK_IS
            Filter.FilterType.PLAYLIST_IS -> QueryFilter.FilterType.PLAYLIST_IS
            Filter.FilterType.DOWNLOADED_STATUS_IS -> QueryFilter.FilterType.DOWNLOADED_STATUS_IS
            Filter.FilterType.PODCAST_EPISODE_IS -> QueryFilter.FilterType.PODCAST_EPISODE_IS
        },
        argument = when (arg) {
            is Boolean -> if (arg) "NOT NULL" else "NULL"
            else -> arg.toString()
        },
        level = level,
        children = children.map { it.toQueryFilter(level + 1) }
    )
}

fun List<Filter<*>>.toQueryFilters() = map { it.toQueryFilter() }

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
//endregion

//region Utilities

private fun String.transformToSortFriendly() = padNumbers()

@Suppress("unused")
private fun String.convertRomanNumerals() = replace(
    Regex("\\b(?=[MDCLXVI])M*(C[MD]|D?C*)(X[CL]|L?X*)(I[XV]|V?I*)\\b")
) { match ->
    val values = match.value.map { romanChar ->
        when (romanChar) {
            'M' -> 1000
            'D' -> 500
            'C' -> 100
            'L' -> 50
            'X' -> 10
            'V' -> 5
            'I' -> 1
            else -> 0
        }
    }
    var added1 = 0
    var added10 = 0
    var added100 = 0
    var result = 0

    values.forEach { value ->
        when (value) {
            1 -> added1 += value

            5 -> {
                if (added1 == 1) {
                    result += 4
                    added1 = 0
                } else {
                    result += 5
                }
            }

            10 -> {
                if (added1 == 1) {
                    result += 9
                    added1 = 0
                } else {
                    added10 += 10
                }
            }

            50 -> {
                if (added10 == 10) {
                    result += 40
                    added10 = 0
                } else {
                    result += 50
                }
            }

            100 -> {
                if (added10 == 10) {
                    result += 90
                    added10 = 0
                } else {
                    added100 += 100
                }
            }

            500 -> {
                if (added100 == 100) {
                    result += 400
                    added100 = 0
                } else {
                    result += 500
                }
            }

            1000 -> {
                if (added100 == 100) {
                    result += 900
                    added100 = 0
                } else {
                    result += 1000
                }
            }
        }
    }

    result += added1 + added10 + added100


    result.takeIf { it > 0 }?.toString() ?: match.value
}

private fun String.padNumbers(): String = replace(
    Regex("[0-9]+")
) {
    it.value.padStart(5, '0')
}

//endregion