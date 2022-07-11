package be.florien.anyflow.data.server

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import be.florien.anyflow.captureValues
import be.florien.anyflow.data.InMemorySharedPreference
import be.florien.anyflow.data.TestingTimeUpdater
import be.florien.anyflow.data.TimeOperations
import be.florien.anyflow.data.server.exception.NoServerException
import be.florien.anyflow.data.server.exception.NotAnAmpacheUrlException
import be.florien.anyflow.data.server.exception.WrongFormatServerUrlException
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.data.user.AuthPersistenceFake
import be.florien.anyflow.injection.UserComponentContainerFake
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AmpacheDataSourceTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var ampacheDataSource: AmpacheDataSource
    private val currentTimeUpdater = TestingTimeUpdater()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        TimeOperations.currentTimeUpdater = currentTimeUpdater
        ampacheDataSource = AmpacheDataSource(AuthPersistenceFake(), UserComponentContainerFake(), InMemorySharedPreference())
    }

    @After
    @Throws(Exception::class)
    fun cleanUp() {
        currentTimeUpdater.clearAll()
    }

    /**
     * openConnection
     */

    @Test
    fun `With no opened connection - When trying to make a call - Then it throws a NoServerException`() {
        runBlocking {
            try {
                ampacheDataSource.authenticate(AmpacheServerFakeDispatcher.USER_NAME, AmpacheServerFakeDispatcher.PASSWORD)
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
                ampacheDataSource.openConnection("ampache")
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
            ampacheDataSource.openConnection(AmpacheServerFakeDispatcher.GOOD_URL)
            ampacheDataSource.authenticate(AmpacheServerFakeDispatcher.USER_NAME, AmpacheServerFakeDispatcher.PASSWORD)
        }
    }

    @Test
    fun `With an non-existent url - When trying to authenticate - Then it throws a NotAnAmpacheUrlException and url is reset`() {
        runBlocking {
            ampacheDataSource.openConnection("http://ballooneyUrl.com")
            try {
                ampacheDataSource.authenticate(AmpacheServerFakeDispatcher.USER_NAME, AmpacheServerFakeDispatcher.PASSWORD)
            } catch (exception: Exception) {
                assertThat(exception).isInstanceOf(NotAnAmpacheUrlException::class.java)
                try {
                    ampacheDataSource.authenticate(AmpacheServerFakeDispatcher.USER_NAME, AmpacheServerFakeDispatcher.PASSWORD)
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
            ampacheDataSource.openConnection(AmpacheServerFakeDispatcher.GOOD_URL)
            try {
                ampacheDataSource.authenticate("wrongUser", AmpacheServerFakeDispatcher.PASSWORD)
            } catch (exception: Exception) {
                assertThat(exception).isInstanceOf(WrongIdentificationPairException::class.java)
            }
        }
    }

    @Test
    fun `With a opened connection - When using an existing user_password - Then the connection can make more call`() {
        runBlocking {
            ampacheDataSource.openConnection(AmpacheServerFakeDispatcher.GOOD_URL)
            ampacheDataSource.authenticate(AmpacheServerFakeDispatcher.USER_NAME, AmpacheServerFakeDispatcher.PASSWORD)
            ampacheDataSource.ping()
        }
    }

    /**
     * ping
     */

    @Test
    fun `With a connection not opened - When calling ping - Then the call throws a NoServerException`() {
        runBlocking {
            try {
                ampacheDataSource.ping()
            } catch (exception: Exception) {
                assertThat(exception).isInstanceOf(NoServerException::class.java)
            }
        }
    }

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
        ampacheDataSource.connectionStatusUpdater.captureValues {
            assertThat(values.last()).isEqualTo(AmpacheDataSource.ConnectionStatus.CONNEXION)
        }
    }

    @Test
    fun withStartingConnection_whenConnected_thenItsConnectedStatus() {
        runBlocking {
            ampacheDataSource.connectionStatusUpdater.captureValues {
                ampacheDataSource.openConnection(AmpacheServerFakeDispatcher.GOOD_URL)
                ampacheDataSource.authenticate(AmpacheServerFakeDispatcher.USER_NAME, AmpacheServerFakeDispatcher.PASSWORD)
                assertThat(values.last()).isEqualTo(AmpacheDataSource.ConnectionStatus.CONNECTED)
            }
        }
    }

    @Test
    fun withStartingConnection_whenWrongId_thenItsWrongIdStatus() {
        runBlocking {
            ampacheDataSource.connectionStatusUpdater.captureValues {
                ampacheDataSource.openConnection(AmpacheServerFakeDispatcher.GOOD_URL)
                try {
                    ampacheDataSource.authenticate("wrongUser", AmpacheServerFakeDispatcher.PASSWORD)
                } catch (ignored: Exception) {
                }
                assertThat(values.last()).isEqualTo(AmpacheDataSource.ConnectionStatus.WRONG_ID_PAIR)
            }
        }
    }

    @Test
    fun `With a starting connection - When a wrong server url is provided - Then its a wrong server url status`() {
        runBlocking {
            ampacheDataSource.connectionStatusUpdater.captureValues {
                ampacheDataSource.openConnection("http://ballooneyUrl.com")
                try {
                    ampacheDataSource.authenticate(AmpacheServerFakeDispatcher.USER_NAME, AmpacheServerFakeDispatcher.PASSWORD)
                } catch (ignored: Exception) {
                }
                assertThat(values.last()).isEqualTo(AmpacheDataSource.ConnectionStatus.WRONG_SERVER_URL)
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