package be.florien.anyflow.feature.player.ui

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.*
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import be.florien.anyflow.feature.auth.AuthRepository
import be.florien.anyflow.feature.player.services.PlayerService
import be.florien.anyflow.feature.player.services.WaveFormRepository
import be.florien.anyflow.feature.player.services.controller.IdlePlayerController
import be.florien.anyflow.feature.player.services.controller.PlayerController
import be.florien.anyflow.feature.player.services.queue.OrderComposer
import be.florien.anyflow.feature.player.services.queue.PlayingQueue
import be.florien.anyflow.feature.player.ui.controls.PlayPauseIconAnimator
import be.florien.anyflow.feature.player.ui.controls.PlayerControls
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.absoluteValue

/**
 * ViewModel for the PlayerActivity
 */
class PlayerViewModel
@Inject
constructor(
    private val playingQueue: PlayingQueue,
    private val orderComposer: OrderComposer,
    private val alarmsSynchronizer: AlarmsSynchronizer,
    private val waveFormRepository: WaveFormRepository,
    val connectionStatus: LiveData<AuthRepository.ConnectionStatus>,
    @Named("Songs")
    val songsUpdatePercentage: LiveData<Int>,
    @Named("Genres")
    val genresUpdatePercentage: LiveData<Int>,
    @Named("Albums")
    val albumsUpdatePercentage: LiveData<Int>,
    @Named("Artists")
    val artistsUpdatePercentage: LiveData<Int>,
    @Named("Playlists")
    val playlistsUpdatePercentage: LiveData<Int>
) : BaseViewModel(), PlayerControls.OnActionListener {

    internal val playerConnection: PlayerConnection = PlayerConnection()
    internal val updateConnection: UpdateConnection = UpdateConnection()
    val isConnecting = connectionStatus.map { it == AuthRepository.ConnectionStatus.CONNEXION }
    private var isBackKeyPreviousSong: Boolean = false

    /**
     * Bindables
     */
    val shouldShowBuffering: LiveData<Boolean> = MutableLiveData(false)
    val state: LiveData<Int> = MediatorLiveData()
    val hasInternet: LiveData<Boolean> = MediatorLiveData()
    val isOrdered: LiveData<Boolean> = playingQueue.isOrderedUpdater

    val currentDuration: LiveData<Int> = MediatorLiveData()
    val totalDuration: LiveData<Int> =
        playingQueue.currentSong.map { ((it as SongInfo?)?.time ?: 0) * 1000 }

    val isPreviousPossible: LiveData<Boolean> = playingQueue.positionUpdater.map { it != 0 }
    val waveForm: LiveData<DoubleArray> =
        playingQueue.currentSong.switchMap { waveFormRepository.getComputedWaveForm(it.id) }
            .distinctUntilChanged()

    val isSeekable: LiveData<Boolean> = MutableLiveData(false)

    var player: PlayerController = IdlePlayerController()
        set(value) {
            (currentDuration as MediatorLiveData).removeSource(field.playTimeNotifier)
            (state as MediatorLiveData).removeSource(field.stateChangeNotifier)
            (hasInternet as MediatorLiveData).removeSource(field.internetChangeNotifier)
            field = value
            currentDuration.addSource(field.playTimeNotifier) {
                // todo differentiate previous and start from the playercontrol
                isBackKeyPreviousSong = it.toInt() < 10000
                currentDuration.mutable.value = it.toInt()
            }
            state.addSource(field.stateChangeNotifier) {
                shouldShowBuffering.mutable.value = it == PlayerController.State.BUFFER
                state.mutable.value = when (it) {
                    PlayerController.State.PLAY -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY
                    PlayerController.State.PAUSE -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
                    else -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_BUFFER
                }
                isSeekable.mutable.value = player.isSeekable()
            }
            hasInternet.addSource(field.internetChangeNotifier) {
                hasInternet.mutable.value = it
            }
        }

    /**
     * PlayerControls.OnActionListener methods
     */
    override fun onPreviousClicked() {
        if (isBackKeyPreviousSong) {
            playingQueue.listPosition -= 1
        } else {
            player.seekTo(0L)
        }
    }

    override fun onNextClicked() {
        playingQueue.listPosition += 1
    }

    override fun onPlayPauseClicked() {
        player.apply {
            if (isPlaying()) {
                pause()
            } else {
                resume()
            }
        }
    }

    override fun onCurrentDurationChanged(newDuration: Long) {
        if (player.isSeekable() && ((currentDuration.value?.minus(newDuration))?.absoluteValue
                ?: 0) > 1000
        ) {
            player.seekTo(newDuration)
        }
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

    fun syncAlarms() {
        viewModelScope.launch {
            alarmsSynchronizer.syncAlarms()
        }
    }

    /**
     * Inner class
     */
    inner class PlayerConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            player = (service as PlayerService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            player = IdlePlayerController()
        }
    }

    inner class UpdateConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}

        override fun onServiceDisconnected(name: ComponentName?) {}
    }
}