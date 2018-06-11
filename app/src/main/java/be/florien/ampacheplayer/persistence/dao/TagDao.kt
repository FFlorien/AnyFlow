package be.florien.ampacheplayer.persistence.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.model.Tag


@Dao
interface TagDao {
    @Query("SELECT * FROM tag")
    fun getTag(): List<Tag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert( tags: List<Tag>)

    @Update
    fun update(vararg tags: Tag)

    @Delete
    fun delete(vararg tags: Tag)
}