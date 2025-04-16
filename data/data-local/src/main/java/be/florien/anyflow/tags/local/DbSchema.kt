package be.florien.anyflow.tags.local

enum class SchemaDomain {
    Songs, Podcast
}

enum class IdEquivalent {
    SongId,
    ArtistId,
    AlbumId,
    AlbumArtistId,
    GenreId,
    PlaylistId,
    PodcastId
}

enum class JoinParameters(val first: DbSchema, val second: DbSchema) {
    SongToArtist(Song.ArtistId, Artist.Id),
    SongToAlbum(Song.AlbumId, Album.Id),
    AlbumToAlbumArtist(Album.ArtistId, AlbumArtist.Id),
    SongToPlaylistSong(Song.Id, PlaylistSong.SongId),
    SongToSongGenre(Song.Id, SongGenre.SongId),
    SongGenreToGenre(SongGenre.GenreId, Genre.Id),
    PlaylistSongToPlaylist(PlaylistSong.PlaylistId, Playlist.Id),
    PlaylistSongToGenreSong(PlaylistSong.SongId, SongGenre.SongId)
}

enum class TableSchema(
    val tableName: String,
    val tableWeight: Int,//todo react to real stats, initialized at startup in a table ?
    val distanceFromAtom: Int,
    val domain: SchemaDomain
) {
    Song("song", 1000, 0, SchemaDomain.Songs),
    Artist("artist", 150, 1, SchemaDomain.Songs),
    Album("album", 85, 1, SchemaDomain.Songs),
    AlbumArtist("artist", 20, 2, SchemaDomain.Songs),
    Genre("genre", 4, 2, SchemaDomain.Songs),
    SongGenre("songGenre", 3, 1, SchemaDomain.Songs),
    Playlist("playlist", 2, 2, SchemaDomain.Songs),
    PlaylistSong("playlistSongs", 1, 1, SchemaDomain.Songs),
    PodcastEpisode("podcastEpisode", 900, distanceFromAtom = 0, SchemaDomain.Podcast),
    Podcast("podcast", 90, 1, SchemaDomain.Podcast)
}

sealed interface DbSchema {
    val table: TableSchema
    val columnName: String
    val idEquivalent: IdEquivalent?
}

enum class Song(override val columnName: String, override val idEquivalent: IdEquivalent? = null) :
    DbSchema {
    Id(columnName = "id", idEquivalent = IdEquivalent.SongId),
    Title(columnName = "title"),
    TitleForSort(columnName = "titleForSort"),
    ArtistId(columnName = "artistId", idEquivalent = IdEquivalent.ArtistId),
    AlbumId(columnName = "albumId", idEquivalent = IdEquivalent.AlbumId),
    Track(columnName = "track"),
    Disk(columnName = "disk"),
    Time(columnName = "time"),
    Year(columnName = "year"),
    Composer(columnName = "composer"),
    Size(columnName = "size"),
    Local(columnName = "local"),
    WaveForm(columnName = "waveForm");

    override val table: TableSchema = TableSchema.Song
}

enum class Artist(
    override val columnName: String,
    override val idEquivalent: IdEquivalent? = null
) :
    DbSchema {
    Id(columnName = "id", idEquivalent = IdEquivalent.ArtistId),
    Name(columnName = "name"),
    Prefix(columnName = "prefix"),
    Basename(columnName = "basename"),
    Summary(columnName = "summary");

    override val table: TableSchema = TableSchema.Artist
}

enum class Album(override val columnName: String, override val idEquivalent: IdEquivalent? = null) :
    DbSchema {
    Id(columnName = "id", idEquivalent = IdEquivalent.AlbumId),
    Name(columnName = "name"),
    ArtistId(columnName = "artistId", idEquivalent = IdEquivalent.AlbumArtistId),
    Prefix(columnName = "prefix"),
    Basename(columnName = "basename"),
    Year(columnName = "year"),
    Diskcount(columnName = "diskcount");

    override val table: TableSchema = TableSchema.Album
}

