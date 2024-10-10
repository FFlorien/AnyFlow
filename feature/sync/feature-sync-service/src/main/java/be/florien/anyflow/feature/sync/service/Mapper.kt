package be.florien.anyflow.feature.sync.service

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
import be.florien.anyflow.tags.local.model.DbAlbum
import be.florien.anyflow.tags.local.model.DbArtist
import be.florien.anyflow.tags.local.model.DbGenre
import be.florien.anyflow.tags.local.model.DbPlaylist
import be.florien.anyflow.tags.local.model.DbPlaylistSongs
import be.florien.anyflow.tags.local.model.DbPodcast
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.tags.local.model.DbSong
import be.florien.anyflow.tags.local.model.DbSongGenre
import be.florien.anyflow.tags.local.model.DbSongId
import be.florien.anyflow.utils.TimeOperations


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
    played = played,
    waveForm = ""
)

//endregion

//region Database to view

//endregion

//region Views to Database

//endregion

//region View to view


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