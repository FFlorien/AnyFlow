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
 * Created by florien on 9/07/17.
 */
class SongListFragment: Fragment() {

    lateinit var vm: SongListFragmentVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_song_list, container, false)
        val binding = DataBindingUtil.bind<FragmentSongListBinding>(view)
        vm = SongListFragmentVM(activity, binding)
        vm.getSongs()
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.destroy()
    }
}