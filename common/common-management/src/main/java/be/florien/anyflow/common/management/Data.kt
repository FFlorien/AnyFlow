package be.florien.anyflow.common.management

import androidx.lifecycle.LiveData
import androidx.paging.*
import kotlinx.coroutines.Dispatchers


fun <T : Any> DataSource.Factory<Int, T>.convertToPagingLiveData(): LiveData<PagingData<T>> =
    Pager(
        PagingConfig(100),
        0,
        asPagingSourceFactory(Dispatchers.IO)
    ).liveData