package be.florien.anyflow.feature.alarms.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentAddAlarmBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.alarms.AlarmActivity
import be.florien.anyflow.feature.menu.implementation.ConfirmAlarmMenuHolder
import be.florien.anyflow.feature.menu.MenuHolder
import kotlinx.coroutines.launch

class AddAlarmFragment : BaseFragment() {
    override fun getTitle() = getString(R.string.alarm_add)

    private lateinit var binding: FragmentAddAlarmBinding
    private lateinit var viewModel: AddAlarmViewModel
    private lateinit var confirmMenuHolder: MenuHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())[AddAlarmViewModel::class.java]
        anyFlowApp.applicationComponent.inject(viewModel)
        confirmMenuHolder = ConfirmAlarmMenuHolder {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.addAlarm()
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
        (requireActivity() as AlarmActivity).menuCoordinator.addMenuHolder(confirmMenuHolder)
        requireActivity().invalidateOptionsMenu()
        confirmMenuHolder.isVisible = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAddAlarmBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.time.setIs24HourView(true)
    }

    override fun onDetach() {
        super.onDetach()
        confirmMenuHolder.isVisible = false
        (requireActivity() as AlarmActivity).menuCoordinator.removeMenuHolder(confirmMenuHolder)
    }
}