package be.florien.ampacheplayer.view.player.songlist

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.FragmentSongListBinding
import be.florien.ampacheplayer.databinding.ItemSongPendingBinding
import be.florien.ampacheplayer.databinding.ItemSongPlayingBinding
import be.florien.ampacheplayer.persistence.model.Song
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.player.PlayerActivity
import javax.inject.Inject

private const val LIST_ITEM_TYPE_PENDING = 0
private const val LIST_ITEM_TYPE_PLAYING = 1

/**
 * Display a list of songs and play it upon selection.
 */
class SongListFragment : Fragment() {

    @Inject lateinit var vm: SongListFragmentVm

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_song_list, container, false)
        val binding = DataBindingUtil.bind<FragmentSongListBinding>(view)
        (activity as PlayerActivity).activityComponent.inject(this)
        binding.vm = vm
        vm.refreshSongs()
        binding.songList.adapter = SongAdapter().apply { songs = vm.getCurrentAudioQueue() }
        binding.songList.layoutManager = LinearLayoutManager(activity)
        activity.bindService(Intent(activity, PlayerService::class.java), vm.connection, Context.BIND_AUTO_CREATE)
        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, id: Int) {
                when (id) {
                    BR.currentAudioQueue -> (binding.songList.adapter as SongAdapter).songs = vm.getCurrentAudioQueue()
                    BR.listPosition -> (binding.songList.adapter as SongAdapter).notifyItemChanged(vm.getListPosition())
                }
            }
        })
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.destroy()
        activity.unbindService(vm.connection)
    }

    inner class SongAdapter : RecyclerView.Adapter<SongViewHolder>() {
        var songs = listOf<Song>()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount() = songs.size

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            holder.bind(songs[position], position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = if (viewType == LIST_ITEM_TYPE_PENDING) SongPendingViewHolder(parent) else SongPlayingViewHolder(parent)

        override fun getItemViewType(position: Int): Int = if (position == vm.getListPosition()) LIST_ITEM_TYPE_PLAYING else LIST_ITEM_TYPE_PENDING
    }

    inner abstract class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(song: Song, position: Int)
    }

    inner class SongPendingViewHolder(
            parent: ViewGroup,
            private val binding: ItemSongPendingBinding = ItemSongPendingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        : SongViewHolder(binding.root) {

        override fun bind(song: Song, position: Int) {
            binding.song = song
            binding.position = position
            binding.vm = vm
        }

    }

    inner class SongPlayingViewHolder(
            parent: ViewGroup,
            private val binding: ItemSongPlayingBinding = ItemSongPlayingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        : SongViewHolder(binding.root) {

        override fun bind(song: Song, position: Int) {
            binding.song = song
            binding.position = position
            binding.vm = vm
        }

    }
}