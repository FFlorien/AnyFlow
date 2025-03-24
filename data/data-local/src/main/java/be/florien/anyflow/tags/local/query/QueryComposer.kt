package be.florien.anyflow.tags.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import be.florien.anyflow.management.filters.domain.model.Filter

interface QueryComposer {
    //region tags
    fun getQueryForSongIds(
        filter: Filter,
        orderingList: List<QueryOrdering>
    ): SimpleSQLiteQuery

    fun getQueryForSongIds(
        filterList: List<Filter>,
        orderingList: List<QueryOrdering>
    ): SimpleSQLiteQuery

    fun getQueryForSong(
        filter: Filter?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForAlbum(
        filter: Filter?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForAlbumArtist(
        filter: Filter?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForArtist(
        filter: Filter?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForGenre(
        filter: Filter?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForSongCount(filter: Filter): SimpleSQLiteQuery
    fun getQueryForTagsCount(filter: Filter?): SimpleSQLiteQuery

    //region playlist
    fun getQueryForPlaylist(
        filter: Filter?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForPlaylistWithCount(
        filter: Filter?,
        search: String?
    ): SimpleSQLiteQuery

    fun getQueryForPlaylistWithPresence(filter: Filter): SimpleSQLiteQuery

    //region podcasts
    fun getQueryForPodcasts(
        filter: Filter?//todo: add ordering handling
    ): SimpleSQLiteQuery

    fun getQueryForPodcastEpisodes(
        filter: Filter?//todo: add ordering handling
    ): SimpleSQLiteQuery

    fun getQueryForPodcastEpisodeIds(
        filter: Filter?//todo: add ordering handling
    ): SimpleSQLiteQuery

    fun getQueryForPodcastEpisodeIds(
        filterList: List<Filter>?//todo: add ordering handling
    ): SimpleSQLiteQuery

    fun getQueryForPodcastCount(filter: Filter?): SimpleSQLiteQuery

    //region download
    fun getQueryForDownload(filter: Filter?): SimpleSQLiteQuery
    fun getQueryForDownloadCount(filter: Filter?): SimpleSQLiteQuery
    fun getQueryForDownloadProgress(filter: Filter?): SimpleSQLiteQuery
}