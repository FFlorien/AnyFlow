package be.florien.ampacheplayer.view.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.FragmentSongListBinding
import be.florien.ampacheplayer.view.viewmodel.SongListFragmentVM

/**
 * Display a list of songs and play it upon selection.
 */
class SongListFragment: Fragment() {

    lateinit var vm: SongListFragmentVM

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_song_list, container, false)
        val binding = DataBindingUtil.bind<FragmentSongListBinding>(view)
        vm = SongListFragmentVM(activity, binding)
        vm.refreshSongs()
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.destroy()
    }
}