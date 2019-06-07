package be.florien.anyflow.persistence

import android.app.job.JobParameters
import android.app.job.JobService
import be.florien.anyflow.local.DownloadHelper
import be.florien.anyflow.persistence.local.LibraryDatabase
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class DownloadStatusService : JobService() {

    @Inject
    lateinit var libraryDatabase: LibraryDatabase

    @Inject
    lateinit var downloadHelper: DownloadHelper

    private var subscription: Disposable? = null

    override fun onStartJob(p0: JobParameters?): Boolean {
        (application as be.florien.anyflow.AnyFlowApp).userComponent?.inject(this)
        subscription = libraryDatabase
                .getSongsWithPendingDownloadStatus()
                .flatMapCompletable { songs ->
                    libraryDatabase.updateDownloaded(songs.filter { downloadHelper.isFileExisting(it) })
                }//todo error handling, i suppose
                //todo handle downloaded that doesn't exist
                .doOnComplete {
                    subscription?.dispose()
                    jobFinished(p0, true)
                }
                .subscribe()
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        subscription?.dispose()
        return true
    }
}