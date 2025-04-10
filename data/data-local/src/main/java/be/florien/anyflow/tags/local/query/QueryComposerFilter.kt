package be.florien.anyflow.tags.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import be.florien.anyflow.common.logging.iLog
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.tags.local.toQueryFilters

class QueryComposerFilter : QueryComposer {
    //todo optimize queries

    //region tags
    override fun getQueryForSongIds(
        filter: Filter,
        orderingList: List<QueryOrdering>
    ): SimpleSQLiteQuery = getQueryForSongIds(listOf(filter), orderingList)

    override fun getQueryForSongIds(
        filterList: List<Filter>,
        orderingList: List<QueryOrdering>
    ): SimpleSQLiteQuery {
        val filterTagList = filterList.onlyTag()
        return ("SELECT DISTINCT song.id FROM song" +
                constructJoinStatement(filterTagList, orderingList) +
                constructWhereStatement(filterTagList, "") +
                constructOrderStatement(orderingList))
            .toSQLiteQuery("getQueryForSongIds", Throwable().stackTrace)
    }

    override fun getQueryForSong(
        filter: Filter?,
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
            constructJoinStatement(filter) +
            constructWhereStatement(filter, " song.title LIKE ?", search) +
            " ORDER BY song.titleForSort COLLATE UNICODE")
        .toSQLiteQuery("getQueryForSong", Throwable().stackTrace, search)

    override fun getQueryForAlbum(
        filter: Filter?,
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
            constructJoinStatement(filter) +
            constructWhereStatement(filter, " album.name LIKE ?", search) +
            " ORDER BY album.basename COLLATE UNICODE")
        .toSQLiteQuery("getQueryForAlbum", Throwable().stackTrace, search)

    override fun getQueryForAlbumArtist(
        filter: Filter?,
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
            constructJoinStatement(filter) +
            constructWhereStatement(filter, " artist.name LIKE ?", search) +
            " ORDER BY artist.basename COLLATE UNICODE")
        .toSQLiteQuery("getQueryForAlbumArtist", Throwable().stackTrace, search)

    override fun getQueryForArtist(
        filter: Filter?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT artist.id, " +
            "artist.name, " +
            "artist.prefix, " +
            "artist.basename, " +
            "artist.summary " +
            "FROM artist " +
            "JOIN song ON song.artistId = artist.id" +
            constructJoinStatement(filter) +
            constructWhereStatement(filter, " artist.name LIKE ?", search) +
            " ORDER BY artist.basename COLLATE UNICODE")
        .toSQLiteQuery("getQueryForArtist", Throwable().stackTrace, search)

    override fun getQueryForGenre(
        filter: Filter?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT genre.id, " +
            "genre.name " +
            "FROM genre " +
            "JOIN songgenre ON genre.id = songgenre.genreid " +
            "JOIN song ON song.id = songgenre.songid " +
            constructJoinStatement(filter) +
            constructWhereStatement(filter, " genre.name LIKE ?", search) +
            " ORDER BY genre.name COLLATE UNICODE")
        .toSQLiteQuery("getQueryForGenre", Throwable().stackTrace, search)

    override fun getQueryForSongCount(filter: Filter): SimpleSQLiteQuery {
        return ("SELECT " +
                "COUNT(DISTINCT Song.id) " +
                "FROM Song " +
                constructJoinStatement(filter) +
                constructWhereStatement(filter, ""))
            .toSQLiteQuery("getQueryForSongCount", Throwable().stackTrace)
    }

    override fun getQueryForTagsCount(filter: Filter?) = ("SELECT " +
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
            constructJoinStatement(filter) +
            constructWhereStatement(filter, ""))
        .toSQLiteQuery("getQueryForTagsCount", Throwable().stackTrace)
    //endregion

    //region playlist
    override fun getQueryForPlaylist(
        filter: Filter?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT playlist.id, " +
            "playlist.name, " +
            "playlist.owner " +
            "FROM playlistsongs " +
            "LEFT JOIN Song on playlistSongs.songid = Song.id " +
            "LEFT JOIN playlist on playlistsongs.playlistid = playlist.id " +
            constructJoinStatement(filter, needJoinSong = false, hasJoinSong = false) +
            constructWhereStatement(filter, " playlist.name LIKE ?", search) +
            " ORDER BY playlist.name COLLATE UNICODE")
        .toSQLiteQuery("getQueryForPlaylist", Throwable().stackTrace, search)

