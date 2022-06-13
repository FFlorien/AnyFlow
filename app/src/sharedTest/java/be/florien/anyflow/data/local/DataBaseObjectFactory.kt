package be.florien.anyflow.data.local

import be.florien.anyflow.data.local.model.*
import java.lang.IllegalArgumentException

class DataBaseObjectFactory {

    fun createSongList(size: Long, seed: Int = 0): List<DbSong> = createSimpleList(size, seed) { createSong(it) }

    fun createArtistList(size: Long, seed: Int = 0): List<DbArtist> = createSimpleList(size, seed) { createArtist(it) }

    fun createArtistList(songs: List<DbSong>): List<DbArtist> {
        val result = mutableListOf<DbArtist>()
        val artistDistinct = songs.distinctBy { it.albumArtistId }
        artistDistinct.forEach {
            result.add(createArtist(it.albumArtistId, it.albumArtistName, 4, 0.0, it.art))
        }
        return result
    }

    fun createAlbumList(size: Long, seed: Int = 0): List<DbAlbum> = createSimpleList(size, seed) { createAlbum(it) }

    fun createPlaylistList(size: Long, seed: Int = 0): List<DbPlaylist> = createSimpleList(size, seed) { createPlaylist(it) }

    fun createFilterList(size: Long, seed: Int = 0): List<DbFilter> = createSimpleList(size, seed) { createFilter(null, it + 1) }

    fun createOrderList(size: Long, startingSeed: Int = 0): List<DbOrder> = createSimpleList(size, startingSeed) {
        createOrder(it.toInt(), it, it.toInt(), it.toInt())
    }

    fun getRandom5LettersName(seed: Int): String {
        if (seed in 0..21) {
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            return alphabet.substring(seed, seed + 5)
        } else {
            throw IllegalArgumentException("The seed should be between 1 and 21 (excluded)")
        }
    }

    private fun <T> createSimpleList(size: Long, seed: Int, creator: (Long) -> T): List<T> {
        val result = mutableListOf<T>()
        for (id in seed until seed + size) {
            result.add(creator(id))
        }
        return result
    }

    fun createSong(
            id: Long,
            song: String = "Song #$id",
            title: String = song,
            name: String = song,
            artistName: String = "Artist #$id",
            artistId: Long = artistName.hashCode().toLong(),
            albumName: String = "Album #$id",
            albumId: Long = albumName.hashCode().toLong(),
            albumArtistName: String = artistName,
            albumArtistId: Long = artistId,
            filename: String = "$artistName - $song",
            track: Int = 0,
            time: Int = 180,
            year: Int = 1985,
            bitrate: Int = 256,
            rate: Int = 256,
            url: String = "http://myMusicServer/$filename",
            art: String = "$url?albumArt",
            preciserating: Int = 5,
            rating: Int = 5,
            averagerating: Double = 5.0,
            composer: String = "Composer #$id",
            comment: String = "Comment #$id",
            publisher: String = "Publisher #$id",
            language: String = "Language #$id",
            genre: String = "Genre #$id"
    ) = DbSong(
            id = id,
            song = song,
            title = title,
            name = name,
            artistName = artistName,
            artistId = artistId,
            albumName = albumName,
            albumId = albumId,
            albumArtistName = albumArtistName,
            albumArtistId = albumArtistId,
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
            genre = genre)

    fun createArtist(id: Long,
                     name: String = "Artist #$id",
                     preciserating: Int = 4,
                     rating: Double = 0.0,
                     art: String = "http://myMusicServer/#$id") = DbArtist(
            id = id,
            name = name,
            preciserating = preciserating,
            rating = rating,
            art = art)

    fun createAlbum(id: Long,
                    name: String = "AlbumName #$id",
                    artistName: String = "Artist #$id",
                    artistId: Long = 0L,
                    year: Int = 1985,
                    tracks: Int = 15,
                    disk: Int = 0,
                    art: String = "http://myMusicServer/#$id?albumArt",
                    preciserating: Int = 0,
                    rating: Double = 0.0) = DbAlbum(
            id = id,
            name = name,
            artistName = artistName,
            artistId = artistId,
            year = year,
            tracks = tracks,
            disk = disk,
            art = art,
            preciserating = preciserating,
            rating = rating)

    fun createPlaylist(id: Long,
                       name: String = "Playlist #$id",
                       owner: String = "Playlist owner #$id") = DbPlaylist(
            id = id,
            name = name,
            owner = owner)

    fun createFilter(id: Int?,
                     expectedId: Long,
                     clause: String = "Clause #$expectedId",
                     argument: String = "Argument #$expectedId",
                     displayText: String = "DisplayText #$expectedId",
                     displayImage: String = "DisplayImage #$expectedId",
                     filterGroup: Long = 0) = DbFilter(
            id = id,
            clause = clause,
            argument = argument,
            displayText = displayText,
            displayImage = displayImage,
            filterGroup = filterGroup)

    fun createOrder(priority: Int,
                    subject: Long,
                    orderingType: Int,
                    orderingArgument: Int) = DbOrder(
            priority = priority,
            subject = subject,
            orderingType = orderingType,
            orderingArgument = orderingArgument)
}

fun DbSong.toDbSongDisplay() = DbSongDisplay(
        id = id,
        title = title,
        artistName = artistName,
        albumName = albumName,
        albumArtistName = albumArtistName,
        time = time,
        art = art,
        url = url,
        genreName = genre
)

fun DbAlbum.toDbAlbumDisplay() = DbAlbumDisplay(
        id = id,
        name = name,
        artistName = artistName,
        art = art
)

fun DbArtist.toDbArtistDisplay() = DbArtistDisplay(
        id = id,
        name = name,
        art = art
)

