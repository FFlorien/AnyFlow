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
        val genre: String) {
    val timeText: String
        get() = String.format("%d:%02d", time / 60, time % 60)
}

data class FilterGroup(
        val id: Long,
        val name: String)