    override fun getQueryForPlaylistWithCount(
        filter: Filter?,
        search: String?
    ) = ("SELECT " +
            "DISTINCT playlist.id, " +
            "playlist.name, " +
            "playlist.owner, " +
            "(SELECT COUNT(songId) FROM playlistSongs WHERE playlistsongs.playlistId = playlist.id) as songCount " +
            "FROM playlist " +
            "LEFT JOIN playlistsongs on playlistsongs.playlistid = playlist.id " +
            constructJoinStatement(filter, needJoinSong = true) +
            constructWhereStatement(filter, " playlist.name LIKE ?", search) +
            " ORDER BY playlist.name COLLATE UNICODE")
        .toSQLiteQuery("getQueryForPlaylistWithCount", Throwable().stackTrace, search)

    override fun getQueryForPlaylistWithPresence(filter: Filter): SimpleSQLiteQuery {
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
            .toSQLiteQuery("getQueryForPlaylistWithPresence", Throwable().stackTrace)
    }
    //endregion

    //region podcasts
    override fun getQueryForPodcasts(
        filter: Filter?//todo: add ordering handling
    ): SimpleSQLiteQuery {
        val podcastFilters = filter.onlyPodcast()

        return ("SELECT DISTINCT podcast.id, podcast.name, podcast.syncDate FROM Podcast " +
                constructWhereStatement(podcastFilters, ""))
            .toSQLiteQuery("getQueryForPodcasts", Throwable().stackTrace)
    }

    override fun getQueryForPodcastEpisodes(
        filter: Filter?//todo: add ordering handling
    ): SimpleSQLiteQuery {
        val podcastFilters = filter.onlyPodcast()

        return ("SELECT DISTINCT podcastEpisode.id AS id, podcastEpisode.title AS title, podcast.name AS podcastName, podcastEpisode.podcastId AS podcastId, podcastEpisode.time AS time, podcastEpisode.description AS description " +
                "FROM podcastEpisode " +
                "JOIN podcast ON podcastEpisode.podcastId = podcast.id" +
                constructWhereStatement(podcastFilters, "") +
                " ORDER BY podcastEpisode.publicationDate DESC")
            .toSQLiteQuery("getQueryForPodcastEpisodes", Throwable().stackTrace)
    }

    override fun getQueryForPodcastEpisodeIds(
        filter: Filter?//todo: add ordering handling
    ): SimpleSQLiteQuery {
        val podcastFilters = filter.onlyPodcast()

        return ("SELECT DISTINCT podcastEpisode.id FROM podcastEpisode " +
                constructJoinStatement(podcastFilters) +
                constructWhereStatement(podcastFilters, "") +
                " ORDER BY podcastEpisode.publicationDate ASC")
            .toSQLiteQuery("getQueryForPodcastEpisodeIds", Throwable().stackTrace)
    }

    override fun getQueryForPodcastEpisodeIds(
        filterList: List<Filter>?//todo: add ordering handling
    ): SimpleSQLiteQuery {
        val podcastFilters = filterList?.onlyPodcast()

        return ("SELECT DISTINCT podcastEpisode.id FROM podcastEpisode " +
                constructJoinStatement(podcastFilters) +
                constructWhereStatement(podcastFilters, "") +
                " ORDER BY podcastEpisode.publicationDate ASC")
            .toSQLiteQuery("getQueryForPodcastEpisodeIds", Throwable().stackTrace)
    }

    override fun getQueryForPodcastCount(filter: Filter?) = ("SELECT " +
            "COUNT(DISTINCT Podcast.id) AS podcasts, " +
            "COUNT(DISTINCT PodcastEpisode.id) AS podcastEpisodes " +
            "FROM Podcast " +
            "JOIN PodcastEpisode on PodcastEpisode.podcastId = Podcast.id" +
            constructJoinStatement(filter) +
            constructWhereStatement(filter, ""))
        .toSQLiteQuery("getQueryForPodcastCount", Throwable().stackTrace)
    //endregion

    //region download
    override fun getQueryForDownload(filter: Filter?) =
        ("INSERT INTO Download (mediaId, mediaType) SELECT Song.id, $SONG_MEDIA_TYPE FROM Song"
                + constructJoinStatement(filter)
                + constructWhereStatement(filter, "")
                + " AND Song.local IS NULL "
                + "AND Song.id NOT IN (SELECT Download.mediaId FROM Download)")
            .toSQLiteQuery("getQueryForDownload", Throwable().stackTrace)

