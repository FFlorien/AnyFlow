package be.florien.anyflow.data.local.dao

import androidx.room.Dao
import be.florien.anyflow.data.local.model.DbSongGenre

@Dao
abstract class SongGenreDao : BaseDao<DbSongGenre>()