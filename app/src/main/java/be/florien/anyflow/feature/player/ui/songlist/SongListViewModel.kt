package be.florien.anyflow.feature.player.ui.songlist

import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.toViewDisplay
import be.florien.anyflow.data.view.SongDisplay
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.download.DownloadManager
import be.florien.anyflow.feature.player.services.queue.FiltersManager
import be.florien.anyflow.feature.player.services.queue.OrderComposer
import be.florien.anyflow.feature.player.services.queue.PlayingQueue
import be.florien.anyflow.feature.player.ui.info.InfoActions
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions
import be.florien.anyflow.injection.ActivityScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Display a list of accounts and play it upon selection.
 */

@ActivityScope
class SongListViewModel
@Inject constructor(
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    sharedPreferences: SharedPreferences,
    downloadManager: DownloadManager,
    urlRepository: UrlRepository,
    private val playingQueue: PlayingQueue,
    private val dataRepository: DataRepository
) : BaseViewModel() {

    private val isLoadingAll: LiveData<Boolean> = MutableLiveData(false)
    private val songInfoActions = SongInfoActions(
        filtersManager,
        orderComposer,
        urlRepository,
        sharedPreferences,
        downloadManager
    )
    val pagedAudioQueue: LiveData<PagingData<SongDisplay>> = playingQueue.songDisplayListUpdater
    val currentSong: LiveData<SongInfo> = playingQueue.currentSong
    val currentSongDisplay: LiveData<SongDisplay> =
        playingQueue.currentSong.map { it.toViewDisplay() }

    val listPosition: LiveData<Int> = playingQueue.positionUpdater.distinctUntilChanged()
    val isSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val searchedText: MutableLiveData<String> = MutableLiveData("")
    val searchResults: MutableLiveData<LiveData<List<Long>>?> = MutableLiveData()
    val searchProgression: MutableLiveData<Int> = MutableLiveData(-1)
    val searchProgressionText: MutableLiveData<String> = MutableLiveData("")
    val playlistListDisplayedFor: LiveData<Triple<Long, SongInfoActions.SongFieldType, Int>> = MutableLiveData(null)
    val quickActions: LiveData<List<InfoActions.InfoRow>> =
        MutableLiveData(songInfoActions.getQuickActions())
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
        playingQueue.listPositionWithIntent =
            PlayingQueue.PositionWithIntent(position, PlayingQueue.PlayingQueueIntent.CONTINUE)
    }

    fun nextSearchOccurrence() {
        if (searchResults.value?.value != null) {
            searchProgression.value =
                if (searchResults.value?.value?.size?.minus(1) == searchProgression.value) 0 else (searchProgression.value
                    ?: 0) + 1
            searchProgressionText.value =
                "${searchProgression.value?.plus(1)}/${searchResults.value?.value?.size ?: 0}"
        }
    }

    fun previousSearchOccurrence() {
        if (searchResults.value?.value != null) {
            searchProgression.value =
                if (searchProgression.value == 0) searchResults.value?.value?.size?.minus(1) else (searchProgression.value
                    ?: 0) - 1
            searchProgressionText.value =
                "${searchProgression.value?.plus(1)}/${searchResults.value?.value?.size ?: 0}"
        }
    }

    fun deleteSearch() {
        searchedText.value = ""
    }

    fun clearPlaylistDisplay() {
        playlistListDisplayedFor.mutable.value = null
    }

    //todo extract some of these actions elsewhere because it's the fragment responsibility
    fun executeSongAction(songDisplay: SongDisplay, row: InfoActions.InfoRow) {
        val fieldType = row.fieldType
        if (fieldType !is SongInfoActions.SongFieldType) {
            return
        }
        viewModelScope.launch {
            val songInfo = runBlocking(Dispatchers.IO) {
                dataRepository.getSongSync(songDisplay.id)
            }
            when (row.actionType) {
                SongInfoActions.SongActionType.AddNext -> songInfoActions.playNext(songDisplay.id)
                SongInfoActions.SongActionType.AddToPlaylist -> displayPlaylistList(songInfo.albumId, fieldType, songInfo.disk )
                // todo selector for multiple values (genre && playlists)
                SongInfoActions.SongActionType.AddToFilter -> songInfoActions.filterOn(
                    songInfo,
                    row
                )

                SongInfoActions.SongActionType.Search -> searchText(
                    songInfoActions.getSearchTerms(
                        songInfo,
                        fieldType
                    )
                )

                SongInfoActions.SongActionType.Download -> songInfoActions.queueDownload(
                    songInfo,
                    fieldType,
                    null
                ) // todo right index
                else -> return@launch
            }
        }
    }

    fun refreshQuickActions() {
        val oldValue = quickActions.value
        val newValue = songInfoActions.getQuickActions()
        if (!newValue.containsAll(
                oldValue ?: listOf()
            ) || oldValue?.containsAll(newValue) == false
        ) {
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

    private fun displayPlaylistList(songId: Long, fieldType: SongInfoActions.SongFieldType, secondId: Int) {
        playlistListDisplayedFor.mutable.value = Triple(songId, fieldType, secondId)
    }
}