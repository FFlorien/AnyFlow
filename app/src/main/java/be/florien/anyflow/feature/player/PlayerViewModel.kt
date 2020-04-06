package be.florien.anyflow.feature.player

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.view.Order
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_ALBUM
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_ALBUM_ARTIST
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_ALL
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_TITLE
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_TRACK
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_YEAR
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.MutableValueLiveData
import be.florien.anyflow.feature.ValueLiveData
import be.florien.anyflow.feature.customView.PlayPauseIconAnimator
import be.florien.anyflow.feature.customView.PlayerControls
import be.florien.anyflow.player.IdlePlayerController
import be.florien.anyflow.player.PlayerController
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.player.PlayingQueue
import io.reactivex.android.schedulers.AndroidSchedulers
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
        private val dataRepository: DataRepository,
        val connectionStatus: ValueLiveData<AmpacheConnection.ConnectionStatus>,
        @Named("Songs")
        val songsUpdatePercentage: ValueLiveData<Int>,
        @Named("Albums")
        val albumsUpdatePercentage: ValueLiveData<Int>,
        @Named("Artists")
        val artistsUpdatePercentage: ValueLiveData<Int>) : BaseViewModel(), PlayerControls.OnActionListener {

    companion object {
        const val PLAYING_QUEUE_CONTAINER = "PlayingQueue"
        const val PLAYER_CONTROLLER_CONTAINER = "PlayerController"
    }

    internal val playerConnection: PlayerConnection = PlayerConnection()
    internal val updateConnection: UpdateConnection = UpdateConnection()
    private var isBackKeyPreviousSong: Boolean = false

    /**
     * Bindables
     */
    val shouldShowBuffering : ValueLiveData<Boolean> = MutableValueLiveData(false)
    val state : ValueLiveData<Int> = MutableValueLiveData(PlayPauseIconAnimator.STATE_PLAY_PAUSE_BUFFER)
    val isOrdered : ValueLiveData<Boolean> = MutableValueLiveData(true)
    val currentDuration : ValueLiveData<Int> = MutableValueLiveData(0)
    val totalDuration : ValueLiveData<Int> = MutableValueLiveData(0)

    val isNextPossible : ValueLiveData<Boolean> = MutableValueLiveData(true)
    val isPreviousPossible : ValueLiveData<Boolean> = MutableValueLiveData(true)

    var player: PlayerController = IdlePlayerController()
        set(value) {
            dispose(PLAYER_CONTROLLER_CONTAINER)
            field = value
            subscribe(
                    field.stateChangeNotifier.subscribeOn(AndroidSchedulers.mainThread()),
                    onNext = {
                        shouldShowBuffering.mutable.value = it == PlayerController.State.BUFFER
                        state.mutable.value = when (it) {
                            PlayerController.State.PLAY -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY
                            PlayerController.State.PAUSE -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
                            else -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_BUFFER
                        }
                    },
                    onError = {
                        this@PlayerViewModel.eLog(it, "error while retrieving the state")
                    },
                    containerKey = PLAYER_CONTROLLER_CONTAINER)
            subscribe(
                    observable = player.playTimeNotifier,
                    onNext = {
                        isBackKeyPreviousSong = it.toInt() < 10000
                        currentDuration.mutable.value = it.toInt()
                    },
                    onError = {
                        this@PlayerViewModel.eLog(it, "error while retrieving the playtime")
                    },
                    containerKey = PLAYER_CONTROLLER_CONTAINER)
        }

    /**
     * Constructor
     */
    init {
        subscribe(
                flowable = playingQueue.isOrderedUpdater,
                onNext = { isOrdered ->
                    this.isOrdered.mutable.value = isOrdered
                },
                containerKey = PLAYING_QUEUE_CONTAINER)
        subscribe(
                flowable = playingQueue.currentSongUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    totalDuration.mutable.value = (it?.time ?: 0) * 1000
                    currentDuration.mutable.value = 0
                },
                containerKey = PLAYING_QUEUE_CONTAINER)
        subscribe(
                playingQueue.positionUpdater,
                onNext = {
                    isNextPossible.mutable.value = playingQueue.listPosition < playingQueue.itemsCount - 1
                    isPreviousPossible.mutable.value = playingQueue.listPosition != 0
                }
        )
    }

    /**
     * PlayerControls.OnActionListener methods
     */
    override fun onPreviousClicked() {
        if (isBackKeyPreviousSong) {
            playingQueue.listPosition -= 1
        } else {
            player.play()
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
        if ((currentDuration.value - newDuration).absoluteValue > 1000) {
            player.seekTo(newDuration)
        }
    }

    fun randomOrder() {
        val orders = mutableListOf(Order(0, SUBJECT_ALL))
        playingQueue.currentSong?.let { song ->
            orders.add(Order(0, song))
        }
        subscribe(dataRepository.setOrders(orders))
    }

    fun classicOrder() {
        subscribe(dataRepository.setOrdersSubject(listOf(SUBJECT_ALBUM_ARTIST, SUBJECT_YEAR, SUBJECT_ALBUM, SUBJECT_TRACK, SUBJECT_TITLE)))
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