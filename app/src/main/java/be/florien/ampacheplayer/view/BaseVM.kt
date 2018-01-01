package be.florien.ampacheplayer.view

import android.databinding.BaseObservable
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

    fun <T> subscribe(observable: Observable<T>, onNext: ((T) -> Unit), onError: ((Throwable) -> Unit) = this::timberLogOnError, onComplete: (() -> Unit) = this::doNothingOnComplete) {
        subscribe(observable,onNext, onError, onComplete, "DEFAULT")
    }

    fun <T> subscribe(observable: Observable<T>, onNext: ((T) -> Unit), onError: ((Throwable) -> Unit) = this::timberLogOnError, onComplete: (() -> Unit) = this::doNothingOnComplete, containerKey: String) {
        var container = disposableContainerMap[containerKey]
        if (container == null) {
            container = CompositeDisposable()
            disposableContainerMap[containerKey] = container
        }
        container.add(observable.observeOn(AndroidSchedulers.mainThread()).subscribe(onNext, onError, onComplete))
    }

    fun dispose(containerKey : String) {
        disposableContainerMap[containerKey]?.clear()
    }

    private fun doNothingOnComplete() {
    }

    private fun timberLogOnError(throwable: Throwable) {
        Timber.e(throwable, "Standard error message from a ViewModel")
    }
}