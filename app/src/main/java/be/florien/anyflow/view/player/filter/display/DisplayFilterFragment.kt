package be.florien.anyflow.view.player.filter.display

import android.content.Context
import androidx.databinding.Observable
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentDisplayFilterBinding
import be.florien.anyflow.databinding.ItemFilterActiveBinding
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterFragment
import be.florien.anyflow.view.player.filter.BaseFilterVM
import be.florien.anyflow.view.player.filter.selectType.SelectFilterTypeFragment
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class DisplayFilterFragment : BaseFilterFragment() {
    override fun getTitle(): String = getString(R.string.menu_filters)
    override val baseVm: BaseFilterVM
        get() = vm
    lateinit var vm: DisplayFilterFragmentVM

    private lateinit var binding: FragmentDisplayFilterBinding
    private val filterListAdapter = FilterListAdapter()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        vm = DisplayFilterFragmentVM(requireActivity())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as PlayerActivity).activityComponent.inject(this)
        vm.resetFilterChanges()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDisplayFilterBinding.inflate(inflater, container, false)
        binding.vm = vm
        binding.filterList.layoutManager = LinearLayoutManager(requireContext())
        binding.filterList.adapter = filterListAdapter
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)?.let {
            binding.filterList.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL).apply { setDrawable(it) })
        }
        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if (propertyId == BR.currentFilters) {
                    filterListAdapter.notifyDataSetChanged()
                }
            }

        })
        binding.fabAdd.setOnClickListener {
            requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, SelectFilterTypeFragment(), SelectFilterTypeFragment::class.java.simpleName)
                    .addToBackStack(null)
                    .commit()
        }
        ViewCompat.setTranslationZ(binding.root, 1f)
        return binding.root
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder = FilterViewHolder(parent)

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            when (position) {
                0 -> {
                    holder.bind(getString(R.string.action_filter_clear), R.drawable.ic_delete)
                    holder.itemView.setOnClickListener { vm.clearFilters() }
                }
                else -> {
                    holder.bind(vm.currentFilters[position - 1])
                    holder.itemView.setOnClickListener(null)
                }
            }
        }

        override fun getItemCount(): Int = vm.currentFilters.size + (if (vm.currentFilters.size > 0) 1 else 0)
    }

    inner class FilterViewHolder(
            parent: ViewGroup,
            val binding: ItemFilterActiveBinding = ItemFilterActiveBinding.inflate(layoutInflater, parent, false))
        : RecyclerView.ViewHolder(binding.root) {

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
            }
            val valueStart = charSequence.lastIndexOf(filter.displayText)
            val stylizedText = SpannableString(charSequence)
            stylizedText.setSpan(StyleSpan(android.graphics.Typeface.BOLD), valueStart, charSequence.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            binding.filterName.text = stylizedText
            binding.vm = vm
            binding.filter = filter
            if (filter.displayImage != null) {
                GlideApp.with(requireActivity())
                        .asBitmap()
                        .load(filter.displayImage)
                        .listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                                return true
                            }

                            override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                val drawable = BitmapDrawable(resources, resource)
                                drawable.bounds = Rect(0, 0, leftDrawableSize, leftDrawableSize)
                                binding.filterName.setCompoundDrawables(drawable, null, null, null)
                                return true
                            }
                        })
                        .submit()
            } else {
                when (filter) {
                    is Filter.AlbumArtistIs,
                    is Filter.ArtistIs -> setCompoundDrawableFromResources(R.drawable.ic_artist)
                    is Filter.GenreIs -> setCompoundDrawableFromResources(R.drawable.ic_genre)
                    is Filter.AlbumIs -> setCompoundDrawableFromResources(R.drawable.ic_album)
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
            setCompoundDrawableFromResources(icon)
        }

        private fun setCompoundDrawableFromResources(resId: Int) {
            ResourcesCompat.getDrawable(resources, resId, requireActivity().theme)?.apply {
                setColorFilter(ResourcesCompat.getColor(resources, R.color.primaryDark, requireActivity().theme), PorterDuff.Mode.SRC_IN)
                bounds = Rect(0, 0, leftDrawableSize, leftDrawableSize)
                binding.filterName.setCompoundDrawables(this, null, null, null)
            }
        }
    }
}