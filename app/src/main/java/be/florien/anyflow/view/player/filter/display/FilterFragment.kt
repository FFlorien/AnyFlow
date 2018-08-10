package be.florien.anyflow.view.player.filter.display

import android.databinding.Observable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentFilterBinding
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.selectType.AddFilterTypeFragment
import javax.inject.Inject


class FilterFragment : Fragment() {
    @Inject
    lateinit var vm: FilterFragmentVM

    private lateinit var binding: FragmentFilterBinding
    private val filterListAdapter = FilterListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as PlayerActivity).activityComponent.inject(this)
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
        return binding.root
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder = FilterViewHolder()

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            when (position) {
                0 -> {
                    (holder.itemView as TextView).text = getString(R.string.action_filter_clear)
                    holder.itemView.setOnClickListener { vm.clearFilters() }
                }
                itemCount - 1 -> {
                    (holder.itemView as TextView).text = getString(R.string.action_filter_add)
                    holder.itemView.setOnClickListener {
                        requireActivity()
                                .supportFragmentManager
                                .beginTransaction()
                                .replace(R.id.container, AddFilterTypeFragment(), AddFilterTypeFragment::class.java.simpleName)
                                .commitNow()
                    }
                }
                else -> {
                    holder.bind(vm.currentFilters[position - 1])
                    holder.itemView.setOnClickListener(null)
                }
            }
        }

        override fun getItemCount(): Int = vm.currentFilters.size + 2
    }

    inner class FilterViewHolder : RecyclerView.ViewHolder(layoutInflater.inflate(R.layout.item_filter_active, binding.filterList, false)) {

        fun bind(filter: Filter<*>) {
            (itemView as TextView).text = when(filter) {
                is Filter.TitleIs -> getString(R.string.filter_display_title_is, filter.displayValue)
                is Filter.TitleContain -> getString(R.string.filter_display_title_contain, filter.displayValue)
                is Filter.GenreIs -> getString(R.string.filter_display_genre_is, filter.displayValue)
                is Filter.SongIs -> getString(R.string.filter_display_song_is, filter.displayValue)
                is Filter.ArtistIs -> getString(R.string.filter_display_artist_is, filter.displayValue)
                is Filter.AlbumArtistIs -> getString(R.string.filter_display_album_artist_is, filter.displayValue)
                is Filter.AlbumIs -> getString(R.string.filter_display_album_is, filter.displayValue)
            }
        }
    }
}