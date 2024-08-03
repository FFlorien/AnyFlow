package be.florien.anyflow.tags.local.dao

import androidx.room.Dao
import be.florien.anyflow.tags.local.model.DbSongGenre

@Dao
abstract class SongGenreDao : BaseDao<DbSongGenre>()