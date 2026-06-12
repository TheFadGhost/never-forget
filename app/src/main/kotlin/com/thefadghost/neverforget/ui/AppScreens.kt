package com.thefadghost.neverforget.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.thefadghost.neverforget.NeverForgetViewModel
import com.thefadghost.neverforget.data.country.CountryOccurrence
import com.thefadghost.neverforget.database.EventEntity
import com.thefadghost.neverforget.database.PersonEntity
import com.thefadghost.neverforget.model.EventType
import com.thefadghost.neverforget.model.Relationship
import com.thefadghost.neverforget.model.ThemePreference
import com.thefadghost.neverforget.navigation.MainDestination
import com.thefadghost.neverforget.settings.AppSettings
import java.time.LocalDate
import java.time.MonthDay
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val shortDate = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.UK)

data class UpcomingItem(
    val stableId: String,
    val title: String,
    val subtitle: String,
    val date: LocalDate,
    val eventId: Long? = null,
)

@Composable
fun destinationContent(
    destination: MainDestination,
    padding: PaddingValues,
    viewModel: NeverForgetViewModel,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
) {
    when (destination) {
        MainDestination.HOME -> HomeScreen(padding, viewModel)
        MainDestination.CALENDAR -> CalendarScreen(padding, viewModel)
        MainDestination.PEOPLE -> PeopleScreen(padding, viewModel)
        MainDestination.OCCASIONS -> OccasionsScreen(padding, viewModel)
        MainDestination.SETTINGS -> SettingsScreen(
            padding,
            viewModel,
            onRequestNotifications,
            onRequestExactAlarms,
        )
    }
}

