package be.florien.anyflow.data

import be.florien.anyflow.R
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.PlatformScheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import java.util.concurrent.Executor
import javax.inject.Inject


class AmpacheDownloadService : DownloadService(3) {

    @Inject
    lateinit var cache: Cache

    @Inject
    lateinit var databaseProvider: ExoDatabaseProvider

    override fun getDownloadManager(): DownloadManager {
        (application as be.florien.anyflow.AnyFlowApp).userComponent?.inject(this)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        val downloadExecutor = Executor { obj: Runnable -> obj.run() }
        return DownloadManager(
                this,
                databaseProvider,
                cache,
                dataSourceFactory,
                downloadExecutor)
    }

    override fun getScheduler() = PlatformScheduler(this, 5)

    override fun getForegroundNotification(downloads: MutableList<Download>) =
            DownloadNotificationHelper(this, DOWNLOAD_CHANNEL)
                    .buildProgressNotification(this, R.drawable.notif, null, null, downloads)

    companion object {
        const val DOWNLOAD_CHANNEL = "AnyFlow downloader"
    }
}