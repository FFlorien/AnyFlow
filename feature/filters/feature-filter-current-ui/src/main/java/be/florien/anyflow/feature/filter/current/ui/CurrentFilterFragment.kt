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
import be.florien.anyflow.architecture.di.viewModelFactory
import be.florien.anyflow.common.ui.GlideApp
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.filter.current.ui.databinding.FragmentCurrentFilterBinding
import be.florien.anyflow.feature.filter.current.ui.databinding.ItemFilterActiveBinding
import be.florien.anyflow.feature.library.ui.BaseFilteringFragment
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.currentFiltersForDisplay
import be.florien.anyflow.feature.library.ui.menu.SaveFilterGroupMenuHolder
import be.florien.anyflow.feature.library.ui.saveFilterGroup
import be.florien.anyflow.management.filters.model.Filter
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
        libraryViewModel.currentFiltersForDisplay.observe(viewLifecycleOwner) {
            filterListAdapter.notifyDataSetChanged()
            saveMenuHolder.isVisible = libraryViewModel
                .currentFiltersForDisplay
                .value
                ?.isNotEmpty() == true
        }
        saveMenuHolder.isVisible = libraryViewModel
            .currentFiltersForDisplay
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
                            .currentFiltersForDisplay
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
            return viewModel.currentFiltersForDisplay.value?.size?.takeIf { it > 0 }?.plus(1) ?: 0
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

        fun bind(filter: Filter<*>) {
            val deepestChild = getFiltersText(filter)
            val artType = deepestChild.type.artType
            val argument = deepestChild.argument
            setImage(artType, argument, filter)
            binding.vm = viewModel
            binding.filter = filter
            binding.lifecycleOwner = viewLifecycleOwner
        }

        private fun getFiltersText(filter: Filter<*>): Filter<*> {
            var filterToTransform: Filter<*>? = filter
            var charSequence = ""
            var isFirstLine = true
            var deepestChild = filter
            while (filterToTransform != null) {
                if (!isFirstLine) {
                    charSequence += "<br>"
                }
                charSequence += getFilterText(filterToTransform)
                filterToTransform = filterToTransform.children.firstOrNull()
                if (filterToTransform != null) {
                    deepestChild = filterToTransform
                }
                isFirstLine = false
            }
            binding.filterName.text = Html.fromHtml(charSequence)
            return deepestChild
        }

        private fun getFilterText(filter: Filter<*>) = when (filter.type) {
            Filter.FilterType.GENRE_IS -> getString(
                R.string.filter_display_genre_is,
                filter.displayText
            )

            Filter.FilterType.SONG_IS -> getString(
                R.string.filter_display_song_is,
                filter.displayText
            )

            Filter.FilterType.ARTIST_IS -> getString(
                R.string.filter_display_artist_is,
                filter.displayText
            )

            Filter.FilterType.ALBUM_ARTIST_IS -> getString(
                R.string.filter_display_album_artist_is,
                filter.displayText
            )

            Filter.FilterType.ALBUM_IS -> getString(
                R.string.filter_display_album_is,
                filter.displayText
            )

            Filter.FilterType.DISK_IS -> getString( //todo is not displayed correctly for now because it is a subfilter
                R.string.filter_display_disk_is,
                filter.displayText
            )

            Filter.FilterType.PLAYLIST_IS -> getString(
                R.string.filter_display_playlist_is,
                filter.displayText
            )

            Filter.FilterType.DOWNLOADED_STATUS_IS -> getString(
                if (filter.argument as Boolean) R.string.filter_display_is_downloaded
                else R.string.filter_display_is_not_downloaded
            )

            Filter.FilterType.PODCAST_EPISODE_IS -> getString(
                R.string.filter_display_podcast_episode_is,
                filter.displayText
            )
        }

        private fun setImage(
            artType: String?,
            argument: Any?,
            filter: Filter<*>
        ) {
            if (artType != null && argument is Long) {
                targets.add(
                    GlideApp.with(requireActivity())
                        .asBitmap()
                        .load(viewModel.getUrlForImage(artType, argument))
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onLoadCleared(placeholder: Drawable?) {
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
                when (filter.type) {
                    Filter.FilterType.ALBUM_ARTIST_IS,
                    Filter.FilterType.ARTIST_IS -> setCompoundDrawableFromResources(
                        R.drawable.ic_artist,
                        leftIconSize
                    )

                    Filter.FilterType.GENRE_IS -> setCompoundDrawableFromResources(
                        R.drawable.ic_genre,
                        leftIconSize
                    )

                    Filter.FilterType.ALBUM_IS -> setCompoundDrawableFromResources(
                        R.drawable.ic_album,
                        leftIconSize
                    )

                    Filter.FilterType.DISK_IS -> setCompoundDrawableFromResources(
                        R.drawable.ic_disk,
                        leftIconSize
                    )

                    Filter.FilterType.PLAYLIST_IS -> setCompoundDrawableFromResources(
                        R.drawable.ic_playlist,
                        leftIconSize
                    )

                    Filter.FilterType.DOWNLOADED_STATUS_IS -> setCompoundDrawableFromResources(
                        R.drawable.ic_download,
                        leftIconSize
                    )

                    Filter.FilterType.SONG_IS -> setCompoundDrawableFromResources(
                        R.drawable.ic_song,
                        leftIconSize
                    )

                    Filter.FilterType.PODCAST_EPISODE_IS -> setCompoundDrawableFromResources(
                        R.drawable.ic_podcast_episode,
                        leftIconSize
                    )
                }
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