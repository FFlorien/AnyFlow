package be.florien.anyflow.common.ui.data

import android.content.res.Resources
import android.text.Spanned
import androidx.annotation.StringRes

data class TextConfig(
    private val text: String?,
    @param:StringRes private val textRes: Int?,
    val textSpanned: Spanned? = null,
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
        textSpanned: Spanned?
    ) : this(null, null, textSpanned, TextConfigStyle.NORMAL, null)

    fun getText(resources: Resources): String {
        return style.startTag +
                getString(resources) +
                style.endTag +
                (nextTextConfig?.getText(resources)?.let { " $it" } ?: "")
    }

    fun getText(): String = text ?: nextTextConfig?.getText() ?: ""

    private fun getString(resources: Resources) = when {
        text == null && textRes != null -> resources.getString(textRes)
        text != null && textRes == null -> text
        text != null && textRes != null -> resources.getString(textRes, text)
        else -> ""
    }
}

enum class TextConfigStyle(val startTag: String, val endTag: String) {
    NORMAL("", ""),
    BOLD("<b>", "</b>"),
    ITALIC("<i>", "</i>"),
}