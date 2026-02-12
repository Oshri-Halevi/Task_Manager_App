package com.example.taskmanagerapp

import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.example.taskmanagerapp.data.local.Task

class TodayTasksFragment : BaseTaskListFragment() {

    override fun provideTasks(): LiveData<List<Task>> = viewModel.todayTasks

    override fun navigateToAddTask() {
        findNavController().navigate(R.id.action_todayTasksFragment_to_addEditTaskFragment)
    }

    override fun navigateToTaskDetails(taskId: Int) {
        val action = TodayTasksFragmentDirections.actionTodayTasksFragmentToTaskDetailFragment(taskId)
        findNavController().navigate(action)
    }
}
