package be.florien.anyflow.management.playlist.model

import android.os.Parcelable
import be.florien.anyflow.common.ui.data.ImageConfig
import kotlinx.parcelize.Parcelize


@Parcelize
data class Playlist(
    val id: Long,
    val name: String,
    val coverConfig: ImageConfig
) : Parcelable

@Parcelize
data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val count: Int,
    val coverConfig: ImageConfig
) : Parcelable

@Parcelize
data class PlaylistWithPresence(
    val id: Long,
    val name: String,
    val count: Int,
    val presence: Int,
    val coverConfig: ImageConfig
) : Parcelable