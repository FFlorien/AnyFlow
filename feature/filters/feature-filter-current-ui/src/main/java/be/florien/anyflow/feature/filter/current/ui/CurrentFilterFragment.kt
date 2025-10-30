package be.florien.anyflow.feature.filter.current.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.filter.current.ui.databinding.FragmentCurrentFilterBinding
import be.florien.anyflow.feature.filter.current.ui.databinding.ItemFilterActiveBinding
import be.florien.anyflow.feature.library.ui.BaseFilteringFragment
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.currentFilters
import be.florien.anyflow.feature.library.ui.menu.SaveFilterGroupMenuHolder
import be.florien.anyflow.feature.library.ui.saveFilterGroup
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.launch

class CurrentFilterFragment : BaseFilteringFragment() {
    override fun getTitle(): String = getString(R.string.menu_filters)
    private val targets: MutableList<Target<Bitmap>> = mutableListOf()
    override val libraryViewModel: LibraryViewModel
        get() = viewModel
    override val navigator: Navigator
        get() = viewModel.navigator
    lateinit var viewModel: CurrentFilterViewModel

    private lateinit var binding: FragmentCurrentFilterBinding
    private lateinit var filterListAdapter: FilterListAdapter

    private val saveMenuHolder = SaveFilterGroupMenuHolder {
        val editText = EditText(requireActivity()) //todo better "ask a name" dialog
        editText.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
        AlertDialog.Builder(requireActivity())
            .setView(editText)
            .setTitle(R.string.filter_group_name)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                lifecycleScope.launch {
                    libraryViewModel.saveFilterGroup(editText.text.toString())
                }
            }
            .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            requireActivity().viewModelFactory
        )[CurrentFilterViewModel::class.java]
        menuCoordinator.addMenuHolder(saveMenuHolder)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCurrentFilterBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        filterListAdapter = FilterListAdapter()
        binding.filterList.layoutManager = LinearLayoutManager(requireContext())
        binding.filterList.adapter = filterListAdapter
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)
            ?.let {
                val dividerItemDecoration = DividerItemDecoration(
                    requireActivity(),
                    DividerItemDecoration.VERTICAL
                ).apply { setDrawable(it) }
                binding
                    .filterList
                    .addItemDecoration(dividerItemDecoration)
            }
        libraryViewModel.currentFilters.observe(viewLifecycleOwner) {
            filterListAdapter.notifyDataSetChanged()
            saveMenuHolder.isVisible = libraryViewModel
                .currentFilters
                .value
                ?.isNotEmpty() == true
        }
        saveMenuHolder.isVisible = libraryViewModel
            .currentFilters
            .value
            ?.isNotEmpty() == true
        ViewCompat.setTranslationZ(binding.root, 1f)
        return binding.root
    }

    override fun onDetach() {
        targets.forEach {
            it.request?.clear()
        }
        menuCoordinator.removeMenuHolder(saveMenuHolder)
        super.onDetach()
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
            return FilterViewHolder(parent)
        }

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            when (position) {
                0 -> {
                    holder.bind(getString(R.string.action_filter_clear), R.drawable.ic_delete)
                    holder.itemView.setOnClickListener { viewModel.clearFilters() }
                }

                else -> {
                    val filter =
                        libraryViewModel
                            .currentFilters
                            .value
                            ?.toList()
                            ?.getOrNull(position - 1)
                            ?: return
                    holder.bind(filter)
                    holder.itemView.setOnClickListener(null)
                }
            }
        }

        override fun getItemCount(): Int {
            return viewModel.currentFilters.value?.size?.takeIf { it > 0 }?.plus(1) ?: 0
        }
    }

    inner class FilterViewHolder(
        parent: ViewGroup,
        val binding: ItemFilterActiveBinding = ItemFilterActiveBinding.inflate(
            layoutInflater,
            parent,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root) {

        private val leftIconSize = resources.getDimensionPixelSize(R.dimen.xLargeDimen)
        private val leftActionSize = resources.getDimensionPixelSize(R.dimen.largeDimen)

        fun bind(filter: Filter) {
            val mainParam = filter.mainParam
            val artType = mainParam.type.artType
            val argument = mainParam.argument
            setImage(artType, argument, mainParam)

            binding.filterName.text =
                Html.fromHtml(filter.joinToString(separator = "<br>") { getFilterText(it) })
            binding.vm = viewModel
            binding.filter = filter
            binding.lifecycleOwner = viewLifecycleOwner
        }

        private fun getFilterText(filterParam: FilterParam<*>) = when (filterParam.type) {
            TagFilterType.GENRE_IS -> getString(
                R.string.filter_display_genre_is,
                filterParam.displayText
            )

            TagFilterType.SONG_IS -> getString(
                R.string.filter_display_song_is,
                filterParam.displayText
            )

            TagFilterType.ARTIST_IS -> getString(
                R.string.filter_display_artist_is,
                filterParam.displayText
            )

            TagFilterType.ALBUM_ARTIST_IS -> getString(
                R.string.filter_display_album_artist_is,
                filterParam.displayText
            )

            TagFilterType.ALBUM_IS -> getString(
                R.string.filter_display_album_is,
                filterParam.displayText
            )

            TagFilterType.DISK_IS -> getString( //todo is not displayed correctly for now because it is a subfilter
                R.string.filter_display_disk_is,
                filterParam.displayText
            )

            TagFilterType.PLAYLIST_IS -> getString(
                R.string.filter_display_playlist_is,
                filterParam.displayText
            )

            TagFilterType.DOWNLOADED_STATUS_IS -> getString(
                if (filterParam.argument as Boolean) R.string.filter_display_is_downloaded
                else R.string.filter_display_is_not_downloaded
            )

            PodcastFilterType.PODCAST_EPISODE_IS -> getString(
                R.string.filter_display_podcast_episode_is,
                filterParam.displayText
            )

            PodcastFilterType.PODCAST_IS -> getString(
                R.string.filter_display_podcast_is,
                filterParam.displayText
            )

            PodcastFilterType.STATE_IS -> getString(
                R.string.filter_display_state_is,
                filterParam.displayText
            )
        }

        private fun setImage(
            artType: String?,
            argument: Any?,
            filterParam: FilterParam<*>
        ) {
            if (artType != null && argument is Long) {
                targets.add(
                    Glide.with(requireActivity())
                        .asBitmap()
                        .load(viewModel.getUrlForImage(artType, argument))
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onLoadCleared(placeholder: Drawable?) {
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                setDefaultDrawable(filterParam)
                            }

                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                if (context == null) {
                                    return
                                }
                                val drawable = BitmapDrawable(resources, resource)
                                drawable.bounds = Rect(0, 0, leftIconSize, leftIconSize)
                                binding.filterName.setCompoundDrawables(
                                    drawable,
                                    null,
                                    null,
                                    null
                                ) //todo verify threading
                            }
                        })
                )
            } else {
                setDefaultDrawable(filterParam)
            }
        }

        private fun setDefaultDrawable(filterParam: FilterParam<*>) {
            when (filterParam.type) {
                TagFilterType.ALBUM_ARTIST_IS,
                TagFilterType.ARTIST_IS -> setCompoundDrawableFromResources(
                    R.drawable.ic_artist,
                    leftIconSize
                )

                TagFilterType.GENRE_IS -> setCompoundDrawableFromResources(
                    R.drawable.ic_genre,
                    leftIconSize
                )

                TagFilterType.ALBUM_IS -> setCompoundDrawableFromResources(
                    R.drawable.ic_album,
                    leftIconSize
                )

                TagFilterType.DISK_IS -> setCompoundDrawableFromResources(
                    R.drawable.ic_disk,
                    leftIconSize
                )

                TagFilterType.PLAYLIST_IS -> setCompoundDrawableFromResources(
                    R.drawable.ic_playlist,
                    leftIconSize
                )

                TagFilterType.DOWNLOADED_STATUS_IS -> setCompoundDrawableFromResources(
                    R.drawable.ic_download,
                    leftIconSize
                )

                TagFilterType.SONG_IS -> setCompoundDrawableFromResources(
                    R.drawable.ic_song,
                    leftIconSize
                )

                PodcastFilterType.PODCAST_EPISODE_IS -> setCompoundDrawableFromResources(
                    R.drawable.ic_podcast_episode,
                    leftIconSize
                )

                PodcastFilterType.PODCAST_IS -> setCompoundDrawableFromResources(
                    R.drawable.ic_podcast,
                    leftIconSize
                )

                PodcastFilterType.STATE_IS -> setCompoundDrawableFromResources(
                    R.drawable.ic_podcast,
                    leftIconSize
                ) //TODO()
            }
        }

        fun bind(text: String, @DrawableRes icon: Int) {
            binding.filter = null
            binding.vm = null
            binding.filterName.text = text
            binding.lifecycleOwner = viewLifecycleOwner
            setCompoundDrawableFromResources(icon, leftActionSize)
        }

        private fun setCompoundDrawableFromResources(resId: Int, size: Int) {
            ResourcesCompat.getDrawable(resources, resId, requireActivity().theme)?.apply {
                val color = ResourcesCompat.getColor(
                    resources,
                    R.color.primaryDark,
                    requireActivity().theme
                )
                colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    color,
                    BlendModeCompat.SRC_IN
                )
                bounds = Rect(0, 0, size, size)
                binding.filterName.setCompoundDrawables(this, null, null, null)
            }
        }
    }
}