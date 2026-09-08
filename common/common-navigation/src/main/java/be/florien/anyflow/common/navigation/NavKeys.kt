package be.florien.anyflow.common.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import be.florien.anyflow.management.filters.domain.model.Filter
import kotlinx.serialization.Serializable
import be.florien.anyflow.management.filters.domain.model.FilterType

@Serializable
sealed interface ConnectedDestination: NavKey {

    val isSearchable: Boolean
}

@Immutable
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
        override val isSearchable: Boolean = false
    }

    @Serializable
    data object PodcastLibrary : BottomNavDestination {
        override val label: Int = R.string.menu_podcast
        override val icon: Int = R.drawable.ic_podcast_episode
        override val isSearchable: Boolean = false
    }

    @Serializable
    data object NowPlaying : BottomNavDestination {
        override val label: Int = R.string.player_playing_now
        override val icon: Int = R.drawable.ic_play
        override val isSearchable: Boolean = true
    }

    @Serializable
    data object Filters : BottomNavDestination {
        override val label: Int = R.string.menu_filters
        override val icon: Int = R.drawable.ic_filter
        override val isSearchable: Boolean = false
    }

    @Serializable
    data object FilterHistory : BottomNavDestination {
        override val label: Int = R.string.filter_title_saved
        override val icon: Int = R.drawable.ic_filter_saved
        override val isSearchable: Boolean = false
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
object AlarmList : ConnectedDestination {
    override val isSearchable: Boolean = false
}

@Serializable
object AddAlarm : ConnectedDestination {
    override val isSearchable: Boolean = false
}

@Serializable
data class EditAlarm(val alarmId: Long) : ConnectedDestination {
    override val isSearchable: Boolean = false
}

@Serializable
data object Playlist : ConnectedDestination {
    override val isSearchable: Boolean = true
}

@Serializable
data object Shortcut : ConnectedDestination {
    override val isSearchable: Boolean = false
}

@Serializable
data object Server : NavKey

@Serializable
data object Authentication : NavKey

@Serializable
data class TagInfo(val id: Long, val type: FilterType, val title: String) : ConnectedDestination {
    override val isSearchable: Boolean = false
}

@Serializable
data class PodcastInfo(val id: Long, val type: FilterType, val title: String) : ConnectedDestination {
    override val isSearchable: Boolean = false
}

@Serializable
data class TagList(val type: String, val filterParent: Filter?) : ConnectedDestination {
    override val isSearchable: Boolean = true
}

@Serializable
data class PodcastList(val type: String, val filterParent: Filter?) : ConnectedDestination {
    override val isSearchable: Boolean = true
}

@Serializable
data class Image(val model: String) : ConnectedDestination {
    override val isSearchable: Boolean = false
}
