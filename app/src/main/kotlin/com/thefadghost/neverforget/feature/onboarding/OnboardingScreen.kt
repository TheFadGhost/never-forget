package com.thefadghost.neverforget.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

data class OnboardingSelection(
    val ukEnabled: Boolean,
    val bulgariaEnabled: Boolean,
    val primaryCountry: String,
)

@Composable
fun OnboardingScreen(
    onComplete: (OnboardingSelection) -> Unit,
    onRequestNotifications: () -> Unit = {},
    onRequestExactAlarms: () -> Unit = {},
) {
    var step by remember { mutableIntStateOf(0) }
    var uk by remember { mutableStateOf(true) }
    var bulgaria by remember { mutableStateOf(true) }
    var primaryCountry by remember { mutableStateOf("GB") }
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Outlined.AutoAwesome,
            eyebrow = "WELCOME",
            title = "Never forget what matters",
            body = "A private calendar that starts early, follows up daily, and stays visible until you act.",
        ),
        OnboardingPage(
            icon = Icons.Outlined.Flag,
            eyebrow = "COUNTRIES",
            title = "Your important calendars",
            body = "UK regions and Bulgaria come with verified holidays, occasions, and source notes.",
        ),
        OnboardingPage(
            icon = Icons.Outlined.NotificationsActive,
            eyebrow = "REMINDERS",
            title = "Persistent by design",
            body = "Daily from 9:00 AM, twice daily for the final three days, then every two hours on the day.",
        ),
        OnboardingPage(
            icon = Icons.Outlined.Alarm,
            eyebrow = "SYSTEM ACCESS",
            title = "Keep reminders reliable",
            body = "Android controls notification and exact-alarm access. Never Forget shows the health of both.",
        ),
    )

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
        ) {
            Text(
                text = "${step + 1} / ${pages.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(28.dp))
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(spring(stiffness = 240f)) togetherWith fadeOut(spring(stiffness = 320f))
                },
                modifier = Modifier.weight(1f),
                label = "onboarding",
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = page.eyebrow,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(page.title, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (index == 1) {
                        Spacer(Modifier.height(28.dp))
                        CountryChoice("United Kingdom", "England & Wales, Scotland, Northern Ireland", uk) {
                            if (it || bulgaria) {
                                uk = it
                                if (!it) primaryCountry = "BG"
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        CountryChoice("Bulgaria", "Public holidays and Bulgarian Name Days", bulgaria) {
                            if (it || uk) {
                                bulgaria = it
                                if (!it) primaryCountry = "GB"
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("Primary calendar", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (uk) {
                                FilterChip(
                                    selected = primaryCountry == "GB",
                                    onClick = { primaryCountry = "GB" },
                                    label = { Text("United Kingdom") },
                                )
                            }
                            if (bulgaria) {
                                FilterChip(
                                    selected = primaryCountry == "BG",
                                    onClick = { primaryCountry = "BG" },
                                    label = { Text("Bulgaria") },
                                )
                            }
                        }
                    }
                    if (index == 3) {
                        Spacer(Modifier.height(28.dp))
                        OutlinedButton(
                            onClick = onRequestNotifications,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Allow notifications")
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onRequestExactAlarms,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Allow precise reminder times")
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > 0) {
                    OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (step == pages.lastIndex) {
                            onComplete(
                                OnboardingSelection(
                                    ukEnabled = uk,
                                    bulgariaEnabled = bulgaria,
                                    primaryCountry = primaryCountry,
                                ),
                            )
                        } else {
                            step++
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (step == pages.lastIndex) "Start planning" else "Continue")
                }
            }
        }
    }
}

@Composable
private fun CountryChoice(
    title: String,
    detail: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onChecked,
                modifier = Modifier.semantics {
                    contentDescription = "$title selected"
                },
            )
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val eyebrow: String,
    val title: String,
    val body: String,
)
