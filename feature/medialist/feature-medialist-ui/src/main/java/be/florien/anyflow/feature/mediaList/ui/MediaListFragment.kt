package be.florien.anyflow.feature.mediaList.ui

import android.content.ComponentName
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.paging.compose.collectAsLazyPagingItems
import be.florien.anyflow.common.base.BaseFragment
import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.library.ui.info.LibraryInfoFragment
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel
import be.florien.anyflow.feature.mediaList.ui.databinding.FragmentMediaListBinding
import be.florien.anyflow.feature.player.service.PlayerService
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import com.google.common.util.concurrent.MoreExecutors


/**
 * Display a list of media and play it upon selection.
 */
@ActivityScope
class MediaListFragment : BaseFragment(), DialogInterface.OnDismissListener {
    override fun getTitle(): String = getString(R.string.player_playing_now)

    lateinit var viewModel: MediaListViewModel

    private lateinit var binding: FragmentMediaListBinding

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(
            requireContext(),
            ComponentName(requireContext(), PlayerService::class.java)
        )
        val mediaController = MediaController.Builder(requireContext(), sessionToken).buildAsync()
        mediaController.addListener({
            viewModel.player = mediaController.get()
        }, MoreExecutors.directExecutor())

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(
                this,
                (requireActivity() as ViewModelFactoryProvider).viewModelFactory
            )[MediaListViewModel::class.java]
        binding = FragmentMediaListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songList.setContent {
            AppTheme(authenticationInterceptor = viewModel.authenticationInterceptor) {
                val state =
                    viewModel.stateFlow.collectAsState(MediaListViewModel.State(null, 0, 0)).value
                val pagingList = state.mediaList?.collectAsLazyPagingItems()
                MediaList(
                    items = pagingList,
                    selectedPosition = state.mediaPosition,
                    selectedChapterTime = state.chapterTime,
                    onMediaItemClick = viewModel::goToMedia,
                    onChapterItemClick = viewModel::goToTime,
                    onItemNavigation = {
                        val filter = when (it) {
                            is MediaItemData.Full.Song -> Filter(FilterParam(TagFilterType.SONG_IS, it.id, it.title))
                            is MediaItemData.Full.PodcastEpisode -> Filter(FilterParam(
                                PodcastFilterType.PODCAST_EPISODE_IS, it.id, it.title))
                        }
                        val type = if (filter.mainParam.type is TagFilterType) {
                            LibraryInfoViewModel.TAGS_TYPE
                        } else {
                            LibraryInfoViewModel.PODCAST_TYPE
                        }
                        viewModel.navigator.displayFragmentOnMain(
                            requireContext(),
                            LibraryInfoFragment(type, filter),
                            type,
                            LibraryInfoFragment::class.java.simpleName
                        )
                    }
                )
            }
        }
        viewModel.playlistListDisplayedFor.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.navigator.displayPlaylistSelection(
                    childFragmentManager,
                    it.first,
                    it.second.toTagType(),
                    it.third
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSongs()
        viewModel.refreshShortcuts()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        viewModel.clearPlaylistDisplay()
    }
}