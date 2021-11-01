package be.florien.anyflow.data.view

data class Alarm(
        val id: Long,
        val hour: Int,
        val minute: Int,
        val isRepeating: Boolean,
        val daysToTrigger: List<Boolean>,
        val active: Boolean
)