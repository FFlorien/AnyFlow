package be.florien.anyflow.common.ui.domain

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import be.florien.anyflow.common.utils.TimeOperations

@Immutable
data class TextConfig( //todo sealed class to avoid impossible states
    private val text: String?,
    @param:StringRes private val textRes: Int?,
    private val duration: TimeOperations.MediaDuration? = null,
    private val style: TextConfigStyle = TextConfigStyle.NORMAL,
    private val nextTextConfig: TextConfig? = null
) {

    constructor(
        text: String?,
        style: TextConfigStyle = TextConfigStyle.NORMAL,
        nextTextConfig: TextConfig? = null
    ) : this(text, null, null, style, nextTextConfig)

    constructor(
        textRes: Int?,
        style: TextConfigStyle = TextConfigStyle.NORMAL,
        nextTextConfig: TextConfig? = null
    ) : this(null, textRes, null, style, nextTextConfig)

    constructor(
        mediaDuration: TimeOperations.MediaDuration,
        style: TextConfigStyle = TextConfigStyle.NORMAL,
        nextTextConfig: TextConfig? = null
    ) : this(null, null, mediaDuration, style, nextTextConfig)

    fun getText(resources: Resources): String {
        return style.startTag +
                getString(resources) +
                style.endTag +
                (nextTextConfig?.getText(resources)?.let { " $it" } ?: "")
    }

    fun Int?.getDurationString(resources: Resources, resourceInt: Int) =
        this?.let { resources.getQuantityString(resourceInt, it, it) } ?: ""

    private fun getString(resources: Resources) = when {
        text == null && textRes != null -> resources.getString(textRes)
        text != null && textRes == null -> text
        text != null && textRes != null -> resources.getString(textRes, text)
        duration != null -> duration.days.getDurationString(resources, R.plurals.days_component) +
                duration.hours.getDurationString(resources, R.plurals.hours_component) +
                duration.minutes.getDurationString(resources, R.plurals.minutes_component) +
                duration.seconds.getDurationString(resources, R.plurals.seconds_component)

        else -> ""
    }
}

enum class TextConfigStyle(val startTag: String, val endTag: String) {
    NORMAL("", ""),
    BOLD("<b>", "</b>"),
    ITALIC("<i>", "</i>"),
}