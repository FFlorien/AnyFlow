package be.florien.anyflow.management.playlist.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import be.florien.anyflow.data.server.datasource.playlist.AmpachePlaylistSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlaylistModificationWorker(
    private val ampachePlaylistSource: AmpachePlaylistSource,
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            try {
                if (inputData.getString(ACTION) == ACTION_ADD) {
                    ampachePlaylistSource.addToPlaylist(
                        inputData.getLong(PLAYLIST_ID, -1),
                        inputData.getLongArray(SONGS_IDS)?.toList() ?: emptyList(),
                        inputData.getInt(POSITION, 0)
                    )
                } else if (inputData.getString(ACTION) == ACTION_REMOVE) {
                    inputData.getLongArray(SONGS_IDS)?.forEach {
                        ampachePlaylistSource.removeSongFromPlaylist(
                            inputData.getLong(PLAYLIST_ID, -1),
                            it
                        )
                    }
                }
                Unit //trick to avoid the above "if" being linted
            } catch (exception: Exception) {
                return@withContext Result.retry()
            }
        }
        return Result.success()
    }

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

        fun getDataForAdding(playlistId: Long, songsIds: Collection<Long>, position: Int) = Data.Builder()
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
}

interface PlaylistModificationWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): CoroutineWorker
}