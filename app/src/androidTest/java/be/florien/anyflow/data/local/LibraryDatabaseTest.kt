package be.florien.anyflow.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.paging.LivePagingDataBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import be.florien.anyflow.captureValues
import be.florien.anyflow.data.local.model.DbFilter
import be.florien.anyflow.data.local.model.DbFilterGroup
import be.florien.anyflow.data.local.model.DbQueueOrder
import be.florien.anyflow.getValueForTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import junit.framework.Assert.fail
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class LibraryDatabaseTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var library: LibraryDatabase
    private val dataFactory = DataBaseObjectFactory()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        library = LibraryDatabase.getInstance(ApplicationProvider.getApplicationContext(), true)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        library.close()
    }

    /**
     * ALBUM DAO
     */

    @Test
    fun withEmptyDb_whenInsertingAlbums_thenOnlyTheseAlbumsAreInserted() {
        runBlocking {

            // With Empty Db

            // When Inserting Albums
            val albumsToInsert = dataFactory.createAlbumList(5)
            library.addOrUpdateAlbums(albumsToInsert)

            // Then Only These Albums Are Inserted
            val albumsInserted = library.getAlbumsFomRawQuery("SELECT * FROM album")
            assertWithMessage("We check that the created albums are well inserted")
                    .that(albumsToInsert)
                    .containsExactlyElementsIn(albumsInserted)
        }
    }

    @Test
    fun withMultipleAlbum_whenQueryingAlbum_thenItIsRetrieved() {
        runBlocking {

            // With Multiple Album
            val albumsToInsert = dataFactory.createAlbumList(40)
            val albumsExpected = albumsToInsert.map { it.toDbAlbumDisplay() }
            library.addOrUpdateAlbums(albumsToInsert)

            // When Querying Album
            val PagingData = LivePagingDataBuilder(library.getAlbums(), 40).build()
            val albums = PagingData.getValueForTest()

            // Then It Is Retrieved
            assertThat(albums).containsExactlyElementsIn(albumsExpected)
        }
    }

    /**
     * ARTIST DAO
     */

    @Test
    fun withEmptyDb_whenInsertingArtists_thenOnlyTheseArtistsAreInserted() {
        runBlocking {

            // With Empty Db

            // When Inserting Artists
            val artistsToInsert = dataFactory.createArtistList(5)
            library.addOrUpdateArtists(artistsToInsert)

            // Then Only These Artists Are Inserted
            val artistsInserted = library.getArtistsFomRawQuery("SELECT * FROM artist")
            assertWithMessage("We check that the created artists are well inserted")
                    .that(artistsToInsert)
                    .containsExactlyElementsIn(artistsInserted)
        }
    }

    @Test
    fun withMultipleAlbumArtistAndSongsCorresponding_whenQueryingAlbumArtist_thenItIsRetrieved() {
        runBlocking {

            // With Multiple Album Artist And Songs Corresponding
            val songs = dataFactory.createSongList(40)
            val artistsToInsert = dataFactory.createArtistList(songs)
            library.addOrUpdateSongs(songs)
            library.addOrUpdateArtists(artistsToInsert)

            // When Querying Album Artist
            val PagingData = LivePagingDataBuilder(library.getAlbumArtists(), 40).build()
            val artists = PagingData.getValueForTest()

            // Then It Is Retrieved
            assertThat(artists).containsExactlyElementsIn(artistsToInsert.map { it.toDbArtistDisplay() })
        }
    }

    /**
     * PLAYLIST DAO
     */

    @Test
    fun withEmptyDb_whenInsertingPlaylists_thenOnlyThesePlaylistsAreInserted() {
        runBlocking {

            // With Empty Db

            // When Inserting Playlists
            val playlistsToInsert = dataFactory.createPlaylistList(5)
            library.addOrUpdatePlayLists(playlistsToInsert)

            // Then Only These Playlists Are Inserted
            val playlistsInserted = library.getPlaylistsFomRawQuery("SELECT * FROM playlist")
            assertWithMessage("We check that the created playlists are well inserted")
                    .that(playlistsToInsert)
                    .containsExactlyElementsIn(playlistsInserted)
        }
    }

    /**
     * SONG DAO AND QUEUE_ORDER DAO
     */

    @Test
    fun withEmptyDb_whenInsertingSongs_thenOnlyTheseSongsAreInserted() {
        runBlocking {

            // With empty Db

            // When inserting songs
            val songsToInsert = dataFactory.createSongList(5)
            library.addOrUpdateSongs(songsToInsert)

            // Then only these songs are inserted
            val songsInserted = library.getSongsFromRawQuery("SELECT * FROM song")
            assertWithMessage("We check that the created song are well inserted")
                    .that(songsToInsert)
                    .containsExactlyElementsIn(songsInserted)
        }
    }

    @Test
    fun withExistingSongsInDb_whenSavingCorrectQueueOrder_thenTheOrderIsSaved() {
        runBlocking {

            // With Existing Songs In Db
            val songList = dataFactory.createSongList(15)
            library.addOrUpdateSongs(songList)

            // When Saving Correct Queue Order
            library.saveQueueOrder(songList.map { it.id })

            // Then The Order Is Saved
            val queueOrderFromRawQuery = library.getQueueOrderFromRawQuery("SELECT * FROM QueueOrder")
            assertThat(queueOrderFromRawQuery)
                    .containsExactlyElementsIn(songList.mapIndexed { index, dbSong -> DbQueueOrder(index, dbSong.id) })
                    .inOrder()
        }
    }

    @Test
    fun withExistingSongsInDb_whenSavingIncorrectQueueOrder_thenTheOrderIsNotSaved() {
        runBlocking {

            // With Existing Songs In Db
            val songList = dataFactory.createSongList(15)
            library.addOrUpdateSongs(songList)
            try {

                // When Saving Incorrect Queue Order
                library.saveQueueOrder(songList.map { it.id + 100 })
                library.getQueueOrderFromRawQuery("SELECT * FROM QueueOrder")
            } catch (exception: Exception) {

                // Then The Order Is NotSaved
                assertThat(exception)
                        .isInstanceOf(SQLiteConstraintException::class.java)
            }
        }
    }

    @Test
    fun withExistingSongsAndOrderedQueueInDb_whenQueryingASongInPosition_thenTheCorrectSongIsRetrieved() {
        runBlocking {

            // With Existing Songs And Ordered Queue In Db
            val songList = dataFactory.createSongList(15)
            val orderList = songList.map { it.id }
            library.addOrUpdateSongs(songList)
            library.saveQueueOrder(orderList)

            // When Querying A Song In Position
            val songAtPosition = library.getSongAtPosition(5)

            // Then The Correct Song Is Retrieved
            assertThat(songAtPosition)
                    .isEqualTo(songList[5])
        }
    }

    @Test
    fun withExistingSongsAndRandomQueueInDb_whenQueryingASongInPosition_thenTheCorrectSongIsRetrieved() {
        runBlocking {

            // With Existing Songs And Random Queue In Db
            val songList = dataFactory.createSongList(15)
            val randomList = songList.map { it.id }.shuffled()
            library.addOrUpdateSongs(songList)
            library.saveQueueOrder(randomList)

            // When Querying A Song In Position
            val songAtPosition = library.getSongAtPosition(5)

            // Then The Correct Song Is Retrieved
            assertThat(songAtPosition)
                    .isEqualTo(songList.first { it.id == randomList[5] })
        }
    }

    @Test
    fun withExistingSongsAndNoQueueInDb_whenQueryingASongInPosition_thenNoResultIsReturned() {
        runBlocking {

            // With Existing Songs And No Queue In Db
            val songList = dataFactory.createSongList(15)
            library.addOrUpdateSongs(songList)

            // When Querying A Song In Position
            val songAtPosition = library.getSongAtPosition(5)

            // Then No Result Is Returned
            assertThat(songAtPosition)
                    .isNull()
        }
    }

    @Test
    fun withEmptyDb_whenQueryingASongInPosition_thenAnEmptyObjectIsRetrieved() {
        runBlocking {

            // With Empty Db

            // When Querying A Song In Position
            val songAtPosition = library.getSongAtPosition(5)

            // Then An Empty Object Is Retrieved
            assertThat(songAtPosition)
                    .isNull()
        }
    }

    @Test
    fun withExistingSongsAndOrderedQueueInDb_whenQueryingASongPosition_thenTheCorrectPositionIsRetrieved() {
        runBlocking {

            // With Existing Songs And Ordered Queue In Db
            val songList = dataFactory.createSongList(15)
            val orderList = songList.map { it.id }
            library.addOrUpdateSongs(songList)
            library.saveQueueOrder(orderList)

            // When Querying A Song Position
            val searchedSong = songList[5]
            val songPosition = library.getPositionForSong(searchedSong.toDbSongDisplay())

            // Then The Correct Position Is Retrieved
            assertThat(songPosition)
                    .isEqualTo(orderList.indexOfFirst { it == searchedSong.id })
        }
    }

    @Test
    fun withExistingSongsAndRandomQueueInDb_whenQueryingASongPosition_thenTheCorrectPositionIsRetrieved() {
        runBlocking {

            // With Existing Songs And Random Queue In Db
            val songList = dataFactory.createSongList(15)
            val randomList = songList.shuffled()
            library.addOrUpdateSongs(songList)
            library.saveQueueOrder(randomList.map { it.id })

            // When Querying A Song Position
            val songPosition = library.getPositionForSong(songList[5].toDbSongDisplay())

            // Then The Correct Position Is Retrieved
            assertThat(songPosition)
                    .isEqualTo(randomList.indexOfFirst { it == songList[5] })
        }
    }

    @Test
    fun withExistingSongsAndNoQueueInDb_whenQueryingASongPosition_thenNoResultIsReturned() {
        runBlocking {

            // With Existing Songs And No Queue In Db
            val songList = dataFactory.createSongList(15)
            library.addOrUpdateSongs(songList)

            // When Querying A Song Position
            val songPosition = library.getPositionForSong(songList[5].toDbSongDisplay())

            // Then NoResultIsReturned
            assertThat(songPosition).isNull()
        }
    }

    @Test
    fun withEmptyDb_whenQueryingASongPosition_thenAnEmptyObjectIsRetrieved() {
        runBlocking {

            // With Empty Db

            // When Querying A Song Position
            val songPosition = library.getPositionForSong(dataFactory.createSong(0L).toDbSongDisplay())

            // Then An Empty Object Is Retrieved
            assertThat(songPosition).isNull()
        }
    }

    @Test
    fun withExistingSongsAndOrderedQueue_whenQueryingQueue_thenItIsRetrieved() {
        runBlocking {

            // With Existing Songs And Ordered Queue
            val songList = dataFactory.createSongList(50)
            library.addOrUpdateSongs(songList)
            library.saveQueueOrder(songList.map { it.id })

            // When Querying Queue
            val PagingData = LivePagingDataBuilder(library.getSongsInQueueOrder(), 50).build()
            val songDisplayList = PagingData.getValueForTest()

            // Then It Is Retrieved
            assertThat(songDisplayList)
                    .isNotNull()
            assertThat(songDisplayList?.toList())
                    .containsExactlyElementsIn(songList.map { it.toDbSongDisplay() })
        }
    }

    @Test
    fun withExistingSongsAndRandomQueue_whenQueryingQueue_thenItIsTheSameOrder() {
        runBlocking {

            // With Existing Songs And Random Queue
            val songList = dataFactory.createSongList(50)
            val randomList = songList.shuffled()
            library.addOrUpdateSongs(songList)
            library.saveQueueOrder(randomList.map { it.id })

            // When Querying Queue
            val PagingData = LivePagingDataBuilder(library.getSongsInQueueOrder(), 50).build()
            val songDisplayList = PagingData.getValueForTest()

            // Then It Is The Same Order
            assertThat(songDisplayList)
                    .isNotNull()
            assertThat(songDisplayList?.toList())
                    .containsExactlyElementsIn(randomList.map { it.toDbSongDisplay() })
                    .inOrder()
        }
    }

    @Test
    fun withExistingSongsAndRandomQueue_whenChangingQueue_thenTheOrderIsChanged() {
        runBlocking {

            // With Existing Songs And Random Queue
            val songList = dataFactory.createSongList(50)
            val randomList = songList.shuffled(Random(5))
            library.addOrUpdateSongs(songList)
            library.saveQueueOrder(randomList.map { it.id })

            // When Changing Queue
            val randomListSecond = songList.shuffled(Random(7))
            library.saveQueueOrder(randomListSecond.map { it.id })

            // Then The Order Is Changed
            val PagingData = LivePagingDataBuilder(library.getSongsInQueueOrder(), 50).build()
            val songDisplayList = PagingData.getValueForTest()
            assertThat(songDisplayList)
                    .isNotNull()
            assertThat(songDisplayList?.toList())
                    .containsExactlyElementsIn(randomListSecond.map { it.toDbSongDisplay() })
                    .inOrder()
        }
    }

    @Test
    fun withExistingSongsAndQueue_whenAddingSongsInQueue_thenItIsNotified() {
        runBlocking {

            // With Existing Songs And Queue
            val songListFirst = dataFactory.createSongList(25)
            val songListNext = dataFactory.createSongList(25, 26)
            val songList = songListFirst + songListNext
            library.addOrUpdateSongs(songListFirst)
            library.saveQueueOrder(songListFirst.map { it.id })
            val PagingData = LivePagingDataBuilder(library.getSongsInQueueOrder(), 50).build()

            // When Adding Songs In Queue
            PagingData.captureValues {
                assertThat(values.first()).containsExactlyElementsIn(songListFirst.map { it.toDbSongDisplay() })
                library.addOrUpdateSongs(songListNext)
                library.saveQueueOrder(songList.map { it.id })

                // Then It Is Notified
                delay(150)
                assertThat(values.last()).containsExactlyElementsIn(songList.map { it.toDbSongDisplay() })
            }
        }
    }

    @Test
    fun withExistingSongsAndQueue_whenAddingSongsNotInQueue_thenItIsNotNotified() {
        runBlocking {

            // With Existing Songs And Queue
            val songListFirst = dataFactory.createSongList(25)
            val songListNext = dataFactory.createSongList(25, 26)
            library.addOrUpdateSongs(songListFirst)
            library.saveQueueOrder(songListFirst.map { it.id })
            library.inTransaction()
            val dataSourceFactory = library.getSongsInQueueOrder()
            val livePagingDataBuilder = LivePagingDataBuilder(dataSourceFactory, 50)
            val PagingData = livePagingDataBuilder.build().distinctUntilChanged()
            PagingData.captureValues {
                assertThat(values.last()).containsExactlyElementsIn(songListFirst.map { it.toDbSongDisplay() })

                // When Adding Songs Not In Queue
                library.addOrUpdateSongs(songListNext)

                // Then It Is Not Notified
                delay(150)
                assertThat(values).hasSize(1)
                assertThat(values.last()).containsExactlyElementsIn(songListFirst.map { it.toDbSongDisplay() })
            }
        }
    }

    @Test
    fun withExistingSongsAndNoQueue_whenSubscribingToQueueChange_thenItIsNotifiedWithNoSongs() {
        runBlocking {

            // With Existing Songs And No Queue

            // When Subscribing To Queue Change
            val PagingData = LivePagingDataBuilder(library.getSongsInQueueOrder(), 50).build()

            // Then It Is Notified With No Songs
            assertThat(PagingData.getValueForTest()).isEmpty()
        }
    }

    @Test
    fun withMultipleGenre_whenQueryingGenre_thenItIsRetrieved() {
        runBlocking {

            // With Multiple Genre
            val songsToInsert = dataFactory.createSongList(40)
            val genresExpected = songsToInsert.map { it.genre }.toSet().toList()
            library.addOrUpdateSongs(songsToInsert)

            // When Querying Genre
            val PagingData = LivePagingDataBuilder(library.getGenres(), 40).build()

            // Then It Is Retrieved
            val genres = PagingData.getValueForTest()
            assertThat(genres).containsExactlyElementsIn(genresExpected)
        }
    }

    /**
     * FILTER DAO AND FILTER_GROUP DAO
     */

    @Test
    fun withEmptyCurrentFilters_whenSettingCurrentFilters_thenTheseFiltersAreInsertedInTheRightFilterGroup() {
        runBlocking {

            // With Empty Current Filters

            // When Setting Current Filters
            val filtersToInsert = dataFactory.createFilterList(5)
            library.setCurrentFilters(filtersToInsert)

            // Then These Filters Are Inserted In The Right FilterGroup
            val filtersInCurrentFilter = filtersToInsert.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + 1, filterGroup = DbFilterGroup.CURRENT_FILTER_GROUP_ID) }
            val filterInserted = library.getFilterFomRawQuery("SELECT * FROM dbFilter WHERE filterGroup = ${DbFilterGroup.CURRENT_FILTER_GROUP_ID}")
            assertWithMessage("We check that the filters are well inserted in the correct group")
                    .that(filtersInCurrentFilter)
                    .containsExactlyElementsIn(filterInserted)

        }
    }

    @Test
    fun withEmptyDb_thenCurrentFilterGroupAlreadyExist() {
        runBlocking {

            // With Empty Db

            // Then Current Filter Group Already Exist
            val filterGroupInserted = library.getFilterGroupFomRawQuery("SELECT * FROM FilterGroup")
            val filterGroupExpected = DbFilterGroup.currentFilterGroup

            assertWithMessage("We check that there is one group: the current filters")
                    .that(filterGroupInserted)
                    .containsExactly(filterGroupExpected)

        }
    }

    @Test
    fun withExistingCurrentFilters_whenReplacingFilters_thenOnlyTheseNewFiltersAreInCurrentFilters() {

        runBlocking {

            // With Existing Current Filters
            val filtersToInsertFirst = dataFactory.createFilterList(5)
            library.setCurrentFilters(filtersToInsertFirst)

            // When Replacing Filters
            val filtersToInsertAfter = dataFactory.createFilterList(3, 6)
            library.setCurrentFilters(filtersToInsertAfter)

            // Then Only These New Filters Are In CurrentFilters
            val filterInserted = library.getFilterFomRawQuery("SELECT * FROM dbFilter WHERE filterGroup = ${DbFilterGroup.CURRENT_FILTER_GROUP_ID}")
            val filterExpected = filtersToInsertAfter.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + filtersToInsertFirst.size + 1, filterGroup = DbFilterGroup.CURRENT_FILTER_GROUP_ID) }
            assertWithMessage("We check that the filters are well inserted in the correct group")
                    .that(filterInserted)
                    .containsExactlyElementsIn(filterExpected)

        }
    }

    @Test
    fun withExistingCurrentFilters_whenSettingAnEmptyFilterList_thenTheCurrentFilterGroupIsEmpty() {
        runBlocking {

            // With Existing Current Filters
            val filtersToInsertFirst = dataFactory.createFilterList(5)
            library.setCurrentFilters(filtersToInsertFirst)

            // When Setting An Empty Filter List
            val filtersToInsertAfter = emptyList<DbFilter>()
            library.setCurrentFilters(filtersToInsertAfter)

            // Then The Current Filter GroupIsEmpty
            val filterInserted = library.getFilterFomRawQuery("SELECT * FROM dbFilter WHERE filterGroup = ${DbFilterGroup.CURRENT_FILTER_GROUP_ID}")
            assertWithMessage("We check that the filters are well inserted in the correct group")
                    .that(filterInserted)
                    .hasSize(0)

        }
    }

    @Test
    fun withExistingCurrentFiltersAndOtherFilterGroups_whenSettingAnEmptyCurrentFilterList_thenTheOtherFiltersAreUntouched() {
        runBlocking {

            // With Existing Current Filters And Other Filter Groups
            val filtersToInsertCurrent = dataFactory.createFilterList(5)
            val filtersGroupName = dataFactory.getRandom5LettersName(0)
            val filtersToInsertGroup = dataFactory.createFilterList(5, 6)
            library.setCurrentFilters(filtersToInsertCurrent)
            library.createFilterGroup(filtersToInsertGroup, filtersGroupName)

            // When Setting An Empty Current Filter List
            val filtersToInsertAfter = emptyList<DbFilter>()
            library.setCurrentFilters(filtersToInsertAfter)

            // Then The Other Filters Are Untouched
            val otherGroupFilterExpected = filtersToInsertGroup.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + filtersToInsertCurrent.size + 1, filterGroup = DbFilterGroup.CURRENT_FILTER_GROUP_ID + 1) }
            val filterInserted = library.getFilterFomRawQuery("SELECT * FROM dbFilter WHERE filterGroup = ${DbFilterGroup.CURRENT_FILTER_GROUP_ID + 1}")
            assertWithMessage("We check that the filters are well inserted in the correct group")
                    .that(filterInserted)
                    .containsExactlyElementsIn(otherGroupFilterExpected)

        }
    }

    @Test
    fun withExistingCurrentFilters_whenSettingAnEmptyCurrentFilterList_thenTheCurrentFilterGroupStillExist() {
        runBlocking {

            // With Existing Current Filters
            val filtersToInsertFirst = dataFactory.createFilterList(5)
            library.setCurrentFilters(filtersToInsertFirst)

            // When Setting An Empty Current Filter List
            val filtersToInsertAfter = emptyList<DbFilter>()
            library.setCurrentFilters(filtersToInsertAfter)

            // Then The Current FilterGroupStillExist
            val filterGroupInserted = library.getFilterGroupFomRawQuery("SELECT * FROM FilterGroup WHERE id = ${DbFilterGroup.CURRENT_FILTER_GROUP_ID}")
            assertWithMessage("We check that there is one group: the current filters")
                    .that(filterGroupInserted)
                    .containsExactly(DbFilterGroup.currentFilterGroup)
        }
    }

    @Test
    fun withEmptyDb_whenInsertingANewFilterGroup_thenTheFiltersAreInsertedWithCorrectFilterGroup() {
        runBlocking {

            // With Empty Db

            // When Inserting A New Filter Group
            val filtersToInsert = dataFactory.createFilterList(5)
            library.createFilterGroup(filtersToInsert, dataFactory.getRandom5LettersName(0))
            val filterGroupsInserted = library.getFilterGroupFomRawQuery("SELECT * FROM FilterGroup")

            // Then The Filters Are Inserted With Correct FilterGroup
            val filtersInserted = library.getFilterFomRawQuery("SELECT * FROM DbFilter")
            val filtersExpected = filtersToInsert.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + 1, filterGroup = filterGroupsInserted[1].id) }
            assertWithMessage("We check that the filters are well inserted")
                    .that(filtersInserted)
                    .containsExactlyElementsIn(filtersExpected)
        }
    }

    @Test
    fun withEmptyDb_whenInsertingANewFilterGroup_thenTheFilterGroupIsCreatedAlongsideTheCurrentFilterGroup() {
        runBlocking {

            // With Empty Db

            // When Inserting A New Filter Group
            val filtersToInsert = dataFactory.createFilterList(5)
            val filterGroupName = dataFactory.getRandom5LettersName(0)
            library.createFilterGroup(filtersToInsert, filterGroupName)

            // Then The Filter Group Is CreatedAlongsideTheCurrentFilterGroup
            val filterGroupExpected = DbFilterGroup(DbFilterGroup.CURRENT_FILTER_GROUP_ID + 1, filterGroupName)
            val filterGroupsInserted = library.getFilterGroupFomRawQuery("SELECT * FROM FilterGroup")
            assertWithMessage("We check that the created filter group is the only one")
                    .that(filterGroupsInserted)
                    .containsExactly(filterGroupExpected, DbFilterGroup.currentFilterGroup)
        }
    }

    @Test
    fun withEmptyDb_whenCreatingMultipleFilterGroups_thenAllFilterGroupsAreCreated() {
        runBlocking {

            // With Empty Db

            // When Creating Multiple Filter Groups
            val filtersToInsertFirst = dataFactory.createFilterList(3)
            val filtersToInsertSecond = dataFactory.createFilterList(6, 4)
            val filtersToInsertThird = dataFactory.createFilterList(4, 10)
            val filterNameFirst = dataFactory.getRandom5LettersName(0)
            val filterNameSecond = dataFactory.getRandom5LettersName(1)
            val filterNameThird = dataFactory.getRandom5LettersName(2)
            library.createFilterGroup(filtersToInsertFirst, filterNameFirst)
            library.createFilterGroup(filtersToInsertSecond, filterNameSecond)
            library.createFilterGroup(filtersToInsertThird, filterNameThird)

            // Then All Filter Groups Are Created
            val groupInsertedFirst = library.getFilterGroupFomRawQuery("SELECT * FROM FilterGroup WHERE name = \"$filterNameFirst\"")
            val groupInsertedSecond = library.getFilterGroupFomRawQuery("SELECT * FROM FilterGroup WHERE name = \"$filterNameSecond\"")
            val groupInsertedThird = library.getFilterGroupFomRawQuery("SELECT * FROM FilterGroup WHERE name = \"$filterNameThird\"")
            var newId = DbFilterGroup.CURRENT_FILTER_GROUP_ID
            val groupExpectedFirst = DbFilterGroup(++newId, filterNameFirst)
            val groupExpectedSecond = DbFilterGroup(++newId, filterNameSecond)
            val groupExpectedThird = DbFilterGroup(++newId, filterNameThird)
            assertWithMessage("Check the retrieved group list: First").that(groupInsertedFirst).containsExactly(groupExpectedFirst)
            assertWithMessage("Check the retrieved group list: Second").that(groupInsertedSecond).containsExactly(groupExpectedSecond)
            assertWithMessage("Check the retrieved group list: Third").that(groupInsertedThird).containsExactly(groupExpectedThird)
            val filtersInsertedFirst = library.getFilterFomRawQuery("SELECT * FROM DbFilter WHERE filterGroup = ${groupInsertedFirst[0].id}")
            val filtersInsertedSecond = library.getFilterFomRawQuery("SELECT * FROM DbFilter WHERE filterGroup = ${groupInsertedSecond[0].id}")
            val filtersInsertedThird = library.getFilterFomRawQuery("SELECT * FROM DbFilter WHERE filterGroup = ${groupInsertedThird[0].id}")
            val filtersExpectedFirst = filtersToInsertFirst.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + 1, filterGroup = groupInsertedFirst[0].id) }
            val filtersExpectedSecond = filtersToInsertSecond.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + filtersExpectedFirst.size + 1, filterGroup = groupInsertedSecond[0].id) }
            val filtersExpectedThird = filtersToInsertThird.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + filtersExpectedFirst.size + filtersExpectedSecond.size + 1, filterGroup = groupInsertedThird[0].id) }
            assertWithMessage("Check the First filter group").that(filtersInsertedFirst).containsExactlyElementsIn(filtersExpectedFirst)
            assertWithMessage("Check the Second filter group").that(filtersInsertedSecond).containsExactlyElementsIn(filtersExpectedSecond)
            assertWithMessage("Check the Third filter group").that(filtersInsertedThird).containsExactlyElementsIn(filtersExpectedThird)
        }
    }

    @Test
    fun withExistingFilterGroup_whenCreatingFilterGroupWithSameName_thenItFailsWithException() {
        runBlocking {

            // With Existing Filter Group
            val filtersToInsertFirst = dataFactory.createFilterList(5)
            val filtersToInsertSecond = dataFactory.createFilterList(3,6)
            val filtersName = dataFactory.getRandom5LettersName(0)
            library.createFilterGroup(filtersToInsertFirst, filtersName)

            // When Creating Filter Group With Same Name
            try {
                library.createFilterGroup(filtersToInsertSecond, filtersName)

                // Then It Fails WithException
                fail("The previous call should have thrown an exception")
            } catch (exception: Exception) {
                assertWithMessage("Check that the createFilterGroup has thrown the right exception")
                        .that(exception)
                        .isInstanceOf(IllegalArgumentException::class.java)
            }
            try {
                library.createFilterGroup(filtersToInsertSecond, filtersName.toUpperCase())

                // Then It Fails WithException
                fail("The previous call with a different case should have thrown an exception")
            } catch (exception: Exception) {
                assertWithMessage("Check that the createFilterGroup has thrown the right exception with a different case")
                        .that(exception)
                        .isInstanceOf(IllegalArgumentException::class.java)
            }
        }
    }

    @Test
    fun withExistingFilterGroup_whenQueryingFilters_thenFiltersAreRetrieved() {
        runBlocking {

            // With Existing Filter Group
            val filtersToInsert = dataFactory.createFilterList(5)
            val filterName = dataFactory.getRandom5LettersName(0)
            library.createFilterGroup(filtersToInsert, filterName)

            // When Querying Filters
            val filterInserted = library.filterForGroupSync(DbFilterGroup.CURRENT_FILTER_GROUP_ID + 1)

            // Then Filters Are Retrieved
            val filterExpected = filtersToInsert.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + 1, filterGroup = DbFilterGroup.CURRENT_FILTER_GROUP_ID + 1) }
            assertWithMessage("Check that the inserted filters are the one expected")
                    .that(filterInserted)
                    .containsExactlyElementsIn(filterExpected)
        }
    }

    @Test
    fun withMultipleFilter_whenQueryingFilter_thenItIsRetrieved() {
        runBlocking {

            // With Multiple Filter
            val filtersToInsert = dataFactory.createFilterList(40)
            library.setCurrentFilters(filtersToInsert)

            // When Querying Filter
            val currentFilters = library.getCurrentFilters()
            delay(150)
            val filters = currentFilters.getValueForTest()
            val filtersExpected = filtersToInsert.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + 1, filterGroup = DbFilterGroup.CURRENT_FILTER_GROUP_ID) }

            // Then It Is Retrieved
            assertThat(filters).containsExactlyElementsIn(filtersExpected)
        }
    }

    @Test
    fun withSomeFilterGroupsPresent_whenRetrievingThem_thenAllArePresent() {
        runBlocking {

            // With Some Filter Groups Present
            val filterGroupNameFirst = dataFactory.getRandom5LettersName(0)
            val filterGroupNameSecond = dataFactory.getRandom5LettersName(1)
            val filtersFirst = dataFactory.createFilterList(5)
            val filtersSecond = dataFactory.createFilterList(8,6)
            library.createFilterGroup(filtersFirst, filterGroupNameFirst)
            library.createFilterGroup(filtersSecond, filterGroupNameSecond)

            // When Retrieving Them
            val filterGroupsLiveData = library.getFilterGroups()
            val filterGroups = filterGroupsLiveData.getValueForTest()

            // Then All Are Present
            assertThat(filterGroups).containsExactly(DbFilterGroup(DbFilterGroup.CURRENT_FILTER_GROUP_ID + 1, filterGroupNameFirst), DbFilterGroup(DbFilterGroup.CURRENT_FILTER_GROUP_ID + 2, filterGroupNameSecond))
        }
    }

    @Test
    fun withNoCurrentFiltersAndSavedFilterGroup_whenSettingSavedGroupAsCurrentFilters_thenTheCurrentFiltersChange() {
        runBlocking {

            // with no current filters and saved filter groups
            val filtersForGroup = dataFactory.createFilterList(5)
            val filterGroupName = dataFactory.getRandom5LettersName(0)
            library.createFilterGroup(filtersForGroup, filterGroupName)

            // when setting saved group as current filters
            library.setSavedGroupAsCurrentFilters(DbFilterGroup(DbFilterGroup.CURRENT_FILTER_GROUP_ID + 1, filterGroupName))

            // then the current filters change
            val filtersExpected = filtersForGroup.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + 1, filterGroup = DbFilterGroup.CURRENT_FILTER_GROUP_ID) }
            val filterFomRawQuery = library.getFilterFomRawQuery("SELECT * FROM DbFilter WHERE filterGroup = ${DbFilterGroup.CURRENT_FILTER_GROUP_ID}")
            assertThat(filterFomRawQuery).containsExactlyElementsIn(filtersExpected)
        }
    }

    @Test
    fun withNoCurrentFiltersAndNoSavedFilterGroup_whenSettingNonExistingSavedGroupAsCurrentFilters_thenItFailsWithException() {
        runBlocking {

            // with no current filters and saved filter groups

            try {

                // when setting saved group as current filters
                library.setSavedGroupAsCurrentFilters(DbFilterGroup(DbFilterGroup.CURRENT_FILTER_GROUP_ID + 1, dataFactory.getRandom5LettersName(0)))

            } catch (exception: Exception) {

                // then it fails with exception
                assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)

            }
        }
    }

    @Test
    fun withCurrentFiltersAndSavedFilterGroup_whenSettingSavedGroupAsCurrentFilters_thenTheCurrentFiltersChange() {
        runBlocking {

            // with current filters and saved filter groups
            val filtersForCurrent = dataFactory.createFilterList(5)
            val filtersForGroup = dataFactory.createFilterList(5,6)
            val filterGroupName = dataFactory.getRandom5LettersName(0)
            library.createFilterGroup(filtersForGroup, filterGroupName)
            library.setCurrentFilters(filtersForCurrent)
            assertThat(filtersForCurrent).containsNoneIn(filtersForGroup)

            // when setting saved group as current filters
            library.setSavedGroupAsCurrentFilters(DbFilterGroup(DbFilterGroup.CURRENT_FILTER_GROUP_ID + 1, filterGroupName))

            // then the current filters change
            val filtersExpected = filtersForGroup.mapIndexed { index, dbFilter -> dbFilter.copy(id = index + 1, filterGroup = DbFilterGroup.CURRENT_FILTER_GROUP_ID) }
            val filterFomRawQuery = library.getFilterFomRawQuery("SELECT * FROM DbFilter WHERE filterGroup = ${DbFilterGroup.CURRENT_FILTER_GROUP_ID}")
            assertThat(filterFomRawQuery).containsExactlyElementsIn(filtersExpected)
        }
    }

    /**
     * ORDER DAO
     */

    @Test
    fun withEmptyDb_whenSetOrders_thenOrdersAreSet() {
        runBlocking {

            // With Empty Db

            // When Set Orders
            val orderList = dataFactory.createOrderList(5)
            library.setOrders(orderList)

            // Then Orders Are Set
            val orderFromRawQuery = library.getOrderFromRawQuery("SELECT * FROM DbOrder")
            assertThat(orderFromRawQuery)
                    .containsExactlyElementsIn(orderList)
        }
    }

    @Test
    fun withEmptyDb_whenSetOrdersWithDuplicates_thenOrdersAreNotSet() {
        runBlocking {

            // With Empty Db

            // When Set Orders With Duplicates
            val orderList = dataFactory.createOrderList(5)
            val orderListDuplicates = orderList + orderList
            assertThat(orderListDuplicates).hasSize(orderList.size * 2)
            library.setOrders(orderListDuplicates)

            // Then Orders Are Not Set
            val orderFromRawQuery = library.getOrderFromRawQuery("SELECT * FROM DbOrder")
            assertWithMessage("Check that we inserted the orders")
                    .that(orderFromRawQuery)
                    .containsExactlyElementsIn(orderList)
            assertWithMessage("Check that we inserted no duplicates")
                    .that(orderFromRawQuery)
                    .containsNoDuplicates()
        }
    }

    @Test
    fun withExistingOrders_whenSetOrders_thenOrdersAreReplaced() {
        runBlocking {

            // With Existing Orders
            val orderListFirst = dataFactory.createOrderList(5)
            library.setOrders(orderListFirst)

            // When Set Orders
            val orderListSecond = dataFactory.createOrderList(3, 6)
            library.setOrders(orderListSecond)

            // Then Orders Are Replaced
            val orderFromRawQuery = library.getOrderFromRawQuery("SELECT * FROM DbOrder")
            assertThat(orderFromRawQuery)
                    .containsExactlyElementsIn(orderListSecond)
        }
    }

    @Test
    fun withExistingOrders_whenRemovingSomeOrders_thenOtherOrdersAreKept() {
        runBlocking {

            // With Existing Orders
            val orderListFirst = dataFactory.createOrderList(5)
            library.setOrders(orderListFirst)

            // When Removing Some Orders
            val orderListSecond = orderListFirst.subList(0, 3)
            library.setOrders(orderListSecond)

            // Then Other Orders Are Kept
            val orderFromRawQuery = library.getOrderFromRawQuery("SELECT * FROM DbOrder")
            assertThat(orderFromRawQuery)
                    .containsExactlyElementsIn(orderListSecond)
        }
    }

    @Test
    fun withEmptyDB_whenInsertingOrders_thenItsSaved() {
        runBlocking {
            // With empty DB

            val ordersLiveData = library.getOrders()
            ordersLiveData.captureValues {

                // When inserting orders
                val orders = dataFactory.createOrderList(5)
                library.setOrders(orders)

                // Then its saved
                delay(50)
                assertThat(values).hasSize(2)
                assertThat(values.last()).containsExactlyElementsIn(orders)
            }
        }
    }

    /**
     * OTHER DATA
     */

    @Test
    fun withEmptyDb_whenInsertingData_thenTheChangeUpdaterUpdatesStartAndStop() {
        runBlocking {

            // With Empty Db

            // When Inserting Data
            val songsToInsert = dataFactory.createSongList(5)
            val albumsToInsert = dataFactory.createAlbumList(5)
            val artistsToInsert = dataFactory.createArtistList(5)
            val playlistsToInsert = dataFactory.createPlaylistList(5)
            val orderList = dataFactory.createOrderList(5)
            val filtersToInsert = dataFactory.createFilterList(5)
            val queueList = songsToInsert.map { it.id }
            library.changeUpdater.captureValues {
                library.addOrUpdateSongs(songsToInsert)
                delay(50)
                library.addOrUpdateAlbums(albumsToInsert)
                delay(50)
                library.addOrUpdateArtists(artistsToInsert)
                delay(50)
                library.addOrUpdatePlayLists(playlistsToInsert)
                delay(50)
                library.setOrders(orderList)
                delay(50)
                library.setCurrentFilters(filtersToInsert)
                delay(50)
                library.saveQueueOrder(queueList)
                delay(50)
                library.createFilterGroup(filtersToInsert, "TUTU")
                delay(50)

                // Then The Change Updater Updates Start And Stop
                assertThat(values[0]).isEqualTo(LibraryDatabase.CHANGE_SONGS)
                assertThat(values[1]).isNull()
                assertThat(values[2]).isEqualTo(LibraryDatabase.CHANGE_ALBUMS)
                assertThat(values[3]).isNull()
                assertThat(values[4]).isEqualTo(LibraryDatabase.CHANGE_ARTISTS)
                assertThat(values[5]).isNull()
                assertThat(values[6]).isEqualTo(LibraryDatabase.CHANGE_PLAYLISTS)
                assertThat(values[7]).isNull()
                assertThat(values[8]).isEqualTo(LibraryDatabase.CHANGE_ORDER)
                assertThat(values[9]).isNull()
                assertThat(values[10]).isEqualTo(LibraryDatabase.CHANGE_FILTERS)
                assertThat(values[11]).isNull()
                assertThat(values[12]).isEqualTo(LibraryDatabase.CHANGE_QUEUE)
                assertThat(values[13]).isNull()
                assertThat(values[14]).isEqualTo(LibraryDatabase.CHANGE_FILTER_GROUP)
                assertThat(values[15]).isNull()
            }
        }
    }
}