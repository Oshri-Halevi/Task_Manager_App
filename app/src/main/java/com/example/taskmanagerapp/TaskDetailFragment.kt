package com.example.taskmanagerapp

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.taskmanagerapp.databinding.FragmentTaskDetailBinding
import com.example.taskmanagerapp.ui.common.showConfirmationDialog
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModel
import com.google.android.material.snackbar.Snackbar

class TaskDetailFragment : Fragment(R.layout.fragment_task_detail) {

    private var _binding: FragmentTaskDetailBinding? = null
    private val binding get() = _binding!!

    private val args: TaskDetailFragmentArgs by navArgs()

    private val viewModel: TaskViewModel by viewModels {
        (requireActivity().application as TaskManagerApp).taskViewModelFactory
    }

    private var pendingDelete = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTaskDetailBinding.bind(view)

        observeState() // מזינה לשינויים במשימה
        viewModel.loadTask(args.taskId)

        binding.btnEdit.setOnClickListener {
            val action =
                TaskDetailFragmentDirections.actionTaskDetailFragmentToAddEditTaskFragment(args.taskId)
            findNavController().navigate(action)
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun observeState() {
        viewModel.selectedTask.observe(viewLifecycleOwner) { task ->
            if (task == null) {
                findNavController().navigateUp()
                return@observe
            }

            binding.titleText.text = task.title
            binding.descriptionText.text = task.description

            // Show due date
            if (task.dueDate != null) {
                val dateString = android.text.format.DateFormat.getDateFormat(requireContext())
                    .format(java.util.Date(task.dueDate))
                binding.textDetailDueDate.text = getString(R.string.label_due_date, dateString)
                binding.textDetailDueDate.visibility = View.VISIBLE
            } else {
                binding.textDetailDueDate.visibility = View.GONE
            }

            // Show priority
            val priorityText = when (task.priority) {
                0 -> getString(R.string.priority_low)
                2 -> getString(R.string.priority_high)
                else -> getString(R.string.priority_normal)
            }
            binding.textDetailPriority.text = getString(R.string.label_priority) + ": " + priorityText
            binding.textDetailPriority.visibility = View.VISIBLE
            
            // Update checkbox without triggering listener
            binding.checkboxDone.setOnCheckedChangeListener(null)
            binding.checkboxDone.isChecked = task.isDone
            binding.checkboxDone.setOnCheckedChangeListener { _, isChecked ->
                if (task.isDone != isChecked) {
                    viewModel.update(task.copy(isDone = isChecked))
                    val message = if (isChecked) R.string.task_marked_done else R.string.task_marked_active
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                }
            }

            if (!task.imageUri.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(task.imageUri)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.detailImage)
            } else {
                binding.detailImage.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.detailProgress.isVisible = loading
            binding.btnEdit.isEnabled = !loading
            binding.btnDelete.isEnabled = !loading
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { processing ->
            binding.detailProgress.isVisible = processing || viewModel.isLoading.value == true
            binding.btnEdit.isEnabled = !processing
            binding.btnDelete.isEnabled = !processing
            binding.checkboxDone.isEnabled = !processing
            if (!processing && pendingDelete) {
                pendingDelete = false
                findNavController().navigateUp()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { messageRes ->
            messageRes?.let {
                pendingDelete = false
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        showConfirmationDialog(
            titleRes = R.string.dialog_delete_title,
            messageRes = R.string.dialog_delete_message,
            onConfirm = {
                val task = viewModel.selectedTask.value
                if (task != null) {
                    pendingDelete = true
                    viewModel.delete(task)
                } else {
                    Snackbar.make(binding.root, R.string.error_task_missing, Snackbar.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
