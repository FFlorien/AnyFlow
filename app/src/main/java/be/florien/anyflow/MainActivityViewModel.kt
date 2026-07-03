package be.florien.anyflow

import android.content.ComponentName
import android.content.ServiceConnection
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.IBinder
import androidx.compose.runtime.Immutable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.component.player.controls.PlayPauseIconAnimator
import be.florien.anyflow.component.player.controls.PlayerControls
import be.florien.anyflow.feature.auth.domain.net.AuthenticationInterceptor
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.feature.sync.service.SyncRepository
import be.florien.anyflow.management.alarm.AlarmsSynchronizer
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.podcast.PodcastPersistence
import be.florien.anyflow.management.podcast.PodcastRepository
import be.florien.anyflow.management.queue.OrderComposer
import be.florien.anyflow.management.queue.PlayingQueue
import be.florien.anyflow.management.waveform.WaveFormRepository
import be.florien.anyflow.tags.TagsRepository
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

/**
 * ViewModel for the PlayerActivity
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModel
@Inject
constructor(
    playingQueue: PlayingQueue,
    private val alarmsSynchronizer: AlarmsSynchronizer,
    private val waveFormRepository: WaveFormRepository,
    private val tagsRepository: TagsRepository,
    private val podcastRepository: PodcastRepository,
    private val podcastPersistence: PodcastPersistence,
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    val authenticationInterceptor: AuthenticationInterceptor,
    connectionStatus: LiveData<AuthRepository.ConnectionStatus>,
    libraryUpdatePercentage: LiveData<SyncRepository.PercentageUpdate>
) : BaseViewModel(), PlayerControls.OnActionListener, Player.Listener {


    //region public fields
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasNet =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            stateFlow.mutable.update { it.copy(isInternetPresent = hasNet) }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            stateFlow.mutable.update { it.copy(isInternetPresent = false) }
        }
    }
    internal val updateConnection: UpdateConnection = UpdateConnection()
    var player: MediaController? = null
        set(value) {
            field?.removeListener(this@MainActivityViewModel)
            field = value
            value?.addListener(this@MainActivityViewModel)
        }

    val stateFlow: StateFlow<State> = MutableStateFlow(
        State(
            isInternetPresent = false,
            connectionStatus = AuthRepository.ConnectionStatus.CONNEXION,
            update = null,
            currentDuration = 0,
            totalDuration = 0,
            shouldShowBuffering = false,
            isSeekable = false,
            isOrdered = false,
            hasPrevious = false,
            hasNext = false,
            playbackState = PlaybackState.STATE_NONE,
            waveForm = emptyList(),
            hasUncommitedChanges = false
        )
    )
    //endregion

    @Immutable
    data class State(
        val isInternetPresent: Boolean,
        val connectionStatus: AuthRepository.ConnectionStatus,
        val update: SyncRepository.PercentageUpdate?,
        val currentDuration: Int,
        val totalDuration: Int,
        val shouldShowBuffering: Boolean,
        val isSeekable: Boolean,
        val isOrdered: Boolean,
        val hasPrevious: Boolean,
        val hasNext: Boolean,
        val playbackState: Int,
        val waveForm: List<Double>,
        val hasUncommitedChanges: Boolean
    )

    init {
        playingQueue//todo awful
            .currentMedia
            .asFlow()
            .flatMapLatest { queueItem ->
                val totalDuration = if (queueItem == null) {
                    0
                } else if (queueItem.mediaType == PODCAST_MEDIA_TYPE) {
                    queueItem.id.let { podcastRepository.getPodcastDuration(it) } * 1000
                } else {
                    queueItem.id.let { tagsRepository.getSongDuration(it) } * 1000
                }
                stateFlow.mutable.update {
                    it.copy(totalDuration = totalDuration)
                }
                if (queueItem?.mediaType == SONG_MEDIA_TYPE) {
                    queueItem.let {
                        waveFormRepository.getComputedWaveForm(
                            it.id,
                            MediaMetadata.MEDIA_TYPE_MUSIC
                        )
                    }
                } else {
                    queueItem.let {
                        waveFormRepository.getComputedWaveForm(
                            it?.id ?: 0L,
                            MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE
                        )
                    }
                }.asFlow()
            }
            .onEach { waveForm ->
                stateFlow.mutable.update {
                    it.copy(waveForm = waveForm.asList())
                }
            }
            .launchIn(viewModelScope)
        playingQueue.positionUpdater.onEach { position ->
            stateFlow.mutable.update {
                it.copy(hasPrevious = position != 0)
            }
        }.launchIn(viewModelScope)

        filtersManager.areFiltersChanged.onEach { hasChanged ->
            stateFlow.mutable.update {
                it.copy(hasUncommitedChanges = hasChanged)
            }
        }.launchIn(viewModelScope)

        connectionStatus.asFlow().onEach { status ->
            stateFlow.mutable.update {
                it.copy(connectionStatus = status)
            }
        }.launchIn(viewModelScope)

        libraryUpdatePercentage.asFlow().onEach { update ->
            stateFlow.mutable.update {
                it.copy(update = update.takeIf { it.subject != SyncRepository.CHANGE_NONE })
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch(Dispatchers.Default) {//todo safer, look for the lifecycle ?
            stateFlow.mutable.update {
                it.copy(isOrdered = orderComposer.isOrdered())
            }
            while (true) {
                delay(10)
                val currentDuration = getFromPlayer(0) { contentPosition }.toInt()
                stateFlow.mutable.update {
                    it.copy(currentDuration = currentDuration)
                }
                if (
                    playingQueue.currentMedia.value?.mediaType == PODCAST_MEDIA_TYPE
                    && (currentDuration % 10000) < 10
                    && (currentDuration / 1000) > 10
                    && getFromPlayer(false) { isPlaying }
                ) {
                    playingQueue.currentMedia.value?.id?.let { podcastId ->
                        podcastPersistence.savePodcastPosition(
                            podcastId,
                            (currentDuration).toLong()
                        )
                    }
                }
            }
        }
    }

    // region PlayerControls.OnActionListener methods
    override fun onPreviousClicked() {
        if (stateFlow.value.currentDuration < 10 * 1000) {
            doOnPlayer { seekToPrevious() }
        } else {
            doOnPlayer { seekTo(0L) }
        }
    }

    override fun onNextClicked() {
        doOnPlayer { seekToNext() }
    }

    override fun onPlayPauseClicked() {
        doOnPlayer {
            if (isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }

    override fun onCurrentDurationChanged(newDuration: Long) {
        doOnPlayer {
            if (
                isCurrentMediaItemSeekable
                && ((contentPosition - newDuration).absoluteValue) > 1000
            ) {
                seekTo(newDuration)
            }
        }
    }

    fun syncAlarms() {
        viewModelScope.launch {
            alarmsSynchronizer.syncAlarms()
        }
    }

    fun commitFiltersChanges() {
        viewModelScope.launch {
            filtersManager.commitChanges()
        }
    }

    fun changeOrdering() {
        viewModelScope.launch(Dispatchers.IO){
            val ordered = orderComposer.isOrdered()
            if (ordered) {
                orderComposer.randomize()
            } else {
                orderComposer.order()
            }
            stateFlow.mutable.update { it.copy(isOrdered = orderComposer.isOrdered()) }
        }
    }

    //endregion
    override fun onPlaybackStateChanged(playbackState: Int) {
        var iconPlaybackState: Int = PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
        viewModelScope.launch(Dispatchers.Main) {
            when (playbackState) {
                Player.STATE_BUFFERING, Player.STATE_IDLE -> {
                    stateFlow.mutable.update { it.copy(shouldShowBuffering = true) }
                    iconPlaybackState = PlayPauseIconAnimator.STATE_PLAY_PAUSE_BUFFER
                }

                Player.STATE_READY -> {
                    stateFlow.mutable.update { it.copy(shouldShowBuffering = false, isSeekable = getFromPlayer(false) { isCurrentMediaItemSeekable }) }
                    iconPlaybackState = if (getFromPlayer(false) { isPlaying }) {
                        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY
                    } else {
                        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
                    }
                }

                else -> {
                    stateFlow.mutable.update { it.copy(shouldShowBuffering = false) }
                }
            }
            stateFlow.mutable.update { it.copy(playbackState = iconPlaybackState) }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            stateFlow.mutable.update {
                it.copy(
                    playbackState = if (playWhenReady) {
                        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY
                    } else {
                        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
                    }
                )
            }
        }
    }

    fun setInternetPresence(hasNet: Boolean) {
        stateFlow.mutable.update { it.copy(isInternetPresent = hasNet) }
    }

    private fun doOnPlayer(action: Player.() -> Unit) { //todo put that in a specific class to avoid direct access to player
        viewModelScope.launch(Dispatchers.Main) {
            player?.action()
        }
    }

    private suspend fun <T> getFromPlayer(defaultValue: T, getter: Player.() -> T) =
        viewModelScope.async(Dispatchers.Main) { player?.getter() ?: defaultValue }.await()

    inner class UpdateConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    fun <T> MutableLiveData<T>.postValueIfChanged(newValue: T) {
        if (newValue != value) {
            postValue(newValue)
        }
    }
}