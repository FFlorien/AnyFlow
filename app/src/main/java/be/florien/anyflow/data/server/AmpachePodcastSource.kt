package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.model.AmpachePodcast
import be.florien.anyflow.injection.ServerScope
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