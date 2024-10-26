package be.florien.anyflow.feature.shortcut.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.common.ui.getDisplayWidth
import be.florien.anyflow.component.info.InfoRow
import be.florien.anyflow.feature.song.base.domain.BaseSongInfoActions.Companion.DUMMY_SONG_ID
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.base.domain.model.ShortcutInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongActionMultipleInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongDownloadInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongDownloadMultipleInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongInfoRow
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoFragment
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.model.SongInfo

class ShortcutSongInfoFragment :
    BaseSongInfoFragment<ShortcutsViewModel>() {
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
                        DUMMY_SONG_ID,
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
                    DUMMY_SONG_ID,
                    title,
                    artistName,
                    albumName,
                    0L,
                    time
                )
            }

    override fun BaseSongInfoRow.toInfoRow(): InfoRow {
        return when (this) {
            is ShortcutInfoRow -> InfoRow.ShortcutInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(null, null),
                ImageConfig(null, fieldType.iconRes)
            ).apply {
                tag = this@toInfoRow
            }

            is SongDownloadInfoRow -> InfoRow.ProgressInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(null, null),
                ImageConfig(null, fieldType.iconRes),
            ).apply {
                tag = this@toInfoRow
                progress = this@toInfoRow.progress.map { it.downloaded.toDouble() / it.total }
                secondaryProgress = this@toInfoRow.progress.map { ((it.downloaded + it.queued).toDouble() / it.total) }
            }

            is SongInfoRow -> InfoRow.NavigationInfoRow(//todo
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(null, null),
                ImageConfig(null, fieldType.iconRes)
            ).apply {
                tag = this@toInfoRow
            }

            is SongActionMultipleInfoRow -> InfoRow.ActionInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(null, null),
                ImageConfig(null, fieldType.iconRes)
            ).apply {
                tag = this@toInfoRow
            }

            is SongDownloadMultipleInfoRow -> InfoRow.ProgressInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(null, null),
                ImageConfig(null, fieldType.iconRes)
            ).apply {
                tag = this@toInfoRow
                progress = this@toInfoRow.progress.map { it.downloaded.toDouble() / it.total }
                secondaryProgress = this@toInfoRow.progress.map { ((it.downloaded + it.queued).toDouble() / it.total) }
            }

            else -> {
                throw IllegalStateException()
            }
        }
    }
}