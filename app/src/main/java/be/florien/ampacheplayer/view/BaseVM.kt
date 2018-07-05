package be.florien.ampacheplayer.view

import android.databinding.BaseObservable
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

open class BaseVM : BaseObservable() {

    private val disposableContainerMap = mutableMapOf<String, CompositeDisposable>()

    open fun destroy() {
        for (container in disposableContainerMap.values) {
            container.clear()
        }
    }

    fun <T> subscribe(observable: Observable<T>, onNext: ((T) -> Unit), onError: ((Throwable) -> Unit) = this::timberLogOnError, onComplete: (() -> Unit) = this::doNothingOnComplete, containerKey: String = "Default") {
        getContainer(containerKey).add(observable.observeOn(AndroidSchedulers.mainThread()).subscribe(onNext, onError, onComplete))
    }

    fun subscribe(completable: Completable, onError: (Throwable) -> Unit = this::timberLogOnError, onComplete: () -> Unit = this::doNothingOnComplete, containerKey: String = "Default") {
        getContainer(containerKey).add(completable.observeOn(AndroidSchedulers.mainThread()).subscribe(onComplete, onError))
    }

    fun <T> subscribe(flowable: Flowable<T>, onNext: ((T) -> Unit), onError: ((Throwable) -> Unit) = this::timberLogOnError, onComplete: (() -> Unit) = this::doNothingOnComplete, containerKey: String = "Default") {
        getContainer(containerKey).add(flowable.observeOn(AndroidSchedulers.mainThread()).doOnNext(onNext).doOnError(onError).doOnComplete(onComplete).subscribe())
    }

    fun dispose(containerKey : String) {
        disposableContainerMap[containerKey]?.clear()
    }

    private fun getContainer(containerKey: String): CompositeDisposable {
        val container = disposableContainerMap[containerKey]
        return if (container == null) {
            val newContainer = CompositeDisposable()
            disposableContainerMap[containerKey] = newContainer
            newContainer
        } else {
            container
        }
    }

    private fun doNothingOnComplete() {
    }

    private fun timberLogOnError(throwable: Throwable) {
        Timber.e(throwable, "Standard error message from a ViewModel")
    }
}