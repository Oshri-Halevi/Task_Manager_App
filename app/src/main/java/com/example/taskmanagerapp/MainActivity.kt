package com.example.taskmanagerapp

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.taskmanagerapp.auth.AuthState
import com.example.taskmanagerapp.auth.AuthViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.NavOptions
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        (application as TaskManagerApp).authViewModelFactory
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", null)
        val fontSize = prefs.getString("font_size", "normal") ?: "normal"

        val context = applySettings(newBase, language, fontSize)
        super.attachBaseContext(context)
    }

    private fun applySettings(context: Context, language: String?, fontSize: String): Context {
        var updatedContext = context

        if (language != null) {
            val locale = Locale(language)
            val config = Configuration(updatedContext.resources.configuration)
            config.setLocale(locale)
            updatedContext = updatedContext.createConfigurationContext(config)
        }

        val scale = when (fontSize) {
            "small" -> 0.85f
            "large" -> 1.15f
            else -> 1.0f
        }
        val config = Configuration(updatedContext.resources.configuration)
        config.fontScale = scale
        return updatedContext.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        setupActionBarWithNavController(navController)
        
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.taskListFragment, R.id.settingsFragment, R.id.todayTasksFragment -> {
                    bottomNav.visibility = View.VISIBLE
                }
                else -> {
                    bottomNav.visibility = View.GONE
                }
            }
        }

        authViewModel.state.observe(this) { state ->
            when (state) {
                is AuthState.Authenticated -> {
                    if (navController.currentDestination?.id == R.id.loginFragment) {
                        navController.navigate(
                            R.id.taskListFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true)
                                .build()
                        )
                    }
                }
                is AuthState.Unauthenticated -> {
                    if (navController.currentDestination?.id != R.id.loginFragment) {
                        navController.navigate(
                            R.id.loginFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build()
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
