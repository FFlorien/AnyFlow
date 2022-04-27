package be.florien.anyflow.feature.player.filter.selectType

import be.florien.anyflow.R
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterTypeViewModel @Inject constructor(filtersManager: FiltersManager) : BaseFilterViewModel(filtersManager) {
    private val genreName = R.string.filter_type_genre
    private val artistName = R.string.filter_type_album_artist
    private val albumName = R.string.filter_type_album
    private val songName = R.string.filter_type_song
    private val playlistName = R.string.filter_type_playlist
    private val downloadedName = R.string.filter_is_downloaded

    val filtersIds = listOf(
            GENRE_ID,
            ARTIST_ID,
            ALBUM_ID,
            SONG_ID,
            PLAYLIST_ID,
            DOWNLOAD_ID)

    val filtersNames = listOf(
            genreName,
            artistName,
            albumName,
            songName,
            playlistName,
            downloadedName)

    val filtersImages = listOf(
            R.drawable.ic_genre,
            R.drawable.ic_album_artist,
            R.drawable.ic_album,
            R.drawable.ic_song,
            R.drawable.ic_playlist,
            R.drawable.ic_download)

    companion object {
        const val GENRE_ID = "Genre"
        const val ARTIST_ID = "Artist"
        const val ALBUM_ID = "Album"
        const val SONG_ID = "Song"
        const val PLAYLIST_ID = "Playlist"
        const val DOWNLOAD_ID = "Download"
    }
}