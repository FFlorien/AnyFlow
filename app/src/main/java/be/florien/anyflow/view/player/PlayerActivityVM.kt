package be.florien.anyflow.view.player

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.databinding.Bindable
import be.florien.anyflow.BR
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.persistence.server.AmpacheConnection
import be.florien.anyflow.player.*
import be.florien.anyflow.player.Order.Companion.SUBJECT_ALBUM
import be.florien.anyflow.player.Order.Companion.SUBJECT_ALBUM_ARTIST
import be.florien.anyflow.player.Order.Companion.SUBJECT_ALL
import be.florien.anyflow.player.Order.Companion.SUBJECT_TITLE
import be.florien.anyflow.player.Order.Companion.SUBJECT_TRACK
import be.florien.anyflow.player.Order.Companion.SUBJECT_YEAR
import be.florien.anyflow.view.BaseVM
import be.florien.anyflow.view.customView.PlayPauseIconAnimator
import be.florien.anyflow.view.customView.PlayerControls
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.absoluteValue

/**
 * ViewModel for the PlayerActivity
 */
@ActivityScope
class PlayerActivityVM
@Inject
constructor(
        private val playingQueue: PlayingQueue,
        private val libraryDatabase: LibraryDatabase,
        connectionStatusUpdater: Observable<AmpacheConnection.ConnectionStatus>,
        @Named("Songs")
        songsPercentageUpdater: Observable<Int>,
        @Named("Albums")
        albumsPercentageUpdater: Observable<Int>,
        @Named("Artists")
        artistsPercentageUpdater: Observable<Int>) : BaseVM(), PlayerControls.OnActionListener {

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

    @Bindable
    var shouldShowBuffering: Boolean = false

    @Bindable
    var connectionStatus: AmpacheConnection.ConnectionStatus = AmpacheConnection.ConnectionStatus.CONNECTED

    @Bindable
    var songsUpdatePercentage: Int = 0

    @Bindable
    var artistsUpdatePercentage: Int = 0

    @Bindable
    var albumsUpdatePercentage: Int = 0

    @Bindable
    var state: Int = PlayPauseIconAnimator.STATE_PLAY_PAUSE_BUFFER

    @Bindable
    var isOrdered: Boolean = true

    @Bindable
    var currentDuration: Int = 0

    @Bindable
    var totalDuration: Int = 0

    @Bindable
    fun isNextPossible(): Boolean = playingQueue.listPosition < playingQueue.itemsCount - 1

    @Bindable
    fun isPreviousPossible(): Boolean = playingQueue.listPosition != 0 || currentDuration > 10000

    var player: PlayerController = IdlePlayerController()
        set(value) {
            dispose(PLAYER_CONTROLLER_CONTAINER)
            field = value
            subscribe(
                    field.stateChangeNotifier.subscribeOn(AndroidSchedulers.mainThread()),
                    onNext = {
                        shouldShowBuffering = it == PlayerController.State.BUFFER
                        state = when (it) {
                            PlayerController.State.PLAY -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY
                            PlayerController.State.PAUSE -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
                            else -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_BUFFER
                        }
                        notifyPropertyChanged(BR.shouldShowBuffering)
                        notifyPropertyChanged(BR.state)
                    },
                    onError = {
                        this@PlayerActivityVM.eLog(it, "error while retrieving the state")
                    },
                    containerKey = PLAYER_CONTROLLER_CONTAINER)
            subscribe(
                    observable = player.playTimeNotifier,
                    onNext = {
                        currentDuration = it.toInt()
                        isBackKeyPreviousSong = currentDuration < 10000
                        notifyPropertyChanged(BR.currentDuration)
                    },
                    onError = {
                        this@PlayerActivityVM.eLog(it, "error while retrieving the playtime")
                    },
                    containerKey = PLAYER_CONTROLLER_CONTAINER)
        }

    /**
     * Constructor
     */
    init {
        subscribe(
                flowable = playingQueue.isRandomUpdater,
                onNext = { isRandom ->
                    isOrdered = !isRandom
                    notifyPropertyChanged(BR.isOrdered)
                },
                containerKey = PLAYING_QUEUE_CONTAINER)
        subscribe(
                flowable = playingQueue.currentSongUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    totalDuration = (it?.time ?: 0) * 1000
                    currentDuration = 0
                    notifyPropertyChanged(BR.currentDuration)
                    notifyPropertyChanged(BR.totalDuration)
                },
                containerKey = PLAYING_QUEUE_CONTAINER)
        subscribe(
                observable = playingQueue.positionUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    notifyPropertyChanged(BR.nextPossible)
                    notifyPropertyChanged(BR.previousPossible)
                    notifyPropertyChanged(BR.currentDuration)
                },
                containerKey = PLAYING_QUEUE_CONTAINER)
        subscribe(
                observable = connectionStatusUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    connectionStatus = it
                    notifyPropertyChanged(BR.connectionStatus)
                }
        )
        subscribe(
                observable = songsPercentageUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    songsUpdatePercentage = it
                    notifyPropertyChanged(BR.songsUpdatePercentage)
                }
        )
        subscribe(
                observable = artistsPercentageUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    artistsUpdatePercentage = it
                    notifyPropertyChanged(BR.artistsUpdatePercentage)
                }
        )
        subscribe(
                observable = albumsPercentageUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    albumsUpdatePercentage = it
                    notifyPropertyChanged(BR.albumsUpdatePercentage)
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
        if ((currentDuration - newDuration).absoluteValue > 1000) {
            player.seekTo(newDuration)
        }
    }

    fun randomOrder() {
        val orders = mutableListOf(Order(0, SUBJECT_ALL).toDbOrder())
        playingQueue.currentSong?.let { song ->
            orders.add(Order(0, song).toDbOrder())
        }
        subscribe(libraryDatabase.setOrders(orders))
    }

    fun classicOrder() {
        subscribe(libraryDatabase.setOrdersSubject(listOf(SUBJECT_ALBUM_ARTIST, SUBJECT_YEAR, SUBJECT_ALBUM, SUBJECT_TRACK, SUBJECT_TITLE)))
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