package be.florien.anyflow.feature.player.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.annotation.OptIn
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import be.florien.anyflow.feature.download.DownloadManager
import be.florien.anyflow.feature.player.services.queue.PlayingQueue
import be.florien.anyflow.feature.player.ui.PlayerActivity
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.tags.local.PodcastPersistence
import be.florien.anyflow.tags.local.model.DbMediaToPlay
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named

const val ALARM_ACTION = "ALARM"

/**
 * Service used to handle the media player.
 */
//@UnstableApi
class PlayerService : MediaSessionService(), Player.Listener, LifecycleOwner {

    private val dispatcher = ServiceLifecycleDispatcher(this)

    override fun onBind(intent: Intent?): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    @CallSuper
    override fun onStart(intent: Intent?, startId: Int) {
        dispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    //todo Own coroutineScope
    private var mediaSession: MediaSession? = null
    private val player: Player?
        get() = mediaSession?.player

    //region injection
    @Inject
    internal lateinit var playingQueue: PlayingQueue

    @Inject
    internal lateinit var waveFormRepository: WaveFormRepository

    @Inject
    internal lateinit var urlRepository: be.florien.anyflow.tags.UrlRepository

    @Inject
    internal lateinit var downloadManager: DownloadManager

    @Inject
    internal lateinit var alarmsSynchronizer: AlarmsSynchronizer

    @Inject
    internal lateinit var filtersManager: FiltersManager

    @Inject
    internal lateinit var podcastPersistence: PodcastPersistence

    @Inject
    internal lateinit var audioManager: AudioManager

    @Named("authenticated")
    @Inject
    internal lateinit var okHttpClient: OkHttpClient

    @SuppressLint("UnsafeOptInUsageError")
    @Inject
    internal lateinit var cache: Cache
    //endregion

    //region MediaSessionService
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        dispatcher.onServicePreSuperOnCreate()
        initPlayer()
        listenToQueueChanges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ALARM_ACTION) {
            playForAlarm()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (player?.playWhenReady == false || player?.mediaItemCount == 0) {
            // Stop the service if not playing, continue playing in the background
            // otherwise.
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
    //endregion

    //region Player.Listener
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        fun MediaItem.checkWaveForm() {
            mediaId
                .toLongOrNull()
                ?.let { waveFormRepository.checkWaveForm(it, mediaItem?.mediaMetadata?.mediaType ?: MediaMetadata.MEDIA_TYPE_MUSIC) }
        }

        MainScope().launch {
            playingQueue.listPosition = player?.currentMediaItemIndex ?: playingQueue.listPosition
            player?.run {
                currentMediaItem?.checkWaveForm()
                if (nextMediaItemIndex in 0..<mediaItemCount) {
                    player?.getMediaItemAt(nextMediaItemIndex)?.checkWaveForm()
                }
            }
        }
    }
    //endregion

    //region private methods
    @SuppressLint("UnsafeOptInUsageError")
    private fun initPlayer() {
        (application as AnyFlowApp).serverComponent?.inject(this)
        val dataSourceFactory = DefaultDataSource.Factory(
            this, CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(okHttpClient))
        )

        val exoPlayer = ExoPlayer
            .Builder(this, DefaultRenderersFactory(this))
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            .build()
            .apply {
                addListener(this@PlayerService)
            }
        val forwardPlayer = object : ForwardingPlayer(exoPlayer) {
            override fun seekToNext() {
                if (player?.currentMediaItem?.mediaMetadata?.mediaType == MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE) {
                    super.seekForward()
                } else {
                    super.seekToNext()
                }
            }

            override fun seekToPrevious() {
                if (player?.currentMediaItem?.mediaMetadata?.mediaType == MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE) {
                    super.seekBack()
                } else {
                    super.seekToPrevious()
                }
            }
        }

        val intent = Intent(this, PlayerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        mediaSession =
            MediaSession
                .Builder(this, forwardPlayer)
                .setSessionActivity(pendingIntent)
                .build()

        playingQueue.currentMedia.observe(this) { mediaItem ->
            if (mediaItem?.mediaType == PODCAST_MEDIA_TYPE) {
                player?.seekTo(podcastPersistence.getPodcastPosition(mediaItem.id))
            }
        }
    }

    private fun listenToQueueChanges() {
        MainScope().launch {
            playingQueue
                .mediaIdsListUpdater
                .distinctUntilChanged()
                .map { songList -> songList.map { it.toMediaItem() } }
                .flowOn(Dispatchers.Default)
                .map { songList ->
                    player?.run {
                        val currentItem = currentMediaItem
                        val state = if (playbackState == Player.STATE_IDLE || currentItem == null) {
                            PlayerPlaylistState.Unprepared
                        } else {
                            PlayerPlaylistState.Prepared(
                                currentMediaItem = currentItem,
                                currentPosition = currentMediaItemIndex,
                                isFirst = currentMediaItemIndex == 0,
                                isLast = currentMediaItemIndex == mediaItemCount - 1

                            )
                        }
                        StateAndSongs(state, songList)
                    }
                }
                .filterNotNull()
                .flowOn(Dispatchers.Main)
                .map {
                    PlaylistModification.Factory.getImplementation(
                        it.songList,
                        it.playlistState,
                        playingQueue.listPosition
                    )
                }
                .flowOn(Dispatchers.Default)
                .collect { playlistModification ->
                    player?.let { safePlayer ->
                        playlistModification.applyModification(safePlayer)
                        playingQueue.listPosition = safePlayer.currentMediaItemIndex
                        val mediaItem = playingQueue.currentMedia.value
                        if (mediaItem?.mediaType == PODCAST_MEDIA_TYPE) {
                            player?.seekTo(podcastPersistence.getPodcastPosition(mediaItem.id))
                        }
                    }
                }
        }
    }

    private fun playForAlarm() {
        MainScope().launch(Dispatchers.Default) {
            alarmsSynchronizer.syncAlarms()
        }
        val connectivityManager =
            applicationContext.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true) {
            filtersManager.clearFilters()
            filtersManager.addFilter(
                be.florien.anyflow.management.filters.model.Filter(
                    be.florien.anyflow.management.filters.model.Filter.FilterType.DOWNLOADED_STATUS_IS,
                    true,
                    "",
                    emptyList()
                )
            )
            MainScope().launch(Dispatchers.Default) {
                filtersManager.commitChanges()
            }
        }
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume.div(3), 0)
        player?.run {
            prepare()
            play()
        }
    }

    private fun DbMediaToPlay.toMediaItem(): MediaItem {
        val mediaType = if (mediaType == SONG_MEDIA_TYPE) {
            "song"
        } else {
            "podcast_episode"
        }
        val songUrl = urlRepository.getMediaUrl(id, mediaType)
        val mediaTypeMetaData =
            if (this.mediaType == SONG_MEDIA_TYPE) MediaMetadata.MEDIA_TYPE_MUSIC else MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE
        return MediaItem.Builder().setMediaMetadata(
            MediaMetadata
                .Builder()
                .setMediaType(mediaTypeMetaData)
                .build()
        ).setUri(Uri.parse(songUrl)).setMediaId(this.id.toString()).build()
    }
    //endregion
}

