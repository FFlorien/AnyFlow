package be.florien.anyflow.management.playlist.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import be.florien.anyflow.data.server.datasource.playlist.AmpachePlaylistSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeoutException
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
            } catch (timeoutException: TimeoutException) { //todo check which exception we can catch
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
        const val PLAYLIST_ID = "PLAYLIST_ID"
        const val SONGS_IDS = "SONGS_IDS"
        const val POSITION = "POSITION"
        const val ACTION = "ACTION"
        const val ACTION_ADD = "ADD"
        const val ACTION_REMOVE = "REMOVE"
    }
}

interface PlaylistModificationWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): CoroutineWorker
}