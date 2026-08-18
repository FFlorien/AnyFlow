package be.florien.anyflow.tags.local.query

data class QueryJoin(val type: JoinType, val level: Int) {

    fun getJoinClause(hasAtomJoin: Boolean = true) = if (hasAtomJoin || type.clauseWithoutAtom == null) {
        type.clauseWithAtom.replace(TABLE_COUNT_FORMAT, level.toString())
    } else {
        type.clauseWithoutAtom.replace(TABLE_COUNT_FORMAT, level.toString())
    }

    enum class JoinType(val clauseWithAtom: String, val clauseWithoutAtom: String? = null) {
        ARTIST("JOIN artist ON song.artistId = artist.id"),
        ALBUM("JOIN album ON song.albumId = album.id"),
        ALBUM_ARTIST("JOIN album ON song.albumId = album.id JOIN artist AS albumArtist ON album.artistId = albumArtist.id"),
        GENRE("JOIN songgenre ON songgenre.songId = song.id JOIN genre ON songgenre.genreId = genre.id"),
        PLAYLIST_SONG("LEFT JOIN song ON song.id = playlistsongs.songid"),
        ALBUM_ARTIST_COUNT("JOIN album AS album$TABLE_COUNT_FORMAT ON album$TABLE_COUNT_FORMAT.id = song.albumid"),
        SONG_GENRE_COUNT(
            "JOIN songgenre AS songgenre$TABLE_COUNT_FORMAT ON songgenre$TABLE_COUNT_FORMAT.songId = song.id",
            "JOIN songgenre AS songgenre$TABLE_COUNT_FORMAT ON songgenre$TABLE_COUNT_FORMAT.songId = songGenre.genreId"
        ),
        PLAYLIST_SONG_COUNT(
            "LEFT JOIN playlistsongs AS playlistsongs$TABLE_COUNT_FORMAT ON playlistsongs$TABLE_COUNT_FORMAT.songId = song.id",
            "LEFT JOIN playlistsongs AS playlistsongs$TABLE_COUNT_FORMAT ON playlistsongs$TABLE_COUNT_FORMAT.songId = playlistsongs.songId"
        ),
        PODCAST_EPISODE("", "JOIN podcastEpisode ON podcastEpisode.id = podcastChapter.podcastEpisodeId")
    }

    companion object {
        const val TABLE_COUNT_FORMAT = "<count>"
    }
}
