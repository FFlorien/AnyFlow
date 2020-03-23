package be.florien.anyflow.feature

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer


abstract class ValueLiveData<T>: LiveData<T>() {
    override fun getValue(): T {
        return super.getValue() ?: throw IllegalStateException("This class shouldn't have null")
    }

    protected fun getMaybeNullValue(): T? {
        return super.getValue()
    }
}

@SuppressWarnings("WeakerAccess")
class MutableValueLiveData<T>(private val initialValue: T): ValueLiveData<T>() {
    init {
        super.setValue(initialValue)
    }

    public override fun setValue(value: T) {
        if (value != null) {
            super.setValue(value)
        }
    }

    public override fun postValue(value: T) {
        if (value != null) {
            super.postValue(value)
        }
    }

    override fun getValue(): T {
        return super.getMaybeNullValue() ?: initialValue
    }
}

fun <T> LiveData<T>.observeNullable(owner: LifecycleOwner, action: (T?) -> Unit) {
    observe(owner, Observer(action))
}

fun <T> ValueLiveData<T>.observeValue(owner: LifecycleOwner, action: (T) -> Unit) {
    observe(owner, Observer(action))
}