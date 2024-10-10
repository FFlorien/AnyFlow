package be.florien.anyflow.feature.alarm.ui

import android.widget.TimePicker
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener


@BindingAdapter("time")
fun TimePicker.setTimeBinding(time: Int) {
    hour = time / 60
    minute = time % 60
}

@InverseBindingAdapter(attribute = "time")
fun TimePicker.getHourBinding(): Int {
    return hour * 60 + minute
}

@BindingAdapter("timeAttrChanged")
fun setListenersForHour(
    view: TimePicker,
    attrChange: InverseBindingListener
) {
    view.setOnTimeChangedListener { _, _, _ -> attrChange.onChange() }
}