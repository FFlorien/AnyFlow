package be.florien.anyflow.management.playlist.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import be.florien.anyflow.data.server.NetSuccess
import be.florien.anyflow.data.server.datasource.playlist.AmpachePlaylistSource
import be.florien.anyflow.data.server.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlaylistModificationWorker(
    private val ampachePlaylistSource: AmpachePlaylistSource,
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    private val action: String? = inputData.getString(ACTION)
    private val songs: LongArray? = inputData.getLongArray(SONGS_IDS)
    private val playlist: Long = inputData.getLong(PLAYLIST_ID, -1)
    private val position = inputData.getInt(POSITION, 0)

    //region overridden methods
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                when (action) {
                    ACTION_ADD -> addToPlaylist()
                    ACTION_REMOVE -> removeFromPlaylist()
                    else -> Result.failure()
                }
            } catch (exception: Exception) {
                Result.retry()
            }
        }
    }
    //endregion

    //region api calls
    private suspend fun addToPlaylist(): Result {
        val idList = songs?.toList()
        return when {
            idList.isNullOrEmpty() -> Result.failure()
            idList.size == 1 -> {
                val netResult = ampachePlaylistSource.addToPlaylist(playlist, idList.first())
                when (netResult) {
                    is NetSuccess -> Result.success()
                    else -> {
                        netResult.logError("Add to playlist")
                        Result.retry()
                    }
                }
            }

            else -> {
                ampachePlaylistSource.addToPlaylist(playlist, idList, position)
                Result.success()
            }
        }
    }

    private suspend fun removeFromPlaylist(): Result {
        val netResults = songs?.map { songId ->
            ampachePlaylistSource.removeSongFromPlaylist(playlist, songId)
        }
        netResults?.forEach { it.logError("Remove from playlist") }
        val areAllSuccess = netResults?.all { it is NetSuccess } ?: return Result.failure()
        return if (areAllSuccess) Result.success() else Result.retry()
    }
    //endregion

    //region DI & utilities
    class Factory @Inject constructor(
        private val ampachePlaylistSource: AmpachePlaylistSource
    ) : PlaylistModificationWorkerFactory {

        override fun create(appContext: Context, params: WorkerParameters): CoroutineWorker {
            return PlaylistModificationWorker(ampachePlaylistSource, appContext, params)
        }
    }

    companion object {
        private const val PLAYLIST_ID = "PLAYLIST_ID"
        private const val SONGS_IDS = "SONGS_IDS"
        private const val POSITION = "POSITION"
        private const val ACTION = "ACTION"
        private const val ACTION_ADD = "ADD"
        private const val ACTION_REMOVE = "REMOVE"

        fun getDataForAdding(playlistId: Long, songsIds: Collection<Long>, position: Int) =
            Data.Builder()
                .putString(ACTION, ACTION_ADD)
                .putLong(PLAYLIST_ID, playlistId)
                .putLongArray(SONGS_IDS, songsIds.toLongArray())
                .putInt(POSITION, position)
                .build()

        fun getDataForRemoving(playlistId: Long, songsIds: Collection<Long>) = Data.Builder()
            .putString(ACTION, ACTION_REMOVE)
            .putLong(PLAYLIST_ID, playlistId)
            .putLongArray(SONGS_IDS, songsIds.toLongArray())
            .build()
    }
    //endregion
}

interface PlaylistModificationWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): CoroutineWorker
}