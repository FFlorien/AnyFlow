package be.florien.anyflow.tags.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import be.florien.anyflow.tags.local.dao.AlarmDao
import be.florien.anyflow.tags.local.dao.AlbumDao
import be.florien.anyflow.tags.local.dao.ArtistDao
import be.florien.anyflow.tags.local.dao.DownloadDao
import be.florien.anyflow.tags.local.dao.FilterDao
import be.florien.anyflow.tags.local.dao.FilterGroupDao
import be.florien.anyflow.tags.local.dao.GenreDao
import be.florien.anyflow.tags.local.dao.OrderingDao
import be.florien.anyflow.tags.local.dao.PlaylistDao
import be.florien.anyflow.tags.local.dao.PlaylistSongDao
import be.florien.anyflow.tags.local.dao.PodcastDao
import be.florien.anyflow.tags.local.dao.PodcastEpisodeDao
import be.florien.anyflow.tags.local.dao.QueueOrderDao
import be.florien.anyflow.tags.local.dao.SongDao
import be.florien.anyflow.tags.local.dao.SongGenreDao
import be.florien.anyflow.tags.local.model.DbAlarm
import be.florien.anyflow.tags.local.model.DbAlbum
import be.florien.anyflow.tags.local.model.DbArtist
import be.florien.anyflow.tags.local.model.DbDownload
import be.florien.anyflow.tags.local.model.DbFilter
import be.florien.anyflow.tags.local.model.DbFilterGroup
import be.florien.anyflow.tags.local.model.DbGenre
import be.florien.anyflow.tags.local.model.DbOrdering
import be.florien.anyflow.tags.local.model.DbPlaylist
import be.florien.anyflow.tags.local.model.DbPlaylistSongs
import be.florien.anyflow.tags.local.model.DbPodcast
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.tags.local.model.DbQueueOrder
import be.florien.anyflow.tags.local.model.DbSong
import be.florien.anyflow.tags.local.model.DbSongGenre
import java.util.Date


@Database(
    version = 3,
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
                .addCallback(object : Callback() { //todo set order the first time
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val currentFilterGroup = DbFilterGroup(
                            DbFilterGroup.CURRENT_FILTER_GROUP_ID,
                            null,
                            Date().time //todo reuse TimeOperation as soon as it's available outside of app
                        )
                        db.execSQL("INSERT INTO FilterGroup VALUES (${currentFilterGroup.id}, \"${currentFilterGroup.name}\", ${currentFilterGroup.dateAdded})")
                    }
                })
                .addMigrations(
                    object: Migration(1, 2) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE PodcastEpisode ADD waveform varchar DEFAULT NULL")
                        }
                    }
                )
                .addMigrations(
                    object: Migration(2, 3) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE PodcastEpisode RENAME waveform TO waveForm")
                        }
                    }
                )
                .build()
        }
    }
}