package be.florien.anyflow.tags.local.query

import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import org.junit.Assert.assertEquals
import org.junit.Test

class QueryComposerTest {

    private val queryComposer: QueryComposer = QueryComposerSchema(QueryComposerFilter())

    @Test
    fun `01 - Song ID - 2 songs selected`() {
        //With
        val expected = "SELECT DISTINCT song.id " +
                "FROM song " +
                "WHERE song.id = 1 OR song.id = 2"
        val filters = listOf(
            Filter(FilterParam(TagFilterType.SONG_IS, 1, "One")),
            Filter(FilterParam(TagFilterType.SONG_IS, 2, "Two")),
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `02 - Song ID - 2 disks selected`() {
        //With
        val expected = "SELECT DISTINCT song.id " +
                "FROM song " +
                "WHERE song.disk = 1 OR song.disk = 2"
        val filters = listOf(
            Filter(FilterParam(TagFilterType.DISK_IS, 1, "One")),
            Filter(FilterParam(TagFilterType.DISK_IS, 2, "Two")),
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `03 - Song ID - 2 artists selected`() {
        //With
        val expected =
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "WHERE song.artistId = 1 OR song.artistId = 2"
        val filters = listOf(
            Filter(FilterParam(TagFilterType.ARTIST_IS, 1, "One")),
            Filter(FilterParam(TagFilterType.ARTIST_IS, 2, "Two")),
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `04 - Song ID - 2 albums selected`() {
        //With
        val expected =
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "WHERE song.albumId = 1 OR song.albumId = 2"
        val filters = listOf(
            Filter(FilterParam(TagFilterType.ALBUM_IS, 1, "One")),
            Filter(FilterParam(TagFilterType.ALBUM_IS, 2, "Two")),
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `05 - Song ID - 2 album artist selected`() {
        //With
        val expected =
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "JOIN album ON song.albumId = album.id " +
                    "WHERE album.artistId = 1 OR album.artistId = 2"
        val filters = listOf(
            Filter(FilterParam(TagFilterType.ALBUM_ARTIST_IS, 1, "One")),
            Filter(FilterParam(TagFilterType.ALBUM_ARTIST_IS, 2, "Two")),
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `06 - Song ID - 2 genres selected`() {
        //With
        val expected =
            "SELECT DISTINCT songGenre.songId " +
                    "FROM songGenre " +
                    "WHERE songGenre.genreId = 1 OR songGenre.genreId = 2"
        val filters = listOf(
            Filter(FilterParam(TagFilterType.GENRE_IS, 1, "One")),
            Filter(FilterParam(TagFilterType.GENRE_IS, 2, "Two")),
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `07 - Song ID - 2 playlists selected`() {
        //With
        val expected =
            "SELECT DISTINCT playlistSongs.songId " +
                    "FROM playlistSongs " +
                    "WHERE playlistSongs.playlistId = 1 OR playlistSongs.playlistId = 2"
        val filters = listOf(
            Filter(FilterParam(TagFilterType.PLAYLIST_IS, 1, "One")),
            Filter(FilterParam(TagFilterType.PLAYLIST_IS, 2, "Two")),
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `08 - Song ID - 1 artist from 1 playlist selected`() {
        //With
        val expected =
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "JOIN playlistSongs ON song.id = playlistSongs.songId " +
                    "WHERE song.artistId = 1 AND playlistSongs.playlistId = 2"
        val filters = listOf(
            Filter(
                FilterParam(TagFilterType.ARTIST_IS, 1, "One"),
                FilterParam(TagFilterType.PLAYLIST_IS, 2, "Two")
            ),
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `09 - Song ID - 1 genre from 1 playlist selected`() {
        //With
        val expected =
            "SELECT DISTINCT songGenre.songId " +
                    "FROM songGenre " +
                    "JOIN playlistSongs ON songGenre.songId = playlistSongs.songId " +
                    "WHERE songGenre.genreId = 1 AND playlistSongs.playlistId = 2"
        val filters = listOf(
            Filter(
                FilterParam(TagFilterType.GENRE_IS, 1, "One"),
                FilterParam(TagFilterType.PLAYLIST_IS, 2, "Two")
            ),
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `10 - Song ID - 1 playlist from 1 playlist selected`() {
        //With
        val expected =
            "SELECT DISTINCT playlistSongs.songId " +
                    "FROM playlistSongs " +
                    "JOIN playlistSongs AS playlistSongs0 ON playlistSongs.songId = playlistSongs0.songId " +
                    "WHERE playlistSongs.playlistId = 1 AND playlistSongs0.playlistId = 2"
        val filters = listOf(
            Filter(
                FilterParam(TagFilterType.PLAYLIST_IS, 1, "One"),
                FilterParam(TagFilterType.PLAYLIST_IS, 2, "Two")
            ),
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `11 - Song ID - 1 genre from 1 playlist and 1 artist selected`() {
        //With
        val expected =
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "JOIN songGenre ON song.id = songGenre.songId " +
                    "JOIN playlistSongs ON song.id = playlistSongs.songId " +
                    "WHERE (songGenre.genreId = 1 AND playlistSongs.playlistId = 2) " +
                    "OR song.artistId = 3"
        val filters = listOf(
            Filter(
                FilterParam(TagFilterType.GENRE_IS, 1, "One"),
                FilterParam(TagFilterType.PLAYLIST_IS, 2, "Two")
            ),
            Filter(
                FilterParam(TagFilterType.ARTIST_IS, 3, "Three")
            )
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `12 - Song ID - 1 genre from 1 playlist and 1 artist from 1 album artist selected`() {
        //With
        val expected =
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "JOIN album ON song.albumId = album.id " +
                    "JOIN songGenre ON song.id = songGenre.songId " +
                    "JOIN playlistSongs ON song.id = playlistSongs.songId " +
                    "WHERE (songGenre.genreId = 1 AND playlistSongs.playlistId = 2) " +
                    "OR (song.artistId = 3 AND album.artistId = 4)"
        val filters = listOf(
            Filter(
                FilterParam(TagFilterType.GENRE_IS, 1, "One"),
                FilterParam(TagFilterType.PLAYLIST_IS, 2, "Two")
            ),
            Filter(
                FilterParam(TagFilterType.ARTIST_IS, 3, "Three"),
                FilterParam(TagFilterType.ALBUM_ARTIST_IS, 4, "Three")
            )
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    //Apparently this is optimized by SQLite, see the sql command "explain query plan"
    //todo BUT it takes a lot of characters, and doesn't allow "artistId IN (1,2,3) which is more efficient
    @Test
    fun `13 - Song ID - 1 genre and 1 artist from the same playlist selected`() {
        //With
        val expected =
            "SELECT DISTINCT playlistSongs.songId " +
                    "FROM song " +
                    "JOIN songGenre ON song.id = songGenre.songId " +
                    "JOIN playlistSongs ON song.id = playlistSongs.songId " +
                    "WHERE (song.artistId = 1 AND playlistSongs.playlistId = 2) " +
                    "OR (songGenre.genreId = 3 AND playlistSongs.playlistId = 2)"
        val filters = listOf(
            Filter(
                FilterParam(TagFilterType.ARTIST_IS, 1, "One"),
                FilterParam(TagFilterType.PLAYLIST_IS, 2, "Two")
            ),
            Filter(
                FilterParam(TagFilterType.GENRE_IS, 3, "Three"),
                FilterParam(TagFilterType.PLAYLIST_IS, 2, "Two")
            )
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `14 - Song ID - 1 playlist and 1 playlist from the same playlist selected`() {
        //With
        val expected =
            "SELECT DISTINCT playlistSongs.songId " +
                    "FROM playlistSongs " +
                    "JOIN playlistSongs AS playlistSongs0 ON playlistSongs.songId = playlistSongs0.songId " +
                    "WHERE (playlistSongs.playlistId = 1 AND playlistSongs0.playlistId = 2) " +
                    "OR (playlistSongs.playlistId = 3 AND playlistSongs0.playlistId = 2)"
        val filters = listOf(
            Filter(
                FilterParam(TagFilterType.PLAYLIST_IS, 1, "One"),
                FilterParam(TagFilterType.PLAYLIST_IS, 2, "Two")
            ),
            Filter(
                FilterParam(TagFilterType.PLAYLIST_IS, 3, "Three"),
                FilterParam(TagFilterType.PLAYLIST_IS, 2, "Two")
            )
        )

        //When
        val queryForSongIds = queryComposer.getQueryForSongIds(filters, emptyList())

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `15 - Song - no filters`() {
        //With
        val expected =
            "SELECT DISTINCT song.title AS title, artist.name AS artistName, album.name AS albumName, song.time AS time, song.id AS id, song.albumId AS albumId " +
                    "FROM song " +
                    "JOIN artist ON song.artistId = artist.id " +
                    "JOIN album ON song.albumId = album.id " +
                    "ORDER BY song.titleForSort COLLATE UNICODE"

        //When
        val queryForSongIds = queryComposer.getQueryForSong(null, null)

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `16 - Album - no filters`() {
        val expected = "SELECT DISTINCT album.name AS albumName, album.year AS year, album.diskcount AS diskcount, artist.name AS albumArtistName, artist.summary AS summary, album.id AS albumId, album.artistId AS albumArtistId " +
                "FROM album " +
                "JOIN artist ON album.artistId = artist.id " +
                "ORDER BY album.basename COLLATE UNICODE"

        //When
        val queryForSongIds = queryComposer.getQueryForAlbum(null, null)

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }
}