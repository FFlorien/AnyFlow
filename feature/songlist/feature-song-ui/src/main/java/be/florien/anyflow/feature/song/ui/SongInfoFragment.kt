package be.florien.anyflow.feature.song.ui

import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoFragment

class SongInfoFragment(songId: Long): BaseSongInfoFragment<SongInfoActions, SongInfoViewModel>(songId = songId) {
    override fun getViewModel(): SongInfoViewModel =
        ViewModelProvider(
            this,
            (activity as ViewModelFactoryProvider).viewModelFactory
        )[SongInfoViewModel::class.java]

}