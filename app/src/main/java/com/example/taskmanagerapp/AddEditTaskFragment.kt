package com.example.taskmanagerapp

import android.app.DatePickerDialog
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.databinding.FragmentAddEditTaskBinding
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModel
import com.google.android.material.chip.Chip
import java.util.Date

class AddEditTaskFragment : Fragment(R.layout.fragment_add_edit_task) {

    private var _binding: FragmentAddEditTaskBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditTaskFragmentArgs by navArgs()

    private val viewModel: TaskViewModel by viewModels {
        (requireActivity().application as TaskManagerApp).taskViewModelFactory
    }

    private var selectedImageUri: String? = null
    private var selectedDueDate: Long? = null
    private var hasLoadedExisting = false
    private var awaitingSaveResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            selectedImageUri = savedInstanceState.getString("image_uri")
            val date = savedInstanceState.getLong("due_date", -1)
            if (date != -1L) selectedDueDate = date
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddEditTaskBinding.bind(view)

        if (args.taskId != -1) {
            viewModel.loadTask(args.taskId)
        } else {
            binding.chipGroupPriority.check(R.id.chipNormal)
            if (selectedDueDate == null) {
                selectedDueDate = startOfToday()
            }
        }

        if (selectedImageUri != null) {
            binding.imagePreview.setImageURI(android.net.Uri.parse(selectedImageUri))
        }
        updateDateText()
        setupPriorityChips()
        setupObservers()

        binding.btnPickDate.setOnClickListener { showDatePicker() }
        binding.btnPickImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSave.setOnClickListener { saveTask() }
    }

    private fun setupObservers() {
        viewModel.selectedTask.observe(viewLifecycleOwner) { task ->
            if (task != null && !hasLoadedExisting) {
                hasLoadedExisting = true
                binding.editTitle.setText(task.title)
                binding.editDescription.setText(task.description)
                selectedDueDate = selectedDueDate ?: task.dueDate
                updateDateText()

                when (task.priority) {
                    0 -> binding.chipGroupPriority.check(R.id.chipLow)
                    2 -> binding.chipGroupPriority.check(R.id.chipHigh)
                    else -> binding.chipGroupPriority.check(R.id.chipNormal)
                }

                if (selectedImageUri == null) {
                    selectedImageUri = task.imageUri
                    if (!task.imageUri.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(task.imageUri)
                            .into(binding.imagePreview)
                    }
                }
            } else if (task == null && args.taskId != -1 && !hasLoadedExisting) {
                Toast.makeText(requireContext(), R.string.error_task_missing, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.saveProgress.isVisible = loading || viewModel.isProcessing.value == true
            binding.btnSave.isEnabled = !loading && viewModel.isProcessing.value != true
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { processing ->
            binding.saveProgress.isVisible = processing || viewModel.isLoading.value == true
            binding.btnSave.isEnabled = !processing && viewModel.isLoading.value != true
            if (!processing && awaitingSaveResult && viewModel.errorMessage.value == null) {
                navigateAfterSave()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { messageRes ->
            messageRes?.let {
                awaitingSaveResult = false
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupPriorityChips() {
        binding.chipGroupPriority.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as Chip
                if (chip == selectedChip) {
                    chip.setChipBackgroundColorResource(R.color.selectedPriority)
                    chip.setTextColor(Color.WHITE)
                } else {
                    val colorRes = when (chip.id) {
                        R.id.chipLow -> R.color.priority_low
                        R.id.chipNormal -> R.color.priority_normal
                        R.id.chipHigh -> R.color.priority_high
                        else -> R.color.priority_normal
                    }
                    chip.setChipBackgroundColorResource(colorRes)
                    chip.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.textColorPrimary)
                    )
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (selectedDueDate != null) {
            calendar.timeInMillis = selectedDueDate!!
        }

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDueDate = calendar.timeInMillis
                updateDateText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun updateDateText() {
        if (selectedDueDate != null) {
            val dateString =
                DateFormat.getDateFormat(requireContext()).format(Date(selectedDueDate!!))
            binding.textDueDate.text = getString(R.string.label_due_date, dateString)
        } else {
            binding.textDueDate.text = getString(R.string.no_date_selected)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("image_uri", selectedImageUri)
        if (selectedDueDate != null) {
            outState.putLong("due_date", selectedDueDate!!)
        }
    }

    private fun saveTask() {
        val title = binding.editTitle.text.toString()
        val description = binding.editDescription.text.toString()

        if (title.isBlank()) {
            val errorMsg = getString(R.string.toast_title_required)
            binding.inputLayoutTitle.error = getString(R.string.error_title_required)
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
            return
        } else {
            binding.inputLayoutTitle.error = null
        }

        val priority = when (binding.chipGroupPriority.checkedChipId) {
            R.id.chipLow -> 0
            R.id.chipHigh -> 2
            else -> 1
        }

        awaitingSaveResult = true

        // Default to today if no date was picked (needed for calendar sync)
        val dueDate = selectedDueDate ?: startOfToday()

        if (args.taskId == -1) {
            val task = Task(
                title = title,
                description = description,
                isDone = false,
                imageUri = selectedImageUri,
                priority = priority,
                dueDate = dueDate
            )
            viewModel.insert(task)
        } else {
            val existing = viewModel.selectedTask.value
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    description = description,
                    imageUri = selectedImageUri,
                    priority = priority,
                    dueDate = dueDate
                )
                viewModel.update(updated)
            } else {
                awaitingSaveResult = false
                Toast.makeText(requireContext(), R.string.error_task_missing, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateAfterSave() {
        awaitingSaveResult = false
        if (args.taskId == -1) {
            findNavController().navigateUp()
        } else {
            val action = AddEditTaskFragmentDirections.actionAddEditTaskFragmentToTaskListFragment()
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it.toString()
            binding.imagePreview.setImageURI(it)
            binding.imagePreview.requestFocus()
        }
    }

    private fun startOfToday(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
