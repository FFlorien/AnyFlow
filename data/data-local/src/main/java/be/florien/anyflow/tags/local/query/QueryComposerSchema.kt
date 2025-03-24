package be.florien.anyflow.tags.local.query

import be.florien.anyflow.tags.local.Album
import be.florien.anyflow.tags.local.AlbumArtist
import be.florien.anyflow.tags.local.Artist
import be.florien.anyflow.tags.local.DbJointSchema
import be.florien.anyflow.tags.local.DbSchema
import be.florien.anyflow.tags.local.Genre
import be.florien.anyflow.tags.local.Playlist
import be.florien.anyflow.tags.local.Song

class QueryComposerSchema(private val delegate: QueryComposer) : QueryComposer by delegate {
    //region tags
//    override fun getQueryForSongIds(
//        filters: List<QueryFilter>,
//        orderingList: List<QueryOrdering>
//    ): SimpleSQLiteQuery {
//
//        val selects = listOf(Select(Song.Id))
//    }

    private fun getQuery(
        selects: List<Select>,
        filters: List<QueryFilter>,
        orderingList: List<QueryOrdering>
    ) {

        //get all relevant tables
        val tableForSelect = selects.map { Join(it.schema.getActiveSchema().table.tableName) }

        val tableAliases = mutableMapOf<String, Int>()

        fun reduceWhereTable(it: QueryFilter): List<Join> {
            val child = it.child
            val list = if (child != null) {
                reduceWhereTable(child)
            } else {
                emptyList()
            }

            val activeSchema = it.toDbSchema().schema.getActiveSchema()
            val tableName = activeSchema.table.tableName
            val tableAlias = if (activeSchema is DbJointSchema) {
                val index = tableAliases[tableName] ?: 0
                tableAliases[tableName] = index + 1
                tableName + index
            } else {
                null
            }
            return list + Join(tableName, tableAlias)
        }

        val tableSet = (tableForSelect + filters.flatMap(::reduceWhereTable)).toSet()
    }

    private fun DbSchema.getActiveSchema(): DbSchema = equivalent ?: this

    private fun QueryFilter.toDbSchema(): Where {
        val schema = when (type) {
            QueryFilter.FilterType.SONG_IS -> Song.Id
            QueryFilter.FilterType.ARTIST_IS -> Album.Id
            QueryFilter.FilterType.ALBUM_ARTIST_IS -> AlbumArtist.Id
            QueryFilter.FilterType.ALBUM_IS -> Album.Id
            QueryFilter.FilterType.GENRE_IS -> Genre.Id
            QueryFilter.FilterType.PLAYLIST_IS -> Playlist.Id
            QueryFilter.FilterType.DOWNLOADED_STATUS_IS -> Song.Local
            QueryFilter.FilterType.DISK_IS -> Song.Disk
            QueryFilter.FilterType.PODCAST_IS -> TODO()
            QueryFilter.FilterType.PODCAST_EPISODE_IS -> TODO()
            QueryFilter.FilterType.STATE_IS -> TODO()
        }

        return Where(schema, argument)
    }

    private fun List<QueryOrdering>.toDbSchema(): List<Order> =
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
            Order(subject)
        }

    data class Select(
        val schema: DbSchema,
        val alias: String? = null,
    )

    data class Where(
        val schema: DbSchema,
        val value: String,
    )

    data class Order(
        val schema: DbSchema,
    )

    data class Join(
        val table: String,
        val tableAlias: String? = null,
    )
}