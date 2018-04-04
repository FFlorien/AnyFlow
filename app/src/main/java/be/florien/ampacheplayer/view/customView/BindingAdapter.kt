package be.florien.ampacheplayer.view.customView

import android.databinding.BindingAdapter
import android.databinding.InverseBindingAdapter
import android.databinding.InverseBindingListener
import kotlin.math.absoluteValue


@BindingAdapter(value = ["currentDurationAttrChanged"])
fun setListener(controls: PlayerControls, listener: InverseBindingListener) {
    controls.onCurrentDurationChanged = object : PlayerControls.OnCurrentDurationChangedListener {
        override fun onCurrentDurationChanged() {
            listener.onChange()
        }
    }
}

@BindingAdapter("app:currentDuration")
fun PlayerControls.setCurrentDuration(value: Int) {
    if ((currentDuration - value).absoluteValue < 1000) {
        currentDuration = value
    }
}

@InverseBindingAdapter(attribute = "app:currentDuration")
fun PlayerControls.getCurrentDuration(): Int = currentDuration