package be.florien.anyflow.data

import be.florien.anyflow.R
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.PlatformScheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import javax.inject.Inject


class AmpacheDownloadService : DownloadService(3) {

    @Inject
    lateinit var downloadManagerInjected: DownloadManager

    override fun getDownloadManager(): DownloadManager {
        (application as be.florien.anyflow.AnyFlowApp).userComponent?.inject(this)
        return downloadManagerInjected
    }

    override fun getScheduler() = PlatformScheduler(this, 5)

    override fun getForegroundNotification(downloads: MutableList<Download>) =
            DownloadNotificationHelper(this, DOWNLOAD_CHANNEL)
                    .buildProgressNotification(this, R.drawable.notif, null, null, downloads)

    companion object {
        const val DOWNLOAD_CHANNEL = "AnyFlow downloader"
    }
}