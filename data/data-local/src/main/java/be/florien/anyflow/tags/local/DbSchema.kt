package be.florien.anyflow.tags.local

enum class TableSchema(
    val tableName: String,
    val tableWeight: Int, //todo reactive to real stats, initialized at startup in a table ?
) {
    Song("Song",1000),
    Artist("Artist",150),
    Album("Album",85),
    AlbumArtist("Artist",20),
    Genre("Genre",1),
    Playlist("Playlist",1),
}

sealed interface DbSchema {
    val table: TableSchema
    val columnName: String
    val equivalent: DbSchema?
}

interface DbJointSchema {
    val jointTable: String
    val centerNodeColumn: DbSchema
    val centerNodeJointColumn: String
    val externNodeJointColumn: String
    val isExternNodeColumn: Boolean
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
    override val equivalent: DbSchema? = null
}

enum class Artist(override val columnName: String, override val equivalent: DbSchema? = null) : DbSchema {
    Id(columnName = "id", equivalent = Song.ArtistId),
    Name(columnName = "name"),
    Prefix(columnName = "prefix"),
    Basename(columnName = "basename"),
    Summary(columnName = "summary");

    override val table: TableSchema = TableSchema.Artist
}

enum class Album(override val columnName: String, override val equivalent: DbSchema? = null) : DbSchema {
    Id(columnName = "id", equivalent = Song.AlbumId),
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
    override val equivalent: DbSchema? = null
) : DbSchema {

    Id(columnName = "id", equivalent = Album.ArtistId),
    Name(columnName = "name"),
    Prefix(columnName = "prefix"),
    Basename(columnName = "basename"),
    Summary(columnName = "summary");

    override val table: TableSchema = TableSchema.Artist

}

enum class Genre(
    override val columnName: String,
    override val isExternNodeColumn: Boolean = false
) : DbSchema, DbJointSchema {
    Id(columnName = "id", isExternNodeColumn = true),
    Name(columnName = "name");

    override val table: TableSchema = TableSchema.Genre
    override val equivalent: DbSchema? = null
    override val jointTable: String = "SongGenre"
    override val centerNodeColumn: DbSchema = Song.Id
    override val centerNodeJointColumn: String = "songId"
    override val externNodeJointColumn: String = "genreId"
}

enum class Playlist(
    override val columnName: String,
    override val isExternNodeColumn: Boolean = false
) : DbSchema, DbJointSchema {
    Id(columnName = "id", isExternNodeColumn = true),
    Name(columnName = "name"),
    Owner(columnName = "owner");

    override val table: TableSchema = TableSchema.Playlist
    override val equivalent: DbSchema? = null
    override val jointTable: String = "PlaylistSongs"
    override val centerNodeColumn: DbSchema = Song.Id
    override val centerNodeJointColumn: String = "songId"
    override val externNodeJointColumn: String = "playlistId"
}


/*sealed interface Song : DbSchema {
    override val tableName: String
        get() = "Song"

    data class Id(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "id"
    }

    data class Title(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "title"
    }

    data class TitleForSort(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "titleForSort"
    }

    data class ArtistId(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "artistId"
    }

    data class AlbumId(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "albumId"
    }

    data class Track(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "track"
    }

    data class Disk(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "disk"
    }

    data class Time(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "time"
    }

    data class Year(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "year"
    }

    data class Composer(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "composer"
    }

    data class Size(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "size"
    }

    data class Local(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "local"
    }

    data class WaveForm(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Song {
        override val columnName: String = "waveForm"
    }
}

sealed interface Artist : DbSchema {
    override val tableName: String
        get() = "Artist"

    data class Id(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Artist {
        override val columnName: String = "id"
    }

    data class Name(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Artist {
        override val columnName: String = "name"
    }

    data class Prefix(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Artist {
        override val columnName: String = "prefix"
    }

    data class Basename(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Artist {
        override val columnName: String = "basename"
    }

    data class Summary(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Artist {
        override val columnName: String = "summary"
    }
}

sealed interface AlbumArtist : DbJointSchema {
    override val tableName: String
        get() = "Artist"
    override val jointTable: String
        get() = "Album"

    data class Id(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : AlbumArtist {
        override val columnName: String = "id"
    }

    data class Name(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : AlbumArtist {
        override val columnName: String = "name"
    }

    data class Prefix(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : AlbumArtist {
        override val columnName: String = "prefix"
    }

    data class Basename(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : AlbumArtist {
        override val columnName: String = "basename"
    }

    data class Summary(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : AlbumArtist {
        override val columnName: String = "summary"
    }
}

sealed interface Album : DbSchema {
    override val tableName: String
        get() = "Album"

    data class Id(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Album {
        override val columnName: String = "id"
    }

    data class Name(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Album {
        override val columnName: String = "name"
    }

    data class ArtistId(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Album {
        override val columnName: String = "artistId"
    }

    data class Prefix(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Album {
        override val columnName: String = "prefix"
    }

    data class Basename(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Album {
        override val columnName: String = "basename"
    }

    data class Year(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Album {
        override val columnName: String = "year"
    }

    data class Diskcount(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Album {
        override val columnName: String = "diskcount"
    }
}

sealed interface Genre : DbJointSchema {
    override val tableName: String
        get() = "Genre"
    override val jointTable: String
        get() = "SongGenre"

    data class Id(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Genre {
        override val columnName: String = "id"
    }

    data class Name(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Genre {
        override val columnName: String = "name"
    }
}

sealed interface Playlist : DbJointSchema {
    override val tableName: String
        get() = "Playlist"
    override val jointTable: String
        get() = "PlaylistSongs"

    data class Id(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Playlist {
        override val columnName: String = "id"
    }

    data class Name(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Playlist {
        override val columnName: String = "name"
    }

    data class Owner(
        override val alias: String? = null,
        override val tableAlias: String? = null
    ) : Playlist {
        override val columnName: String = "owner"
    }
}*/