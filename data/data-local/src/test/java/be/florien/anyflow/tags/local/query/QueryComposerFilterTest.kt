package be.florien.anyflow.tags.local.query

import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QueryComposerFilterTest {

    private val queryComposer: QueryComposer = QueryComposerFilter()

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
                    "JOIN album AS album0 ON album0.id = song.albumid " +
                    "WHERE album0.artistId = 1 OR album0.artistId = 2"
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
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "JOIN songgenre AS songgenre0 ON songgenre0.songId = song.id " +
                    "WHERE songgenre0.genreId = 1 OR songgenre0.genreId = 2"
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
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "LEFT JOIN playlistsongs AS playlistsongs0 ON playlistsongs0.songId = song.id " +
                    "WHERE playlistSongs0.playlistId = 1 OR playlistSongs0.playlistId = 2"
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
                    "LEFT JOIN playlistsongs AS playlistsongs1 ON playlistsongs1.songId = song.id " +
                    "WHERE ( song.artistId = 1 AND ( playlistSongs1.playlistId = 2))"
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
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "JOIN songgenre AS songgenre0 ON songgenre0.songId = song.id " +
                    "LEFT JOIN playlistsongs AS playlistsongs1 ON playlistsongs1.songId = song.id " +
                    "WHERE ( songgenre0.genreId = 1 AND ( playlistSongs1.playlistId = 2))"
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
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "LEFT JOIN playlistsongs AS playlistsongs0 ON playlistsongs0.songId = song.id L" +
                    "EFT JOIN playlistsongs AS playlistsongs1 ON playlistsongs1.songId = song.id " +
                    "WHERE ( playlistSongs0.playlistId = 1 AND ( playlistSongs1.playlistId = 2))"
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
                    "JOIN songgenre AS songgenre0 ON songgenre0.songId = song.id " +
                    "LEFT JOIN playlistsongs AS playlistsongs1 ON playlistsongs1.songId = song.id " +
                    "WHERE ( songgenre0.genreId = 1 AND ( playlistSongs1.playlistId = 2)) OR song.artistId = 3"
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
                    "JOIN songgenre AS songgenre0 ON songgenre0.songId = song.id " +
                    "LEFT JOIN playlistsongs AS playlistsongs1 ON playlistsongs1.songId = song.id " +
                    "JOIN album AS album1 ON album1.id = song.albumid " +
                    "WHERE ( songgenre0.genreId = 1 AND ( playlistSongs1.playlistId = 2)) OR ( song.artistId = 3 AND ( album1.artistId = 4))"
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
            "SELECT DISTINCT song.id " +
                    "FROM song " +
                    "LEFT JOIN playlistsongs AS playlistsongs1 ON playlistsongs1.songId = song.id " +
                    "JOIN songgenre AS songgenre0 ON songgenre0.songId = song.id " +
                    "WHERE ( song.artistId = 1 AND ( playlistSongs1.playlistId = 2)) OR ( songgenre0.genreId = 3 AND ( playlistSongs1.playlistId = 2))"
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
            "SELECT DISTINCT song.id " +
                    "FROM song LEFT JOIN playlistsongs AS playlistsongs0 ON playlistsongs0.songId = song.id " +
                    "LEFT JOIN playlistsongs AS playlistsongs1 ON playlistsongs1.songId = song.id " +
                    "WHERE ( playlistSongs0.playlistId = 1 AND ( playlistSongs1.playlistId = 2)) OR ( playlistSongs0.playlistId = 3 AND ( playlistSongs1.playlistId = 2))"
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
            "SELECT DISTINCT song.id AS id,song.title AS title,artist.name AS artistName,album.name AS albumName,album.id AS albumId,song.time AS time,song.titleForSort AS titleForSort " +
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
        val expected = "SELECT DISTINCT album.id AS albumId, album.name AS albumName, album.artistId AS albumArtistId, album.year,album.diskcount, artist.name AS albumArtistName, artist.summary ,album.basename " +
                "FROM album JOIN artist ON album.artistid = artist.id " +
                "JOIN song ON song.albumId = album.id " +
                "ORDER BY album.basename COLLATE UNICODE"

        //When
        val queryForSongIds = queryComposer.getQueryForAlbum(null, null)

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `17 - Podcast - no filters`() {
        val expected = "SELECT DISTINCT podcast.id, podcast.name, podcast.syncDate " +
                "FROM Podcast "

        //When
        val queryForSongIds = queryComposer.getQueryForPodcasts(null)

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }

    @Test
    fun `18 - Podcast - 1 podcast episode`() {
        val expected = "SELECT DISTINCT podcast.id, podcast.name, podcast.syncDate " +
                "FROM Podcast " +
                "JOIN podcastEpisode ON podcastEpisode.podcastId = podcast.id " +
                "WHERE podcastEpisode.id = 1"
        val filters = Filter(
                FilterParam(PodcastFilterType.PODCAST_EPISODE_IS, 1, "One")
            )

        //When
        val queryForSongIds = queryComposer.getQueryForPodcasts(filters)

        //Then
        assertEquals(expected, queryForSongIds.sql)
    }
}