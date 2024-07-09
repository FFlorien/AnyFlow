package be.florien.anyflow.feature.player.ui.info.song.shortcuts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.R
import be.florien.anyflow.tags.view.QueueItemDisplay
import be.florien.anyflow.databinding.ActivityShortcutBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.extension.getDisplayWidth
import be.florien.anyflow.extension.startActivity
import be.florien.anyflow.feature.auth.UserConnectActivity
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions
import be.florien.anyflow.feature.player.ui.info.song.SongInfoFragment
import be.florien.anyflow.feature.player.ui.songlist.SongListViewHolderListener
import be.florien.anyflow.feature.player.ui.songlist.SongListViewHolderProvider
import be.florien.anyflow.feature.player.ui.songlist.SongViewHolder
import be.florien.anyflow.architecture.di.AnyFlowViewModelFactory
import be.florien.anyflow.injection.ShortcutsComponent
import be.florien.anyflow.injection.ViewModelFactoryHolder
import javax.inject.Inject

class ShortcutsActivity : AppCompatActivity(), ViewModelFactoryHolder {

    private lateinit var shortcutExample: SongViewHolder
    private lateinit var binding: ActivityShortcutBinding
    private lateinit var activityComponent: ShortcutsComponent
    private lateinit var viewModel: ShortcutsViewModel

    @Inject
    lateinit var viewModelFactory: AnyFlowViewModelFactory

    private val fakeComponent = object : ShortcutsComponent {
        override fun inject(shortcutsActivity: ShortcutsActivity) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val component = anyFlowApp.serverComponent
            ?.shortcutsComponentBuilder()
            ?.build()
        activityComponent = if (component != null) {
            component
        } else {
            startActivity(UserConnectActivity::class)
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
                    SongInfoFragment(SongInfoActions.DUMMY_SONG_ID),
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

    override fun getFactory() = viewModelFactory

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
}