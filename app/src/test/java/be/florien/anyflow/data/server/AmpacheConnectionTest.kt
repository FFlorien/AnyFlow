package be.florien.anyflow.data.server

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import be.florien.anyflow.captureValues
import be.florien.anyflow.data.InMemorySharedPreference
import be.florien.anyflow.data.user.AuthPersistenceFake
import be.florien.anyflow.injection.UserComponentContainerFake
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking

import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AmpacheConnectionTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var ampacheConnection: AmpacheConnection

    @Before
    @Throws(Exception::class)
    fun setUp() {
        ampacheConnection = AmpacheConnection(AuthPersistenceFake(), UserComponentContainerFake(), InMemorySharedPreference())
    }


    /**
     * connectionStatusUpdater
     */

    @Test
    fun withStartingConnection_whenQueryingStatus_thenItsConnecting() {
        ampacheConnection.connectionStatusUpdater.captureValues {
            assertThat(values.last()).isEqualTo(AmpacheConnection.ConnectionStatus.CONNEXION)
        }
    }

    @Test
    fun withStartingConnection_whenConnected_thenItsConnectedStatus() {
        runBlocking {
            ampacheConnection.connectionStatusUpdater.captureValues {
                ampacheConnection.openConnection("http://ampache")
                ampacheConnection.authenticate("admin", "password")
                assertThat(values.last()).isEqualTo(AmpacheConnection.ConnectionStatus.CONNECTED)
            }
        }
    }

    /**
     * songsPercentageUpdater
     */

    /**
     * artistsPercentageUpdater
     */

    /**
     * albumsPercentageUpdater
     */

    /**
     * openConnection
     */

    /**
     * ensureConnection
     */

    /**
     * resetReconnectionCount
     */

    /**
     * authenticate
     */

    /**
     * ping
     */

    /**
     * reconnect
     */

    /**
     * getSongs
     */

    /**
     * getArtists
     */

    /**
     * getAlbums
     */

    /**
     * getTags
     */

    /**
     * getPlaylists
     */

    /**
     * getSongUrl
     */
}