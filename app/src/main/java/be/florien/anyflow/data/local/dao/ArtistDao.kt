package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.Artist
import be.florien.anyflow.data.local.model.ArtistDisplay

@Dao
interface ArtistDao : BaseDao<Artist> {
    @Query("SELECT DISTINCT song.albumArtistId AS id, song.albumArtistName AS name, artist.art AS art FROM song LEFT JOIN artist ON song.albumArtistId = artist.id ORDER BY albumArtistName COLLATE UNICODE")
    fun orderByName(): DataSource.Factory<Int, ArtistDisplay>
}