package be.florien.anyflow.data.server.datasource.podcast

import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.data.server.NetResult
import be.florien.anyflow.data.server.model.AmpachePodcast
import be.florien.anyflow.data.server.toNetResult
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named

@ServerScope
class AmpachePodcastSource @Inject constructor(@Named("authenticated") retrofit: Retrofit) {

    private val api = retrofit.create(AmpachePodcastApi::class.java)

    suspend fun getPodcasts(): NetResult<List<AmpachePodcast>> =
        api.getPodcasts().toNetResult()

    suspend fun getPodcastsWithEpisodes(): NetResult<List<AmpachePodcast>> =
        api.getPodcastsWithEpisode().toNetResult()

    suspend fun updatePodcast(id: String) = api.updatePodcast(id = id)
}