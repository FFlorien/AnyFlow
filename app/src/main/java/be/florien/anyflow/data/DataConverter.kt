package be.florien.anyflow.data

import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.data.server.model.*
import be.florien.anyflow.data.view.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/**
 * Server to Database
 */
fun AmpacheSong.toDbSong() = DbSong(
    id = id,
    title = title,
    artistId = artist.id,
    albumId = album.id,
    track = track,
    time = time,
    year = year,
    composer = composer ?: "",
    disk = disk,
    local = null,
    bars = ""
)

fun AmpacheSong.toDbSongGenres(): List<DbSongGenre> {
    val songId = id
    return genre.map { DbSongGenre(songId = songId, it.id) }
}

fun AmpacheSongId.toDbSongId() = DbSongId(id)

fun AmpacheArtist.toDbArtist() = DbArtist(
    id = id,
    name = name,
    summary = summary
)

fun AmpacheNameId.toDbGenre() = DbGenre(
    id = id,
    name = name
)

fun AmpacheAlbum.toDbAlbum() = DbAlbum(
    id = id,
    name = name,
    artistId = artist.id,
    year = year,
    diskcount = diskcount
)

fun AmpachePlayListWithSongs.toDbPlaylist() = DbPlaylist(
    id = id,
    name = name,
    owner = owner
)

fun AmpachePlaylistSong.toDbPlaylistSong(playlistId: Long) = DbPlaylistSongs(
    order = playlisttrack,
    songId = id,
    playlistId = playlistId
)

/**
 * Database to view
 */

fun DbSongDisplay.toViewSongInfo() = SongInfo(
    id = song.id,
    track = song.track,
    title = song.title,
    artistName = artist.name,
    artistId = song.artistId,
    albumName = album.album.name,
    albumId = song.albumId,
    albumArtistName = album.artist.name,
    albumArtistId = album.artist.id,
    genreNames = genres.map { it.name },
    genreIds = genres.map { it.id },
    time = song.time,
    year = song.year,
    local = song.local
)

fun DbPlaylistWithCount.toViewPlaylist(coverUrl: String) = Playlist(id, name, songCount, coverUrl)

fun DbFilter.toViewFilter(filterList: List<DbFilter>): Filter<*> = Filter(
    argument = if (clause == DbFilter.DOWNLOADED || clause == DbFilter.NOT_DOWNLOADED) argument.toBoolean() else argument.toLong(),
    type = when (clause) {
        DbFilter.GENRE_IS -> Filter.FilterType.GENRE_IS
        DbFilter.SONG_ID -> Filter.FilterType.SONG_IS
        DbFilter.ARTIST_ID -> Filter.FilterType.ARTIST_IS
        DbFilter.ALBUM_ARTIST_ID -> Filter.FilterType.ALBUM_ARTIST_IS
        DbFilter.ALBUM_ID -> Filter.FilterType.ALBUM_IS
        DbFilter.PLAYLIST_ID -> Filter.FilterType.PLAYLIST_IS
        DbFilter.DOWNLOADED,
        DbFilter.NOT_DOWNLOADED -> Filter.FilterType.DOWNLOADED_STATUS_IS
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

fun DbFilterGroup.toViewFilterGroup() = FilterGroup(
    id = id,
    name = name
)

fun DbOrder.toViewOrder(): Order {
    return when (orderingType) {
        Order.ASCENDING -> Order.Ordered(priority, subject)
        Order.PRECISE_POSITION -> Order.Precise(orderingArgument, subject, priority)
        Order.RANDOM -> Order.Random(priority, subject, orderingArgument)
        else -> Order.Ordered(priority, subject)
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
    genres = genres.takeIf { it > 1 },
    albumArtists = albumArtists.takeIf { it > 1 },
    albums = albums.takeIf { it > 1 },
    artists = artists.takeIf { it > 1 },
    songs = songs.takeIf { it > 1 },
    playlists = playlists.takeIf { it > 1 }
)

/**
 * View to database
 */

fun SongInfo.toDbSongToPlay() = DbSongToPlay(
    id = id,
    local = local
)

fun Filter<*>.toDbFilter(groupId: Long) = DbFilter(
    id = null,
    clause = when (this.type) {
        Filter.FilterType.GENRE_IS -> DbFilter.GENRE_IS
        Filter.FilterType.SONG_IS -> DbFilter.SONG_ID
        Filter.FilterType.ARTIST_IS -> DbFilter.ARTIST_ID
        Filter.FilterType.ALBUM_ARTIST_IS -> DbFilter.ALBUM_ARTIST_ID
        Filter.FilterType.ALBUM_IS -> DbFilter.ALBUM_ID
        Filter.FilterType.PLAYLIST_IS -> DbFilter.PLAYLIST_ID
        Filter.FilterType.DOWNLOADED_STATUS_IS -> if (this.argument == true) DbFilter.DOWNLOADED else DbFilter.NOT_DOWNLOADED
    },
    joinClause = when (this.type) {
        Filter.FilterType.PLAYLIST_IS -> DbFilter.PLAYLIST_ID_JOIN
        else -> null
    },
    argument = argument.toString(),
    displayText = displayText,
    filterGroup = groupId
)

fun FilterGroup.toDbFilterGroup() = DbFilterGroup(
    id = id,
    name = name
)

fun Order.toDbOrder() = DbOrder(
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