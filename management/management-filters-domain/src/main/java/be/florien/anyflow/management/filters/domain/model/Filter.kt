package be.florien.anyflow.management.filters.domain.model

class Filter(vararg items: FilterParam<*>) : LinkedHashSet<FilterParam<*>>() {

    val mainParam: FilterParam<*>
        get() = last()

    init {
        addAll(items)
    }

    fun getFullDisplay(): String = joinToString(separator = " > ") { it.displayText }

    fun getFilterIfTypePresent(filterType: FilterType): FilterParam<*>? =
        firstOrNull { it.type == filterType }
}