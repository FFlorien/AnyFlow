package be.florien.anyflow.common.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import be.florien.anyflow.management.filters.domain.model.Filter
import kotlinx.serialization.Serializable
import be.florien.anyflow.management.filters.domain.model.FilterType


@Serializable
sealed interface TopDestination : NavKey {
    val isMainScreen: Boolean
}

@Serializable
sealed interface BottomNavDestination : TopDestination {
    @get:StringRes
    val label: Int

    @get:DrawableRes
    val icon: Int

    @Serializable
    data object TagLibrary : BottomNavDestination {
        override val label: Int = R.string.menu_library
        override val icon: Int = R.drawable.ic_library
        override val isMainScreen = true
    }

    @Serializable
    data object PodcastLibrary : BottomNavDestination {
        override val label: Int = R.string.menu_podcast
        override val icon: Int = R.drawable.ic_podcast_episode
        override val isMainScreen = true
    }

    @Serializable
    data object NowPlaying : BottomNavDestination {
        override val label: Int = R.string.player_playing_now
        override val icon: Int = R.drawable.ic_play
        override val isMainScreen = true
    }

    @Serializable
    data object Filters : BottomNavDestination {
        override val label: Int = R.string.menu_filters
        override val icon: Int = R.drawable.ic_filter
        override val isMainScreen = true
    }

    @Serializable
    data object FilterHistory : BottomNavDestination {
        override val label: Int = R.string.filter_title_saved
        override val icon: Int = R.drawable.ic_filter_saved
        override val isMainScreen = true
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
data object Alarm : TopDestination {
    override val isMainScreen: Boolean = false
}

@Serializable
data object Playlist : TopDestination {
    override val isMainScreen: Boolean = false
}

@Serializable
data object Server : TopDestination {
    override val isMainScreen: Boolean = false
}

@Serializable
data class TagInfo(val id: Long, val type: FilterType, val title: String) : NavKey

@Serializable
data class PodcastInfo(val id: Long, val type: FilterType, val title: String) : NavKey

@Serializable
data class TagList(val type: String, val filterParent: Filter?) : NavKey

@Serializable
data class PodcastList(val type: String, val filterParent: Filter?) : NavKey
