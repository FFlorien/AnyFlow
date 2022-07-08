package be.florien.anyflow.feature.player.filter.display

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.databinding.FragmentDisplayFilterBinding
import be.florien.anyflow.databinding.ItemFilterActiveBinding
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.player.filter.BaseFilterFragment
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.feature.player.filter.saved.SavedFilterGroupFragment
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeFragment
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition

class DisplayFilterFragment : BaseFilterFragment() {
    override fun getTitle(): String = getString(R.string.menu_filters)
    private val targets: MutableList<Target<Bitmap>> = mutableListOf()
    override val baseViewModel: BaseFilterViewModel
        get() = viewModel
    lateinit var viewModel: DisplayFilterViewModel

    private lateinit var binding: FragmentDisplayFilterBinding
    private lateinit var filterListAdapter: FilterListAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(this, requireActivity().viewModelFactory).get(DisplayFilterViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.resetFilterChanges()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDisplayFilterBinding.inflate(inflater, container, false)
        binding.vm = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        filterListAdapter = FilterListAdapter()
        binding.filterList.layoutManager = LinearLayoutManager(requireContext())
        binding.filterList.adapter = filterListAdapter
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)?.let {
            binding
                .filterList
                .addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL).apply { setDrawable(it) })
        }
        baseViewModel.currentFilters.observe(viewLifecycleOwner) {
            filterListAdapter.notifyDataSetChanged()
            saveMenuHolder.isVisible = baseViewModel.currentFilters.value?.isNotEmpty() == true
        }
        binding.fabSavedFilterGroups.setOnClickListener {
            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, SavedFilterGroupFragment(), SavedFilterGroupFragment::class.java.simpleName)
                .addToBackStack(null)
                .commit()
        }
        saveMenuHolder.isVisible = viewModel.currentFilters.value?.isNotEmpty() == true
        ViewCompat.setTranslationZ(binding.root, 1f)
        return binding.root
    }

    override fun onDetach() {
        targets.forEach {
            it.request?.clear()
        }
        viewModel.cancelChanges()
        super.onDetach()
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
            return FilterViewHolder(parent)
        }

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            when (position) {
                0 -> {
                    holder.bind(getString(R.string.action_filter_add), R.drawable.ic_add)
                    holder.itemView.setOnClickListener {
                        requireActivity()
                            .supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.container, SelectFilterTypeFragment(), SelectFilterTypeFragment::class.java.simpleName)
                            .addToBackStack(null)
                            .commit()
                    }
                }
                1 -> {
                    holder.bind(getString(R.string.action_filter_clear), R.drawable.ic_delete)
                    holder.itemView.setOnClickListener { viewModel.clearFilters() }
                }
                else -> {
                    val filter = viewModel.currentFilters.value?.getOrNull(position - 2) ?: return
                    holder.bind(filter)
                    holder.itemView.setOnClickListener(null)
                }
            }
        }

        override fun getItemCount(): Int {
            val value = viewModel.currentFilters.value ?: return 1
            return value.size + (if (value.isNotEmpty()) 2 else 1)
        }
    }

    inner class FilterViewHolder(
        parent: ViewGroup,
        val binding: ItemFilterActiveBinding = ItemFilterActiveBinding.inflate(layoutInflater, parent, false)
    ) : RecyclerView.ViewHolder(binding.root) {

        private val leftDrawableSize = resources.getDimensionPixelSize(R.dimen.xLargeDimen)

        fun bind(filter: Filter<*>) {
            val charSequence: CharSequence = when (filter) {
                is Filter.TitleIs -> getString(R.string.filter_display_title_is, filter.displayText)
                is Filter.TitleContain -> getString(R.string.filter_display_title_contain, filter.displayText)
                is Filter.GenreIs -> getString(R.string.filter_display_genre_is, filter.displayText)
                is Filter.Search -> getString(R.string.filter_display_search, filter.displayText)
                is Filter.SongIs -> getString(R.string.filter_display_song_is, filter.displayText)
                is Filter.ArtistIs -> getString(R.string.filter_display_artist_is, filter.displayText)
                is Filter.AlbumArtistIs -> getString(R.string.filter_display_album_artist_is, filter.displayText)
                is Filter.AlbumIs -> getString(R.string.filter_display_album_is, filter.displayText)
                is Filter.PlaylistIs -> getString(R.string.filter_display_playlist_is, filter.displayText)
                is Filter.DownloadedStatusIs -> getString(if (filter.argument) R.string.filter_display_is_downloaded else R.string.filter_display_is_not_downloaded)
            }
            if (filter.displayText.isNotBlank() && charSequence.contains(filter.displayText)) {
                val valueStart = charSequence.lastIndexOf(filter.displayText)
                val stylizedText = SpannableString(charSequence)
                stylizedText.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD),
                        valueStart,
                        charSequence.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                binding.filterName.text = stylizedText
            } else {
                binding.filterName.text = charSequence
            }
            binding.vm = viewModel
            binding.filter = filter
            binding.lifecycleOwner = viewLifecycleOwner
            if (filter.displayImage != null) {
                targets.add(
                    GlideApp.with(requireActivity())
                        .asBitmap()
                        .load(filter.displayImage)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onLoadCleared(placeholder: Drawable?) {
                            }

                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                if (context == null) {
                                    return
                                }
                                val drawable = BitmapDrawable(resources, resource)
                                drawable.bounds = Rect(0, 0, leftDrawableSize, leftDrawableSize)
                                binding.filterName.setCompoundDrawables(drawable, null, null, null) //todo verify threading
                            }
                        })
                )
            } else {
                when (filter) {
                    is Filter.AlbumArtistIs,
                    is Filter.ArtistIs -> setCompoundDrawableFromResources(R.drawable.ic_artist)
                    is Filter.GenreIs -> setCompoundDrawableFromResources(R.drawable.ic_genre)
                    is Filter.AlbumIs -> setCompoundDrawableFromResources(R.drawable.ic_album)
                    is Filter.PlaylistIs -> setCompoundDrawableFromResources(R.drawable.ic_playlist)
                    is Filter.DownloadedStatusIs -> setCompoundDrawableFromResources(R.drawable.ic_download)
                    is Filter.Search,
                    is Filter.TitleIs,
                    is Filter.TitleContain,
                    is Filter.SongIs -> setCompoundDrawableFromResources(R.drawable.ic_song)
                }
            }
        }

        fun bind(text: String, @DrawableRes icon: Int) {
            binding.filter = null
            binding.vm = null
            binding.filterName.text = text
            binding.lifecycleOwner = viewLifecycleOwner
            setCompoundDrawableFromResources(icon)
        }

        private fun setCompoundDrawableFromResources(resId: Int) {
            ResourcesCompat.getDrawable(resources, resId, requireActivity().theme)?.apply {
                val color = ResourcesCompat.getColor(resources, R.color.primaryDark, requireActivity().theme)
                colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
                bounds = Rect(0, 0, leftDrawableSize, leftDrawableSize)
                binding.filterName.setCompoundDrawables(this, null, null, null)
            }
        }
    }
}