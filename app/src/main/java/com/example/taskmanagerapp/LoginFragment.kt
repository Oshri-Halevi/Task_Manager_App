package com.example.taskmanagerapp

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import android.util.Log
import android.widget.Toast
import com.example.taskmanagerapp.auth.AuthState
import com.example.taskmanagerapp.auth.AuthViewModel
import com.example.taskmanagerapp.auth.AppConfig
import com.example.taskmanagerapp.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment(R.layout.fragment_login) {

    companion object {
        private const val TAG = "LoginFragment"
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels {
        (requireActivity().application as TaskManagerApp).authViewModelFactory
    }
    private var hasGoogleClientId: Boolean = false

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google sign-in result received, resultCode=${result.resultCode}")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google account: ${account.email}, idToken present: ${account.idToken != null}")
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                Log.e(TAG, "Google sign-in succeeded but idToken is null. Check GOOGLE_WEB_CLIENT_ID in local.properties matches the OAuth Web Client ID in Google Cloud Console.")
                Snackbar.make(binding.root, R.string.error_auth_failed, Snackbar.LENGTH_LONG).show()
                binding.btnGoogle.isEnabled = true
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in ApiException: statusCode=${e.statusCode}, message=${e.message}", e)
            Snackbar.make(binding.root, R.string.error_auth_failed, Snackbar.LENGTH_LONG).show()
            binding.btnGoogle.isEnabled = true
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in unexpected error", e)
            Snackbar.make(binding.root, R.string.error_auth_failed, Snackbar.LENGTH_LONG).show()
            binding.btnGoogle.isEnabled = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        // Log config status for debugging
        AppConfig.logConfigStatus()

        hasGoogleClientId = AppConfig.hasGoogleClientId()

        // NOTE: Only request email + idToken at sign-in time.
        // Calendar scope is requested separately when importing calendar events.
        val googleClient = if (hasGoogleClientId) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(AppConfig.googleWebClientId)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(requireContext(), gso)
        } else null

        binding.googleMissingMessage.isVisible = false
        binding.btnGoogle.isEnabled = true

        // Demo button only visible in debug builds
        binding.btnDemo.isVisible = AppConfig.isDebugBuild

        binding.btnGoogle.setOnClickListener {
            Log.d(TAG, "Google sign-in clicked")
            if (!hasGoogleClientId || googleClient == null) {
                Snackbar.make(binding.root, R.string.error_missing_google_client_id, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnGoogle.isEnabled = false
            googleLauncher.launch(googleClient.signInIntent)
        }

        binding.btnDemo.setOnClickListener {
            Log.d(TAG, "Demo sign-in clicked")
            binding.btnDemo.isEnabled = false
            binding.btnGoogle.isEnabled = false
            viewModel.signInFake()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state is AuthState.Loading
            val enable = state !is AuthState.Loading
            binding.btnDemo.isEnabled = enable
            binding.btnGoogle.isEnabled = enable
            when (state) {
                is AuthState.Authenticated -> navigateToMain()
                is AuthState.Error -> Snackbar.make(requireView(), getString(state.message), Snackbar.LENGTH_LONG).show()
                else -> Unit
            }
        }
    }

    private fun navigateToMain() {
        if (findNavController().currentDestination?.id != R.id.loginFragment) return
        findNavController().navigate(
            LoginFragmentDirections.actionLoginFragmentToTaskListFragment()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
