package com.example.taskmanagerapp

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.taskmanagerapp.databinding.FragmentSettingsBinding
import com.example.taskmanagerapp.ui.viewmodel.TaskViewModel
import com.example.taskmanagerapp.ui.viewmodel.SyncResultState
import com.example.taskmanagerapp.auth.AppConfig
import com.example.taskmanagerapp.auth.AuthViewModel
import com.example.taskmanagerapp.ui.common.showConfirmationDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

/**
 * Settings UI for account preferences and sync/import actions.
 * All task operations are routed through TaskViewModel.
 */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskViewModel by viewModels {
        (requireActivity().application as TaskManagerApp).taskViewModelFactory
    }

    private val authViewModel: AuthViewModel by activityViewModels {
        (requireActivity().application as TaskManagerApp).authViewModelFactory
    }

    private lateinit var prefs: SharedPreferences

    private enum class PendingGoogleAction { IMPORT, SYNC }
    private var pendingGoogleAction: PendingGoogleAction? = null

    // Launcher for requesting calendar scope consent
    private val calendarScopeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        when (pendingGoogleAction) {
            PendingGoogleAction.IMPORT -> {
                if (account != null && hasScopes(account, viewModel.requiredCalendarImportScopes())) {
                    Log.d("SettingsFragment", "Google scopes granted, proceeding with import")
                    viewModel.importCalendar()
                } else {
                    Log.w("SettingsFragment", "Google scopes were not granted for import")
                    val reason = getString(R.string.calendar_permission_denied)
                    val message = getString(R.string.error_calendar_import_with_reason, reason)
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                }
            }
            PendingGoogleAction.SYNC -> {
                if (account != null && hasScopes(account, viewModel.requiredCalendarSyncScopes())) {
                    Log.d("SettingsFragment", "Google scopes granted, proceeding with sync")
                    viewModel.syncNow()
                } else {
                    Log.w("SettingsFragment", "Google scopes were not granted for sync")
                    val message = getString(R.string.sync_error, getString(R.string.calendar_permission_denied))
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                }
            }
            null -> Unit
        }
        pendingGoogleAction = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        setupAccountInfo()
        binding.btnDeleteAll.setOnClickListener { showDeleteAllConfirmationDialog() }
        setupSyncButton()
        setupLogoutButton()
        setupImportCalendarButton()

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        binding.switchDarkMode.isChecked = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            applyTheme(isChecked)
        }

        val deviceLang = Locale.getDefault().language
        val savedLang = prefs.getString("language", null)
        val currentLang = savedLang ?: deviceLang
        if (currentLang == "iw" || currentLang == "he") {
            binding.radioHebrew.isChecked = true
        } else {
            binding.radioEnglish.isChecked = true
        }

        binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            val selectedLangCode = when (checkedId) {
                R.id.radioHebrew -> "iw"
                else -> "en"
            }
            if (currentLang != selectedLangCode) {
                prefs.edit().putString("language", selectedLangCode).apply()
                requireActivity().recreate()
            }
        }

        val fontSize = prefs.getString("font_size", "normal")
        when (fontSize) {
            "small" -> binding.radioFontSmall.isChecked = true
            "large" -> binding.radioFontLarge.isChecked = true
            else -> binding.radioFontNormal.isChecked = true
        }

        binding.radioGroupFont.setOnCheckedChangeListener { _, checkedId ->
            val size = when (checkedId) {
                R.id.radioFontSmall -> "small"
                R.id.radioFontLarge -> "large"
                else -> "normal"
            }
            if (prefs.getString("font_size", "normal") != size) {
                prefs.edit().putString("font_size", size).apply()
                requireActivity().recreate()
            }
        }

        observeState()
    }

    private fun observeState() {
        viewModel.isProcessing.observe(viewLifecycleOwner) { processing ->
            binding.btnDeleteAll.isEnabled = !processing
            binding.progressDeleteAll.isVisible = processing
            // Disable all action buttons while processing
            binding.btnSyncNow.isEnabled = !processing && viewModel.syncLoading.value != true
            binding.btnImportCalendar.isEnabled = !processing && viewModel.calendarImportLoading.value != true
        }

        viewModel.deleteAllDone.observe(viewLifecycleOwner) { done ->
            if (done == true) {
                Snackbar.make(binding.root, R.string.delete_all_success, Snackbar.LENGTH_LONG).show()
                viewModel.clearDeleteAllDone()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { messageRes ->
            messageRes?.let {
                Snackbar.make(binding.root, getString(it), Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.syncLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnSyncNow.isEnabled = !loading && viewModel.isProcessing.value != true
            binding.progressSync.isVisible = loading
        }

        viewModel.syncResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                val message = when (it) {
                    is SyncResultState.Success -> {
                        val changed = it.calendarCreated + it.calendarUpdated
                        if (changed == 0) {
                            getString(R.string.sync_success_no_changes)
                        } else {
                            getString(
                                R.string.sync_success_with_calendar,
                                it.calendarCreated,
                                it.calendarUpdated,
                                it.calendarSkipped
                            )
                        }
                    }
                    is SyncResultState.Error -> {
                        val details = it.message?.takeIf { text -> text.isNotBlank() }
                            ?: getString(R.string.error_unknown)
                        getString(R.string.sync_error, details)
                    }
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearSyncResult()
            }
        }

        viewModel.calendarImportLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnImportCalendar.isEnabled = !loading && viewModel.isProcessing.value != true
            binding.progressImportCalendar.isVisible = loading
        }

        viewModel.calendarImportResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                val message = if (it.totalEvents == 0) {
                    getString(R.string.calendar_import_no_events)
                } else {
                    getString(R.string.calendar_import_success, it.imported, it.skipped)
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearCalendarImportResult()
            }
        }

        viewModel.calendarImportError.observe(viewLifecycleOwner) { error ->
            error?.let {
                val message = if (it.isBlank()) {
                    getString(R.string.error_calendar_import)
                } else {
                    getString(R.string.error_calendar_import_with_reason, it)
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearCalendarImportError()
            }
        }
    }

    private fun setupAccountInfo() {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            binding.textUserName.text = account.displayName ?: account.email ?: ""
            binding.textUserEmail.text = account.email ?: ""
        } else {
            binding.textUserName.text = getString(R.string.account_demo_user)
            binding.textUserEmail.visibility = View.GONE
        }
    }

    private fun setupSyncButton() {
        binding.btnSyncNow.setOnClickListener {
            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            val syncScopes = viewModel.requiredCalendarSyncScopes()
            if (account != null && hasScopes(account, syncScopes)) {
                viewModel.syncNow()
            } else if (AppConfig.hasGoogleClientId()) {
                pendingGoogleAction = PendingGoogleAction.SYNC
                calendarScopeLauncher.launch(buildGoogleClientWithScopes(syncScopes).signInIntent)
            } else {
                viewModel.syncNow()
            }
        }
    }

    private fun setupImportCalendarButton() {
        binding.btnImportCalendar.setOnClickListener {
            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            val importScopes = viewModel.requiredCalendarImportScopes()
            if (account != null && hasScopes(account, importScopes)) {
                viewModel.importCalendar()
            } else if (AppConfig.hasGoogleClientId()) {
                pendingGoogleAction = PendingGoogleAction.IMPORT
                calendarScopeLauncher.launch(buildGoogleClientWithScopes(importScopes).signInIntent)
            } else {
                viewModel.importCalendar()
            }
        }
    }

    private fun hasScopes(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
        scopes: List<String>
    ): Boolean {
        if (scopes.isEmpty()) return true
        val scopeObjects = scopes.map(::Scope).toTypedArray()
        return GoogleSignIn.hasPermissions(account, *scopeObjects)
    }

    private fun buildGoogleClientWithScopes(scopes: List<String>): com.google.android.gms.auth.api.signin.GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(AppConfig.googleWebClientId)
            .requestEmail()

        if (scopes.isNotEmpty()) {
            val first = Scope(scopes.first())
            val rest = scopes.drop(1).map(::Scope).toTypedArray()
            builder.requestScopes(first, *rest)
        }
        return GoogleSignIn.getClient(requireContext(), builder.build())
    }

    private fun showDeleteAllConfirmationDialog() {
        showConfirmationDialog(
            titleRes = R.string.dialog_delete_all_title,
            messageRes = R.string.dialog_delete_all_message,
            onConfirm = {
                viewModel.deleteAllTasks()
            }
        )
    }

    private fun applyTheme(isDark: Boolean) {
        val mode = if (isDark) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            showConfirmationDialog(
                titleRes = R.string.dialog_logout_title,
                messageRes = R.string.dialog_logout_message,
                onConfirm = {
                    // Revoke Google Sign-In state so account picker shows next time
                    if (AppConfig.hasGoogleClientId()) {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(AppConfig.googleWebClientId)
                            .requestEmail()
                            .build()
                        GoogleSignIn.getClient(requireContext(), gso).signOut()
                    }
                    authViewModel.signOut()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

