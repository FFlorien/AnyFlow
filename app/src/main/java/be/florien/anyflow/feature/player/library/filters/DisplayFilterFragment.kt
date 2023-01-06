package be.florien.anyflow.feature.player.library.filters

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
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
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.databinding.FragmentDisplayFilterBinding
import be.florien.anyflow.databinding.ItemFilterActiveBinding
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.menu.implementation.SaveFilterGroupMenuHolder
import be.florien.anyflow.feature.player.library.BaseFilteringFragment
import be.florien.anyflow.feature.player.library.LibraryActions
import be.florien.anyflow.feature.player.library.saved.SavedFilterGroupFragment
import be.florien.anyflow.feature.player.library.info.LibraryInfoFragment
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.launch

class DisplayFilterFragment : BaseFilteringFragment() {
    override fun getTitle(): String = getString(R.string.menu_filters)
    private val targets: MutableList<Target<Bitmap>> = mutableListOf()
    override val libraryActions: LibraryActions
        get() = viewModel
    lateinit var viewModel: DisplayFilterViewModel

    private lateinit var binding: FragmentDisplayFilterBinding
    private lateinit var filterListAdapter: FilterListAdapter

    private val saveMenuHolder = SaveFilterGroupMenuHolder {
        val editText = EditText(requireActivity()) //todo better "ask a name" dialog
        editText.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
        AlertDialog.Builder(requireActivity())
            .setView(editText)
            .setTitle(R.string.filter_group_name)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                lifecycleScope.launch {
                    libraryActions.saveFilterGroup(editText.text.toString())
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
        )[DisplayFilterViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.resetFilterChanges()
        menuCoordinator.addMenuHolder(saveMenuHolder)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDisplayFilterBinding.inflate(inflater, container, false)
        binding.vm = viewModel
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
        libraryActions.currentFilters.observe(viewLifecycleOwner) {
            filterListAdapter.notifyDataSetChanged()
            saveMenuHolder.isVisible = libraryActions.currentFilters.value?.isNotEmpty() == true
        }
        binding.fabSavedFilterGroups.setOnClickListener {
            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.container,
                    SavedFilterGroupFragment(),
                    SavedFilterGroupFragment::class.java.simpleName
                )
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
                    holder.bind(getString(R.string.action_filter_add), R.drawable.ic_add)
                    holder.itemView.setOnClickListener {
                        requireActivity()
                            .supportFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.container,
                                LibraryInfoFragment(),
                                LibraryInfoFragment::class.java.simpleName
                            )
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
        val binding: ItemFilterActiveBinding = ItemFilterActiveBinding.inflate(
            layoutInflater,
            parent,
            false
        )
    ) : RecyclerView.ViewHolder(binding.root) {

        private val leftIconSize = resources.getDimensionPixelSize(R.dimen.xLargeDimen)
        private val leftActionSize = resources.getDimensionPixelSize(R.dimen.largeDimen)

        fun bind(filter: Filter<*>) {
            val charSequence: CharSequence = when (filter.type) {
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
                Filter.FilterType.PLAYLIST_IS -> getString(
                    R.string.filter_display_playlist_is,
                    filter.displayText
                )
                Filter.FilterType.DOWNLOADED_STATUS_IS -> getString(
                    if (filter.argument as Boolean) R.string.filter_display_is_downloaded
                    else R.string.filter_display_is_not_downloaded
                )
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
            if (filter.type.artType != null && filter.argument is Long) {
                targets.add(
                    GlideApp.with(requireActivity())
                        .asBitmap()
                        .load(viewModel.getUrlForImage(filter.type.artType, filter.argument))
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