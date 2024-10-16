package be.florien.anyflow.feature.shortcut.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.getDisplayWidth
import be.florien.anyflow.common.ui.list.SongListViewHolderListener
import be.florien.anyflow.common.ui.list.SongListViewHolderProvider
import be.florien.anyflow.common.ui.list.SongViewHolder
import be.florien.anyflow.feature.shortcut.ui.databinding.ActivityShortcutBinding
import be.florien.anyflow.feature.shortcut.ui.di.ShortcutActivityComponentCreator
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoFragment
import be.florien.anyflow.management.queue.model.QueueItemDisplay
import javax.inject.Inject

class ShortcutsActivity : AppCompatActivity(), ViewModelFactoryProvider {

    private lateinit var shortcutExample: SongViewHolder
    private lateinit var binding: ActivityShortcutBinding
    private lateinit var viewModel: ShortcutsViewModel

    @Inject
    override lateinit var viewModelFactory: AnyFlowViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        val component = (application as ShortcutActivityComponentCreator)
                .createShortcutActivityComponent()
                ?: throw IllegalStateException()
        component.inject(this)

        super.onCreate(savedInstanceState)

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
        viewModel.currentActionsCountDisplay.observe(this) {
            shortcutExample.setShortcuts()
        }
        val fragment =
            supportFragmentManager.findFragmentByTag(BaseSongInfoFragment::class.java.simpleName)
        if (fragment == null) {
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container_view,
                    ShortcutSongInfoFragment(),
                    BaseSongInfoFragment::class.java.simpleName
                )
                .commit()
        }
    }

    private fun initSongExample() {
        val provider = object : SongListViewHolderProvider {
            override fun getShortcuts() = viewModel.shortcutsList
            override fun getCurrentPosition(): Int = 0
            override fun getCurrentSongTranslationX(): Float = 0F
            override fun getArtUrl(id: Long, isPodcast: Boolean): String = ""
        }
        val listener = object : SongListViewHolderListener {
            override fun onShortcut(item: QueueItemDisplay, row: InfoActions.InfoRow) {}
            override fun onShortcutOpened(position: Int?) {}
            override fun onCurrentSongShortcutsClosed() {}
            override fun onInfoDisplayAsked(item: QueueItemDisplay) {}
        }
        shortcutExample = SongViewHolder(
            binding.root as ConstraintLayout,
            listener,
            provider,
            null,
            binding.songExample,
            true
        )
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
}