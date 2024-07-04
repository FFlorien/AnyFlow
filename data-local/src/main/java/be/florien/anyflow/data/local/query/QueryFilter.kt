package be.florien.anyflow.data.local.query

data class QueryFilter(
    val type: FilterType,
    val argument: String,
    var children: List<QueryFilter> = emptyList()
) {
    enum class FilterType(val clause: String) {
        SONG_IS("song.id ="),
        ARTIST_IS("song.artistId ="),
        ALBUM_ARTIST_IS("album$TABLE_COUNT_FORMAT.artistId ="),
        ALBUM_IS("song.albumId ="),
        GENRE_IS("songgenre$TABLE_COUNT_FORMAT.genreId ="),
        PLAYLIST_IS("playlistSongs$TABLE_COUNT_FORMAT.playlistId ="),
        DOWNLOADED_STATUS_IS("song.local IS"),
        PODCAST_EPISODE_IS("podcastEpisode.id ="),
        DISK_IS("song.disk =")
    }

    companion object {
        const val TABLE_COUNT_FORMAT = "<count>"
    }
}

