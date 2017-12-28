package be.florien.ampacheplayer.view.fragment

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.FragmentSongListBinding
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.viewmodel.SongListFragmentVM

/**
 * Display a list of songs and play it upon selection.
 */
class SongListFragment: Fragment() {

    lateinit var vm: SongListFragmentVM

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_song_list, container, false)
        val binding = DataBindingUtil.bind<FragmentSongListBinding>(view)
        vm = SongListFragmentVM()
        activity.ampacheApp.applicationComponent.inject(vm)
        binding.vm = vm
        vm.refreshSongs()
        binding.songList.adapter = vm.getSongAdapter()
        binding.songList.layoutManager = LinearLayoutManager(activity)
        activity.bindService(Intent(activity, PlayerService::class.java), vm.connection, Context.BIND_AUTO_CREATE)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.onViewCreated()
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.destroy()
        activity.unbindService(vm.connection)
    }
}