package be.florien.anyflow.common.utils

import android.content.res.Resources
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration

object TimeOperations {

    private const val AMPACHE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZ"
    private const val AMPACHE_REQUEST_DATE_FORMAT = "yyyy-MM-dd"
    private const val DISPLAY_DATE_FORMAT = "dd/MM/yyyy"
    private const val DISPLAY_MONTH_DATE_FORMAT = "MM/yyyy"
    private const val DISPLAY_DATETIME_FORMAT = "dd/MM/yyyy HH:mm"
    private val ampacheCompleteFormatter =
        SimpleDateFormat(AMPACHE_DATE_FORMAT, Locale.getDefault())
    private val ampacheRequestFormatter =
        SimpleDateFormat(AMPACHE_REQUEST_DATE_FORMAT, Locale.getDefault())
    private val displayFormatter =
        SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault())
    private val displayMonthFormatter =
        SimpleDateFormat(DISPLAY_MONTH_DATE_FORMAT, Locale.getDefault())
    private val displayDateTimeFormatter =
        SimpleDateFormat(DISPLAY_DATETIME_FORMAT, Locale.getDefault())
    var currentTimeUpdater: CurrentTimeUpdater? = null

    fun getCurrentMillis() = Date().time

    fun getCurrentDate(): Calendar {
        var calendar = Calendar.getInstance()
        calendar = currentTimeUpdater?.getCurrentTimeUpdated(calendar) ?: calendar
        return calendar
    }

    fun getCurrentDatePlus(field: Int, increment: Int): Calendar {
        var calendar = Calendar.getInstance().apply {
            add(field, increment)
        }
        calendar = currentTimeUpdater?.getCurrentTimeUpdated(calendar) ?: calendar
        return calendar
    }

    fun getDateFromMillis(millis: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = millis
    }

    fun getDateFromAmpacheComplete(formatted: String): Calendar = Calendar.getInstance().apply {
        time = ampacheCompleteFormatter.parse(formatted)
            ?: throw IllegalArgumentException("The provided string could not be parsed to an ampache date")
    }

    fun getAmpacheCompleteFormatted(time: Calendar): String =
        ampacheRequestFormatter.format(time.time)

    data class MediaDuration(
        val days: Int?,
        val hours: Int?,
        val minutes: Int?,
        val seconds: Int?
    )

    fun Int?.getDurationString(resources: Resources, resourceInt: Int) =
        this?.let { resources.getQuantityString(resourceInt, it, it) } ?: ""

    fun toMediaDuration(duration: Duration) =
        duration.toComponents { days, hours, minutes, seconds, _ ->

            MediaDuration(
                days.toInt().takeIf { it > 0 },
                hours.takeIf { it > 0 || days > 0 },
                minutes.takeIf { it > 0 || days > 0 || hours > 0 },
                seconds.takeIf { it > 0 || days > 0 || hours > 0 || minutes > 0 })
        }

    fun toShortDuration(timeInSeconds: Int) = if (timeInSeconds < (60 * 60)) {
        String.format(Locale.getDefault(), "%d:%02d", timeInSeconds / 60, timeInSeconds % 60)
    } else {
        String.format(
            Locale.getDefault(),
            "%d:%02d:%02d",
            timeInSeconds / (60 * 60),
            (timeInSeconds / 60) % 60,
            timeInSeconds % 60
        )
    }

    fun toDisplayDate(date: Long): String = displayFormatter.format(Date(date))
    fun toDisplayMonthDate(date: Long): String = displayMonthFormatter.format(Date(date))

    fun toDisplayDateTime(date: Long): String = displayDateTimeFormatter.format(Date(date))

    interface CurrentTimeUpdater {
        fun getCurrentTimeUpdated(current: Calendar): Calendar
    }

}