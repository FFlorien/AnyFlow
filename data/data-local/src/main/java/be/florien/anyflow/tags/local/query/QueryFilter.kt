package be.florien.anyflow.tags.local.query

data class QueryFilter(
    val type: FilterType,
    val argument: String,
    val level: Int,
    var child: QueryFilter? = null
) {
    fun getCondition() = " ${type.clause} $argument".replace(
        TABLE_COUNT_FORMAT,
        level.toString()
    )

    fun getJoins(): Set<QueryJoin> =
        setOfNotNull(getJoin(), *(child?.getJoins()?.toTypedArray() ?: emptyArray()))

    private fun getJoin() = type.joinType?.let { QueryJoin(it, level) }

    enum class FilterType(val clause: String, val joinType: QueryJoin.JoinType? = null) {
        SONG_IS("song.id ="),
        ARTIST_IS("song.artistId ="),
        ALBUM_ARTIST_IS(
            "album$TABLE_COUNT_FORMAT.artistId =",
            QueryJoin.JoinType.ALBUM_ARTIST_COUNT
        ),
        ALBUM_IS("song.albumId ="),
        GENRE_IS("songgenre$TABLE_COUNT_FORMAT.genreId =", QueryJoin.JoinType.SONG_GENRE_COUNT),
        PLAYLIST_IS(
            "playlistSongs$TABLE_COUNT_FORMAT.playlistId =",
            QueryJoin.JoinType.PLAYLIST_SONG_COUNT
        ),
        DOWNLOADED_STATUS_IS("song.local IS"),
        PODCAST_IS("podcastEpisode.podcastId ="),
        PODCAST_EPISODE_IS("podcastEpisode.id =", QueryJoin.JoinType.PODCAST),
        DISK_IS("song.disk ="),
        STATE_IS("podcastEpisode.state =")
    }

    companion object {
        const val TABLE_COUNT_FORMAT = "<count>"
    }
}