    override fun getQueryForDownloadCount(filter: Filter?) =
        ("SELECT Song.local IS NOT NULL AS downloaded, COUNT(Song.id) AS count FROM Song"
                + constructJoinStatement(filter)
                + constructWhereStatement(filter, "")
                + " GROUP BY song.local IS NOT NULL")
            .toSQLiteQuery("getQueryForDownloadCount", Throwable().stackTrace)

    override fun getQueryForDownloadProgress(filter: Filter?) = ("SELECT " +
            "COUNT(DISTINCT Song.id) as total, " +
            "COUNT(DISTINCT download.mediaId) as queued, " +
            "COUNT(DISTINCT song.local) AS downloaded " +
            "FROM song " +
            "LEFT JOIN download ON song.id = download.mediaId"
            + constructJoinStatement(filter)
            + constructWhereStatement(filter, ""))
        .toSQLiteQuery("getQueryForDownloadProgress", Throwable().stackTrace)
    //endregion

    //region private methods

    private fun constructJoinStatement(
        filter: Filter?,
        orderingList: List<QueryOrdering> = emptyList(),
        needJoinSong: Boolean = false,
        hasJoinSong: Boolean = true,
    ): String =
        constructJoinStatement(listOfNotNull(filter), orderingList, needJoinSong, hasJoinSong)

    private fun constructJoinStatement(
        filterList: List<Filter>?,
        orderingList: List<QueryOrdering> = emptyList(),
        needJoinSong: Boolean = false,
        hasJoinSong: Boolean = true
    ): String {
        if (filterList.isNullOrEmpty() && orderingList.isEmpty()) {
            return ""
        }
        val onlyTagFilters = filterList?.onlyTag()
        val hasSongJoin = orderingList.isNotEmpty() || onlyTagFilters?.toQueryFilters()
            ?.any { it.getJoins().any { it.type.clauseWithoutSong == null } } == true || hasJoinSong
        val orderingJoins = orderingList.mapNotNull { it.getJoin() }.toSet()
        val filterJoin = filterList?.toQueryFilters()?.flatMap { it.getJoins() }?.toSet() ?: emptySet()
        val playlistJoinSong = if (needJoinSong) {
            setOf(QueryJoin(QueryJoin.JoinType.PLAYLIST_SONG, 0))
        } else {
            emptySet()
        }
        val joinsUnfiltered = orderingJoins + filterJoin + playlistJoinSong
        val joins = if (joinsUnfiltered.any { it.type == QueryJoin.JoinType.ALBUM_ARTIST }) {
            joinsUnfiltered.filterNot { it.type == QueryJoin.JoinType.ALBUM }
        } else {
            joinsUnfiltered
        }
        return if (joins.isEmpty()) "" else joins.joinToString(separator = " ", prefix = " ") {
            it.getJoinClause(hasSongJoin)
        }
    }

    private fun constructWhereStatement(
        filterList: Filter?,
        searchCondition: String,
        search: String? = null
    ): String = constructWhereStatement(listOfNotNull(filterList), searchCondition, search)

    private fun constructWhereStatement(
        filterList: List<Filter>?,
        searchCondition: String,
        search: String? = null
    ): String {
        val queryFilters = filterList?.toQueryFilters()
        return if (!queryFilters.isNullOrEmpty() || !search.isNullOrBlank()) {
            var where = " WHERE"
            if (!search.isNullOrBlank()) {
                where += searchCondition
            }
            if (!queryFilters.isNullOrEmpty()) {
                where += constructWhereSubStatement(queryFilters)
            }
            where
        } else {
            ""
        }
    }

    private fun constructWhereSubStatement( //todo check parenthesis && merge filter together when possible
        filterList: List<QueryFilter>
    ): String {
        var whereStatement = ""
        filterList
            .forEachIndexed { index, filter ->
                val child = filter.child
                if (child != null) {
                    whereStatement += " ("
                }

                whereStatement += filter.getCondition()
                if (child != null) {
                    whereStatement += " AND (" + constructWhereSubStatement(
                        listOf(child)
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

    private fun List<Filter>.onlyPodcast() = filter {
        it.isPodcast()
    }

    private fun Filter?.isPodcast() = this != null && all { it.isPodcast() }

    private fun Filter?.onlyPodcast() =
        if (this != null && isPodcast()) listOf(this) else emptyList()

    private fun FilterParam<*>?.isPodcast() = this != null && type is PodcastFilterType

    private fun List<Filter>.onlyTag() = filter {
        it.isTag()
    }

    private fun Filter?.onlyTag() =
        if (this != null && isTag()) listOf(this) else emptyList()

    private fun Filter?.isTag() = this != null && all { it.type is TagFilterType }
    //endregion
}