package be.florien.anyflow.feature.player.songlist

import android.content.Context
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.view.SongInfo
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
    sharedPreferences: SharedPreferences,
    private val playingQueue: PlayingQueue,
    private val dataRepository: DataRepository,
) : BaseViewModel() {

    private val isLoadingAll: LiveData<Boolean> = MutableLiveData(false)
    private val songInfoActions = SongInfoActions(context.contentResolver, ampache, filtersManager, orderComposer, dataRepository, sharedPreferences)
    val pagedAudioQueue: LiveData<PagingData<SongInfo>> = playingQueue.songDisplayListUpdater
    val currentSong: LiveData<SongInfo> = playingQueue.currentSong

    val listPosition: LiveData<Int> = playingQueue.positionUpdater
    val isSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val searchedText: MutableLiveData<String> = MutableLiveData("")
    val searchResults: MutableLiveData<LiveData<List<Long>>> = MutableLiveData()
    val searchProgression: MutableLiveData<Int> = MutableLiveData(-1)
    val searchProgressionText: MutableLiveData<String> = MutableLiveData("")
    val playlistListDisplayedFor: LiveData<Long> = MutableLiveData(-1L)
    val quickActions: LiveData<List<SongInfoActions.SongRow>> = MutableLiveData(songInfoActions.getQuickActions())
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

    fun clearPlaylistDisplay() {
        playlistListDisplayedFor.mutable.value = -1L
    }

    //todo extract some of these actions elsewhere because it's the fragment responsibility
    fun executeSongAction(songInfo: SongInfo, actionType: SongInfoActions.ActionType, fieldType: SongInfoActions.FieldType) {
        viewModelScope.launch {
            when (actionType) {
                SongInfoActions.ActionType.ADD_NEXT -> songInfoActions.playNext(songInfo.id)
                SongInfoActions.ActionType.ADD_TO_PLAYLIST -> displayPlaylistList(songInfo.id)
                SongInfoActions.ActionType.ADD_TO_FILTER -> songInfoActions.filterOn(songInfo, fieldType)
                SongInfoActions.ActionType.SEARCH -> searchText(songInfoActions.getSearchTerms(songInfo, fieldType))
                SongInfoActions.ActionType.DOWNLOAD -> songInfoActions.download(songInfo)
                else -> return@launch
            }
        }
    }

    fun refreshQuickActions() {
        val oldValue = quickActions.value
        val newValue = songInfoActions.getQuickActions()
        if (!newValue.containsAll(oldValue ?: listOf()) || oldValue?.containsAll(newValue) == false) {
            quickActions.mutable.value = songInfoActions.getQuickActions()
        }
    }

    fun getArtUrl(albumId: Long) = songInfoActions.getAlbumArtUrl(albumId)

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