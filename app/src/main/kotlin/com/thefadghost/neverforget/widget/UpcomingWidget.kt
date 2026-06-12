package com.thefadghost.neverforget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.thefadghost.neverforget.MainActivity
import com.thefadghost.neverforget.R
import com.thefadghost.neverforget.database.NeverForgetDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class UpcomingWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val next = NeverForgetDatabase.get(context).eventsDao().snapshot()
            .map { event ->
                val source = LocalDate.ofEpochDay(event.startEpochDay)
                val date = if (event.recurrenceType == "YEARLY") {
                    val thisYear = safeDate(LocalDate.now().year, source.monthValue, source.dayOfMonth)
                    if (thisYear >= LocalDate.now()) thisYear else safeDate(
                        LocalDate.now().year + 1,
                        source.monthValue,
                        source.dayOfMonth,
                    )
                } else {
                    source
                }
                event.title to date
            }
            .filter { it.second >= LocalDate.now() }
            .minByOrNull { it.second }

        provideContent {
            WidgetContent(next)
        }
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate =
        runCatching { LocalDate.of(year, month, day) }.getOrElse { LocalDate.of(year, 2, 28) }
}

@Composable
private fun WidgetContent(next: Pair<String, LocalDate>?) {
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.UK)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFFFF8F5)))
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                "NEVER FORGET",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFB34E3B)),
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(android.R.drawable.ic_input_add),
                contentDescription = "Open quick add",
                modifier = GlanceModifier.size(24.dp),
            )
        }
        Spacer(GlanceModifier.height(14.dp))
        if (next == null) {
            Text("No personal dates yet", style = TextStyle(fontWeight = FontWeight.Bold))
            Text("Tap to add a birthday or reminder")
        } else {
            Text(next.first, style = TextStyle(fontWeight = FontWeight.Bold))
            Text(next.second.format(formatter))
        }
    }
}

class UpcomingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingWidget()
}
