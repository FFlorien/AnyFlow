package be.florien.anyflow.data

import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.data.server.model.AmpacheAlbum
import be.florien.anyflow.data.server.model.AmpacheArtist
import be.florien.anyflow.data.server.model.AmpachePlayList
import be.florien.anyflow.data.server.model.AmpacheSong
import be.florien.anyflow.data.view.*


/**
 * Server to Database
 */
fun AmpacheSong.toDbSong() = DbSong(
    id = id,
    song = song,
    title = title,
    name = name,
    artistName = artist.name,
    artistId = artist.id,
    albumName = album.name,
    albumId = album.id,
    albumArtistName = albumartist.name,
    albumArtistId = albumartist.id,
    filename = filename,
    track = track,
    time = time,
    year = year,
    bitrate = bitrate,
    rate = rate,
    url = url,
    art = art,
    preciserating = preciserating,
    rating = rating,
    averagerating = averagerating,
    composer = composer,
    genre = genre.joinToString(",") { it.name },
    null
)

fun AmpacheArtist.toDbArtist() = DbArtist(
    id = id,
    name = name,
    preciserating = preciserating,
    rating = rating,
    art = art
)

fun AmpacheAlbum.toDbAlbum() = DbAlbum(
    id = id,
    name = name,
    artistName = artist.name,
    artistId = artist.id,
    year = year,
    tracks = tracks,
    disk = disk,
    art = art,
    preciserating = preciserating,
    rating = rating
)

fun AmpachePlayList.toDbPlaylist() = DbPlaylist(
    id = id,
    name = name,
    owner = owner
)

/**
 * Database to view
 */

fun DbSong.toViewSong() = Song(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumArtistName = albumArtistName,
    time = time,
    url = url,
    art = art,
    genre = genre
)

fun DbSong.toViewSongInfo() = SongInfo(
    id = id,
    track = track,
    title = title,
    artistName = artistName,
    artistId = artistId,
    albumName = albumName,
    albumId = albumId,
    albumArtistName = albumArtistName,
    albumArtistId = albumArtistId,
    time = time,
    url = url,
    art = art,
    year = year,
    genre = genre,
    fileName = filename,
    local = local
)

fun DbSongDisplay.toViewSong() = Song(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumArtistName = albumArtistName,
    time = time,
    art = art,
    url = url,
    genre = genre
)

fun DbFilter.toViewFilter(): Filter<*> = when (clause) {
    DbFilter.TITLE_IS -> Filter.TitleIs(argument)
    DbFilter.TITLE_CONTAIN -> Filter.TitleContain(argument)
    DbFilter.GENRE_IS -> Filter.GenreIs(argument)
    DbFilter.SONG_ID -> Filter.SongIs(argument.toLong(), displayText, displayImage)
    DbFilter.ARTIST_ID -> Filter.ArtistIs(argument.toLong(), displayText, displayImage)
    DbFilter.ALBUM_ARTIST_ID -> Filter.AlbumArtistIs(argument.toLong(), displayText, displayImage)
    DbFilter.ALBUM_ID -> Filter.AlbumIs(argument.toLong(), displayText, displayImage)
    DbFilter.PLAYLIST_ID -> Filter.PlaylistIs(argument.toLong(), displayText)
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

fun Song.toDbSongDisplay() = DbSongDisplay(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumArtistName = albumArtistName,
    time = time,
    art = art,
    url = url,
    genre = genre
)

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

/**
 * View to View
 */

fun SongInfo.toSong() = Song(id = id, title = title, artistName = artistName, albumName = albumName, albumArtistName = albumArtistName, time = time, art = art, url = url, genre = genre)