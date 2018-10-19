package be.florien.anyflow.view.player.filter.display

import android.databinding.Observable
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.*
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentFilterBinding
import be.florien.anyflow.databinding.ItemFilterActiveBinding
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.menu.ConfirmMenuHolder
import be.florien.anyflow.view.menu.MenuCoordinator
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.selectType.AddFilterTypeFragment
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import javax.inject.Inject

class FilterFragment : Fragment() {
    @Inject
    lateinit var vm: FilterFragmentVM

    private lateinit var binding: FragmentFilterBinding
    private val filterListAdapter = FilterListAdapter()
    private val menuCoordinator = MenuCoordinator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as PlayerActivity).activityComponent.inject(this)
        setHasOptionsMenu(true)
        menuCoordinator.addMenuHolder(ConfirmMenuHolder {
            //todo
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFilterBinding.inflate(inflater, container, false)
        binding.vm = vm
        binding.filterList.layoutManager = LinearLayoutManager(requireContext())
        binding.filterList.adapter = filterListAdapter
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
                    .replace(R.id.container, AddFilterTypeFragment(), AddFilterTypeFragment::class.java.simpleName)
                    .addToBackStack(null)
                    .commit()
        }
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menuCoordinator.inflateMenus(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menuCoordinator.prepareMenus(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return menuCoordinator.handleMenuClick(item.itemId)
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder = FilterViewHolder(parent)

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            when (position) {
                0 -> {
                    holder.bind(getString(R.string.action_filter_clear), R.drawable.ic_clear)
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

        private val leftDrawableSize = resources.getDimensionPixelSize(R.dimen.x_large_dimen)
        private val paddingSize = resources.getDimensionPixelSize(R.dimen.list_item_padding)

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
            if (filter.displayImage != null) {
                binding.filterName.setPadding(paddingSize, paddingSize, paddingSize, paddingSize)
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
                binding.filterName.setPadding(leftDrawableSize + paddingSize, paddingSize, paddingSize, paddingSize)
            }
        }

        fun bind(text: String, @DrawableRes icon: Int) {
            binding.filterName.setPadding(paddingSize, paddingSize, paddingSize, paddingSize)
            binding.filterName.text = text
            val drawable = ResourcesCompat.getDrawable(resources, icon, requireActivity().theme)
            drawable?.bounds = Rect(0, 0, leftDrawableSize, leftDrawableSize)
            binding.filterName.setCompoundDrawables(drawable, null, null, null)
        }
    }
}