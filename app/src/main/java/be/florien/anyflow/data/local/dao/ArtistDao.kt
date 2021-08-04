package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbArtist
import be.florien.anyflow.data.local.model.DbArtistDisplay

@Dao
interface ArtistDao : BaseDao<DbArtist> {
    @Query("SELECT DISTINCT song.albumArtistId AS id, song.albumArtistName AS name, artist.art AS art FROM song LEFT JOIN artist ON song.albumArtistId = artist.id ORDER BY albumArtistName COLLATE UNICODE")
    fun orderByName(): LiveData<List<DbArtistDisplay>>

    @Query("SELECT DISTINCT song.albumArtistId AS id, song.albumArtistName AS name, artist.art AS art FROM song LEFT JOIN artist ON song.albumArtistId = artist.id WHERE song.albumArtistName LIKE :filter ORDER BY albumArtistName COLLATE UNICODE")
    fun orderByNameFiltered(filter: String): LiveData<List<DbArtistDisplay>>
}