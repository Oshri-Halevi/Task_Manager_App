package com.example.taskmanagerapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.ui.viewmodel.FilterType
import com.example.taskmanagerapp.ui.viewmodel.SortType

class TaskListFragment : BaseTaskListFragment() {

    private val args: TaskListFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun provideTasks(): LiveData<List<Task>> {
        return if (args.listId == Task.DEFAULT_LIST_ID) {
            // "All Tasks" tab opens this fragment with default list id.
            // Show all lists there so imported tasks are visible.
            viewModel.tasks
        } else {
            viewModel.getTasksByList(args.listId)
        }
    }

    override fun navigateToAddTask() {
        findNavController().navigate(R.id.action_taskListFragment_to_addEditTaskFragment)
    }

    override fun navigateToTaskDetails(taskId: Int) {
        val action = TaskListFragmentDirections.actionTaskListFragmentToTaskDetailFragment(taskId)
        findNavController().navigate(action)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.task_filter_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.sort_by_date -> {
                viewModel.setSort(SortType.DATE)
                true
            }
            R.id.sort_by_priority -> {
                viewModel.setSort(SortType.PRIORITY)
                true
            }
            R.id.filter_all -> {
                viewModel.setFilter(FilterType.ALL)
                true
            }
            R.id.filter_active -> {
                viewModel.setFilter(FilterType.ACTIVE)
                true
            }
            R.id.filter_done -> {
                viewModel.setFilter(FilterType.DONE)
                true
            }
            R.id.action_share_list -> {
                findNavController().navigate(R.id.action_taskListFragment_to_shareListFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
