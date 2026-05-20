package be.florien.anyflow.feature.library.domain

import be.florien.anyflow.feature.library.domain.model.IdText
import be.florien.anyflow.feature.library.domain.model.LibraryInfoRow
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterType

interface LibraryInfoRepository {
    fun getArtUrl(artType: String?, id: Long): String?
    suspend fun getFilteredInfo(filterType: FilterType, filter: Filter?): IdText?
    suspend fun getInfoRowList(filter: Filter?): MutableList<LibraryInfoRow>
}