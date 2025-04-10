package be.florien.anyflow.tags.local

enum class TableSchema(
    val tableName: String,
    val tableWeight: Int,
    val isAtom: Boolean = false//todo react to real stats, initialized at startup in a table ?
) {
    Song("song",1000, true),
    Artist("artist",150),
    Album("album",85),
    AlbumArtist("artist",20),
    Genre("genre",4),
    SongGenre("songGenre",3),
    Playlist("playlist",2),
    PlaylistSong("playlistSongs",1),
}

sealed interface DbSchema {
    val table: TableSchema
    val columnName: String
    val pathToAtom: DbSchema?
}

enum class Song(override val columnName: String) : DbSchema {
    Id(columnName = "id"),
    Title(columnName = "title"),
    TitleForSort(columnName = "titleForSort"),
    ArtistId(columnName = "artistId"),
    AlbumId(columnName = "albumId"),
    Track(columnName = "track"),
    Disk(columnName = "disk"),
    Time(columnName = "time"),
    Year(columnName = "year"),
    Composer(columnName = "composer"),
    Size(columnName = "size"),
    Local(columnName = "local"),
    WaveForm(columnName = "waveForm");

    override val table: TableSchema = TableSchema.Song
    override val pathToAtom: DbSchema? = null
}

enum class Artist(override val columnName: String, override val pathToAtom: DbSchema? = null) : DbSchema {
    Id(columnName = "id", pathToAtom = Song.ArtistId),
    Name(columnName = "name"),
    Prefix(columnName = "prefix"),
    Basename(columnName = "basename"),
    Summary(columnName = "summary");

    override val table: TableSchema = TableSchema.Artist
}

enum class Album(override val columnName: String, override val pathToAtom: DbSchema? = null) : DbSchema {
    Id(columnName = "id", pathToAtom = Song.AlbumId),
    Name(columnName = "name"),
    ArtistId(columnName = "artistId"),
    Prefix(columnName = "prefix"),
    Basename(columnName = "basename"),
    Year(columnName = "year"),
    Diskcount(columnName = "diskcount");

    override val table: TableSchema = TableSchema.Album
}

enum class AlbumArtist(
    override val columnName: String,
    override val pathToAtom: DbSchema? = null
) : DbSchema {

    Id(columnName = "id", pathToAtom = Album.ArtistId),
    Name(columnName = "name"),
    Prefix(columnName = "prefix"),
    Basename(columnName = "basename"),
    Summary(columnName = "summary");

    override val table: TableSchema = TableSchema.Artist

}

enum class Genre(
    override val columnName: String,
    override val pathToAtom: DbSchema? = null
) : DbSchema {
    Id(columnName = "id", pathToAtom = SongGenre.GenreId),
    Name(columnName = "name");

    override val table: TableSchema = TableSchema.Genre
}

enum class SongGenre(
    override val columnName: String,
    override val pathToAtom: DbSchema? = null
): DbSchema {
    SongId(columnName = "songId", pathToAtom = Song.Id),
    GenreId(columnName = "genreId");

    override val table: TableSchema = TableSchema.SongGenre
}

enum class Playlist(
    override val columnName: String,
    override val pathToAtom: DbSchema? = null
) : DbSchema {
    Id(columnName = "id", pathToAtom = PlaylistSong.PlaylistId),
    Name(columnName = "name"),
    Owner(columnName = "owner");

    override val table: TableSchema = TableSchema.Playlist
}

enum class PlaylistSong(
    override val columnName: String,
    override val pathToAtom: DbSchema? = null
): DbSchema {
    SongId(columnName = "songId", pathToAtom = Song.Id),
    PlaylistId(columnName = "playlistId"),
    Order(columnName = "order");

    override val table: TableSchema = TableSchema.PlaylistSong
}

enum class Equivalent(val equivalents: Set<DbSchema>) {
    SongId(setOf(Song.Id, SongGenre.SongId, PlaylistSong.SongId)),
    ArtistId(setOf(Artist.Id, Song.ArtistId)),
    AlbumId(setOf(Album.Id, Song.AlbumId)),
    AlbumArtistId(setOf(AlbumArtist.Id, Album.ArtistId)),
    GenreId(setOf(Genre.Id, SongGenre.GenreId)),
    PlaylistId(setOf(Playlist.Id, PlaylistSong.PlaylistId))
}

fun DbSchema.getEquivalent() = Equivalent.entries.firstOrNull { it.equivalents.contains(this) }

fun TableSchema.getPathToAtom(): DbSchema = when (this) {
    TableSchema.Song -> Song.Id
    TableSchema.Artist -> Artist.Id
    TableSchema.Album -> Album.Id
    TableSchema.AlbumArtist -> AlbumArtist.Id
    TableSchema.Genre -> Genre.Id
    TableSchema.SongGenre -> SongGenre.SongId
    TableSchema.Playlist -> Playlist.Id
    TableSchema.PlaylistSong -> PlaylistSong.SongId
}

fun TableSchema.getEquivalent() = when(this) {
    TableSchema.Song -> listOf(Equivalent.SongId, Equivalent.AlbumId, Equivalent.ArtistId)
    TableSchema.Artist -> listOf(Equivalent.ArtistId)
    TableSchema.Album -> listOf(Equivalent.AlbumId, Equivalent.AlbumArtistId)
    TableSchema.AlbumArtist -> listOf(Equivalent.AlbumArtistId)
    TableSchema.Genre -> listOf(Equivalent.GenreId)
    TableSchema.SongGenre -> listOf(Equivalent.SongId, Equivalent.GenreId)
    TableSchema.Playlist -> listOf(Equivalent.PlaylistId)
    TableSchema.PlaylistSong -> listOf(Equivalent.SongId, Equivalent.PlaylistId)
}