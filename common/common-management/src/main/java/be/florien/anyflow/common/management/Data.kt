package be.florien.anyflow.common.management

import androidx.lifecycle.LiveData
import androidx.paging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow


fun <T : Any> DataSource.Factory<Int, T>.convertToPagingLiveData(): LiveData<PagingData<T>> =
    Pager(
        PagingConfig(100),
        0,
        asPagingSourceFactory(Dispatchers.IO)
    ).liveData

fun <T : Any> DataSource.Factory<Int, T>.convertToPagingFlow(): Flow<PagingData<T>> =
    Pager(
        PagingConfig(100),
        0,
        asPagingSourceFactory(Dispatchers.IO)
    ).flow