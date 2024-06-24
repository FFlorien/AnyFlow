package be.florien.anyflow.data

import be.florien.anyflow.data.local.model.DbAlarm
import be.florien.anyflow.data.local.model.DbAlbum
import be.florien.anyflow.data.local.model.DbArtist
import be.florien.anyflow.data.local.model.DbFilter
import be.florien.anyflow.data.local.model.DbFilterCount
import be.florien.anyflow.data.local.model.DbFilterGroup
import be.florien.anyflow.data.local.model.DbGenre
import be.florien.anyflow.data.local.model.DbOrdering
import be.florien.anyflow.data.local.model.DbPlaylist
import be.florien.anyflow.data.local.model.DbPlaylistSongs
import be.florien.anyflow.data.local.model.DbPlaylistWithCount
import be.florien.anyflow.data.local.model.DbPlaylistWithCountAndPresence
import be.florien.anyflow.data.local.model.DbPodcast
import be.florien.anyflow.data.local.model.DbPodcastEpisode
import be.florien.anyflow.data.local.model.DbPodcastWithEpisodes
import be.florien.anyflow.data.local.model.DbSong
import be.florien.anyflow.data.local.model.DbQueueItemDisplay
import be.florien.anyflow.data.local.model.DbSongDisplay
import be.florien.anyflow.data.local.model.DbSongGenre
import be.florien.anyflow.data.local.model.DbSongId
import be.florien.anyflow.data.local.model.DbSongInfo
import be.florien.anyflow.data.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.data.local.model.SONG_MEDIA_TYPE
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
import be.florien.anyflow.data.view.Alarm
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.FilterCount
import be.florien.anyflow.data.view.FilterGroup
import be.florien.anyflow.data.view.Ordering
import be.florien.anyflow.data.view.Playlist
import be.florien.anyflow.data.view.PlaylistWithPresence
import be.florien.anyflow.data.view.Podcast
import be.florien.anyflow.data.view.PodcastEpisode
import be.florien.anyflow.data.view.PodcastEpisodeDisplay
import be.florien.anyflow.data.view.SongDisplay
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.ImageConfig
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions
import java.util.Calendar
import kotlin.time.DurationUnit
import kotlin.time.toDuration


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
    buildDate = build_date,
    syncDate = sync_date
)

