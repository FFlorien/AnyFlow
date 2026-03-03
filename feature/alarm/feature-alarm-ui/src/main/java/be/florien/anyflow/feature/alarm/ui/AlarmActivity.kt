package be.florien.anyflow.feature.alarm.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.alarm.ui.di.AlarmActivityComponentCreator
import javax.inject.Inject


class AlarmActivity : AppCompatActivity(), ViewModelFactoryProvider {
    @Inject
    override lateinit var viewModelFactory: AnyFlowViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityComponent = (application as AlarmActivityComponentCreator)
            .createAlarmActivityComponent()
            ?: throw IllegalStateException()
        activityComponent.inject(this)
        setContent {
            AppTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = AlarmList
                ) {
                    alarmListDestination(navController, viewModelFactory) { this@AlarmActivity.finish()}
                    addAlarmDestination(navController,viewModelFactory)
                    editAlarmDestination(navController,viewModelFactory)
                }
            }
        }
    }
}