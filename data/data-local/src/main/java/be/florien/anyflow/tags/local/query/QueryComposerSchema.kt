package be.florien.anyflow.tags.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterType
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import be.florien.anyflow.tags.local.Album
import be.florien.anyflow.tags.local.AlbumArtist
import be.florien.anyflow.tags.local.Artist
import be.florien.anyflow.tags.local.DbSchema
import be.florien.anyflow.tags.local.Genre
import be.florien.anyflow.tags.local.Playlist
import be.florien.anyflow.tags.local.Song
import be.florien.anyflow.tags.local.getEquivalent
import be.florien.anyflow.tags.local.getPathToAtom

class QueryComposerSchema(private val delegate: QueryComposer) : QueryComposer by delegate {

    override fun getQueryForSongIds(
        filterList: List<Filter>,
        orderingList: List<QueryOrdering>
    ): SimpleSQLiteQuery = composeQuery(
        QueryParameters(
            listOf(
                QueryParameters.Select(Song.Id)
            ),
            filterList.toWheres(),
            orderingList.toDbSchema()

        )
    ).toSQLiteQuery("getQueryForSongIds", Throwable().stackTrace)

    override fun getQueryForSong(filter: Filter?, search: String?): SimpleSQLiteQuery =
        composeQuery(
            QueryParameters(
                listOf(
                    QueryParameters.Select(Song.Id, "id"),
                    QueryParameters.Select(Song.Title, "title"),
                    QueryParameters.Select(Artist.Name, "artistName"),
                    QueryParameters.Select(Album.Name, "albumName"),
                    QueryParameters.Select(Album.Id, "albumId"),
                    QueryParameters.Select(Song.Time, "time"),
                ),
                listOfNotNull(filter).toWheres(),
                listOf(
                    QueryParameters.Order(Song.TitleForSort)
                )
            )
        ).toSQLiteQuery("getQueryForSong", Throwable().stackTrace)

    // region private methods

    private fun List<Filter>.toWheres(): List<List<QueryParameters.Where>> =
        map { filter ->
            filter.map {
                QueryParameters.Where(
                    it.type.toDbSchema(),
                    it.argument.toString()
                )
            }
        }

    private fun composeQuery(
        queryParameters: QueryParameters,
        distinct: Boolean = true
    ): String {
        val clauses = queryParameters.getClauses()
        return composeSelect(clauses, distinct) +
                composeFrom(clauses) +
                composeWhere(clauses) +
                composeOrder(clauses)
    }

    private fun composeSelect(
        clauses: QueryParameters.Clauses,
        distinct: Boolean
    ) = "SELECT " +
            (if (distinct) "DISTINCT " else "") +
            clauses.selects.joinToString { select ->
                select.schema.getTableColumnString(clauses.joins.first { it.schema.table == select.schema.table }) + (select.alias?.let { " AS $it" }
                    ?: "")
            } + " "

    private fun composeFrom(
        clauses: QueryParameters.Clauses
    ): String {
        val joins = clauses.joins
        return "FROM " +
                joins.first().tableAndAlias() +
                if (joins.size > 1) {
                    joins.drop(1).joinToString(separator = "") { join ->
                        val otherTableSchema =
                            join.schema.getEquivalent()?.equivalents?.firstOrNull { schemaEquivalent -> joins.any { it != join && schemaEquivalent.table == it.schema.table } }
                        val otherSchemaString = otherTableSchema?.getTableColumnString()
                        val thisSchemaString = join.schema.getTableColumnString(join)
                        " JOIN ${join.tableAndAlias()} ON $otherSchemaString = $thisSchemaString"
                    }
                } else {
                    ""
                }
    }

    private fun composeWhere(clauses: QueryParameters.Clauses) =
        if (clauses.wheres.isNotEmpty()) {
            " WHERE " + clauses.wheres.joinToString(separator = " OR ") { whereList ->
                whereList.joinToString(
                    prefix = "(".takeIf { whereList.size > 1 && clauses.wheres.size > 1 } ?: "",
                    separator = " AND ",
                    postfix = ")".takeIf { whereList.size > 1 && clauses.wheres.size > 1 } ?: ""
                ) { where ->
                    val index = whereList.filter { it.schema.table == where.schema.table }.indexOf(where)
                    "${where.schema.getTableColumnString(clauses.joins.filter { it.schema.table == where.schema.table }[index])} = ${where.value}"
                }

            }
        } else {
            ""
        }

    private fun composeOrder(clauses: QueryParameters.Clauses) =
        if (clauses.orders.isEmpty()) {
            ""
        } else {
            clauses.orders.joinToString(prefix = " ORDER BY ") { order ->
                order.schema.getTableColumnString(
                    clauses.joins.first { it.schema.table == order.schema.table }) + " COLLATE UNICODE"
            }
        }

    private fun DbSchema.getTableColumnString(join: QueryParameters.JoinParameter) =
        (join.tableAlias ?: table.tableName) + ".${columnName}"

    private fun DbSchema.getTableColumnString() = "${table.tableName }.${columnName}"

    private fun QueryParameters.JoinParameter.tableAndAlias() =
        schema.table.tableName + (if (tableAlias != null) " AS $tableAlias" else "")

    private fun FilterType.toDbSchema(): DbSchema = when (this) {
        TagFilterType.SONG_IS -> Song.Id
        TagFilterType.ARTIST_IS -> Artist.Id
        TagFilterType.ALBUM_ARTIST_IS -> AlbumArtist.Id
        TagFilterType.ALBUM_IS -> Album.Id
        TagFilterType.GENRE_IS -> Genre.Id
        TagFilterType.PLAYLIST_IS -> Playlist.Id
        TagFilterType.DISK_IS -> Song.Disk
        TagFilterType.DOWNLOADED_STATUS_IS -> Song.Local
        PodcastFilterType.PODCAST_EPISODE_IS -> TODO()
        PodcastFilterType.PODCAST_IS -> TODO()
        PodcastFilterType.STATE_IS -> TODO()
    }

    private fun List<QueryOrdering>.toDbSchema(): List<QueryParameters.Order> =
        map {
            val subject = when (it.subject) {
                QueryOrdering.Subject.ALL -> Song.Id
                QueryOrdering.Subject.ARTIST -> Artist.Basename
                QueryOrdering.Subject.ALBUM_ARTIST -> AlbumArtist.Basename
                QueryOrdering.Subject.ALBUM -> Album.Basename
                QueryOrdering.Subject.DISC -> Song.Disk
                QueryOrdering.Subject.YEAR -> Album.Year
                QueryOrdering.Subject.GENRE -> Genre.Name
                QueryOrdering.Subject.TRACK -> Song.Track
                QueryOrdering.Subject.TITLE -> Song.Title
            }
            QueryParameters.Order(subject)
        }
    //endregion
}