package com.thefadghost.neverforget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.thefadghost.neverforget.feature.onboarding.OnboardingScreen
import com.thefadghost.neverforget.navigation.MainDestination
import com.thefadghost.neverforget.navigation.MainShell
import com.thefadghost.neverforget.ui.QuickAddDialog
import com.thefadghost.neverforget.ui.destinationContent

@Composable
fun NeverForgetRoot(
    viewModel: NeverForgetViewModel,
    onboardingCompleted: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
) {
    if (!onboardingCompleted) {
        OnboardingScreen(
            onComplete = viewModel::completeOnboarding,
            onRequestNotifications = onRequestNotifications,
            onRequestExactAlarms = onRequestExactAlarms,
        )
        return
    }

    var destination by remember { mutableStateOf(MainDestination.HOME) }
    var quickAddAction by remember { mutableStateOf<String?>(null) }
    MainShell(
        selected = destination,
        onDestinationSelected = { destination = it },
        onQuickAdd = { quickAddAction = it },
        content = { selected, padding ->
            destinationContent(
                destination = selected,
                padding = padding,
                viewModel = viewModel,
                onRequestNotifications = onRequestNotifications,
                onRequestExactAlarms = onRequestExactAlarms,
            )
        },
    )
    quickAddAction?.let { action ->
        QuickAddDialog(
            action = action,
            viewModel = viewModel,
            onDismiss = { quickAddAction = null },
        )
    }
}
