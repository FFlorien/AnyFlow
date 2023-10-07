package be.florien.anyflow.extension

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.postValueIfChanged(newValue: T) {
    if (newValue != value){
        postValue(newValue)
    }
}

var <T> MutableLiveData<T>.valueIfChanged: T?
    set(newValue) {
        if (newValue != value) {
            value = newValue
        }
    }
    get() = value