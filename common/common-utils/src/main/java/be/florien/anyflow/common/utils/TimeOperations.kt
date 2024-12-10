package be.florien.anyflow.common.utils

import android.content.res.Resources
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration

object TimeOperations {

    private const val AMPACHE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZ"
    private const val AMPACHE_REQUEST_DATE_FORMAT = "yyyy-MM-dd"
    private val ampacheCompleteFormatter =
        SimpleDateFormat(AMPACHE_DATE_FORMAT, Locale.getDefault())
    private val ampacheRequestFormatter =
        SimpleDateFormat(AMPACHE_REQUEST_DATE_FORMAT, Locale.getDefault())
    var currentTimeUpdater: CurrentTimeUpdater? = null

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

    fun toMediaDuration(duration: Duration, resources: Resources) =
        duration.toComponents { days, hours, minutes, seconds, _ ->
            fun Int?.getDurationString(resource: Int) =
                this?.let { resources.getQuantityString(resource, it, it) } ?: ""

            val d = days.toInt().takeIf { it > 0 }
            val h = hours.takeIf { it > 0 || days > 0 }
            val m = minutes.takeIf { it > 0 || days > 0 || hours > 0 }
            val s = seconds.takeIf { it > 0 || days > 0 || hours > 0 || minutes > 0 }
            d.getDurationString(R.plurals.days_component) +
                    h.getDurationString(R.plurals.hours_component) +
                    m.getDurationString(R.plurals.minutes_component) +
                    s.getDurationString(R.plurals.seconds_component)
        }

    fun toMediaDuration(timeInSeconds: Int) = if (timeInSeconds < (60 * 60)) {
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

    interface CurrentTimeUpdater {
        fun getCurrentTimeUpdated(current: Calendar): Calendar
    }

}