package be.florien.anyflow.feature

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel() {

    /**
     * LiveData
     */

    protected val <T> LiveData<T>.mutable: MutableLiveData<T>
            get() = this as MutableLiveData<T>

    protected val <T> ValueLiveData<T>.mutable: MutableValueLiveData<T>
            get() = this as MutableValueLiveData<T>
}