enum class AlbumArtist(
    override val columnName: String,
    override val idEquivalent: IdEquivalent? = null
) : DbSchema {

    Id(columnName = "id", idEquivalent = IdEquivalent.AlbumArtistId),
    Name(columnName = "name"),
    Prefix(columnName = "prefix"),
    Basename(columnName = "basename"),
    Summary(columnName = "summary");

    override val table: TableSchema = TableSchema.AlbumArtist

}

enum class Genre(
    override val columnName: String,
    override val idEquivalent: IdEquivalent? = null
) : DbSchema {
    Id(columnName = "id", idEquivalent = IdEquivalent.GenreId),
    Name(columnName = "name");

    override val table: TableSchema = TableSchema.Genre
}

enum class SongGenre(
    override val columnName: String,
    override val idEquivalent: IdEquivalent
) : DbSchema {
    SongId(columnName = "songId", idEquivalent = IdEquivalent.SongId),
    GenreId(columnName = "genreId", idEquivalent = IdEquivalent.GenreId);

    override val table: TableSchema = TableSchema.SongGenre
}

enum class Playlist(
    override val columnName: String,
    override val idEquivalent: IdEquivalent? = null
) : DbSchema {
    Id(columnName = "id", idEquivalent = IdEquivalent.PlaylistId),
    Name(columnName = "name"),
    Owner(columnName = "owner");

    override val table: TableSchema = TableSchema.Playlist
}

enum class PlaylistSong(
    override val columnName: String,
    override val idEquivalent: IdEquivalent? = null
) : DbSchema {
    SongId(columnName = "songId", idEquivalent = IdEquivalent.SongId),
    PlaylistId(columnName = "playlistId", idEquivalent = IdEquivalent.PlaylistId),
    Order(columnName = "order");

    override val table: TableSchema = TableSchema.PlaylistSong
}

enum class PodcastEpisode(
    override val columnName: String,
    override val idEquivalent: IdEquivalent? = null
) : DbSchema {
    Id(columnName = "id"),
    Title(columnName = "title"),
    PodcastId(columnName = "podcastId", idEquivalent = IdEquivalent.PodcastId),
    Description(columnName = "description"),
    Category(columnName = "category"),
    AuthorFull(columnName = "authorFull"),
    Website(columnName = "website"),
    PublicationDate(columnName = "publicationDate"),
    State(columnName = "state"),
    Time(columnName = "time"),
    Size(columnName = "size"),
    PlayCount(columnName = "playCount"),
    Played(columnName = "played"),
    WaveForm(columnName = "waveForm");

    override val table: TableSchema = TableSchema.PodcastEpisode
}

enum class Podcast(
    override val columnName: String,
    override val idEquivalent: IdEquivalent? = null
) : DbSchema {
    Id("id", IdEquivalent.PodcastId),
    Name("name"),
    Description("description"),
    Language("language"),
    FeedUrl("feedUrl"),
    Website("website"),
    BuildDate("buildDate"),
    SyncDate("syncDate");

    override val table: TableSchema = TableSchema.Podcast
}

private val songIdEquivalents: Set<DbSchema> = setOf(Song.Id, SongGenre.SongId, PlaylistSong.SongId)
private val artistIdEquivalents: Set<DbSchema> = setOf(Artist.Id, Song.ArtistId)
private val albumIdEquivalents: Set<DbSchema> = setOf(Album.Id, Song.AlbumId)
private val albumArtistIdEquivalents: Set<DbSchema> = setOf(AlbumArtist.Id, Album.ArtistId)
private val genreIdEquivalents: Set<DbSchema> = setOf(Genre.Id, SongGenre.GenreId)
private val playlistIdEquivalents: Set<DbSchema> = setOf(Playlist.Id, PlaylistSong.PlaylistId)
private val podcastIdEquivalents: Set<DbSchema> = setOf(PodcastEpisode.PodcastId, Podcast.Id)

