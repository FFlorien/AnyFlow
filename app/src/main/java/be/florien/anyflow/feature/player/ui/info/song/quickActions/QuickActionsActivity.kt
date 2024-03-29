package be.florien.anyflow.feature.player.ui.info.song.quickActions

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ActivityQuickActionsBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.extension.getDisplayWidth
import be.florien.anyflow.extension.startActivity
import be.florien.anyflow.feature.auth.UserConnectActivity
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions
import be.florien.anyflow.feature.player.ui.info.song.SongInfoFragment
import be.florien.anyflow.injection.AnyFlowViewModelFactory
import be.florien.anyflow.injection.QuickActionsComponent
import be.florien.anyflow.injection.ViewModelFactoryHolder
import javax.inject.Inject

class QuickActionsActivity : AppCompatActivity(), ViewModelFactoryHolder {

    private lateinit var binding: ActivityQuickActionsBinding
    private lateinit var activityComponent: QuickActionsComponent
    private lateinit var viewModel: QuickActionsViewModel

    @Inject
    lateinit var viewModelFactory: AnyFlowViewModelFactory

    private val fakeComponent = object : QuickActionsComponent {
        override fun inject(quickActionsActivity: QuickActionsActivity) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val component = anyFlowApp.serverComponent
            ?.quickActionsComponentBuilder()
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
            ViewModelProvider(this, viewModelFactory)[QuickActionsViewModel::class.java]
                .apply {
                    val width = getDisplayWidth()
                    val itemWidth = resources.getDimensionPixelSize(R.dimen.minClickableSize)
                    val margin = resources.getDimensionPixelSize(R.dimen.smallDimen)
                    val itemFullWidth = itemWidth + margin + margin
                    maxItems = (width / itemFullWidth) - 1
                }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quick_actions)
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