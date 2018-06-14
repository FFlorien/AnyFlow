package be.florien.ampacheplayer.persistence

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import be.florien.ampacheplayer.persistence.dao.*
import be.florien.ampacheplayer.persistence.model.*


@Database(entities = [Album::class, Artist::class, Playlist::class, QueueOrder::class, Song::class, Tag::class], version = 1)
abstract class LibraryDatabase : RoomDatabase() {

    abstract fun getAlbumDao(): AlbumDao
    abstract fun getArtistDao(): ArtistDao
    abstract fun getPlaylistDao(): PlaylistDao
    abstract fun getQueueOrderDao(): QueueOrderDao
    abstract fun getTagDao(): TagDao
    abstract fun getSongDao(): SongDao
    abstract fun getFilterDao(): FilterDao

    companion object {
        @Volatile
        private var instance: LibraryDatabase? = null
        private const val DB_NAME = "ampacheDatabase.db"

        fun getInstance(context: Context): LibraryDatabase {
            if (instance == null) {
                instance = create(context)
            }
            return instance!!
        }

        @Synchronized
        private fun create(context: Context): LibraryDatabase {
            return Room.databaseBuilder(context, LibraryDatabase::class.java, DB_NAME).build()
        }

    }
}