private data class StateAndSongs(
    val playlistState: PlayerPlaylistState,
    val songList: List<MediaItem>
)

private sealed interface PlayerPlaylistState {
    data object Unprepared : PlayerPlaylistState
    data class Prepared(
        val currentMediaItem: MediaItem,
        val currentPosition: Int,
        val isFirst: Boolean,
        val isLast: Boolean
    ) : PlayerPlaylistState
}

private sealed interface PlaylistModification {
    suspend fun applyModification(player: Player)

    data class InitialSetup(val songList: List<MediaItem>, val position: Int) :
        PlaylistModification {
        override suspend fun applyModification(player: Player) {
            withContext(Dispatchers.Main) {
                player.setMediaItems(songList)
                player.seekToDefaultPosition(position)
                player.prepare()
            }
        }
    }

    data class SongNotPresent(val songList: List<MediaItem>) : PlaylistModification {
        override suspend fun applyModification(player: Player) {
            player.setMediaItems(songList)
        }
    }

    data class SongPresent(
        val futurePosition: Int,
        val previousSongs: List<MediaItem>,
        val nextSongs: List<MediaItem>
    ) : PlaylistModification {
        override suspend fun applyModification(player: Player) {
            if (previousSongs.isNotEmpty()) {
                if (player.currentMediaItemIndex == 0) {
                    player.addMediaItems(0, previousSongs)
                } else {
                    player.replaceMediaItems(0, player.currentMediaItemIndex, previousSongs)
                }
            }
            if (nextSongs.isNotEmpty()) {
                if (player.currentMediaItemIndex == player.mediaItemCount - 1) {
                    player.addMediaItems(nextSongs)
                } else {
                    player.replaceMediaItems(
                        player.currentMediaItemIndex + 1,
                        Int.MAX_VALUE,
                        nextSongs
                    )
                }
            }
        }
    }

