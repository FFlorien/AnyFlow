package be.florien.anyflow.feature.player.ui.library.list

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.paging.filter
import androidx.recyclerview.widget.*
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.databinding.FragmentSelectFilterBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.menu.implementation.SearchMenuHolder
import be.florien.anyflow.feature.menu.implementation.SelectAllMenuHolder
import be.florien.anyflow.feature.menu.implementation.SelectNoneMenuHolder
import be.florien.anyflow.feature.player.ui.PlayerActivity
import be.florien.anyflow.feature.player.ui.details.DetailViewHolderListener
import be.florien.anyflow.feature.player.ui.details.ItemInfoTouchAdapter
import be.florien.anyflow.feature.player.ui.library.BaseFilteringFragment
import be.florien.anyflow.feature.player.ui.library.LibraryViewModel
import be.florien.anyflow.feature.player.ui.library.info.LibraryInfoFragment
import be.florien.anyflow.feature.player.ui.library.info.LibraryInfoViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryAlbumArtistListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryAlbumListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryArtistListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryDownloadedListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryGenreListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryPlaylistListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibraryPodcastEpisodeListViewModel
import be.florien.anyflow.feature.player.ui.library.list.viewmodels.LibrarySongListViewModel
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.injection.ServerScope
import com.google.android.material.snackbar.Snackbar

