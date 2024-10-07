package be.florien.anyflow.feature.song.ui.di

import androidx.lifecycle.ViewModelStoreOwner
import be.florien.anyflow.feature.song.ui.BaseSongViewModel

interface SongViewModelProvider<T : BaseSongViewModel<*>> {
    fun  getSongViewModel(owner: ViewModelStoreOwner? = null): T
}