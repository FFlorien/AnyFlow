package be.florien.anyflow.feature.mediaList.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.paging.PagingData
import androidx.paging.flatMap
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongActionType
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.feature.song.domain.SongInfoActions
import be.florien.anyflow.management.podcast.PodcastRepository
import be.florien.anyflow.management.queue.PlayingQueue
import be.florien.anyflow.management.queue.QueueRepository
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.tags.TagsRepository
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.urls.UrlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(FlowPreview::class)
@ActivityScope
class MediaListViewModel
@Inject constructor(
    playingQueue: PlayingQueue,
    private val songInfoActions: SongInfoActions,
    private val urlRepository: UrlRepository,
    private val tagsRepository: TagsRepository,
    private val podcastRepository: PodcastRepository,
    private val queueRepository: QueueRepository,
    internal val navigator: Navigator
) : BaseViewModel() {

    @Immutable
    data class ShortcutData(
        val action: SongActionType,
        val field: SongFieldType,
        val position: Int
    )

    @Immutable
    data class State(
        val mediaList: Flow<PagingData<MediaItemData>>?,
        val mediaPosition: Int,
        val chapterTime: Long,
        val shortcuts: List<ShortcutData>,
        val searchPosition: Int,
        val searchItemPosition: Int,
        val searchTotal: Int
    )
    // region fields

    var player: MediaController? = null
    var searchPositionList: List<Int>? = null

    // list
    private val pagedAudioQueue: Flow<PagingData<MediaItemData>> =
        playingQueue.getQueueItemDisplayUpdater { dbItem ->
            dbItem.toMediaItemData(::getSongArtUrl, ::getPodcastArtUrl)
        }.map { pagingData ->
            pagingData.flatMap { mediaItemData ->
                if (mediaItemData is MediaItemData.Full.PodcastEpisode) {
                    val chapters = mediaItemData.chapters.mapIndexed { index, chapter ->
                        MediaItemData.PodcastChapter(
                            id = (mediaItemData.id * 100) + index,
                            title = chapter.title,
                            podcastPosition = mediaItemData.position,
                            time = chapter.time
                        )
                    }
                    listOf(mediaItemData) + chapters
                } else {
                    listOf(mediaItemData)
                }
            }
        }
    private var currentPodcastDisplay: PodcastEpisodeDisplay? = null
    private val isLoadingAll = MutableLiveData(false)
    private val searchTerm = MutableSharedFlow<String>()

    // endregion
    val stateFlow: StateFlow<State> = MutableStateFlow(
        State(
            pagedAudioQueue,
            0,
            0L,
            emptyList(),
            0,
            0,
            0
        )
    )

    init {
        viewModelScope.launch(Dispatchers.Default) {
            playingQueue.positionUpdater.collect { newPosition ->
                stateFlow.mutable.update { it.copy(mediaPosition = newPosition) }
                withContext(Dispatchers.IO) {
                    val item = queueRepository
                        .getMediaItemAtPosition(newPosition)
                        ?.takeIf { it.mediaType == PODCAST_MEDIA_TYPE }
                    currentPodcastDisplay = item?.let {
                        val podcastEpisodeDisplay =
                            podcastRepository.getPodcastEpisodeDisplay(it.id)
                        podcastEpisodeDisplay?.toViewPodcastEpisodeDisplay()
                    }
                }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            searchTerm.collect { term ->
                if (term.isBlank()) {
                    stateFlow.mutable.update {
                        it.copy(searchPosition = 0, searchItemPosition = 0, searchTotal = 0)
                    }
                } else {
                    val positionList = tagsRepository.searchSongs(term)
                    searchPositionList = positionList
                    if (positionList.isNotEmpty()) {
                        stateFlow.mutable.update {
                            it.copy(searchItemPosition = positionList[0], searchPosition = 0, searchTotal = positionList.size)
                        }
                    } else {
                        stateFlow.mutable.update {
                            it.copy(searchItemPosition = 0, searchPosition = 0, searchTotal = 0)
                        }
                    }
                }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(500)//todo magic number
                withContext(Dispatchers.Main) {
                    val currentPosition = player?.currentPosition?.div(1000) ?: Long.MAX_VALUE
                    val chapterTime = currentPodcastDisplay
                        ?.chapters
                        ?.lastOrNull { currentPosition >= it.time }
                        ?.time
                        ?: 0L
                    stateFlow.mutable.update {
                        it.copy(chapterTime = chapterTime)
                    }
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

    fun goToMedia(position: Int) {
        player?.seekToDefaultPosition(position)
    }

    fun goToTime(position: Int, time: Long) {
        player?.seekTo(position, time * 1000)
    }

    //todo extract some of these actions elsewhere because it's the fragment responsibility
    fun executeAction(queueItem: MediaItemData.Full, row: ShortcutData) {
        executeSongAction(queueItem.id, songInfoActions.getShortcuts()[row.position])
    }

    private fun executeSongAction(id: Long, row: BaseSongInfoRow) {
        val fieldType = row.fieldType
        viewModelScope.launch {
            val songInfo = withContext(Dispatchers.IO) {
                tagsRepository.getSongSync(id)
            }
            when (row.actionType) {
                SongActionType.AddNext -> songInfoActions.playNext(id)
                // todo selector for multiple values (genre && playlists)
                SongActionType.AddToFilter -> songInfoActions.filterOn(
                    songInfo,
                    row
                )

                SongActionType.Search -> search(
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

    fun refreshShortcuts() {
        val oldValue = stateFlow.value.shortcuts
        val newValue = songInfoActions.getShortcuts().mapIndexed { index, row ->
            ShortcutData(
                row.actionType,
                row.fieldType,
                index
            )
        }
        if (!newValue.containsAll(oldValue) || !oldValue.containsAll(newValue)) {
            stateFlow.mutable.update {
                it.copy(
                    shortcuts = newValue
                )
            }
        }
    }

    fun getSongArtUrl(albumId: Long) = urlRepository.getAlbumArtUrl(albumId)

    fun getPodcastArtUrl(podcastId: Long) = urlRepository.getPodcastArtUrl(podcastId)

    /**
     * Private methods
     */

    fun search(text: String) {
        viewModelScope.launch {
            searchTerm.emit(text)
        }
    }

    fun setSearchPosition(searchPosition: Int) {
        val position = searchPositionList?.getOrNull(searchPosition)
        if (position != null) {
            stateFlow.mutable.update {
                it.copy(searchPosition = searchPosition, searchItemPosition = position)
            }
        }
    }
}