package be.florien.anyflow.feature.song.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.TagType
import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.info.InfoRow
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoActions
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoFragment
import be.florien.anyflow.feature.song.domain.SongInfoActions

class SongInfoFragment(songId: Long) :
    BaseSongInfoFragment<SongInfoActions, SongInfoViewModel>(songId = songId) {
    override fun getSongViewModel(): SongInfoViewModel =
        ViewModelProvider(
            this,
            (activity as ViewModelFactoryProvider).viewModelFactory
        )[SongInfoViewModel::class.java]

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.apply {
//            if (songListViewModel != null) {
//                searchTerm.observe(viewLifecycleOwner) {
//                    if (it != null) {
//                        songListViewModel?.isSearching?.value = true
//                        songListViewModel?.searchedText?.value = it
//                        dismiss()
//                    }
//                }
//            }
            isPlaylistListDisplayed.observe(viewLifecycleOwner) {
                if (it != null) {
                    navigator.displayPlaylistSelection(
                        childFragmentManager,
                        it.id,
                        it.type.toTagType(),
                        it.secondId ?: -1
                    )
                }
            }
        }
    }

    override fun InfoActions.InfoRow.toInfoRow(): InfoRow {
        if (this !is BaseSongInfoActions.InfoRow) {
            throw IllegalStateException()
        }

        return when (this) {
            is BaseSongInfoActions.ShortcutInfoRow -> InfoRow.ShortcutInfoRow(
                title,
                TextConfig(text, textRes),
                ImageConfig(imageUrl, fieldType.iconRes),
                this
            )

            is BaseSongInfoActions.SongDownloadInfoRow -> InfoRow.ProgressInfoRow(
                title,
                TextConfig(text, textRes),
                ImageConfig(imageUrl, actionType.iconRes),
                this,
                progress.map { it.downloaded.toDouble() / it.total },
                progress.map { ((it.downloaded + it.queued).toDouble() / it.total) }
            )

            is BaseSongInfoActions.SongInfoRow -> when (this.actionType) {
                BaseSongInfoActions.SongActionType.InfoTitle,
                BaseSongInfoActions.SongActionType.None -> InfoRow.BasicInfoRow(
                    title,
                    TextConfig(text, textRes),
                    ImageConfig(imageUrl, fieldType.iconRes),
                    this
                )

                BaseSongInfoActions.SongActionType.ExpandableTitle -> InfoRow.ContainerInfoRow(
                    title,
                    TextConfig(text, textRes),
                    ImageConfig(imageUrl, fieldType.iconRes),
                    this,
                    viewModel.getActionsRowsFor(this).map { it.toInfoRow() }

                )

                BaseSongInfoActions.SongActionType.AddToFilter,
                BaseSongInfoActions.SongActionType.AddToPlaylist,
                BaseSongInfoActions.SongActionType.AddNext,
                BaseSongInfoActions.SongActionType.Download,
                BaseSongInfoActions.SongActionType.Search -> InfoRow.ActionInfoRow(
                    title,
                    TextConfig(text, textRes),
                    ImageConfig(imageUrl, actionType.iconRes),
                    this
                )
            }

            is BaseSongInfoActions.SongActionMultipleInfoRow ->when(actionType) {
                BaseSongInfoActions.SongActionType.ExpandableTitle -> InfoRow.ContainerInfoRow(
                    title,
                    TextConfig(text, textRes),
                    ImageConfig(imageUrl, fieldType.iconRes),
                    this,
                    viewModel.getActionsRowsFor(this).map { it.toInfoRow() }
                )
                else ->InfoRow.ActionInfoRow(
                    title,
                    TextConfig(text, textRes),
                    ImageConfig(imageUrl, actionType.iconRes),
                    this
                )
            }

            is BaseSongInfoActions.SongDownloadMultipleInfoRow -> InfoRow.ProgressInfoRow(
                title,
                TextConfig(text, textRes),
                ImageConfig(imageUrl, actionType.iconRes),
                this,
                progress.map { it.downloaded.toDouble() / it.total },
                progress.map { ((it.downloaded + it.queued).toDouble() / it.total) }
            )

            else -> {
                throw IllegalStateException()
            }
        }
    }

    private fun BaseSongInfoActions.SongFieldType.toTagType() =
        when (this) {//todo copy paste from feature-songlist-ui, make it common
            BaseSongInfoActions.SongFieldType.Title -> TagType.Title
            BaseSongInfoActions.SongFieldType.Artist -> TagType.Artist
            BaseSongInfoActions.SongFieldType.Album -> TagType.Album
            BaseSongInfoActions.SongFieldType.Disk -> TagType.Disk
            BaseSongInfoActions.SongFieldType.AlbumArtist -> TagType.AlbumArtist
            BaseSongInfoActions.SongFieldType.Genre -> TagType.Genre
            BaseSongInfoActions.SongFieldType.Playlist -> TagType.Playlist
            BaseSongInfoActions.SongFieldType.Year,
            BaseSongInfoActions.SongFieldType.Duration,
            BaseSongInfoActions.SongFieldType.PodcastEpisode,
            BaseSongInfoActions.SongFieldType.Track -> throw UnsupportedOperationException()
        }

}