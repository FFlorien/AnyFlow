package be.florien.anyflow.common.ui.data

import android.content.res.Resources
import androidx.annotation.StringRes

data class TextConfig(
    private val text: String?,
    @StringRes private val textRes: Int?
) {

    fun getText(resources: Resources): String = when {
        text == null && textRes != null -> resources.getString(textRes)
        text != null && textRes == null -> text
        text != null && textRes != null -> resources.getString(textRes, text)
        else -> ""
    }
}