package be.florien.anyflow.feature.player.ui.info.song.shortcuts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ActivityShortcutBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.extension.getDisplayWidth
import be.florien.anyflow.extension.startActivity
import be.florien.anyflow.feature.auth.UserConnectActivity
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions
import be.florien.anyflow.feature.player.ui.info.song.SongInfoFragment
import be.florien.anyflow.injection.AnyFlowViewModelFactory
import be.florien.anyflow.injection.ShortcutsComponent
import be.florien.anyflow.injection.ViewModelFactoryHolder
import javax.inject.Inject

class ShortcutsActivity : AppCompatActivity(), ViewModelFactoryHolder {

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