package be.florien.anyflow.feature.player.songlist

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.player.IdlePlayerController
import be.florien.anyflow.player.PlayerController
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.player.PlayingQueue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Display a list of accounts and play it upon selection.
 */

@ActivityScope
class SongListViewModel
@Inject constructor(private val playingQueue: PlayingQueue, private val dataRepository: DataRepository) : BaseViewModel() {

    internal var connection: PlayerConnection = PlayerConnection()
    private var player: PlayerController = IdlePlayerController()
    private val isLoadingAll: LiveData<Boolean> = MutableLiveData(false)
    val pagedAudioQueue: LiveData<PagingData<Song>> = playingQueue.songDisplayListUpdater
    val currentSong: LiveData<Song> = playingQueue.currentSong

    val listPosition: LiveData<Int> = playingQueue.positionUpdater
    val isSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val searchedText: MutableLiveData<String> = MutableLiveData("")
    val searchResults: MutableLiveData<LiveData<List<Long>>> = MutableLiveData()
    val searchProgression: MutableLiveData<Int> = MutableLiveData(-1)
    val searchProgressionText: MutableLiveData<String> = MutableLiveData("")
    var searchJob: Job? = null
    val searchTextWatcher: TextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(300)
                if (s.isBlank()) {
                    resetSearch()
                } else {
                    searchResults.value = dataRepository.searchSongs(s.toString())
                }
            }
        }
    }
    private var oldLiveData: LiveData<List<Long>>? = null
    private val listObserver = Observer<List<Long>> {
        if (it.isEmpty()) {
            searchProgression.value = -1
            searchProgressionText.value = "no results"
        } else {
            searchProgression.value = 0
            searchProgressionText.value = "1/${it.size}"
        }
    }

    init {
        searchResults.observeForever {
            oldLiveData?.removeObserver(listObserver)
            it?.observeForever(listObserver)
        }
        isSearching.observeForever {
            if (!it) {
                resetSearch()
            }
        }
    }

    /**
     * Public methods
     */

    fun refreshSongs() {
        isLoadingAll.mutable.value = true
    }

    fun play(position: Int) {
        playingQueue.listPosition = position
        player.play()
    }

    fun nextSearchOccurrence() {
        if (searchResults.value?.value != null) {
            searchProgression.value = if (searchResults.value?.value?.size?.minus(1) == searchProgression.value) 0 else (searchProgression.value ?: 0) + 1
            searchProgressionText.value = "${searchProgression.value?.plus(1)}/${searchResults.value?.value?.size ?: 0}"
        }
    }

    fun previousSearchOccurrence() {
        if (searchResults.value?.value != null) {
            searchProgression.value = if (searchProgression.value == 0) searchResults.value?.value?.size?.minus(1) else (searchProgression.value ?: 0) - 1
            searchProgressionText.value = "${searchProgression.value?.plus(1)}/${searchResults.value?.value?.size ?: 0}"
        }
    }

    fun deleteSearch() {
        searchedText.value = ""
    }

    /**
     * Private methods
     */

    private fun resetSearch() {
        searchResults.value = null
        searchProgression.value = -1
        searchProgressionText.value = ""
    }

    /**
     * Inner class
     */
    inner class PlayerConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            player = IdlePlayerController()
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            player = (service as PlayerService.LocalBinder).service
        }
    }
}