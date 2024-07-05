package be.florien.anyflow.data.local.query

data class QueryJoin(val type: JoinType, val level: Int) {

    fun getJoinClause() = type.clause.replace(TABLE_COUNT_FORMAT, level.toString())

    enum class JoinType(val clause: String) {
        ARTIST("JOIN artist ON song.artistId = artist.id"),
        ALBUM("JOIN album ON song.albumId = album.id"),
        ALBUM_ARTIST("JOIN album ON song.albumId = album.id JOIN artist AS albumArtist ON album.artistId = albumArtist.id"),
        GENRE("JOIN songgenre ON songgenre.songId = song.id JOIN genre ON songgenre.genreId = genre.id"),
        PLAYLIST_SONG("LEFT JOIN song ON song.id = playlistsongs.songid"),
        ALBUM_ARTIST_COUNT("JOIN album AS album$TABLE_COUNT_FORMAT ON album$TABLE_COUNT_FORMAT.id = song.albumid"),
        SONG_GENRE_COUNT("JOIN songgenre AS songgenre$TABLE_COUNT_FORMAT ON songgenre$TABLE_COUNT_FORMAT.songId = song.id"),
        PLAYLIST_SONG_COUNT("LEFT JOIN playlistsongs AS playlistsongs$TABLE_COUNT_FORMAT ON playlistsongs$TABLE_COUNT_FORMAT.songId = song.id"),
    }

    companion object {
        const val TABLE_COUNT_FORMAT = "<count>"
    }
}