@Composable
private fun HomeScreen(padding: PaddingValues, viewModel: NeverForgetViewModel) {
    val events by viewModel.events.collectAsState()
    val today = LocalDate.now()
    val upcoming = remember(events, today) {
        buildUpcoming(events, emptyList(), today)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Stay ahead", style = MaterialTheme.typography.displaySmall)
            Text(
                "The next important dates, ordered by urgency.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            ReminderHealthBanner()
            Spacer(Modifier.height(8.dp))
        }
        if (upcoming.isEmpty()) {
            item {
                EmptyPanel(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "Your runway is clear",
                    body = "Use the add button to create a birthday, event, reminder, person, or task.",
                )
            }
        } else {
            items(upcoming.take(30), key = { it.stableId }) { item ->
                UpcomingRow(item)
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun ReminderHealthBanner() {
    val context = LocalContext.current
    val notifications = notificationPermissionGranted(context)
    val exact = exactAlarmsGranted(context)
    val healthy = notifications && exact
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (healthy) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (healthy) Icons.Outlined.Shield else Icons.Outlined.Alarm,
                contentDescription = null,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (healthy) "Reminder system ready" else "Reminder access needs attention",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    if (healthy) {
                        "Notifications and precise timing are available."
                    } else {
                        "Open Settings to enable missing Android permissions."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UpcomingRow(item: UpcomingItem) {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), item.date)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    days == 0L -> "NOW"
                    days < 10 -> days.toString()
                    else -> item.date.dayOfMonth.toString()
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.date.format(shortDate)} · ${item.subtitle}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            when {
                days == 0L -> "Today"
                days == 1L -> "Tomorrow"
                else -> "${days}d"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
}

@Composable
private fun CalendarScreen(padding: PaddingValues, viewModel: NeverForgetViewModel) {
    val events by viewModel.events.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    val eventDates = remember(events, month) { events.associateWith { dateInYear(it, month.year) } }
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = remember(month) {
        List(42) { index ->
            val day = index - offset + 1
            if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    month.month.getDisplayName(TextStyle.FULL, Locale.UK),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(month.year.toString(), color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Outlined.ArrowBack, "Previous month")
            }
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.Outlined.ArrowForward, "Next month")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.weight(1f),
            userScrollEnabled = false,
        ) {
            items(cells) { date ->
                val count = if (date == null) 0 else {
                    eventDates.count { it.value == date }
                }
                DayCell(date, count)
            }
        }
        val monthItems = buildUpcoming(events, emptyList(), month.atDay(1))
            .filter { YearMonth.from(it.date) == month }
        Text("This month", style = MaterialTheme.typography.titleLarge)
        LazyColumn(
            modifier = Modifier.weight(1.1f),
            contentPadding = PaddingValues(bottom = 90.dp),
        ) {
            if (monthItems.isEmpty()) {
                item { Text("No visible occasions this month.", modifier = Modifier.padding(vertical = 18.dp)) }
            } else {
                items(monthItems, key = { it.stableId }) { UpcomingRow(it) }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate?, count: Int) {
    val today = date == LocalDate.now()
    Box(
        modifier = Modifier
            .aspectRatio(0.92f)
            .padding(2.dp)
            .background(
                if (today) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                RoundedCornerShape(12.dp),
            ),
    ) {
        if (date != null) {
            Text(
                date.dayOfMonth.toString(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            if (count > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(7.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    repeat(count.coerceAtMost(3)) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeopleScreen(padding: PaddingValues, viewModel: NeverForgetViewModel) {
    val people by viewModel.people.collectAsState()
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val filtered = remember(people, query) {
        people.filter { it.displayName.contains(query, ignoreCase = true) }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        item {
            Text("Your people", style = MaterialTheme.typography.displaySmall)
            Text(
                "Birthdays and relationships in one quiet place.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search people") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add person")
            }
            Spacer(Modifier.height(12.dp))
        }
        if (filtered.isEmpty()) {
            item {
                EmptyPanel(
                    Icons.Outlined.People,
                    "No people yet",
                    "Add someone manually or use contact import from Settings.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { person -> PersonRow(person) }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }
    if (showAdd) {
        PersonDialog(viewModel = viewModel, onDismiss = { showAdd = false })
    }
}

@Composable
private fun PersonRow(person: PersonEntity) {
    ListItem(
        headlineContent = { Text(person.displayName, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Text(
                buildString {
                    append(person.relationship.lowercase().replaceFirstChar { it.uppercase() })
                    val month = person.birthdayMonth
                    val day = person.birthdayDay
                    if (month != null && day != null) {
                        append(" · ")
                        append(MonthDay.of(month, day))
                    }
                },
            )
        },
        leadingContent = {
            Box(
                Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(person.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold)
            }
        },
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
}

@Composable
private fun OccasionsScreen(padding: PaddingValues, viewModel: NeverForgetViewModel) {
    val settings by viewModel.settings.collectAsState()
    val followed by viewModel.followedNameDays.collectAsState()
    var section by remember { mutableStateOf("Occasions") }
    var query by remember { mutableStateOf("") }
    val currentYear = LocalDate.now().year
    val occasions = remember(settings, currentYear) { viewModel.countryOccurrences(currentYear, settings) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        item {
            Text("Occasions", style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = section == "Occasions",
                    onClick = { section = "Occasions" },
                    label = { Text("Country dates") },
                )
                FilterChip(
                    selected = section == "Names",
                    onClick = { section = "Names" },
                    label = { Text("Name Days") },
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        if (section == "Occasions") {
            items(occasions, key = { "${it.eventId}-${it.date}" }) { occasion ->
                OccasionRow(occasion)
            }
        } else {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Find a Bulgarian name") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                )
                Text(
                    "Notifications stay off until you follow a name. Linking a person is recommended, not required.",
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val names = viewModel.nameDays.filter {
                query.isBlank() ||
                    it.canonicalName.contains(query, true) ||
                    it.localName.contains(query, true) ||
                    it.aliases.any { alias -> alias.contains(query, true) }
            }
            items(names, key = { "${it.canonicalName}-${it.month}-${it.day}" }) { nameDay ->
                val isFollowed = followed.any { it.canonicalName == nameDay.canonicalName }
                ListItem(
                    headlineContent = { Text("${nameDay.canonicalName} · ${nameDay.localName}") },
                    supportingContent = {
                        Text("${nameDay.date} · ${nameDay.aliases.take(4).joinToString()}")
                    },
                    trailingContent = {
                        Checkbox(
                            checked = isFollowed,
                            onCheckedChange = { viewModel.followNameDay(nameDay, it) },
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
private fun OccasionRow(occasion: CountryOccurrence) {
    ListItem(
        headlineContent = { Text(occasion.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Text(
                buildString {
                    append(occasion.date.format(shortDate))
                    occasion.localName?.let { append(" · $it") }
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(
                if (occasion.classification == "PUBLIC_HOLIDAY") {
                    Icons.Outlined.CalendarMonth
                } else {
                    Icons.Outlined.Celebration
                },
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = { Text("${occasion.leadDays}d", color = MaterialTheme.colorScheme.primary) },
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
}

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    viewModel: NeverForgetViewModel,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var passwordMode by remember { mutableStateOf<String?>(null) }
    var pendingBackup by remember { mutableStateOf<ByteArray?>(null) }
    var pendingBackupExport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    var pendingIcs by remember { mutableStateOf<String?>(null) }
    val backupCreate = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val bytes = pendingBackupExport
        if (uri != null && bytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
                .onSuccess { message = "Encrypted backup saved" }
                .onFailure { message = it.message ?: "Backup export failed" }
        }
        pendingBackupExport = null
    }
    val backupOpen = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                .onSuccess {
                    pendingBackup = it
                    passwordMode = "restore"
                }
                .onFailure { message = it.message ?: "Could not read backup" }
        }
    }
    val csvCreate = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val csv = pendingCsv
        if (uri != null && csv != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.writer()?.use { it.write(csv) }
            }.onSuccess { message = "People CSV saved" }
                .onFailure { message = it.message ?: "CSV export failed" }
        }
        pendingCsv = null
    }
    val csvOpen = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                .onSuccess { csv ->
                    viewModel.importPeopleCsv(csv.orEmpty()) { result ->
                        message = result.fold(
                            onSuccess = {
                                "CSV imported: ${it.validRows.size} valid, ${it.errors.size} errors"
                            },
                            onFailure = { it.message ?: "CSV import failed" },
                        )
                    }
                }
                .onFailure { message = it.message ?: "Could not read CSV" }
        }
    }
    val icsCreate = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar"),
    ) { uri ->
        val ics = pendingIcs
        if (uri != null && ics != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.writer()?.use { it.write(ics) }
            }.onSuccess { message = "Calendar export saved" }
                .onFailure { message = it.message ?: "Calendar export failed" }
        }
        pendingIcs = null
    }
    val icsOpen = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                .onSuccess { ics ->
                    viewModel.importCalendarIcs(ics.orEmpty()) { result ->
                        message = result.fold(
                            onSuccess = {
                                "Calendar imported: ${it.imported}; duplicates skipped: " +
                                    "${it.skippedDuplicates}; errors: ${it.errors.size}"
                            },
                            onFailure = { it.message ?: "Calendar import failed" },
                        )
                    }
                }
                .onFailure { message = it.message ?: "Could not read calendar" }
        }
    }
    val contactPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.importContactBirthdays { result ->
                message = result.fold(
                    onSuccess = {
                        "Contacts imported: ${it.imported}; duplicates skipped: ${it.skippedDuplicates}"
                    },
                    onFailure = { it.message ?: "Contact import failed" },
                )
            }
        } else {
            message = "Contacts permission was not granted"
        }
    }
    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.importDeviceCalendar { result ->
                message = result.fold(
                    onSuccess = {
                        "Calendar imported: ${it.imported}; duplicates skipped: ${it.skippedDuplicates}"
                    },
                    onFailure = { it.message ?: "Calendar import failed" },
                )
            }
        } else {
            message = "Calendar permission was not granted"
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.displaySmall)
            Text(
                "Control the noise without weakening the safety net.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            SettingsGroup("Reminder health") {
                HealthAction(
                    "Notifications",
                    notificationPermissionGranted(context),
                    onRequestNotifications,
                )
                HealthAction("Precise timing", exactAlarmsGranted(context), onRequestExactAlarms)
                Text(
                    "ColorOS: allow notifications, Auto launch, background activity, and set battery use to Unrestricted.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SettingsGroup("Countries") {
                ToggleRow("United Kingdom", settings.ukEnabled) {
                    viewModel.setCountry("GB", it)
                }
                ToggleRow("England & Wales", settings.englandWalesEnabled) {
                    viewModel.setUkRegion("GB-EAW", it)
                }
                ToggleRow("Scotland", settings.scotlandEnabled) {
                    viewModel.setUkRegion("GB-SCT", it)
                }
                ToggleRow("Northern Ireland", settings.northernIrelandEnabled) {
                    viewModel.setUkRegion("GB-NIR", it)
                }
                ToggleRow("Bulgaria", settings.bulgariaEnabled) {
                    viewModel.setCountry("BG", it)
                }
                Text(
                    "Primary calendar",
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (settings.ukEnabled) {
                        FilterChip(
                            selected = settings.primaryCountry == "GB",
                            onClick = { viewModel.setPrimaryCountry("GB") },
                            label = { Text("United Kingdom") },
                        )
                    }
                    if (settings.bulgariaEnabled) {
                        FilterChip(
                            selected = settings.primaryCountry == "BG",
                            onClick = { viewModel.setPrimaryCountry("BG") },
                            label = { Text("Bulgaria") },
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup("Themes") {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreference.entries.forEach { theme ->
                        FilterChip(
                            selected = settings.theme == theme,
                            onClick = { viewModel.setTheme(theme) },
                            label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (settings.theme == theme) {
                                { Icon(Icons.Outlined.Check, null) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup("Data") {
                ListItem(
                    headlineContent = { Text("Encrypted backup") },
                    supportingContent = { Text("Password-protected, authenticated file") },
                    leadingContent = { Icon(Icons.Outlined.Shield, null) },
                    trailingContent = {
                        TextButton(onClick = { passwordMode = "export" }) { Text("Export") }
                    },
                )
                ListItem(
                    headlineContent = { Text("Restore encrypted backup") },
                    supportingContent = { Text("Replaces local people, events, and progress") },
                    trailingContent = {
                        TextButton(onClick = { backupOpen.launch("application/octet-stream") }) {
                            Text("Restore")
                        }
                    },
                )
                ListItem(
                    headlineContent = { Text("People CSV") },
                    supportingContent = { Text("Versioned import/export with duplicate detection") },
                    leadingContent = { Icon(Icons.Outlined.People, null) },
                    trailingContent = {
                        Row {
                            TextButton(
                                onClick = {
                                    viewModel.exportPeopleCsv { result ->
                                        result.onSuccess {
                                            pendingCsv = it
                                            csvCreate.launch("never-forget-people.csv")
                                        }.onFailure {
                                            message = it.message ?: "CSV export failed"
                                        }
                                    }
                                },
                            ) { Text("Export") }
                            TextButton(onClick = { csvOpen.launch("text/*") }) { Text("Import") }
                        }
                    },
                )
                ListItem(
                    headlineContent = { Text("Calendar file") },
                    supportingContent = { Text("Import or export standard .ics events") },
                    leadingContent = { Icon(Icons.Outlined.Event, null) },
                    trailingContent = {
                        Row {
                            TextButton(
                                onClick = {
                                    viewModel.exportCalendarIcs { result ->
                                        result.onSuccess {
                                            pendingIcs = it
                                            icsCreate.launch("never-forget-calendar.ics")
                                        }.onFailure {
                                            message = it.message ?: "Calendar export failed"
                                        }
                                    }
                                },
                            ) { Text("Export") }
                            TextButton(onClick = { icsOpen.launch("text/calendar") }) {
                                Text("Import")
                            }
                        }
                    },
                )
                ListItem(
                    headlineContent = { Text("Contact birthdays") },
                    supportingContent = { Text("One-time import with duplicate detection") },
                    leadingContent = { Icon(Icons.Outlined.CalendarMonth, null) },
                    trailingContent = {
                        TextButton(onClick = { contactPermission.launch(Manifest.permission.READ_CONTACTS) }) {
                            Text("Import")
                        }
                    },
                )
                ListItem(
                    headlineContent = { Text("Device calendar") },
                    supportingContent = { Text("One-time copy; no ongoing Google Calendar dependency") },
                    leadingContent = { Icon(Icons.Outlined.Event, null) },
                    trailingContent = {
                        TextButton(onClick = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) }) {
                            Text("Import")
                        }
                    },
                )
            }
        }
        message?.let { status ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(status, modifier = Modifier.padding(14.dp))
                }
            }
        }
        item {
            Text("Never Forget 1.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Offline-first · no account · no analytics",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(80.dp))
        }
    }
    passwordMode?.let { mode ->
        PasswordDialog(
            title = if (mode == "export") "Protect backup" else "Unlock backup",
            confirmLabel = if (mode == "export") "Export" else "Restore",
            onDismiss = {
                passwordMode = null
                pendingBackup = null
            },
            onConfirm = { password ->
                if (mode == "export") {
                    viewModel.createBackup(password) { result ->
                        result.onSuccess {
                            pendingBackupExport = it
                            backupCreate.launch("never-forget-1.1.0.nfg")
                        }.onFailure {
                            message = it.message ?: "Backup export failed"
                        }
                    }
                } else {
                    val bytes = pendingBackup
                    if (bytes != null) {
                        viewModel.restoreBackup(bytes, password) { result ->
                            message = result.fold(
                                onSuccess = { "Backup restored" },
                                onFailure = { it.message ?: "Backup restore failed" },
                            )
                        }
                    }
                }
                passwordMode = null
                pendingBackup = null
            },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(content = { content() })
        }
    }
}

@Composable
private fun HealthAction(label: String, granted: Boolean, action: () -> Unit) {
    ListItem(
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(label) },
        supportingContent = { Text(if (granted) "Ready" else "Action required") },
        trailingContent = {
            if (!granted) {
                TextButton(onClick = action) { Text("Fix") }
            } else {
                Icon(Icons.Outlined.Check, "Ready", tint = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            modifier = Modifier.semantics {
                contentDescription = "$label enabled"
            },
        )
    }
}

@Composable
fun QuickAddDialog(
    action: String,
    viewModel: NeverForgetViewModel,
    onDismiss: () -> Unit,
) {
    when (action) {
        "Birthday" -> BirthdayDialog(viewModel, onDismiss)
        "Person" -> PersonDialog(viewModel, onDismiss)
        else -> EventDialog(action, viewModel, onDismiss)
    }
}

@Composable
private fun BirthdayDialog(viewModel: NeverForgetViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf(Relationship.FRIEND) }
    val validDate = runCatching { MonthDay.of(month.toInt(), day.toInt()) }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add birthday") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(month, { month = it }, "Month", Modifier.weight(1f))
                    NumberField(day, { day = it }, "Day", Modifier.weight(1f))
                    NumberField(year, { year = it }, "Year optional", Modifier.weight(1.4f))
                }
                Text("Relationship", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Relationship.PARTNER, Relationship.MOTHER, Relationship.FATHER, Relationship.FRIEND).forEach {
                        FilterChip(
                            selected = relationship == it,
                            onClick = { relationship = it },
                            label = { Text(it.name.lowercase().replaceFirstChar { char -> char.uppercase() }) },
                        )
                    }
                }
                Text(
                    "Default reminder: 14 days before",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && validDate != null,
                onClick = {
                    viewModel.addBirthday(name, relationship, requireNotNull(validDate), year.toIntOrNull())
                    onDismiss()
                },
            ) { Text("Save birthday") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PersonDialog(viewModel: NeverForgetViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf(Relationship.FRIEND) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add person") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                Text("Relationship", style = MaterialTheme.typography.labelLarge)
                Relationship.entries.take(9).forEach {
                    AssistChip(
                        onClick = { relationship = it },
                        label = { Text(it.name.lowercase().replaceFirstChar { char -> char.uppercase() }) },
                        leadingIcon = if (relationship == it) {
                            { Icon(Icons.Outlined.Check, null) }
                        } else {
                            null
                        },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    viewModel.addPerson(name, relationship, null, null)
                    onDismiss()
                },
            ) { Text("Save person") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EventDialog(action: String, viewModel: NeverForgetViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
    val type = when (action) {
        "Task" -> EventType.TASK
        "Reminder" -> EventType.REMINDER
        else -> EventType.CUSTOM
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                when (type) {
                    EventType.TASK -> Icons.Outlined.TaskAlt
                    EventType.REMINDER -> Icons.Outlined.NotificationsActive
                    else -> Icons.Outlined.Event
                },
                null,
            )
        },
        title = { Text("Add ${action.lowercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(
                    dateText,
                    { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    isError = date == null,
                )
                Text(
                    "You can adjust reminder intensity and notes after saving.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && date != null,
                onClick = {
                    viewModel.addEvent(title, type, requireNotNull(date))
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun PasswordDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Text(
                    "This password is never stored. Losing it makes the backup unrecoverable.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(enabled = password.length >= 8, onClick = { onConfirm(password) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EmptyPanel(icon: ImageVector, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun buildUpcoming(
    events: List<EventEntity>,
    country: List<CountryOccurrence>,
    from: LocalDate,
): List<UpcomingItem> {
    val eventItems = events.map { event ->
        val date = nextDate(event, from)
        UpcomingItem(
            stableId = "event-${event.id}-$date",
            title = event.title,
            subtitle = event.type.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
            date = date,
            eventId = event.id,
        )
    }
    val countryItems = country
        .filter { it.date >= from.minusDays(1) }
        .map {
            UpcomingItem(
                stableId = "country-${it.eventId}-${it.date}",
                title = it.name,
                subtitle = it.localName ?: it.classification.lowercase().replace('_', ' '),
                date = it.date,
            )
        }
    return (eventItems + countryItems)
        .filter { it.date >= from }
        .sortedWith(compareBy<UpcomingItem> { it.date }.thenBy { it.title })
}

private fun nextDate(event: EventEntity, from: LocalDate): LocalDate {
    val source = LocalDate.ofEpochDay(event.startEpochDay)
    if (event.recurrenceType != "YEARLY") return source
    val thisYear = dateWithLeapFallback(source.monthValue, source.dayOfMonth, from.year)
    return if (thisYear >= from) thisYear else dateWithLeapFallback(source.monthValue, source.dayOfMonth, from.year + 1)
}

private fun dateInYear(event: EventEntity, year: Int): LocalDate {
    val source = LocalDate.ofEpochDay(event.startEpochDay)
    return if (event.recurrenceType == "YEARLY") {
        dateWithLeapFallback(source.monthValue, source.dayOfMonth, year)
    } else {
        source
    }
}

private fun dateWithLeapFallback(month: Int, day: Int, year: Int): LocalDate =
    runCatching { LocalDate.of(year, month, day) }.getOrElse { LocalDate.of(year, 2, 28) }

private fun notificationPermissionGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun exactAlarmsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
