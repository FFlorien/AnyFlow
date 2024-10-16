package be.florien.anyflow.feature.alarm.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.feature.alarm.ui.R
import be.florien.anyflow.feature.alarm.ui.databinding.FragmentAlarmListBinding
import be.florien.anyflow.feature.alarm.ui.databinding.ItemAlarmBinding
import be.florien.anyflow.feature.alarm.ui.edit.EditAlarmFragment
import be.florien.anyflow.management.alarm.model.Alarm

class AlarmListFragment : BaseFragment() {
    private lateinit var binding: FragmentAlarmListBinding
    private lateinit var viewModel: AlarmListViewModel

    override fun getTitle() = getString(R.string.alarm_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, requireActivity().viewModelFactory)[AlarmListViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAlarmListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.alarmList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.alarmList.adapter = AlarmAdapter()

        viewModel.alarmList.observe(viewLifecycleOwner) {
            binding.alarmList.adapter?.notifyDataSetChanged()
        }
    }

    inner class AlarmAdapter : RecyclerView.Adapter<AlarmViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AlarmViewHolder(parent)

        override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
            viewModel.alarmList.value?.let {
                holder.bind(it[position])
            }
        }

        override fun getItemCount(): Int = viewModel.alarmList.value?.size ?: 0

    }

    inner class AlarmViewHolder(container: ViewGroup, val binding: ItemAlarmBinding = ItemAlarmBinding.inflate(LayoutInflater.from(container.context), container, false))
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(alarm: Alarm) {
            binding.alarm = alarm
            binding.viewModel = viewModel
            val repetitionText = viewModel.repetitionText(alarm)
            binding.weekDays.text = when (repetitionText.size) {
                0 -> ""
                1 -> getString(repetitionText[0])
                else -> repetitionText.joinToString(separator = ", ") { getString(it) }
            }
            binding.active.setOnCheckedChangeListener { _, isChecked -> viewModel.setAlarmActive(alarm, isChecked) }
            binding.root.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction().replace(R.id.container, EditAlarmFragment(alarm)).addToBackStack(null).commit()
            }
        }

    }
}