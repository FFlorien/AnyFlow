package be.florien.anyflow.management.filters.domain.model

import kotlinx.serialization.Serializable

@Serializable
class Filter() : LinkedHashSet<FilterParam<*>>() {

    constructor(vararg items: FilterParam<*>) : this() {
        addAll(items)
    }

    val mainParam: FilterParam<*>
        get() = last()

    fun getFullDisplay(): String = joinToString(separator = " > ") { it.displayText }

    fun getFilterIfTypePresent(filterType: FilterType): FilterParam<*>? =
        firstOrNull { it.type == filterType }
}