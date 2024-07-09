package be.florien.anyflow.tags.local.query

data class QueryFilter(
    val type: FilterType,
    val argument: String,
    val level: Int,
    var children: List<QueryFilter> = emptyList()
) {
    fun getCondition() = " ${type.clause} $argument".replace(
        TABLE_COUNT_FORMAT,
        level.toString()
    )

    fun getJoins(): Set<QueryJoin> =
        children.flatMap { it.getJoins() }.plus(getJoin()).filterNotNull().toSet()

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
        PODCAST_EPISODE_IS("podcastEpisode.id ="),
        DISK_IS("song.disk =")
    }

    companion object {
        const val TABLE_COUNT_FORMAT = "<count>"
    }
}

