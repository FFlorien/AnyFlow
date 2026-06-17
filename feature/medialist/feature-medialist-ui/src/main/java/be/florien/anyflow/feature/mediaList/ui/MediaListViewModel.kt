package be.florien.anyflow.feature.mediaList.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.paging.PagingData
import androidx.paging.flatMap
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.auth.domain.net.AuthenticationInterceptor
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongActionType
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.feature.song.domain.SongInfoActions
import be.florien.anyflow.feature.songlist.base.domain.model.QueueItemInfoRow
import be.florien.anyflow.management.podcast.PodcastRepository
import be.florien.anyflow.management.queue.OrderComposer
import be.florien.anyflow.management.queue.PlayingQueue
import be.florien.anyflow.management.queue.QueueRepository
import be.florien.anyflow.management.queue.model.ErrorDisplay
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.QueueItemDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.TagsRepository
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.urls.UrlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ActivityScope
class MediaListViewModel
@Inject constructor(
    playingQueue: PlayingQueue,
    private val songInfoActions: SongInfoActions,
    private val urlRepository: UrlRepository,
    private val orderComposer: OrderComposer,
    private val tagsRepository: TagsRepository,
    private val podcastRepository: PodcastRepository,
    private val queueRepository: QueueRepository,
    internal val navigator: Navigator,
    val authenticationInterceptor: AuthenticationInterceptor
) : BaseViewModel() {

    @Immutable
    data class State(
        val mediaList: Flow<PagingData<MediaItemData>>?,
        val mediaPosition: Int,
        val chapterTime: Long
    )
    // region fields

    var player: MediaController? = null

    // list
    private val pagedAudioQueue: Flow<PagingData<MediaItemData>> =
        playingQueue.getQueueItemDisplayUpdater { dbItem ->
            dbItem.toMediaItemData(::getSongArtUrl, ::getPodcastArtUrl)
        }.map { pagingData ->
            pagingData.flatMap { mediaItemData ->
                if (mediaItemData is MediaItemData.Full.PodcastEpisode) {
                    val chapters = mediaItemData.chapters.mapIndexed { index, chapter ->
                        MediaItemData.PodcastChapter(
                            id = mediaItemData.id + index,
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
    private val isLoadingAll: LiveData<Boolean> = MutableLiveData(false)


    // song actions
    val playlistListDisplayedFor: LiveData<Triple<Long, SongFieldType, Int>> =
        MutableLiveData(null)
    val shortcuts: LiveData<List<BaseSongInfoRow>> =
        MutableLiveData(songInfoActions.getShortcuts())

    // endregion
    val stateFlow: StateFlow<State> = MutableStateFlow(
        State(
            pagedAudioQueue,
            0,
            0L
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
                    currentPodcastDisplay = item?.let { val podcastEpisodeDisplay =
                        podcastRepository.getPodcastEpisodeDisplay(it.id)
                        podcastEpisodeDisplay?.toViewPodcastEpisodeDisplay()
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
                        ?.last { currentPosition >= it.time }
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

    fun clearPlaylistDisplay() {
        playlistListDisplayedFor.mutable.value = null
    }

    //todo extract some of these actions elsewhere because it's the fragment responsibility
    fun executeAction(queueItem: QueueItemDisplay, row: QueueItemInfoRow<*, *>) {
        when (queueItem) {
            is SongDisplay -> executeSongAction(queueItem, row as BaseSongInfoRow)
            is PodcastEpisodeDisplay,
            ErrorDisplay -> Unit
        }
    }

    private fun executeSongAction(songDisplay: SongDisplay, row: BaseSongInfoRow) {
        val fieldType = row.fieldType
        viewModelScope.launch {
            val songInfo = withContext(Dispatchers.IO) {
                tagsRepository.getSongSync(songDisplay.id)
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

    private fun searchText(text: String) {
    }

    private fun displayPlaylistList(
        songId: Long,
        fieldType: SongFieldType,
        secondId: Int
    ) {
        playlistListDisplayedFor.mutable.value = Triple(songId, fieldType, secondId)
    }
}