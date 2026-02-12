package com.example.taskmanagerapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.taskmanagerapp.databinding.FragmentListsBinding
import com.example.taskmanagerapp.ui.adapter.ListsAdapter
import com.example.taskmanagerapp.ui.viewmodel.ListViewModel
import com.google.android.material.snackbar.Snackbar

class ListsFragment : Fragment(R.layout.fragment_lists) {

    private var _binding: FragmentListsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ListViewModel by viewModels {
        (requireActivity().application as TaskManagerApp).listViewModelFactory
    }

    private lateinit var adapter: ListsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentListsBinding.bind(view)

        setupRecyclerView()
        setupFab()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = ListsAdapter { list ->
            // Navigate to tasks for this list
            val action = ListsFragmentDirections.actionListsFragmentToTaskListFragment(list.id)
            findNavController().navigate(action)
        }
        binding.recyclerViewLists.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddList.setOnClickListener {
            showAddListDialog()
        }
    }

    private fun observeData() {
        viewModel.listsWithCounts.observe(viewLifecycleOwner) { lists ->
            adapter.submitList(lists)
            binding.loadingIndicator.isVisible = false
            binding.emptyStateText.isVisible = lists.isEmpty()
            binding.recyclerViewLists.isVisible = lists.isNotEmpty()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.loadingIndicator.isVisible = loading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showAddListDialog() {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.hint_list_name)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_add_list_title)
            .setView(editText)
            .setPositiveButton(R.string.btn_create) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createList(name)
                } else {
                    editText.error = getString(R.string.error_list_name_required)
                    Snackbar.make(binding.root, R.string.error_list_name_required, Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
