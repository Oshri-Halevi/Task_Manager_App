package com.example.taskmanagerapp

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.taskmanagerapp.auth.AuthState
import com.example.taskmanagerapp.auth.AuthViewModel
import com.example.taskmanagerapp.data.local.Task
import com.example.taskmanagerapp.databinding.FragmentShareListBinding
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModel
import com.google.android.material.snackbar.Snackbar

class ShareListFragment : Fragment(R.layout.fragment_share_list) {

    private var _binding: FragmentShareListBinding? = null
    private val binding get() = _binding!!

    private val taskViewModel: TaskViewModel by viewModels {
        (requireActivity().application as TaskManagerApp).taskViewModelFactory
    }

    private val authViewModel: AuthViewModel by activityViewModels {
        (requireActivity().application as TaskManagerApp).authViewModelFactory
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShareListBinding.bind(view)

        binding.btnShare.setOnClickListener {
            val invited = binding.inputUserId.text.toString().trim()
            if (invited.isEmpty()) {
                binding.inputLayoutUserId.error = getString(R.string.error_user_required)
                return@setOnClickListener
            }
            binding.inputLayoutUserId.error = null
            val currentUser = currentUserIdOrDefault()
            taskViewModel.shareList(Task.DEFAULT_LIST_ID, invited, currentUser)
        }

        taskViewModel.shareLoading.observe(viewLifecycleOwner) { loading ->
            binding.progress.isVisible = loading
            binding.btnShare.isEnabled = !loading
        }

        taskViewModel.shareResult.observe(viewLifecycleOwner) { success ->
            success?.let {
                val msg = if (it) R.string.share_success else R.string.share_error
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                if (it) findNavController().navigateUp()
                taskViewModel.clearShareResult()
            }
        }
    }

    private fun currentUserIdOrDefault(): String {
        return when (val state = authViewModel.state.value) {
            is AuthState.Authenticated -> state.userId
            else -> "user_1"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
