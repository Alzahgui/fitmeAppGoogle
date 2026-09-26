package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.PulseAmber
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onAskCoachForAudit: (UserProfile) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var goal by remember(userProfile.goal) { mutableStateOf(userProfile.goal) }
    var level by remember(userProfile.experienceLevel) { mutableStateOf(userProfile.experienceLevel) }
    var weightText by remember(userProfile.weightKg) { mutableStateOf(userProfile.weightKg.toString()) }
    var targetWeightText by remember(userProfile.targetWeightKg) { mutableStateOf(userProfile.targetWeightKg.toString()) }
    var useMetric by remember(userProfile.useMetric) { mutableStateOf(userProfile.useMetric) }
    var workoutsPerWeek by remember(userProfile.workoutsPerWeekGoal) { mutableStateOf(userProfile.workoutsPerWeekGoal) }
    var location by remember(userProfile.workoutLocation) { mutableStateOf(userProfile.workoutLocation) }
    var showSavedSnackbar by remember { mutableStateOf(false) }

    val goals = listOf("Build Muscle & Strength", "Fat Loss & Shred", "Athletic Performance", "General Health")
    val levels = listOf("Beginner", "Intermediate", "Advanced")
    val locations = listOf("Gym", "Home Gym", "Bodyweight Only")

    Scaffold(
        modifier = modifier.testTag("profile_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Athlete Profile & Goals",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val updated = userProfile.copy(
                                name = name.ifBlank { "Athlete" },
                                goal = goal,
                                experienceLevel = level,
                                weightKg = weightText.toFloatOrNull() ?: userProfile.weightKg,
                                targetWeightKg = targetWeightText.toFloatOrNull() ?: userProfile.targetWeightKg,
                                useMetric = useMetric,
                                workoutsPerWeekGoal = workoutsPerWeek,
                                workoutLocation = location
                            )
                            onSaveProfile(updated)
                            showSavedSnackbar = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen, contentColor = Color(0xFF00381B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_profile_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(PulseGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = Color(0xFF00381B),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$level • $goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // AI Strategy Assessment Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, PulseCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Coach Pulse Strategy Plan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PulseCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = userProfile.aiStrategySummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Personal Information
            Text("Athlete Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Athlete Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Weight & Target Weight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (${if (useMetric) "kg" else "lbs"})") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetWeightText,
                    onValueChange = { targetWeightText = it },
                    label = { Text("Target (${if (useMetric) "kg" else "lbs"})") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Unit toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Unit of Measurement", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(if (useMetric) "Metric (Kilograms)" else "Imperial (Pounds)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = useMetric,
                    onCheckedChange = { useMetric = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = PulseGreen, checkedTrackColor = PulseGreen.copy(alpha = 0.3f))
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Primary Goal Selection
            Text("Primary Goal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                goals.forEach { g ->
                    FilterChip(
                        selected = goal == g,
                        onClick = { goal = g },
                        label = { Text(g, fontWeight = if (goal == g) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PulseGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PulseGreen
                        )
                    )
                }
            }

            // Experience Level
            Text("Experience Level", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                levels.forEach { lvl ->
                    FilterChip(
                        selected = level == lvl,
                        onClick = { level = lvl },
                        label = { Text(lvl, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Training Location
            Text("Training Environment", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                locations.forEach { loc ->
                    FilterChip(
                        selected = location == loc,
                        onClick = { location = loc },
                        label = { Text(loc, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Workouts per week
            Text("Weekly Workout Frequency: $workoutsPerWeek days/week", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Slider(
                value = workoutsPerWeek.toFloat(),
                onValueChange = { workoutsPerWeek = it.toInt() },
                valueRange = 2f..6f,
                steps = 3,
                colors = SliderDefaults.colors(thumbColor = PulseGreen, activeTrackColor = PulseGreen)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
