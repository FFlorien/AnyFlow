package be.florien.anyflow.feature.song.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.TagType
import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.component.info.InfoRow
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.base.domain.model.ShortcutInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongActionMultipleInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongActionType
import be.florien.anyflow.feature.song.base.domain.model.SongDownloadInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongDownloadMultipleInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.feature.song.base.domain.model.SongInfoContainerRow
import be.florien.anyflow.feature.song.base.domain.model.SongInfoMultipleContainerRow
import be.florien.anyflow.feature.song.base.domain.model.SongInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongMultipleInfoRow
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoFragment
import be.florien.anyflow.feature.song.base.ui.R
import be.florien.anyflow.tags.model.SongInfo

class SongInfoFragment(songId: Long) :
    BaseSongInfoFragment<SongInfoViewModel>(songId = songId) {
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

    override fun BaseSongInfoRow.toInfoRow(): InfoRow {
        return when (this) {
            is ShortcutInfoRow -> InfoRow.ShortcutInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(viewModel.songInfo.getTextFromField(fieldType, -1), null),
                ImageConfig(null, fieldType.iconRes)
            ).apply {
                tag = this@toInfoRow
            }

            is SongDownloadInfoRow -> InfoRow.ProgressInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(viewModel.songInfo.getTextFromField(fieldType, -1), null),
                ImageConfig(null, actionType.iconRes),
            ).apply {
                tag = this@toInfoRow
                progress = this@toInfoRow.progress.map { it.downloaded.toDouble() / it.total }
                secondaryProgress =
                    this@toInfoRow.progress.map { ((it.downloaded + it.queued).toDouble() / it.total) }
            }
            is SongInfoContainerRow -> {
                InfoRow.ContainerInfoRow(
                    actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                    TextConfig(viewModel.songInfo.getTextFromField(fieldType, -1), null),
                    ImageConfig(null, fieldType.iconRes),
                    subRows.map { it.toInfoRow() }
                ).apply {
                    tag = this@toInfoRow
                }

            }

            is SongInfoRow -> when (this.actionType) {
                SongActionType.InfoTitle -> InfoRow.BasicInfoRow(
                    actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                    TextConfig(viewModel.songInfo.getTextFromField(fieldType, -1), null),
                    ImageConfig(null, fieldType.iconRes)
                ).apply {
                    tag = this@toInfoRow
                }

                SongActionType.None -> InfoRow.BasicInfoRow(
                    actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                    TextConfig(null, R.string.info_action_downloaded_description),
                    ImageConfig(null, fieldType.iconRes)
                ).apply {
                    tag = this@toInfoRow
                }

                SongActionType.AddToFilter,
                SongActionType.AddToPlaylist,
                SongActionType.AddNext,
                SongActionType.Download,
                SongActionType.Search -> InfoRow.ActionInfoRow(
                    actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                    TextConfig(
                        viewModel.songInfo.getTextFromField(fieldType, -1),
                        actionType.getText()
                    ),
                    ImageConfig(null, actionType.iconRes)
                ).apply {
                    tag = this@toInfoRow
                }
                else -> throw IllegalArgumentException()
            }
            is SongInfoMultipleContainerRow -> InfoRow.ContainerInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(
                    viewModel.songInfo.getTextFromField(
                        fieldType,
                        (this as? SongMultipleInfoRow)?.index ?: -1
                    ), null
                ),
                ImageConfig(null, fieldType.iconRes),
                subRows.map { it.toInfoRow() }
            ).apply {
                tag = this@toInfoRow
            }

            is SongActionMultipleInfoRow -> when (actionType) {
                else -> InfoRow.ActionInfoRow(
                    actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                    TextConfig(
                        viewModel.songInfo.getTextFromField(
                            fieldType,
                            (this as? SongMultipleInfoRow)?.index ?: -1
                        ), null
                    ),
                    ImageConfig(null, actionType.iconRes)
                ).apply {
                    tag = this@toInfoRow
                }
            }

            is SongDownloadMultipleInfoRow -> InfoRow.ProgressInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(
                    viewModel.songInfo.getTextFromField(
                        fieldType,
                        (this as? SongMultipleInfoRow)?.index ?: -1
                    ), null
                ),
                ImageConfig(null, actionType.iconRes)
            ).apply {
                tag = this@toInfoRow
                progress = this@toInfoRow.progress.map { it.downloaded.toDouble() / it.total }
                secondaryProgress =
                    this@toInfoRow.progress.map { ((it.downloaded + it.queued).toDouble() / it.total) }
            }

            else -> {
                throw IllegalStateException()
            }
        }
    }

    private fun SongFieldType.toTagType() =
        when (this) {//todo copy paste from feature-songlist-ui, make it common
            SongFieldType.Title -> TagType.Title
            SongFieldType.Artist -> TagType.Artist
            SongFieldType.Album -> TagType.Album
            SongFieldType.Disk -> TagType.Disk
            SongFieldType.AlbumArtist -> TagType.AlbumArtist
            SongFieldType.Genre -> TagType.Genre
            SongFieldType.Playlist -> TagType.Playlist
            SongFieldType.Year,
            SongFieldType.Duration,
            SongFieldType.Track -> throw UnsupportedOperationException()
        }

    private fun SongInfo.getTextFromField(field: SongFieldType, index: Int): String {
        return try {
            when (field) {
                SongFieldType.Title -> title
                SongFieldType.Track -> track.toString()
                SongFieldType.Artist -> artistName
                SongFieldType.Album -> albumName
                SongFieldType.Disk -> disk.toString()
                SongFieldType.AlbumArtist -> albumArtistName
                SongFieldType.Genre -> genreNames[index]
                SongFieldType.Playlist -> playlistNames[index]
                SongFieldType.Year -> year.toString()
                SongFieldType.Duration -> timeText
            }
        } catch (exception: Exception) {
            ""
        }
    }

    private fun SongActionType.getText(): Int? {
        return when (this) {
            SongActionType.None,
            SongActionType.InfoTitle,
            SongActionType.ExpandableTitle -> null

            SongActionType.AddToFilter -> R.string.info_action_filter_on
            SongActionType.AddToPlaylist -> R.string.info_action_select_playlist_detail
            SongActionType.AddNext -> R.string.info_action_track_next
            SongActionType.Search -> R.string.info_action_search_on
            SongActionType.Download -> R.string.info_action_download_description
        }
    }

}