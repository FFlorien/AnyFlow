package be.florien.anyflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import be.florien.anyflow.data.local.dao.*
import be.florien.anyflow.data.local.model.*
import java.util.Date


@Database(
    version = 1,
    entities = [
        DbAlbum::class,
        DbArtist::class,
        DbPlaylist::class,
        DbQueueOrder::class,
        DbSong::class,
        DbGenre::class,
        DbSongGenre::class,
        DbFilter::class,
        DbFilterGroup::class,
        DbOrdering::class,
        DbPlaylistSongs::class,
        DbAlarm::class,
        DbDownload::class,
        DbPodcast::class,
        DbPodcastEpisode::class
    ],
    exportSchema = false //todo ?
)
abstract class LibraryDatabase : RoomDatabase() {

    abstract fun getSongDao(): SongDao
    abstract fun getArtistDao(): ArtistDao
    abstract fun getAlbumDao(): AlbumDao
    abstract fun getGenreDao(): GenreDao
    abstract fun getSongGenreDao(): SongGenreDao
    abstract fun getPlaylistDao(): PlaylistDao
    abstract fun getPlaylistSongsDao(): PlaylistSongDao
    abstract fun getQueueOrderDao(): QueueOrderDao
    abstract fun getFilterDao(): FilterDao
    abstract fun getFilterGroupDao(): FilterGroupDao
    abstract fun getOrderingDao(): OrderingDao
    abstract fun getAlarmDao(): AlarmDao
    abstract fun getDownloadDao(): DownloadDao
    abstract fun getPodcastDao(): PodcastDao
    abstract fun getPodcastEpisodeDao(): PodcastEpisodeDao

    companion object {

        @Volatile
        private var instance: LibraryDatabase? = null
        private const val DB_NAME = "anyflow.db"

        fun getInstance(context: Context): LibraryDatabase {
            if (instance == null) {
                instance = create(context)
            }
            return instance!!
        }

        @Synchronized
        private fun create(context: Context): LibraryDatabase {
            val databaseBuilder =
                Room.databaseBuilder(context, LibraryDatabase::class.java, DB_NAME)
            return databaseBuilder
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val currentFilterGroup =
                            DbFilterGroup(DbFilterGroup.CURRENT_FILTER_GROUP_ID, null, Date().time)
                        db.execSQL("INSERT INTO FilterGroup VALUES (${currentFilterGroup.id}, \"${currentFilterGroup.name}\", ${currentFilterGroup.dateAdded})")
                    }
                })
                .build()
        }
    }
}