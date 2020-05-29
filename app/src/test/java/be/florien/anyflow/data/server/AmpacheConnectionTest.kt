package be.florien.anyflow.data.server

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import be.florien.anyflow.captureValues
import be.florien.anyflow.data.InMemorySharedPreference
import be.florien.anyflow.data.server.exception.NoServerException
import be.florien.anyflow.data.server.exception.NotAnAmpacheUrlException
import be.florien.anyflow.data.server.exception.WrongFormatServerUrlException
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.data.user.AuthPersistenceFake
import be.florien.anyflow.injection.UserComponentContainerFake
import com.google.common.truth.Truth.assertThat
import junit.framework.Assert.fail
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
     * openConnection
     */

    @Test
    fun `With no opened connection - When trying to make a call - Then it throws a NoServerException`() {
        runBlocking {
            try {
                ampacheConnection.authenticate("admin", "password")
            } catch (exception: Exception) {
                assertThat(exception).isInstanceOf(NoServerException::class.java)
                return@runBlocking
            }
            fail("No exception was caught")
        }
    }

    @Test
    fun `With no opened connection - When setting wrong formed url - Then it throws a WrongFormatServerUrlException`() {
        runBlocking {
            try {
                ampacheConnection.openConnection("ampache")
            } catch (exception: Exception) {
                assertThat(exception).isInstanceOf(WrongFormatServerUrlException::class.java)
                return@runBlocking
            }
            fail("No exception was caught")
        }
    }

    @Test
    fun `With no opened connection - When setting a well formed url - Then it can make a call`() {
        runBlocking {
            ampacheConnection.openConnection("http://ampache")
            ampacheConnection.authenticate("admin", "password")
        }
    }

    @Test
    fun `With an non-existent url - When trying to authenticate - Then it throws a NotAnAmpacheUrlException and url is reset`() {
        runBlocking {
            ampacheConnection.openConnection("http://ballooneyUrl.com")
            try {
                ampacheConnection.authenticate("admin", "password")
            } catch (exception: Exception) {
                assertThat(exception).isInstanceOf(NotAnAmpacheUrlException::class.java)
                try {
                    ampacheConnection.authenticate("admin", "password")
                } catch (exception: Exception) {
                    assertThat(exception).isInstanceOf(NoServerException::class.java)
                }
            }
        }
    }

    /**
     * authenticate
     */

    @Test
    fun `With a opened connection - When the user_password is wrong - Then a WrongIdentificationException is thrown`() {
        runBlocking {
            ampacheConnection.openConnection("http://ampache.com")
            try {
                ampacheConnection.authenticate("wrongUser", "password")
            } catch (exception: Exception) {
                assertThat(exception).isInstanceOf(WrongIdentificationPairException::class.java)
            }
        }
    }

    @Test
    fun `With a opened connection - When using an existing user_password - Then the connection can make more call`() {
        runBlocking {
            ampacheConnection.openConnection("http://ampache.com")
            ampacheConnection.authenticate("admin", "password")
            ampacheConnection.ping()
        }
    }


    /**
     * ping
     */

    /**
     * reconnect
     */

    /**
     * ensureConnection
     */

    /**
     * resetReconnectionCount
     */


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
                ampacheConnection.openConnection("http://ampache.com")
                ampacheConnection.authenticate("admin", "password")
                assertThat(values.last()).isEqualTo(AmpacheConnection.ConnectionStatus.CONNECTED)
            }
        }
    }

    @Test
    fun withStartingConnection_whenWrongId_thenItsWrongIdStatus() {
        runBlocking {
            ampacheConnection.connectionStatusUpdater.captureValues {
                ampacheConnection.openConnection("http://ampache.com")
                try {
                    ampacheConnection.authenticate("wrongUser", "password")
                } catch (ignored: Exception) {
                }
                assertThat(values.last()).isEqualTo(AmpacheConnection.ConnectionStatus.WRONG_ID_PAIR)
            }
        }
    }

    @Test
    fun `With a starting connection - When a wrong server url is provided - Then its a wrong server url status`() {
        runBlocking {
            ampacheConnection.connectionStatusUpdater.captureValues {
                ampacheConnection.openConnection("http://ballooneyUrl.com")
                try {
                    ampacheConnection.authenticate("admin", "password")
                } catch (ignored: Exception) {
                }
                assertThat(values.last()).isEqualTo(AmpacheConnection.ConnectionStatus.WRONG_SERVER_URL)
            }
        }
    }

    /**
     * getSongs
     */

    /**
     * getSongUrl
     */

    /**
     * songsPercentageUpdater
     */

    /**
     * getArtists
     */

    /**
     * artistsPercentageUpdater
     */

    /**
     * getAlbums
     */

    /**
     * albumsPercentageUpdater
     */

    /**
     * getTags
     */

    /**
     * getPlaylists
     */
}