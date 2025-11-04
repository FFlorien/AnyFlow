package be.florien.anyflow.common.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class BaseViewModel : ViewModel() {

    /**
     * Observable
     */

    protected val <T> LiveData<T>.mutable: MutableLiveData<T>
            get() = this as MutableLiveData<T>
    protected val <T> StateFlow<T>.mutable: MutableStateFlow<T>
            get() = this as MutableStateFlow<T>
    protected val <T> Flow<T>.mutable
        get() = this as MutableStateFlow<T>
}