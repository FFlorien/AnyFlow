package be.florien.anyflow.feature.player.songlist

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.*
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.toSong
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import be.florien.anyflow.player.PlayingQueue
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Display a list of accounts and play it upon selection.
 */

@ActivityScope
class SongListViewModel
@Inject constructor(
    context: Context,
    ampache: AmpacheConnection,
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    private val playingQueue: PlayingQueue,
    private val dataRepository: DataRepository,
) : BaseViewModel() {

    private val isLoadingAll: LiveData<Boolean> = MutableLiveData(false)
    private val songInfoOptions = SongInfoOptions(context.contentResolver, ampache, filtersManager, orderComposer, dataRepository)
    val pagedAudioQueue: LiveData<PagingData<Song>> = playingQueue.songDisplayListUpdater
    val currentSong: LiveData<Song> = playingQueue.currentSong.map { it.toSong() }

    val listPosition: LiveData<Int> = playingQueue.positionUpdater
    val isSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val searchedText: MutableLiveData<String> = MutableLiveData("")
    val searchResults: MutableLiveData<LiveData<List<Long>>> = MutableLiveData()
    val searchProgression: MutableLiveData<Int> = MutableLiveData(-1)
    val searchProgressionText: MutableLiveData<String> = MutableLiveData("")
    val playlistListDisplayedFor: LiveData<Long> = MutableLiveData(-1L)
    var searchJob: Job? = null
    val searchTextWatcher: TextWatcher = object : TextWatcher {
        private val coroutineScope = CoroutineScope(Dispatchers.Main)
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
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
        playingQueue.listPositionWithIntent = PlayingQueue.PositionWithIntent(position, PlayingQueue.PlayingQueueIntent.CONTINUE)
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

    fun executeSongAction(songId: Long, actionType: SongInfoOptions.ActionType, fieldType: SongInfoOptions.FieldType) {
        viewModelScope.launch {
            when (actionType) {
                SongInfoOptions.ActionType.ADD_NEXT -> songInfoOptions.playNext(songId)
                SongInfoOptions.ActionType.ADD_TO_PLAYLIST -> displayPlaylistList(songId)
                SongInfoOptions.ActionType.ADD_TO_FILTER -> songInfoOptions.filterOn(songId, fieldType)
                SongInfoOptions.ActionType.SEARCH -> searchText(songInfoOptions.getSearchTerms(songId, fieldType))
                SongInfoOptions.ActionType.DOWNLOAD -> songInfoOptions.download(songId)
                else -> return@launch
            }
        }
    }

    /**
     * Private methods
     */

    private fun resetSearch() {
        searchResults.value = null
        searchProgression.value = -1
        searchProgressionText.value = ""
    }

    private fun searchText(text: String) {
        isSearching.mutable.value = true
        searchedText.mutable.value = text
    }

    private fun displayPlaylistList(songId: Long) {
        playlistListDisplayedFor.mutable.value = songId
    }
}