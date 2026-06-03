package com.example.vedaahar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.ui.theme.BeigeBorder
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.LightSage
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray

private val ReminderBackground = Color(0xFFFAF6EC)
private val ReminderCard = Color(0xFFFFFCF5)
private val ReminderGold = Color(0xFFE4BF65)
private val ReminderAmber = Color(0xFFA07850)
private val ReminderBlue = Color(0xFF527B8B)

private data class HealthReminderUi(
    val type: String,
    val title: String,
    val time: String,
    val repeat: String,
    val note: String,
    val accent: Color,
    val badge: String,
    val enabled: Boolean = true
)

private val sampleHealthReminders = listOf(
    HealthReminderUi("Diet", "Breakfast", "8:00 AM", "Daily", "Warm, fresh, balanced meal", ForestGreen, "D"),
    HealthReminderUi("Yoga", "Morning Yoga", "6:30 AM", "Weekdays", "Practice guided breathwork", SageGreen, "Y"),
    HealthReminderUi("Medicine", "Night medicine", "9:00 PM", "Daily", "Take with warm water", ReminderAmber, "M"),
    HealthReminderUi("Exercise", "Evening walk", "6:00 PM", "Weekends", "Gentle movement for digestion", ReminderBlue, "E")
)

@Composable
fun HealthReminderScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxSize(), color = ReminderBackground) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(ReminderBackground, Cream, LightSage.copy(alpha = 0.35f))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BackButton(onClick = onBack, text = "Wellness")
                ReminderHero()
                TodayRoutineCard()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MY HEALTH REMINDERS",
                        color = SageGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp
                    )
                    OutlinedButton(
                        onClick = {},
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                sampleHealthReminders.groupBy { it.type }.forEach { (type, reminders) ->
                    ReminderGroup(type = type, reminders = reminders)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReminderHero() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = ForestGreen.copy(alpha = 0.08f), spotColor = ReminderGold.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = ReminderCard),
        border = BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ReminderGold.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(21.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Health Reminder",
                color = DarkForestGreen,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 32.sp
            )
            Text(
                text = "Smart alarms for diet, yoga, medicine, and exercise routines.",
                color = SoftBlueGray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun TodayRoutineCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Today's Health Routine", color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("2 of 4 completed", color = SoftBlueGray, fontSize = 12.sp)
                }
                Text(
                    text = "50%",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ReminderGold.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    color = DarkForestGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            RoutineLine("Done", "Morning Yoga", "6:30 AM", ForestGreen)
            RoutineLine("Next", "Breakfast", "8:00 AM", ReminderGold)
            RoutineLine("Later", "Night medicine", "9:00 PM", ReminderAmber)
        }
    }
}

@Composable
private fun RoutineLine(status: String, title: String, time: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(status, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
        Text(title, color = DarkForestGreen, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(time, color = SoftBlueGray, fontSize = 12.sp)
    }
}

@Composable
private fun ReminderGroup(type: String, reminders: List<HealthReminderUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(type.uppercase(), color = SageGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        reminders.forEach { reminder ->
            ReminderListCard(reminder = reminder)
        }
    }
}

@Composable
private fun ReminderListCard(reminder: HealthReminderUi) {
    var enabled by remember { mutableStateOf(reminder.enabled) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ReminderCard.copy(alpha = if (enabled) 1f else 0.62f)),
        border = BorderStroke(1.dp, reminder.accent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(reminder.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(reminder.badge, color = reminder.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, color = DarkForestGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${reminder.time} • ${reminder.repeat}", color = SoftBlueGray, fontSize = 12.sp)
                Text(reminder.note, color = SoftBlueGray.copy(alpha = 0.82f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhite,
                    checkedTrackColor = ForestGreen,
                    uncheckedThumbColor = PureWhite,
                    uncheckedTrackColor = SoftBlueGray.copy(alpha = 0.35f)
                )
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Edit ${reminder.title}",
                tint = DarkForestGreen,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(20.dp)
                    .clickable { }
            )
        }
    }
}
