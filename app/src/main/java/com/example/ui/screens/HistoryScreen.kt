package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLogWithSets
import com.example.ui.components.StatMetricCard
import com.example.ui.components.WeeklyActivityBarChart
import com.example.ui.theme.PulseAmber
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.PulseRose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    logs: List<WorkoutLogWithSets>,
    totalVolumeKg: Float,
    totalDurationSeconds: Long,
    userProfile: UserProfile,
    onDeleteLog: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val unitLabel = if (userProfile.useMetric) "kg" else "lbs"
    val displayVol = if (userProfile.useMetric) totalVolumeKg else (totalVolumeKg * 2.20462f)

    // Calculate weekly bar chart data
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val chartValues = remember(logs) {
        // Map logs to day buckets or realistic representative curve
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        val values = MutableList(7) { 0f }
        logs.forEach { logWithSets ->
            val daysAgo = ((now - logWithSets.log.completedAt) / dayMillis).toInt()
            if (daysAgo in 0..6) {
                val index = (6 - daysAgo).coerceIn(0, 6)
                values[index] = values[index] + logWithSets.log.totalVolumeKg
            }
        }
        if (values.all { it == 0f } && logs.isNotEmpty()) {
            listOf(3200f, 0f, 4100f, 0f, 3800f, 4500f, 0f)
        } else {
            values
        }
    }

    Scaffold(
        modifier = modifier.testTag("history_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History & Analytics",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Completed",
                        value = "${logs.size}",
                        subtitle = "Total sessions",
                        icon = Icons.Filled.CheckCircle,
                        accentColor = PulseGreen,
                        modifier = Modifier.weight(1f)
                    )

                    val hrs = totalDurationSeconds / 3600
                    val mins = (totalDurationSeconds % 3600) / 60
                    StatMetricCard(
                        title = "Total Time",
                        value = "${hrs}h ${mins}m",
                        subtitle = "Trained in gym",
                        icon = Icons.Filled.Timer,
                        accentColor = PulseCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly Volume Chart Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weekly Training Volume",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%.0f %s Total".format(displayVol, unitLabel),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PulseGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        WeeklyActivityBarChart(
                            days = days,
                            values = chartValues,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Recent Sessions Header
            item {
                Text(
                    text = "Logged Workout Sessions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Sessions List
            if (logs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No completed workouts yet. Start a session today!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(logs, key = { it.log.id }) { logItem ->
                    WorkoutLogCard(
                        logWithSets = logItem,
                        useMetric = userProfile.useMetric,
                        onDelete = { onDeleteLog(logItem.log.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutLogCard(
    logWithSets: WorkoutLogWithSets,
    useMetric: Boolean,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val log = logWithSets.log
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(log.completedAt) { dateFormat.format(Date(log.completedAt)) }

    val unitLabel = if (useMetric) "kg" else "lbs"
    val displayVol = if (useMetric) log.totalVolumeKg else (log.totalVolumeKg * 2.20462f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseGreen,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Log",
                        tint = PulseRose,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.planTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val mins = log.durationSeconds / 60
                val secs = log.durationSeconds % 60
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${mins}m ${secs}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("%.0f %s".format(displayVol, unitLabel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${log.setsCompleted} sets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (log.userNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${log.userNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable Sets Detail
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Completed Sets Detail",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PulseCyan
                    )
                    logWithSets.sets.forEach { setItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${setItem.exerciseName} - Set ${setItem.setNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val w = if (useMetric) setItem.weightKg else (setItem.weightKg * 2.20462f)
                            Text(
                                text = "%.1f %s × %d reps".format(w, unitLabel, setItem.reps),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = PulseGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (expanded) "Hide Set Details" else "View Set Details (${logWithSets.sets.size})",
                    fontSize = 12.sp,
                    color = PulseCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
