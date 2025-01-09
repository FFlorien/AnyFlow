package be.florien.anyflow.tags.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.common.logging.iLog

class QueryComposer {
    //todo optimize queries

    //region tags
    fun getQueryForSongIds(
        filters: List<QueryFilter>,
        orderingList: List<QueryOrdering>
    ): SimpleSQLiteQuery {
        val filterList = filters.onlyTag()
        return ("SELECT DISTINCT song.id FROM song" +
                constructJoinStatement(filterList, orderingList) +
                constructWhereStatement(filterList, "") +
                constructOrderStatement(orderingList))
            .toSQLiteQuery()

    }

    fun getQueryForSong(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT song.id AS id," +
            "song.title AS title," +
            "artist.name AS artistName," +
            "album.name AS albumName," +
            "album.id AS albumId," +
            "song.time AS time " +
            "FROM song " +
            "JOIN artist ON song.artistId = artist.id " +
            "JOIN album ON song.albumId = album.id" +
            constructJoinStatement(filterList) +
            constructWhereStatement(filterList, " song.title LIKE ?", search) +
            " ORDER BY song.titleForSort COLLATE UNICODE")
        .toSQLiteQuery(search)

    fun getQueryForAlbum(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT album.id AS albumId, " +
            "album.name AS albumName, " +
            "album.artistId AS albumArtistId, " +
            "album.year,album.diskcount, " +
            "artist.name AS albumArtistName, " +
            "artist.summary " +
            "FROM album " +
            "JOIN artist ON album.artistid = artist.id " +
            "JOIN song ON song.albumId = album.id" +
            constructJoinStatement(filterList) +
            constructWhereStatement(filterList, " album.name LIKE ?", search) +
            " ORDER BY album.basename COLLATE UNICODE")
        .toSQLiteQuery(search)

    fun getQueryForAlbumArtist(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT artist.id, " +
            "artist.name, " +
            "artist.prefix, " +
            "artist.basename, " +
            "artist.summary " +
            "FROM artist " +
            "JOIN album ON album.artistId = artist.id " +
            "JOIN song ON song.albumId = album.id" +
            constructJoinStatement(filterList) +
            constructWhereStatement(filterList, " artist.name LIKE ?", search) +
            " ORDER BY artist.basename COLLATE UNICODE")
        .toSQLiteQuery(search)

    fun getQueryForArtist(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT artist.id, " +
            "artist.name, " +
            "artist.prefix, " +
            "artist.basename, " +
            "artist.summary " +
            "FROM artist " +
            "JOIN song ON song.artistId = artist.id" +
            constructJoinStatement(filterList) +
            constructWhereStatement(filterList, " artist.name LIKE ?", search) +
            " ORDER BY artist.basename COLLATE UNICODE")
        .toSQLiteQuery(search)

    fun getQueryForGenre(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT genre.id, " +
            "genre.name " +
            "FROM genre " +
            "JOIN songgenre ON genre.id = songgenre.genreid " +
            "JOIN song ON song.id = songgenre.songid " +
            constructJoinStatement(filterList) +
            constructWhereStatement(filterList, " genre.name LIKE ?", search) +
            " ORDER BY genre.name COLLATE UNICODE")
        .toSQLiteQuery(search)

