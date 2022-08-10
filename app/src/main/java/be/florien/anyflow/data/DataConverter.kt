package be.florien.anyflow.data

import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.data.server.model.*
import be.florien.anyflow.data.view.*


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
    local = null
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

fun DbFilter.toViewFilter(): Filter<*> = when (clause) {
    DbFilter.TITLE_IS -> Filter.TitleIs(argument)
    DbFilter.TITLE_CONTAIN -> Filter.TitleContain(argument)
    DbFilter.GENRE_IS -> Filter.GenreIs(argument.toLong(), displayText)
    DbFilter.SONG_ID -> Filter.SongIs(argument.toLong(), displayText, displayImage)
    DbFilter.ARTIST_ID -> Filter.ArtistIs(argument.toLong(), displayText, displayImage)
    DbFilter.ALBUM_ARTIST_ID -> Filter.AlbumArtistIs(argument.toLong(), displayText, displayImage)
    DbFilter.ALBUM_ID -> Filter.AlbumIs(argument.toLong(), displayText, displayImage)
    DbFilter.PLAYLIST_ID -> Filter.PlaylistIs(argument.toLong(), displayText, displayImage)
    DbFilter.DOWNLOADED -> Filter.DownloadedStatusIs(true)
    DbFilter.NOT_DOWNLOADED -> Filter.DownloadedStatusIs(false)
    else -> Filter.TitleIs("")
}

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

/**
 * View to database
 */

fun SongInfo.toDbSongToPlay() = DbSongToPlay(
    id = id,
    local = local
)

fun Filter<*>.toDbFilter(groupId: Long) = DbFilter(
    id = null,
    clause = when (this) {
        is Filter.TitleIs -> DbFilter.TITLE_IS
        is Filter.TitleContain -> DbFilter.TITLE_CONTAIN
        is Filter.GenreIs -> DbFilter.GENRE_IS
        is Filter.Search -> DbFilter.SEARCH
        is Filter.SongIs -> DbFilter.SONG_ID
        is Filter.ArtistIs -> DbFilter.ARTIST_ID
        is Filter.AlbumArtistIs -> DbFilter.ALBUM_ARTIST_ID
        is Filter.AlbumIs -> DbFilter.ALBUM_ID
        is Filter.PlaylistIs -> DbFilter.PLAYLIST_ID
        is Filter.DownloadedStatusIs -> if (this.argument) DbFilter.DOWNLOADED else DbFilter.NOT_DOWNLOADED
    },
    joinClause = when (this) {
        is Filter.PlaylistIs -> DbFilter.PLAYLIST_ID_JOIN
        else -> null
    },
    argument = argument.toString(),
    displayText = displayText,
    displayImage = displayImage,
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