    object Factory {
        fun getImplementation(
            songList: List<MediaItem>,
            playlistState: PlayerPlaylistState,
            savedPosition: Int
        ): PlaylistModification =
            if (playlistState is PlayerPlaylistState.Prepared) {
                val nextPosition = songList.indexOf(playlistState.currentMediaItem)
                if (nextPosition == -1) {
                    SongNotPresent(songList)
                } else {
                    val previousSongs =
                        songList.takeIf { nextPosition > 0 }?.subList(0, nextPosition)
                            ?: emptyList()
                    val nextSongs = songList.takeIf { nextPosition < songList.size - 1 }
                        ?.subList(nextPosition + 1, songList.size) ?: emptyList()
                    SongPresent(nextPosition, previousSongs, nextSongs)
                }
            } else {
                InitialSetup(songList, savedPosition)
            }
    }
}

/*


    @OptIn(UnstableApi::class)
    override fun onPlayerError(error: PlaybackException) {

        fun resetItems() {
            MainScope().launch {
                val firstItem = dbSongToMediaItem(playingQueue.stateUpdater.value?.currentSong)
                val secondItem = dbSongToMediaItem(playingQueue.stateUpdater.value?.nextSong)
                mediaPlayer.setMediaItems(listOfNotNull(firstItem, secondItem))
                mediaPlayer.seekTo(0, C.TIME_UNSET)
                prepare()
            }
        }

        if (
            error is ExoPlaybackException
            && error.type == ExoPlaybackException.TYPE_SOURCE
            && error.sourceException is UnrecognizedInputFormatException
        ) {
            fixSourceError(error)
        }

        if (
            (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 403
            || (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 400
        ) {
            (stateChangeNotifier as MutableLiveData).value = PlayerController.State.RECONNECT
            exoplayerScope.launch {
                resetItems()
            }
        } else if (
            error is ExoPlaybackException
            && error.cause is IllegalStateException
            && error.cause?.message?.contains(
                "Playback stuck buffering and not loading",
                false
            ) == true
        ) {
            val position = mediaPlayer.currentPosition
            prepare()
            seekTo(position)
            resume()
        } else {
            eLog(error, "Error while playback")
        }
    }

    @OptIn(UnstableApi::class)
    private fun fixSourceError(error: ExoPlaybackException) {
        val sourceException = error.sourceException as UnrecognizedInputFormatException
        val uri = sourceException.uri
        val songId = uri.getQueryParameter("id")?.toLongOrNull()
        if (songId != null) {
            fixRemoteSourceError(songId)
        } else if (uri.scheme?.equals("content") == true) {
            fixLocalSourceError()
        }
    }

    private fun fixLocalSourceError() {
        mediaSession?.player?.clearMediaItems()
        MainScope().launch { //todo mark as faulty download / try again
            downloadManager.removeDownload(playingQueue.currentSong.value?.id)
        }
    }

    private fun fixRemoteSourceError(songId: Long) {
        MainScope().launch(Dispatchers.IO) { //todo this is probably ugly
            //todo wow, this is ugly. Intercept response ?
            val ampacheError = ampacheDataSource.getStreamError(songId)
            if (ampacheError.error.errorCode == 4701) {
                resetItems()
            }
        }
    }
 */