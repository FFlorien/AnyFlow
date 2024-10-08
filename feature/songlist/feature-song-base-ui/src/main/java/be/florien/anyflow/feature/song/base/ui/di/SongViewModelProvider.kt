package be.florien.anyflow.feature.song.base.ui.di

import androidx.lifecycle.ViewModelStoreOwner
import be.florien.anyflow.feature.song.base.ui.BaseSongViewModel

interface SongViewModelProvider<T : BaseSongViewModel<*>> {
    fun  getSongViewModel(owner: ViewModelStoreOwner? = null): T
}