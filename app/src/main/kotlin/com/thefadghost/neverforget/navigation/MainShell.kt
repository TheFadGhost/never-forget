package com.thefadghost.neverforget.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class MainDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    CALENDAR("Calendar", Icons.Outlined.CalendarMonth),
    PEOPLE("People", Icons.Outlined.People),
    OCCASIONS("Occasions", Icons.Outlined.Celebration),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

data class QuickAddAction(val label: String, val icon: ImageVector)

private val QuickActions = listOf(
    QuickAddAction("Birthday", Icons.Outlined.Celebration),
    QuickAddAction("Event", Icons.Outlined.Event),
    QuickAddAction("Reminder", Icons.Outlined.NotificationsActive),
    QuickAddAction("Person", Icons.Outlined.PersonAdd),
    QuickAddAction("Task", Icons.Outlined.TaskAlt),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    selected: MainDestination = MainDestination.HOME,
    onDestinationSelected: (MainDestination) -> Unit = {},
    onQuickAdd: (String) -> Unit = {},
    content: @Composable (MainDestination, PaddingValues) -> Unit = { destination, padding ->
        PlaceholderScreen(destination, padding)
    },
) {
    var expanded by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Never Forget", style = MaterialTheme.typography.titleLarge)
                        Text(
                            selected.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == selected,
                        onClick = { onDestinationSelected(destination) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add important date",
                )
            }
        },
    ) { padding ->
        content(selected, padding)
    }
    if (expanded) {
        ModalBottomSheet(onDismissRequest = { expanded = false }) {
            Text(
                "Add something important",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                style = MaterialTheme.typography.headlineMedium,
            )
            QuickActions.forEach { action ->
                ListItem(
                    modifier = Modifier.clickable {
                        expanded = false
                        onQuickAdd(action.label)
                    },
                    headlineContent = { Text(action.label) },
                    leadingContent = {
                        Icon(
                            action.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun PlaceholderScreen(destination: MainDestination, padding: PaddingValues) {
    val rows = when (destination) {
        MainDestination.HOME -> listOf(
            "Nothing urgent today",
            "Your next important dates will appear here",
            "Reminder health: ready",
        )
        MainDestination.CALENDAR -> listOf("Month", "Agenda", "Search and filters")
        MainDestination.PEOPLE -> listOf("Add family and friends", "Import birthdays from contacts")
        MainDestination.OCCASIONS -> listOf("United Kingdom", "Bulgaria", "Bulgarian Name Days")
        MainDestination.SETTINGS -> listOf("Reminder defaults", "Themes", "Import and backup")
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        item {
            Text(destination.label, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.size(24.dp))
        }
        items(rows) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(14.dp))
                Text(row, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
        }
    }
}
