package be.florien.anyflow.management.playlist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Playlist(
    val id: Long,
    val name: String,
    val count: Int
) : Parcelable

@Parcelize
data class PlaylistWithPresence(
    val id: Long,
    val name: String,
    val count: Int,
    val presence: Int
) : Parcelable