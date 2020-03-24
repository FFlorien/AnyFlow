package be.florien.anyflow.data.view

data class Song(
        val id: Long,
        val title: String,
        val artistName: String,
        val albumName: String,
        val albumArtistName: String,
        val time: Int,
        val art: String,
        val url: String,
        val genre: String)

data class FilterGroup(
        val id: Long,
        val name: String)