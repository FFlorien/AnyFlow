package be.florien.anyflow.feature.player.ui.songlist

import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.paging.PagingData
import be.florien.anyflow.architecture.di.ActivityScope
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.tags.toViewDisplay
import be.florien.anyflow.tags.toViewPodcastEpisodeDisplay
import be.florien.anyflow.tags.view.QueueItemDisplay
import be.florien.anyflow.tags.view.SongDisplay
import be.florien.anyflow.feature.download.DownloadManager
import be.florien.anyflow.feature.player.services.queue.OrderComposer
import be.florien.anyflow.feature.player.services.queue.PlayingQueue
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.podcast.PodcastRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named

/**
 * Display a list of accounts and play it upon selection.
 */

@ActivityScope
class SongListViewModel
@Inject constructor(
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    @Named("preferences") sharedPreferences: SharedPreferences,
    downloadManager: DownloadManager,
    urlRepository: be.florien.anyflow.tags.UrlRepository,
    playingQueue: PlayingQueue,
    private val dataRepository: be.florien.anyflow.tags.DataRepository,
    private val podcastRepository: PodcastRepository,
) : BaseViewModel() {

    var player: MediaController? = null
    private val isLoadingAll: LiveData<Boolean> = MutableLiveData(false)
    private val songInfoActions = SongInfoActions(
        filtersManager,
        orderComposer,
        urlRepository,
        sharedPreferences,
        downloadManager
    )
    val pagedAudioQueue: LiveData<PagingData<QueueItemDisplay>> =
        playingQueue.queueItemDisplayListUpdater
    val currentSongDisplay: LiveData<QueueItemDisplay?> =
        playingQueue.currentMedia.switchMap { queueItem ->
            if (queueItem?.mediaType == SONG_MEDIA_TYPE) {
                queueItem.id.let { id -> dataRepository.getSong(id).map { it.toViewDisplay() } }
            } else {
                queueItem?.id?.let { id ->
                    podcastRepository.getPodcastEpisode(id).map { it.toViewPodcastEpisodeDisplay() }
                }
            }
        }

    val listPosition: LiveData<Int> = playingQueue.positionUpdater.distinctUntilChanged()
    val isSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val searchedText: MutableLiveData<String> = MutableLiveData("")
    val searchResults: MutableLiveData<LiveData<List<Long>>?> = MutableLiveData()
    val searchProgression: MutableLiveData<Int> = MutableLiveData(-1)
    val searchProgressionText: MutableLiveData<String> = MutableLiveData("")
    val playlistListDisplayedFor: LiveData<Triple<Long, SongInfoActions.SongFieldType, Int>> =
        MutableLiveData(null)
    val shortcuts: LiveData<List<InfoActions.InfoRow>> =
        MutableLiveData(songInfoActions.getShortcuts())
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

    fun select(position: Int) {
        player?.seekToDefaultPosition(position)
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
    fun executeSongAction(songDisplay: QueueItemDisplay, row: InfoActions.InfoRow) {
        val fieldType = row.fieldType
        if (songDisplay !is SongDisplay || fieldType !is SongInfoActions.SongFieldType) {
            return
        }
        viewModelScope.launch {
            val songInfo = runBlocking(Dispatchers.IO) {
                dataRepository.getSongSync(songDisplay.id)
            }
            when (row.actionType) {
                SongInfoActions.SongActionType.AddNext -> songInfoActions.playNext(songDisplay.id)
                //todo get correct id depending on the fieldType
                SongInfoActions.SongActionType.AddToPlaylist -> displayPlaylistList(
                    songDisplay.id,
                    fieldType,
                    songInfo.disk
                )
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

    fun refreshShortcuts() {
        val oldValue = shortcuts.value
        val newValue = songInfoActions.getShortcuts()
        if (!newValue.containsAll(
                oldValue ?: listOf()
            ) || oldValue?.containsAll(newValue) == false
        ) {
            shortcuts.mutable.value = songInfoActions.getShortcuts()
        }
    }

    fun getArtUrl(albumId: Long, isPodcast: Boolean) =
        if (isPodcast) songInfoActions.getPodcastArtUrl(albumId) else songInfoActions.getAlbumArtUrl(
            albumId
        )

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

    private fun displayPlaylistList(
        songId: Long,
        fieldType: SongInfoActions.SongFieldType,
        secondId: Int
    ) {
        playlistListDisplayedFor.mutable.value = Triple(songId, fieldType, secondId)
    }
}