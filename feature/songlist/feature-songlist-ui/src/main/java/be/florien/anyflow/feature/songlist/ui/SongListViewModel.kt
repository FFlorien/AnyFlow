package be.florien.anyflow.feature.songlist.ui

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
import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.feature.podcast.base.domain.model.BasePodcastInfoRow
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongActionType
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.feature.song.domain.SongInfoActions
import be.florien.anyflow.feature.songlist.base.domain.model.QueueItemInfoRow
import be.florien.anyflow.management.podcast.PodcastRepository
import be.florien.anyflow.management.queue.OrderComposer
import be.florien.anyflow.management.queue.PlayingQueue
import be.florien.anyflow.management.queue.model.Chapter
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.QueueItemDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.DataRepository
import be.florien.anyflow.urls.UrlRepository
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ActivityScope
class SongListViewModel
@Inject constructor(
    playingQueue: PlayingQueue,
    private val songInfoActions: SongInfoActions,
    private val urlRepository: UrlRepository,
    private val orderComposer: OrderComposer,
    private val dataRepository: DataRepository,
    private val podcastRepository: PodcastRepository,
    internal val navigator: Navigator
) : BaseViewModel() {
    // region fields

    val currentChapter: LiveData<Chapter?>
        get() = chapterMutable
    var player: MediaController? = null
    private val chapterMutable = MutableLiveData<Chapter?>(null)

    // list
    val pagedAudioQueue: LiveData<PagingData<QueueItemDisplay>> =
        playingQueue.queueItemDisplayListUpdater
    val listPosition: LiveData<Int> = playingQueue.positionUpdater.distinctUntilChanged()
    val currentQueueItemDisplay: LiveData<QueueItemDisplay?> =
        playingQueue.currentMedia.switchMap { queueItem ->
            val id = queueItem?.id
            if (id == null) {
                null
            } else if (queueItem.mediaType == SONG_MEDIA_TYPE) {
                dataRepository.getSong(id).map { it.toViewDisplay() }
            } else {
                podcastRepository.getPodcastEpisodeDisplay(id).map {
                    it?.toViewPodcastEpisodeDisplay()
                }
            }
        }
    val currentSongDisplay: LiveData<SongDisplay?> =
        currentQueueItemDisplay.map { if (it is SongDisplay) it else null }
    val currentPodcastDisplay: LiveData<PodcastEpisodeDisplay?> =
        currentQueueItemDisplay.map { if (it is PodcastEpisodeDisplay) it else null }
    private val isLoadingAll: LiveData<Boolean> = MutableLiveData(false)
    val isOrdered: LiveData<Boolean> = playingQueue.isOrderedUpdater

    // search
    val isSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val searchedText: MutableLiveData<String> = MutableLiveData("")
    val searchResults: MutableLiveData<LiveData<List<Long>>?> = MutableLiveData()
    val searchProgression: MutableLiveData<Int> = MutableLiveData(-1)
    val searchProgressionText: MutableLiveData<String> = MutableLiveData("")
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
    private val listObserver = Observer<List<Long>> {
        if (it.isEmpty()) {
            searchProgression.value = -1
            searchProgressionText.value = "no results"
        } else {
            searchProgression.value = 0
            searchProgressionText.value = "1/${it.size}"
        }
    }

    // song actions
    val playlistListDisplayedFor: LiveData<Triple<Long, SongFieldType, Int>> =
        MutableLiveData(null)
    val shortcuts: LiveData<List<BaseSongInfoRow>> =
        MutableLiveData(songInfoActions.getShortcuts())
    private var oldLiveData: LiveData<List<Long>>? = null
    // endregion

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
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(200)//todo magic number
                val podcastEpisode = currentPodcastDisplay.value
                withContext(Dispatchers.Main) {
                    val currentPosition = player?.currentPosition?.div(1000) ?: Long.MAX_VALUE
                    chapterMutable.mutable.value = podcastEpisode
                        ?.chapters
                        ?.findLast { currentPosition >= it.time }
                }

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

    fun randomOrder() {
        viewModelScope.launch {
            orderComposer.randomize()
        }
    }

    fun classicOrder() {
        viewModelScope.launch {
            orderComposer.order()
        }
    }

    //todo extract some of these actions elsewhere because it's the fragment responsibility
    fun executeAction(queueItem: QueueItemDisplay, row: QueueItemInfoRow<*, *>) {
        when (queueItem) {
            is SongDisplay -> executeSongAction(queueItem, row as BaseSongInfoRow)
            is PodcastEpisodeDisplay -> executePodcastAction(queueItem, row as BasePodcastInfoRow)
        }
    }

    private fun executeSongAction(songDisplay: SongDisplay, row: BaseSongInfoRow) {
        val fieldType = row.fieldType
        viewModelScope.launch {
            val songInfo = runBlocking(Dispatchers.IO) {
                dataRepository.getSongSync(songDisplay.id)
            }
            when (row.actionType) {
                SongActionType.AddNext -> songInfoActions.playNext(songDisplay.id)
                //todo get correct id depending on the fieldType
                SongActionType.AddToPlaylist -> displayPlaylistList(
                    songDisplay.id,
                    fieldType,
                    songInfo.disk
                )
                // todo selector for multiple values (genre && playlists)
                SongActionType.AddToFilter -> songInfoActions.filterOn(
                    songInfo,
                    row
                )

                SongActionType.Search -> searchText(
                    songInfoActions.getSearchTerms(
                        songInfo,
                        fieldType
                    )
                )

                SongActionType.Download -> songInfoActions.queueDownload(
                    songInfo,
                    fieldType,
                    null
                ) // todo right index
                else -> return@launch
            }
        }
    }

    private fun executePodcastAction(
        podcastDisplay: PodcastEpisodeDisplay,
        row: BasePodcastInfoRow
    ) {

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

    fun getSongArtUrl(albumId: Long) = urlRepository.getAlbumArtUrl(albumId)

    fun getPodcastArtUrl(podcastId: Long) = urlRepository.getPodcastArtUrl(podcastId)

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
        fieldType: SongFieldType,
        secondId: Int
    ) {
        playlistListDisplayedFor.mutable.value = Triple(songId, fieldType, secondId)
    }
}