fun DbSchema.getEquivalents() = when (idEquivalent) {
    IdEquivalent.SongId -> songIdEquivalents
    IdEquivalent.ArtistId -> artistIdEquivalents
    IdEquivalent.AlbumId -> albumIdEquivalents
    IdEquivalent.AlbumArtistId -> albumArtistIdEquivalents
    IdEquivalent.GenreId -> genreIdEquivalents
    IdEquivalent.PlaylistId -> playlistIdEquivalents
    IdEquivalent.PodcastId -> podcastIdEquivalents
    null -> emptySet()
}

private val allEquivalents = listOf(
    songIdEquivalents,
    artistIdEquivalents,
    albumIdEquivalents,
    albumArtistIdEquivalents,
    genreIdEquivalents,
    playlistIdEquivalents,
    podcastIdEquivalents
)

fun getJoinFromTableToTable(firstTable: TableSchema, secondTable: TableSchema): Set<JoinParameters> = when (firstTable) {
    TableSchema.Song -> when (secondTable) {
        TableSchema.Song -> emptySet()
        TableSchema.Artist -> setOf(JoinParameters.SongToArtist)
        TableSchema.Album -> setOf(JoinParameters.SongToAlbum)
        TableSchema.AlbumArtist -> setOf(JoinParameters.SongToAlbum, JoinParameters.AlbumToAlbumArtist)
        TableSchema.Genre -> setOf(JoinParameters.SongToSongGenre, JoinParameters.SongGenreToGenre)
        TableSchema.SongGenre -> setOf(JoinParameters.SongToSongGenre)
        TableSchema.Playlist -> setOf(JoinParameters.SongToPlaylistSong, JoinParameters.PlaylistSongToPlaylist)
        TableSchema.PlaylistSong -> setOf(JoinParameters.SongToPlaylistSong)
        TableSchema.PodcastEpisode -> emptySet()
        TableSchema.Podcast -> emptySet()
    }

    TableSchema.Artist -> when (secondTable) {
        TableSchema.Song -> setOf(JoinParameters.SongToArtist)
        TableSchema.Artist -> emptySet()
        TableSchema.Album -> setOf(JoinParameters.SongToAlbum, JoinParameters.SongToArtist)
        TableSchema.AlbumArtist -> setOf(JoinParameters.SongToArtist, JoinParameters.SongToAlbum, JoinParameters.AlbumToAlbumArtist)
        TableSchema.Genre -> setOf(JoinParameters.SongToArtist, JoinParameters.SongToSongGenre, JoinParameters.SongGenreToGenre)
        TableSchema.SongGenre -> setOf(JoinParameters.SongToArtist, JoinParameters.SongToSongGenre)
        TableSchema.Playlist -> setOf(JoinParameters.SongToArtist, JoinParameters.SongToPlaylistSong, JoinParameters.PlaylistSongToPlaylist)
        TableSchema.PlaylistSong -> setOf(JoinParameters.SongToArtist, JoinParameters.SongToPlaylistSong)
        TableSchema.PodcastEpisode -> emptySet()
        TableSchema.Podcast -> emptySet()
    }

    TableSchema.Album -> when (secondTable) {
        TableSchema.Song -> setOf()
        TableSchema.Artist -> setOf()
        TableSchema.Album -> setOf()
        TableSchema.AlbumArtist -> setOf()
        TableSchema.Genre -> setOf()
        TableSchema.SongGenre -> setOf()
        TableSchema.Playlist -> setOf()
        TableSchema.PlaylistSong -> setOf()
        TableSchema.PodcastEpisode -> emptySet()
        TableSchema.Podcast -> emptySet()
    }

    TableSchema.AlbumArtist -> when (secondTable) {
        TableSchema.Song -> setOf()
        TableSchema.Artist -> setOf()
        TableSchema.Album -> setOf()
        TableSchema.AlbumArtist -> setOf()
        TableSchema.Genre -> setOf()
        TableSchema.SongGenre -> setOf()
        TableSchema.Playlist -> setOf()
        TableSchema.PlaylistSong -> setOf()
        TableSchema.PodcastEpisode -> emptySet()
        TableSchema.Podcast -> emptySet()
    }

    TableSchema.Genre -> when (secondTable) {
        TableSchema.Song -> setOf()
        TableSchema.Artist -> setOf()
        TableSchema.Album -> setOf()
        TableSchema.AlbumArtist -> setOf()
        TableSchema.Genre -> setOf()
        TableSchema.SongGenre -> setOf()
        TableSchema.Playlist -> setOf()
        TableSchema.PlaylistSong -> setOf()
        TableSchema.PodcastEpisode -> emptySet()
        TableSchema.Podcast -> emptySet()
    }

    TableSchema.SongGenre -> when (secondTable) {
        TableSchema.Song -> setOf()
        TableSchema.Artist -> setOf()
        TableSchema.Album -> setOf()
        TableSchema.AlbumArtist -> setOf()
        TableSchema.Genre -> setOf()
        TableSchema.SongGenre -> setOf()
        TableSchema.Playlist -> setOf()
        TableSchema.PlaylistSong -> setOf()
        TableSchema.PodcastEpisode -> emptySet()
        TableSchema.Podcast -> emptySet()
    }

    TableSchema.Playlist -> when (secondTable) {
        TableSchema.Song -> setOf()
        TableSchema.Artist -> setOf()
        TableSchema.Album -> setOf()
        TableSchema.AlbumArtist -> setOf()
        TableSchema.Genre -> setOf()
        TableSchema.SongGenre -> setOf()
        TableSchema.Playlist -> setOf()
        TableSchema.PlaylistSong -> setOf()
        TableSchema.PodcastEpisode -> emptySet()
        TableSchema.Podcast -> emptySet()
    }

    TableSchema.PlaylistSong -> when (secondTable) {
        TableSchema.Song -> setOf()
        TableSchema.Artist -> setOf()
        TableSchema.Album -> setOf()
        TableSchema.AlbumArtist -> setOf()
        TableSchema.Genre -> setOf()
        TableSchema.SongGenre -> setOf()
        TableSchema.Playlist -> setOf()
        TableSchema.PlaylistSong -> setOf()
        TableSchema.PodcastEpisode -> emptySet()
        TableSchema.Podcast -> emptySet()
    }

    TableSchema.PodcastEpisode -> when (secondTable) {
        TableSchema.Song -> setOf()
        TableSchema.Artist -> setOf()
        TableSchema.Album -> setOf()
        TableSchema.AlbumArtist -> setOf()
        TableSchema.Genre -> setOf()
        TableSchema.SongGenre -> setOf()
        TableSchema.Playlist -> setOf()
        TableSchema.PlaylistSong -> setOf()
        TableSchema.PodcastEpisode -> setOf()
        TableSchema.Podcast -> setOf()
    }

    TableSchema.Podcast -> when (secondTable) {
        TableSchema.Song -> emptySet()
        TableSchema.Artist -> emptySet()
        TableSchema.Album -> emptySet()
        TableSchema.AlbumArtist -> emptySet()
        TableSchema.Genre -> emptySet()
        TableSchema.SongGenre -> emptySet()
        TableSchema.Playlist -> emptySet()
        TableSchema.PlaylistSong -> emptySet()
        TableSchema.PodcastEpisode -> emptySet()
        TableSchema.Podcast -> emptySet()
    }
}

fun TableSchema.getPathToAtom(): DbSchema = when (this) {
    TableSchema.Song -> Song.Id
    TableSchema.Artist -> Artist.Id
    TableSchema.Album -> Album.Id
    TableSchema.AlbumArtist -> AlbumArtist.Id
    TableSchema.Genre -> Genre.Id
    TableSchema.SongGenre -> SongGenre.SongId
    TableSchema.Playlist -> Playlist.Id
    TableSchema.PlaylistSong -> PlaylistSong.SongId
    TableSchema.PodcastEpisode -> PodcastEpisode.Id
    TableSchema.Podcast -> Podcast.Id
}