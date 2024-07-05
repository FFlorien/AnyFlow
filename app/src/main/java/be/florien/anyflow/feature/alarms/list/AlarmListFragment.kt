package be.florien.anyflow.feature.alarms.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Alarm
import be.florien.anyflow.databinding.FragmentAlarmListBinding
import be.florien.anyflow.databinding.ItemAlarmBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.feature.alarms.edit.EditAlarmFragment

class AlarmListFragment : BaseFragment() {
    private lateinit var binding: FragmentAlarmListBinding
    private lateinit var viewModel: AlarmListViewModel

    override fun getTitle() = getString(R.string.alarm_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(AlarmListViewModel::class.java)
        anyFlowApp.serverComponent?.inject(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAlarmListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.songList.adapter = AlarmAdapter()

        viewModel.alarmList.observe(viewLifecycleOwner) {
            binding.songList.adapter?.notifyDataSetChanged()
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