fun AmpachePodcastEpisode.toDbPodcastEpisode() = DbPodcastEpisode(
    id = id.toLong(),
    title = title,
    podcastId = podcast.id,
    description = description,
    category = category,
    authorFull = author_full,
    website = website,
    publicationDate = pubdate,
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

fun DbQueueItemDisplay.toViewQueueItemDisplay() =
    if (
        mediaType == SONG_MEDIA_TYPE &&
        songId != null &&
        songTitle != null &&
        songArtistName != null &&
        songAlbumName != null &&
        songAlbumId != null &&
        songTime != null ) {
        SongDisplay(
            id = songId,
            title = songTitle,
            artistName = songArtistName,
            albumName = songAlbumName,
            albumId = songAlbumId,
            time = songTime
        )
    } else if (
        mediaType == PODCAST_MEDIA_TYPE &&
        podcastEpisodeId != null &&
        podcastTitle != null &&
        podcastTime != null &&
        podcastId != null) {
        PodcastEpisodeDisplay(
            id = podcastEpisodeId,
            title = podcastTitle,
            author = podcastAuthor ?: "",
            time = podcastTime,
            album = podcastName ?: "",
            albumId = podcastId
        )
    } else {
        throw IllegalArgumentException("DbQueueItemDisplay is not a valid SongDisplay or PodcastEpisodeDisplay\n$this")
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

fun DbPlaylistWithCount.toViewPlaylist(coverUrl: String) =
    Playlist(id, name, songCount, ImageConfig(url = coverUrl, resource = null))

fun DbPlaylistWithCountAndPresence.toViewPlaylist(coverUrl: String) =
    PlaylistWithPresence(
        id,
        name,
        songCount,
        presence,
        ImageConfig(url = coverUrl, resource = null)
    )

fun DbFilter.toViewFilter(filterList: List<DbFilter>): Filter<*> = Filter(
    argument = if (clause == DbFilter.DOWNLOADED || clause == DbFilter.NOT_DOWNLOADED) argument.toBoolean() else argument.toLong(),
    type = when (clause) {
        DbFilter.GENRE_IS -> Filter.FilterType.GENRE_IS
        DbFilter.SONG_ID -> Filter.FilterType.SONG_IS
        DbFilter.ARTIST_ID -> Filter.FilterType.ARTIST_IS
        DbFilter.ALBUM_ARTIST_ID -> Filter.FilterType.ALBUM_ARTIST_IS
        DbFilter.ALBUM_ID -> Filter.FilterType.ALBUM_IS
        DbFilter.DISK -> Filter.FilterType.DISK_IS
        DbFilter.PLAYLIST_ID -> Filter.FilterType.PLAYLIST_IS
        DbFilter.DOWNLOADED,
        DbFilter.NOT_DOWNLOADED -> Filter.FilterType.DOWNLOADED_STATUS_IS
        DbFilter.PODCAST_EPISODE_ID -> Filter.FilterType.PODCAST_EPISODE_IS

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

fun DbFilterGroup.toViewFilterGroup(): FilterGroup = when {
    dateAdded == null -> FilterGroup.CurrentFilterGroup(id)
    name == null -> FilterGroup.HistoryFilterGroup(
        id,
        Calendar.getInstance().apply { timeInMillis = dateAdded })

    else -> FilterGroup.SavedFilterGroup(
        id,
        Calendar.getInstance().apply { timeInMillis = dateAdded },
        name
    )
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

fun DbFilterCount.toViewFilterCount() = FilterCount(
    duration = duration.toDuration(DurationUnit.SECONDS),
    genres = genres,
    albumArtists = albumArtists,
    albums = albums,
    artists = artists,
    songs = songs,
    playlists = playlists
)

fun DbPodcast.toViewPodcast() = Podcast(
    id = id,
    name = name,
    description = description,
    syncDate = syncDate
)

fun DbPodcastEpisode.toViewPodcastEpisode() = PodcastEpisode(
    id = id,
    title = title,
    description = description,
    authorFull = authorFull,
    publicationDate = publicationDate,
    state = state,
    time = time,
    playCount = playCount,
    played = played
)

fun DbPodcastWithEpisodes.toViewPodcast() = podcast.toViewPodcast().copy(episodes = episodes.map { it.toViewPodcastEpisode() })

//endregion

//region Views to Database

fun Filter.FilterType.toDbFilterType(argument: Boolean?) = when (this) {
    Filter.FilterType.GENRE_IS -> DbFilter.GENRE_IS
    Filter.FilterType.SONG_IS -> DbFilter.SONG_ID
    Filter.FilterType.ARTIST_IS -> DbFilter.ARTIST_ID
    Filter.FilterType.ALBUM_ARTIST_IS -> DbFilter.ALBUM_ARTIST_ID
    Filter.FilterType.ALBUM_IS -> DbFilter.ALBUM_ID
    Filter.FilterType.DISK_IS -> DbFilter.DISK
    Filter.FilterType.PLAYLIST_IS -> DbFilter.PLAYLIST_ID
    Filter.FilterType.DOWNLOADED_STATUS_IS -> if (argument == true) DbFilter.DOWNLOADED else DbFilter.NOT_DOWNLOADED
    Filter.FilterType.PODCAST_EPISODE_IS -> DbFilter.PODCAST_EPISODE_ID
}

fun Filter<*>.toDbFilter(groupId: Long, parentId: Long? = null) = DbFilter(
    id = null,
    clause = this.type.toDbFilterType(this.argument as? Boolean),
    joinClause = when (this.type) {
        Filter.FilterType.PLAYLIST_IS -> DbFilter.PLAYLIST_ID_JOIN
        else -> null
    },
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

fun SongInfoActions.SongFieldType.toViewFilterType(): Filter.FilterType = when (this) {
    SongInfoActions.SongFieldType.Genre -> Filter.FilterType.GENRE_IS
    SongInfoActions.SongFieldType.Title -> Filter.FilterType.SONG_IS
    SongInfoActions.SongFieldType.Artist -> Filter.FilterType.ARTIST_IS
    SongInfoActions.SongFieldType.AlbumArtist -> Filter.FilterType.ALBUM_ARTIST_IS
    SongInfoActions.SongFieldType.Album -> Filter.FilterType.ALBUM_IS
    SongInfoActions.SongFieldType.Disk -> Filter.FilterType.DISK_IS
    SongInfoActions.SongFieldType.Playlist -> Filter.FilterType.PLAYLIST_IS
    SongInfoActions.SongFieldType.PodcastEpisode -> Filter.FilterType.PODCAST_EPISODE_IS
    SongInfoActions.SongFieldType.Year,
    SongInfoActions.SongFieldType.Duration,
    SongInfoActions.SongFieldType.Track -> throw UnsupportedOperationException()
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