@ActivityScope
@ServerScope
class LibraryListFragment @SuppressLint("ValidFragment")
constructor(
    private var filterType: String = LibraryInfoViewModel.GENRE_ID,
    private var parentFilter: Filter<*>? = null
) :
    BaseFilteringFragment(),
    DetailViewHolderListener<LibraryListViewModel.FilterItem> {

    companion object {
        private const val FILTER_TYPE = "TYPE"
        private const val PARENT_FILTER = "PARENT_FILTER"
    }

    override val libraryViewModel: LibraryViewModel
        get() = viewModel
    lateinit var viewModel: LibraryListViewModel
    private lateinit var fragmentBinding: FragmentSelectFilterBinding
    private val searchMenuHolder by lazy {
        SearchMenuHolder(viewModel.isSearching.value == true, requireContext()) {
            val currentValue = viewModel.isSearching.value ?: false
            viewModel.isSearching.value = !currentValue
        }
    }
    private val selectAllMenuHolder by lazy {
        SelectAllMenuHolder {
            viewModel.selectAllInSelection()
        }
    }
    private val selectNoneMenuHolder by lazy {
        SelectNoneMenuHolder {
            viewModel.selectNoneInSelection()
        }
    }

    override fun getTitle(): String = getString(R.string.library_title_main)

    override fun getSubtitle(): String? = when (filterType) {
        LibraryInfoViewModel.ALBUM_ID -> getString(R.string.library_type_album)
        LibraryInfoViewModel.ALBUM_ARTIST_ID -> getString(R.string.library_type_album_artist)
        LibraryInfoViewModel.ARTIST_ID -> getString(R.string.library_type_artist)
        LibraryInfoViewModel.GENRE_ID -> getString(R.string.library_type_genre)
        LibraryInfoViewModel.SONG_ID -> getString(R.string.library_type_song)
        LibraryInfoViewModel.PLAYLIST_ID -> getString(R.string.library_type_playlist)
        LibraryInfoViewModel.DOWNLOAD_ID -> getString(R.string.library_type_download)
        LibraryInfoViewModel.PODCAST_EPISODE_ID -> getString(R.string.library_type_podcast_episode)
        else -> null
    }

    init {
        arguments?.let {
            filterType = it.getString(FILTER_TYPE, LibraryInfoViewModel.GENRE_ID)
            parentFilter = it.getParcelable(PARENT_FILTER)
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putString(FILTER_TYPE, filterType)
                putParcelable(PARENT_FILTER, parentFilter)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuCoordinator.addMenuHolder(searchMenuHolder)
        menuCoordinator.addMenuHolder(selectAllMenuHolder)
        menuCoordinator.addMenuHolder(selectNoneMenuHolder)
        viewModel.isSearching.observe(this) {
            searchMenuHolder.changeState(!it)
            val imm: InputMethodManager? =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (it) {
                Handler(Looper.getMainLooper()).postDelayed({
                    fragmentBinding.search.requestFocus()
                    imm?.showSoftInput(fragmentBinding.search, InputMethodManager.SHOW_IMPLICIT)
                }, 200)
            } else {
                imm?.hideSoftInputFromWindow(fragmentBinding.root.windowToken, 0)
            }
        }
        viewModel.hasFilterOfThisType.observe(this) { isAnythingSelected ->
            selectAllMenuHolder.isVisible = !isAnythingSelected
            selectNoneMenuHolder.isVisible = isAnythingSelected
        }
        viewModel.errorMessage.observe(this) {
            if (it > 0) {
                Snackbar.make(fragmentBinding.filterList, it, Snackbar.LENGTH_SHORT).show()
                viewModel.errorMessage.value = -1
            }
        }
        searchMenuHolder.isVisible = viewModel.hasSearch
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(this, requireActivity().viewModelFactory)[
            when (filterType) {
                LibraryInfoViewModel.ALBUM_ID -> LibraryAlbumListViewModel::class.java
                LibraryInfoViewModel.ALBUM_ARTIST_ID -> LibraryAlbumArtistListViewModel::class.java
                LibraryInfoViewModel.ARTIST_ID -> LibraryArtistListViewModel::class.java
                LibraryInfoViewModel.GENRE_ID -> LibraryGenreListViewModel::class.java
                LibraryInfoViewModel.SONG_ID -> LibrarySongListViewModel::class.java
                LibraryInfoViewModel.DOWNLOAD_ID -> LibraryDownloadedListViewModel::class.java
                LibraryInfoViewModel.PODCAST_EPISODE_ID -> LibraryPodcastEpisodeListViewModel::class.java
                else -> LibraryPlaylistListViewModel::class.java
            }]
        viewModel.navigationFilter = parentFilter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentSelectFilterBinding.inflate(inflater, container, false)
        fragmentBinding.lifecycleOwner = viewLifecycleOwner
        fragmentBinding.viewModel = viewModel
        fragmentBinding.filterList.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        fragmentBinding.filterList.adapter = FilterListAdapter(
            viewModel::hasFilter,
            viewModel::toggleFilterSelection,
            this
        )
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)
            ?.let {
                fragmentBinding.filterList.addItemDecoration(
                    DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL)
                        .apply { setDrawable(it) })
            }
        viewModel.values.observe(viewLifecycleOwner) { pagingData ->
            val pagingDataNew = pagingData.filter {
                !viewModel.shouldFilterOut(it)
            }
            (fragmentBinding.filterList.adapter as FilterListAdapter).submitData(
                lifecycle,
                pagingDataNew
            )
            fragmentBinding.filterList.addOnItemTouchListener(object : ItemInfoTouchAdapter(),
                RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    return onInterceptTouchEvent(e)
                }

                override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
                    val childView = rv.findChildViewUnder(downTouchX, downTouchY) ?: return
                    val viewHolder =
                        (rv.findContainingViewHolder(childView) as? FilterViewHolder) ?: return
                    if (!onTouch(viewHolder, event)) {
                        viewHolder.swipeToClose()
                    }
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                }
            })
        }
        return fragmentBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        menuCoordinator.removeMenuHolder(searchMenuHolder)
        menuCoordinator.removeMenuHolder(selectAllMenuHolder)
        menuCoordinator.removeMenuHolder(selectNoneMenuHolder)
    }

    override fun onInfoDisplayAsked(item: LibraryListViewModel.FilterItem) {
        val filter = viewModel.getFilter(item)
        (activity as PlayerActivity).supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.container,
                LibraryInfoFragment(filter),
                LibraryInfoFragment::class.java.simpleName
            )
            .addToBackStack(null)
            .commit()
    }
}
