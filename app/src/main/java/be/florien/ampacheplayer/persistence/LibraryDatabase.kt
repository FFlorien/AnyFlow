package be.florien.ampacheplayer.persistence

import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.arch.persistence.room.*
import android.content.Context
import be.florien.ampacheplayer.persistence.dao.*
import be.florien.ampacheplayer.persistence.model.*


@Database(entities = [Album::class, Artist::class, Playlist::class, QueueOrder::class, Song::class, Tag::class], version = 1)
abstract class LibraryDatabase : RoomDatabase() {

    open val isMockObject = false

    abstract fun getAlbumDao(): AlbumDao
    abstract fun getArtistDao(): ArtistDao
    abstract fun getPlaylistDao(): PlaylistDao
    abstract fun getQueueOrderDao(): QueueOrderDao
    abstract fun getTagDao(): TagDao
    abstract fun getSongDao(): SongDao

    companion object {
        @Volatile
        private var instance: LibraryDatabase = MockLibraryDatabase()
        private const val DB_NAME = "ampacheDatabase.db"

        fun getInstance(context: Context): LibraryDatabase {
            if (instance.isMockObject) {
                instance = create(context)
            }
            return instance
        }

        @Synchronized
        private fun create(context: Context): LibraryDatabase {
            return Room.databaseBuilder(context, LibraryDatabase::class.java, DB_NAME).build()
        }

    }
}

private class MockLibraryDatabase : LibraryDatabase() {
    override fun getTagDao(): TagDao {
        TODO("not implemented")
    }

    override fun getArtistDao(): ArtistDao {
        TODO("not implemented")
    }

    override fun getPlaylistDao(): PlaylistDao {
        TODO("not implemented")
    }

    override fun getQueueOrderDao(): QueueOrderDao {
        TODO("not implemented")
    }

    override fun getSongDao(): SongDao {
        TODO("not implemented")
    }

    override fun getAlbumDao(): AlbumDao {
        TODO("not implemented")
    }

    override fun createOpenHelper(config: DatabaseConfiguration?): SupportSQLiteOpenHelper {
        TODO("not implemented")
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        TODO("not implemented")
    }

    override fun clearAllTables() {
        TODO("not implemented")
    }

    override val isMockObject = true
}