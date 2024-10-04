package be.florien.anyflow.extension

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.postValueIfChanged(newValue: T) {
    if (newValue != value){
        postValue(newValue)
    }
}
