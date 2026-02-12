package com.example.taskmanagerapp

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.databinding.FragmentTaskListBinding
import com.example.taskmanagerapp.ui.TaskAdapter
import com.example.taskmanagerapp.ui.common.showConfirmationDialog
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModel
import com.google.android.material.snackbar.Snackbar

abstract class BaseTaskListFragment : Fragment(R.layout.fragment_task_list) {

    private var _binding: FragmentTaskListBinding? = null
    protected val binding get() = _binding!!

    protected lateinit var adapter: TaskAdapter

    protected val viewModel: TaskViewModel by viewModels {
        (requireActivity().application as TaskManagerApp).taskViewModelFactory
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTaskListBinding.bind(view)
        viewModel.markListLoading()
        setupList()
        observeTasks()
        observeState()

        binding.fabAddTask.setOnClickListener { navigateToAddTask() }
    }

    protected open fun provideTasks(): LiveData<List<Task>> = viewModel.tasks

    protected abstract fun navigateToAddTask()

    protected abstract fun navigateToTaskDetails(taskId: Int)

    private fun setupList() {
        adapter = TaskAdapter(
            onItemClick = { task -> navigateToTaskDetails(task.id) },
            onCheckChanged = { task, isChecked ->
                viewModel.update(task.copy(isDone = isChecked))
            }
        )

        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter
        ItemTouchHelper(createSwipeCallback()).attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun createSwipeCallback(): ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION || position >= adapter.itemCount) {
                    return
                }
                val task = adapter.getTaskAt(position)
                showDeleteConfirmation(task, position)
            }
    }

    private fun showDeleteConfirmation(task: Task, position: Int) {
        var restoreOnDismiss = true
        showConfirmationDialog(
            titleRes = R.string.dialog_delete_title,
            messageRes = R.string.dialog_delete_message,
            onConfirm = {
                restoreOnDismiss = false
                viewModel.delete(task)
                Snackbar.make(binding.root, R.string.task_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.insert(task) }
                    .show()
            },
            onCancel = {
                restoreOnDismiss = false
                restoreSwipedItem(position)
            },
            onDismiss = {
                if (restoreOnDismiss) {
                    restoreSwipedItem(position)
                }
            }
        )
    }

    private fun observeTasks() {
        provideTasks().observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateProgress(list)
            binding.loadingIndicator.isVisible = false
            binding.emptyStateText.isVisible = list.isEmpty()
            binding.recyclerViewTasks.isVisible = list.isNotEmpty()
            viewModel.markListLoaded()
        }
    }

    private fun observeState() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.loadingIndicator.isVisible = loading || viewModel.isProcessing.value == true
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { processing ->
            binding.fabAddTask.isEnabled = !processing
            binding.recyclerViewTasks.isEnabled = !processing
            binding.loadingIndicator.isVisible = processing || viewModel.isLoading.value == true
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { messageRes ->
            messageRes?.let {
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateProgress(tasks: List<Task>) {
        val total = tasks.size
        val done = tasks.count { it.isDone }
        val progress = if (total > 0) (done * 100) / total else 0
        binding.progressBarToday.progress = progress
        binding.textProgressCount.text = getString(R.string.progress_text, done, total)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun restoreSwipedItem(position: Int) {
        if (position in 0 until adapter.itemCount) {
            adapter.notifyItemChanged(position)
        }
    }
}
