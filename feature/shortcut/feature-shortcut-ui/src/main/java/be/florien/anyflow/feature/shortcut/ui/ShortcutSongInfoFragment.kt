package be.florien.anyflow.feature.shortcut.ui

import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.ui.getDisplayWidth
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoActions
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoFragment
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.model.SongInfo

class ShortcutSongInfoFragment: BaseSongInfoFragment<ShortcutSongInfoActions, ShortcutsViewModel>() {
    override fun getSongViewModel(): ShortcutsViewModel =
        ViewModelProvider(
            this,
            (activity as ViewModelFactoryProvider).viewModelFactory
        )[ShortcutsViewModel::class.java]
            .apply {
                val width = requireActivity().getDisplayWidth()
                val itemWidth = resources.getDimensionPixelSize(R.dimen.minClickableSize)
                val margin = resources.getDimensionPixelSize(R.dimen.smallDimen)
                val itemFullWidth = itemWidth + margin + margin
                maxItems = (width / itemFullWidth) - 1
                val title = getString(R.string.dummy_title)
                val artistName = getString(R.string.dummy_artist)
                val albumName = getString(R.string.dummy_album)
                val time = 120
                dummySongInfo =
                    SongInfo(
                        BaseSongInfoActions.DUMMY_SONG_ID,
                        title,
                        artistName,
                        0L,
                        albumName,
                        0L,
                        1,
                        artistName,
                        0L,
                        listOf(getString(R.string.dummy_genre)),
                        listOf(0L),
                        listOf(getString(R.string.dummy_playlist)),
                        listOf(0L),
                        1,
                        time,
                        2000,
                        0,
                        null
                    )
                dummySongDisplay = SongDisplay(
                    BaseSongInfoActions.DUMMY_SONG_ID,
                    title,
                    artistName,
                    albumName,
                    0L,
                    time
                )
            }

}