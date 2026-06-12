package com.thefadghost.neverforget

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefadghost.neverforget.designsystem.NeverForgetTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<NeverForgetViewModel>()
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.attributes = window.attributes.apply { preferredRefreshRate = 120f }
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            NeverForgetTheme(preference = settings.theme) {
                NeverForgetRoot(
                    viewModel = viewModel,
                    onboardingCompleted = settings.onboardingCompleted,
                    onRequestNotifications = ::requestNotifications,
                    onRequestExactAlarms = ::requestExactAlarms,
                )
            }
        }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(
                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName"),
                ),
            )
        }
    }
}
