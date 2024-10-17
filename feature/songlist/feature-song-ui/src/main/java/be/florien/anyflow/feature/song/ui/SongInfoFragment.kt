package be.florien.anyflow.feature.song.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.ui.data.TagType
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoActions
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoFragment
import be.florien.anyflow.feature.song.domain.SongInfoActions

class SongInfoFragment(songId: Long): BaseSongInfoFragment<SongInfoActions, SongInfoViewModel>(songId = songId) {
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
                    navigator.displayPlaylistSelection(childFragmentManager, it.id, it.type.toTagType(), it.secondId ?: -1)
                }
            }
        }
    }

    private fun BaseSongInfoActions.SongFieldType.toTagType() = when (this) {//todo copy paste from feature-songlist-ui, make it common
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