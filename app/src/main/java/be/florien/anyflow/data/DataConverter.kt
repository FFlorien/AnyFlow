package be.florien.anyflow.data

import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.data.server.model.*
import be.florien.anyflow.data.view.*
import be.florien.anyflow.extension.ImageConfig
import be.florien.anyflow.feature.player.info.song.SongInfoActions
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
    size = size,
    local = null,
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
    basename = basename,
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
    basename = basename,
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

fun DbSongDisplay.toViewSongDisplay() = SongDisplay(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumId = albumId,
    time = time
)

fun DbSongInfo.toViewSongInfo() = SongInfo(
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
    PlaylistWithPresence(id, name, songCount, presence, ImageConfig(url = coverUrl, resource = null))

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
    genres = genres,
    albumArtists = albumArtists,
    albums = albums,
    artists = artists,
    songs = songs,
    playlists = playlists
)

/**
 * Views to Database
 */

fun Filter.FilterType.toDbFilterType(argument: Boolean?) = when (this) {
    Filter.FilterType.GENRE_IS -> DbFilter.GENRE_IS
    Filter.FilterType.SONG_IS -> DbFilter.SONG_ID
    Filter.FilterType.ARTIST_IS -> DbFilter.ARTIST_ID
    Filter.FilterType.ALBUM_ARTIST_IS -> DbFilter.ALBUM_ARTIST_ID
    Filter.FilterType.ALBUM_IS -> DbFilter.ALBUM_ID
    Filter.FilterType.PLAYLIST_IS -> DbFilter.PLAYLIST_ID
    Filter.FilterType.DOWNLOADED_STATUS_IS -> if (argument == true) DbFilter.DOWNLOADED else DbFilter.NOT_DOWNLOADED
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

/**
 * View to view
 */

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
    SongInfoActions.SongFieldType.Playlist -> Filter.FilterType.PLAYLIST_IS
    SongInfoActions.SongFieldType.Year,
    SongInfoActions.SongFieldType.Duration,
    SongInfoActions.SongFieldType.Track -> throw UnsupportedOperationException()
}