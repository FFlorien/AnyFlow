package be.florien.ampacheplayer.view.viewmodel

import android.databinding.BaseObservable
import android.databinding.ViewDataBinding
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

open class BaseVM<out B : ViewDataBinding>(val binding: B) : BaseObservable() {

    private val disposableContainer = CompositeDisposable()

    open fun destroy() {
        binding.unbind()
        disposableContainer.clear()
    }

    fun <T> subscribe(observable: Observable<T>, onNext: ((T) -> Unit), onError: ((Throwable) -> Unit) = this::timberLogOnError, onComplete: (() -> Unit) = this::doNothingOnComplete) {
        disposableContainer.add(observable.observeOn(AndroidSchedulers.mainThread()).subscribe(onNext, onError, onComplete))
    }

    private fun doNothingOnComplete() {
    }

    private fun timberLogOnError(throwable: Throwable) {
        Timber.e(throwable, "Standard error message from a ViewModel")
    }

    open fun onViewCreated() {
    }
}