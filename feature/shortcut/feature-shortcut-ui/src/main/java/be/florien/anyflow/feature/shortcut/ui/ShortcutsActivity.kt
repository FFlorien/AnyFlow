package be.florien.anyflow.feature.shortcut.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import be.florien.anyflow.architecture.di.AnyFlowViewModelFactory
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.getDisplayWidth
import be.florien.anyflow.common.ui.list.SongListViewHolderListener
import be.florien.anyflow.common.ui.list.SongListViewHolderProvider
import be.florien.anyflow.common.ui.list.SongViewHolder
import be.florien.anyflow.common.ui.navigation.Navigator
import be.florien.anyflow.feature.shortcut.ui.databinding.ActivityShortcutBinding
import be.florien.anyflow.feature.shortcut.ui.di.ShortcutActivityComponent
import be.florien.anyflow.feature.shortcut.ui.di.ShortcutActivityComponentCreator
import be.florien.anyflow.feature.song.ui.BaseSongInfoActions
import be.florien.anyflow.feature.song.ui.SongInfoFragment
import be.florien.anyflow.feature.song.ui.di.SongViewModelProvider
import be.florien.anyflow.management.queue.model.QueueItemDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.model.SongInfo
import javax.inject.Inject

class ShortcutsActivity : AppCompatActivity(), SongViewModelProvider<ShortcutsViewModel> {

    private lateinit var shortcutExample: SongViewHolder
    private lateinit var binding: ActivityShortcutBinding
    private lateinit var activityComponent: ShortcutActivityComponent
    private lateinit var viewModel: ShortcutsViewModel

    @Inject
    lateinit var viewModelFactory: AnyFlowViewModelFactory

    @Inject
    lateinit var navigator: Navigator

    private val fakeComponent = object : ShortcutActivityComponent {
        override fun inject(shortcutsActivity: ShortcutsActivity) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val component = (application as ShortcutActivityComponentCreator).createShortcutActivityComponent()
        activityComponent = if (component != null) {
            component
        } else {
            navigator.navigateToConnect(this)
            finish()
            fakeComponent
        }
        super.onCreate(savedInstanceState)

        if (activityComponent == fakeComponent) {
            return
        }
        activityComponent.inject(this)

        viewModel =
            ViewModelProvider(this, viewModelFactory)[ShortcutsViewModel::class.java]
                .apply {
                    val width = getDisplayWidth()
                    val itemWidth = resources.getDimensionPixelSize(R.dimen.minClickableSize)
                    val margin = resources.getDimensionPixelSize(R.dimen.smallDimen)
                    val itemFullWidth = itemWidth + margin + margin
                    maxItems = (width / itemFullWidth) - 1
                }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_shortcut)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        initToolbar()
        initSongExample()
        viewModel.currentActionsCountDisplay.observe(this ){
            shortcutExample.setShortcuts()
        }
        val fragment =
            supportFragmentManager.findFragmentByTag(SongInfoFragment::class.java.simpleName)
        if (fragment == null) {
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container_view,
                    SongInfoFragment(ShortcutsViewModel::class.java),
                    SongInfoFragment::class.java.simpleName
                )
                .commit()
        }
    }

    private fun initSongExample() {
        val provider = object: SongListViewHolderProvider {
            override fun getShortcuts() = viewModel.shortcutsList
            override fun getCurrentPosition(): Int  = 0
            override fun getCurrentSongTranslationX(): Float = 0F
            override fun getArtUrl(id: Long, isPodcast: Boolean): String = ""
        }
        val listener= object: SongListViewHolderListener {
            override fun onShortcut(item: QueueItemDisplay, row: InfoActions.InfoRow) {}
            override fun onShortcutOpened(position: Int?) {}
            override fun onCurrentSongShortcutsClosed() {}
            override fun onInfoDisplayAsked(item: QueueItemDisplay) {}
        }
        shortcutExample = SongViewHolder(binding.root as ConstraintLayout, listener, provider, null, binding.songExample, true)
        shortcutExample.setShortcuts()
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_up)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            if (!supportFragmentManager.popBackStackImmediate()) {
                finish()
            }
        }
    }

    override fun getSongViewModel(owner: ViewModelStoreOwner?): ShortcutsViewModel =
        ViewModelProvider(this, viewModelFactory)[ShortcutsViewModel::class.java]
            .apply {
                val width = getDisplayWidth()
                val itemWidth = resources.getDimensionPixelSize(R.dimen.minClickableSize)
                val margin = resources.getDimensionPixelSize(R.dimen.smallDimen)
                val itemFullWidth = itemWidth + margin + margin
                maxItems = (width / itemFullWidth) - 1
                val title = getString(R.string.dummy_title)
                val artistName = getString(R.string.dummy_artist)
                val albumName = getString(R.string.dummy_album)
                val time = 120
                dummySongInfo =
                    SongInfo(
                        BaseSongInfoActions.DUMMY_SONG_ID,
                        title,
                        artistName,
                        0L,
                        albumName,
                        0L,
                        1,
                        artistName,
                        0L,
                        listOf(getString(R.string.dummy_genre)),
                        listOf(0L),
                        listOf(getString(R.string.dummy_playlist)),
                        listOf(0L),
                        1,
                        time,
                        2000,
                        0,
                        null
                    )
                dummySongDisplay = SongDisplay(
                    BaseSongInfoActions.DUMMY_SONG_ID,
                    title,
                    artistName,
                    albumName,
                    0L,
                    time
                )
            }
}