    fun getQueryForSongCount(filter: QueryFilter): SimpleSQLiteQuery {
        val filterList = listOf(filter)
        return ("SELECT " +
                "COUNT(DISTINCT Song.id) " +
                "FROM Song " +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList, ""))
            .toSQLiteQuery()
    }

    fun getQueryForTagsCount(filterList: List<QueryFilter>) = ("SELECT " +
            "SUM(Song.time) AS duration, " +
            "COUNT(DISTINCT SongGenre.genreId) AS genres, " +
            "COUNT(DISTINCT Album.artistid) AS albumArtists, " +
            "COUNT(DISTINCT Song.albumId) AS albums, " +
            "COUNT(DISTINCT Song.artistId) AS artists, " +
            "COUNT(DISTINCT Song.id) AS songs, " +
            "COUNT(DISTINCT PlaylistSongs.playlistId) AS playlists, " +
            "COUNT(DISTINCT Song.local) AS downloaded " +
            "FROM Song " +
            "LEFT JOIN SongGenre ON Song.id = SongGenre.songId " +
            "JOIN Album ON Song.albumId = Album.id " +
            "LEFT JOIN PlaylistSongs ON Song.id = PlaylistSongs.songId" +
            constructJoinStatement(filterList) +
            constructWhereStatement(filterList, ""))
        .toSQLiteQuery()
    //endregion

    //region playlist
    fun getQueryForPlaylist(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT playlist.id, " +
            "playlist.name, " +
            "playlist.owner " +
            "FROM playlist " +
            "LEFT JOIN playlistsongs on playlistsongs.playlistid = playlist.id " +
            constructJoinStatement(filterList, shouldJoinSong = true) +
            constructWhereStatement(filterList, " playlist.name LIKE ?", search) +
            " ORDER BY playlist.name COLLATE UNICODE")
        .toSQLiteQuery(search)

    fun getQueryForPlaylistWithCount(
        filterList: List<QueryFilter>?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT playlist.id, " +
            "playlist.name, " +
            "playlist.owner, " +
            "(SELECT COUNT(songId) FROM playlistSongs WHERE playlistsongs.playlistId = playlist.id) as songCount " +
            "FROM playlist " +
            "LEFT JOIN playlistsongs on playlistsongs.playlistid = playlist.id " +
            constructJoinStatement(filterList, shouldJoinSong = true) +
            constructWhereStatement(filterList, " playlist.name LIKE ?", search) +
            " ORDER BY playlist.name COLLATE UNICODE")
        .toSQLiteQuery(search)

    fun getQueryForPlaylistWithPresence(filter: QueryFilter): SimpleSQLiteQuery {
        val filterList = listOf(filter)
        val selectForPresence = "SELECT " +
                "COUNT(*) " +
                "FROM playlistSongs " +
                "JOIN song ON PlaylistSongs.songId = song.id" +
                constructJoinStatement(filterList) +
                constructWhereStatement(filterList, "") +
                " AND playlistsongs.playlistId = playlist.id"
        return ("SELECT " +
                "DISTINCT playlist.id, " +
                "playlist.name, " +
                "(SELECT COUNT(*) FROM playlistSongs WHERE playlistsongs.playlistId = playlist.id) as songCount, " +
                "($selectForPresence) as presence " +
                "FROM playlist " +
                "ORDER BY playlist.name COLLATE UNICODE")
            .toSQLiteQuery()
    }
    //endregion

    //region podcasts
    fun getQueryForPodcasts(
        filters: List<QueryFilter>//todo: add ordering handling
    ): SimpleSQLiteQuery {
        val podcastFilters = filters.onlyPodcast()

        return ("SELECT DISTINCT podcast.id, podcast.name, podcast.syncDate FROM Podcast " +
                constructWhereStatement(podcastFilters, ""))
            .toSQLiteQuery()
    }

    fun getQueryForPodcastEpisodes(
        filters: List<QueryFilter>//todo: add ordering handling
    ): SimpleSQLiteQuery {
        val podcastFilters = filters.onlyPodcast()

        return ("SELECT DISTINCT podcastEpisode.id AS id, podcastEpisode.title AS title, podcast.name AS podcastName, podcastEpisode.podcastId AS podcastId, podcastEpisode.time AS time, podcastEpisode.description AS description " +
                "FROM podcastEpisode " +
                "JOIN podcast ON podcastEpisode.podcastId = podcast.id" +
                constructWhereStatement(podcastFilters, "") +
                " ORDER BY podcastEpisode.publicationDate DESC")
            .toSQLiteQuery()
    }

    fun getQueryForPodcastEpisodeIds(
        filters: List<QueryFilter>//todo: add ordering handling
    ): SimpleSQLiteQuery {
        val podcastFilters = filters.onlyPodcast()

        return ("SELECT DISTINCT podcastEpisode.id FROM podcastEpisode " +
                constructJoinStatement(podcastFilters) +
                constructWhereStatement(podcastFilters, "") +
                " ORDER BY podcastEpisode.publicationDate DESC")
            .toSQLiteQuery()
    }

    fun getQueryForPodcastCount(filterList: List<QueryFilter>) = ("SELECT " +
            "COUNT(DISTINCT Podcast.id) AS podcasts, " +
            "COUNT(DISTINCT PodcastEpisode.id) AS podcastEpisodes " +
            "FROM Podcast " +
            "JOIN PodcastEpisode on PodcastEpisode.podcastId = Podcast.id" +
            constructJoinStatement(filterList) +
            constructWhereStatement(filterList, ""))
        .toSQLiteQuery()
    //endregion

    //region download
    fun getQueryForDownload(filterList: List<QueryFilter>?) =
        ("INSERT INTO Download (mediaId, mediaType) SELECT Song.id, $SONG_MEDIA_TYPE FROM Song"
                + constructJoinStatement(filterList)
                + constructWhereStatement(filterList, "")
                + " AND Song.local IS NULL "
                + "AND Song.id NOT IN (SELECT Download.mediaId FROM Download)")
            .toSQLiteQuery()

    fun getQueryForDownloadCount(filterList: List<QueryFilter>?) =
        ("SELECT Song.local IS NOT NULL AS downloaded, COUNT(Song.id) AS count FROM Song"
                + constructJoinStatement(filterList)
                + constructWhereStatement(filterList, "")
                + " GROUP BY song.local IS NOT NULL")
            .toSQLiteQuery()

    fun getQueryForDownloadProgress(filterList: List<QueryFilter>?) = ("SELECT " +
            "COUNT(DISTINCT Song.id) as total, " +
            "COUNT(DISTINCT download.mediaId) as queued, " +
            "COUNT(DISTINCT song.local) AS downloaded " +
            "FROM song " +
            "LEFT JOIN download ON song.id = download.mediaId"
            + constructJoinStatement(filterList)
            + constructWhereStatement(filterList, ""))
        .toSQLiteQuery()
    //endregion

    //region private methods
    private fun String.toSQLiteQuery(search: String? = null): SimpleSQLiteQuery {
        iLog("Query:\n$this")
        search?.let {
            iLog("Search: $it")
        }
        return SimpleSQLiteQuery(this, search?.takeIf { it.isNotBlank() }?.let { arrayOf("%$it%") })
    }

    private fun constructJoinStatement(
        filterList: List<QueryFilter>?,
        orderingList: List<QueryOrdering> = emptyList(),
        shouldJoinSong: Boolean = false
    ): String {
        if (filterList.isNullOrEmpty() && orderingList.isEmpty()) {
            return " "
        }
        val orderingJoins = orderingList.mapNotNull { it.getJoin() }.toSet()
        val filterJoin = filterList?.flatMap { it.getJoins() }?.toSet() ?: emptySet()
        val playlistJoinSong =
            if (shouldJoinSong) setOf(
                QueryJoin(
                    QueryJoin.JoinType.PLAYLIST_SONG,
                    0
                )
            ) else emptySet()
        val joinsUnfiltered = orderingJoins + filterJoin + playlistJoinSong
        val joins = if (joinsUnfiltered.any { it.type == QueryJoin.JoinType.ALBUM_ARTIST }) {
            joinsUnfiltered.filterNot { it.type == QueryJoin.JoinType.ALBUM }
        } else {
            joinsUnfiltered
        }
        return joins.joinToString(separator = " ", prefix = " ") { it.getJoinClause() }
    }

    private fun constructWhereStatement(
        filterList: List<QueryFilter>?,
        searchCondition: String,
        search: String? = null
    ): String {
        return if (!filterList.isNullOrEmpty() || !search.isNullOrBlank()) {
            var where = " WHERE"
            if (!search.isNullOrBlank()) {
                where += searchCondition
            }
            if (!filterList.isNullOrEmpty()) {
                where += constructWhereSubStatement(filterList)
            }
            where
        } else {
            ""
        }
    }

    private fun constructWhereSubStatement( //todo check parenthesis
        filterList: List<QueryFilter>
    ): String {
        var whereStatement = ""
        filterList
            .forEachIndexed { index, filter ->
                if (filter.children.isNotEmpty()) {
                    whereStatement += " ("
                }

                whereStatement += filter.getCondition()
                if (filter.children.isNotEmpty()) {
                    whereStatement += " AND (" + constructWhereSubStatement(
                        filter.children
                    )
                    whereStatement += "))"
                }
                if (index < filterList.size - 1) {
                    whereStatement += " OR"
                }
            }
        return whereStatement
    }

    private fun constructOrderStatement(
        orderingList: List<QueryOrdering>
    ): String {
        val filteredOrderedList = orderingList
            .filter { it !is QueryOrdering.Precise }
            .sortedBy { it.priority }

        val isSorted = filteredOrderedList.isNotEmpty()
                && filteredOrderedList.none { it is QueryOrdering.Random }

        var orderStatement = if (isSorted) {
            " ORDER BY "
        } else {
            ""
        }

        if (isSorted) {
            orderStatement += filteredOrderedList.joinToString { it.getOrderingClause() }
        }

        return orderStatement
    }

    private fun List<QueryFilter>.onlyPodcast() = filter {
        it.type == QueryFilter.FilterType.PODCAST_IS
                || it.type == QueryFilter.FilterType.PODCAST_EPISODE_IS
    }

    private fun List<QueryFilter>.onlyTag() = filter {
        it.type == QueryFilter.FilterType.SONG_IS
                || it.type == QueryFilter.FilterType.ARTIST_IS
                || it.type == QueryFilter.FilterType.ALBUM_IS
                || it.type == QueryFilter.FilterType.ALBUM_ARTIST_IS
                || it.type == QueryFilter.FilterType.DISK_IS
                || it.type == QueryFilter.FilterType.GENRE_IS
                || it.type == QueryFilter.FilterType.PLAYLIST_IS
    }
    //endregion
}