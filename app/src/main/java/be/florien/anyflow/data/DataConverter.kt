package be.florien.anyflow.data

import be.florien.anyflow.data.local.model.*
import be.florien.anyflow.data.server.model.AmpacheAlbum
import be.florien.anyflow.data.server.model.AmpacheArtist
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
        comment = comment,
        publisher = publisher,
        language = language,
        genre = genre.joinToString(","))

fun AmpacheArtist.toDbArtist() = DbArtist(
        id = id,
        name = name,
        preciserating = preciserating,
        rating = rating,
        art = art)

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
        rating = rating)

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
        genre = genre)

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
    Filter.TITLE_IS -> Filter.TitleIs(argument)
    Filter.TITLE_CONTAIN -> Filter.TitleContain(argument)
    Filter.GENRE_IS -> Filter.GenreIs(argument)
    Filter.SONG_ID -> Filter.SongIs(argument.toLong(), displayText, displayImage)
    Filter.ARTIST_ID -> Filter.ArtistIs(argument.toLong(), displayText, displayImage)
    Filter.ALBUM_ARTIST_ID -> Filter.AlbumArtistIs(argument.toLong(), displayText, displayImage)
    Filter.ALBUM_ID -> Filter.AlbumIs(argument.toLong(), displayText, displayImage)
    else -> Filter.TitleIs("")
}

fun DbFilterGroup.toViewFilterGroup() = FilterGroup(
        id = id,
        name = name
)

fun DbOrder.toViewOrder() = Order(
        priority = priority,
        subject = subject,
        ordering = orderingType,
        argument = orderingArgument)

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
        genre = genre)

fun Filter<*>.toDbFilter(groupId: Long) = DbFilter(
        id = 0,
        clause = clause,
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