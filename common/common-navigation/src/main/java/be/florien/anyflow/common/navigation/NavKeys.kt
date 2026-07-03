package be.florien.anyflow.common.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import be.florien.anyflow.management.filters.domain.model.Filter
import kotlinx.serialization.Serializable
import be.florien.anyflow.management.filters.domain.model.FilterType

@Serializable
sealed interface ConnectedDestination: NavKey

@Serializable
sealed interface BottomNavDestination : ConnectedDestination {
    @get:StringRes
    val label: Int

    @get:DrawableRes
    val icon: Int

    @Serializable
    data object TagLibrary : BottomNavDestination {
        override val label: Int = R.string.menu_library
        override val icon: Int = R.drawable.ic_library
    }

    @Serializable
    data object PodcastLibrary : BottomNavDestination {
        override val label: Int = R.string.menu_podcast
        override val icon: Int = R.drawable.ic_podcast_episode
    }

    @Serializable
    data object NowPlaying : BottomNavDestination {
        override val label: Int = R.string.player_playing_now
        override val icon: Int = R.drawable.ic_play
    }

    @Serializable
    data object Filters : BottomNavDestination {
        override val label: Int = R.string.menu_filters
        override val icon: Int = R.drawable.ic_filter
    }

    @Serializable
    data object FilterHistory : BottomNavDestination {
        override val label: Int = R.string.filter_title_saved
        override val icon: Int = R.drawable.ic_filter_saved
    }

    companion object {
        val items = listOf(
            TagLibrary,
            PodcastLibrary,
            NowPlaying,
            Filters,
            FilterHistory
        )
    }
}

@Serializable
object AlarmList : ConnectedDestination

@Serializable
object AddAlarm : ConnectedDestination

@Serializable
data class EditAlarm(val alarmId: Long) : ConnectedDestination

@Serializable
data object Playlist : ConnectedDestination

@Serializable
data object Shortcut : ConnectedDestination

@Serializable
data object Server : NavKey

@Serializable
data object Authentication : NavKey

@Serializable
data class TagInfo(val id: Long, val type: FilterType, val title: String) : ConnectedDestination

@Serializable
data class PodcastInfo(val id: Long, val type: FilterType, val title: String) : ConnectedDestination

@Serializable
data class TagList(val type: String, val filterParent: Filter?) : ConnectedDestination

@Serializable
data class PodcastList(val type: String, val filterParent: Filter?) : ConnectedDestination
