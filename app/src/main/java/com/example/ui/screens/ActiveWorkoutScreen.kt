package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutPlanWithExercises
import com.example.ui.components.CircularRestTimerCountdown
import com.example.ui.theme.PulseAmber
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.PulseRose
import com.example.ui.viewmodel.ActiveSet
import com.example.ui.viewmodel.WorkoutSessionSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    activePlan: WorkoutPlanWithExercises?,
    currentExerciseIndex: Int,
    elapsedSeconds: Long,
    isPaused: Boolean,
    exerciseSets: Map<Int, List<ActiveSet>>,
    isRestTimerActive: Boolean,
    restSecondsRemaining: Int,
    restDuration: Int,
    completedSummary: WorkoutSessionSummary?,
    aiAdvice: String?,
    isLoadingAiAdvice: Boolean,
    userProfile: UserProfile,
    onTogglePause: () -> Unit,
    onSetExerciseIndex: (Int) -> Unit,
    onNextExercise: () -> Unit,
    onPrevExercise: () -> Unit,
    onToggleSetCompleted: (exerciseIdx: Int, setIdx: Int) -> Unit,
    onUpdateSetWeight: (exerciseIdx: Int, setIdx: Int, weight: Float) -> Unit,
    onUpdateSetReps: (exerciseIdx: Int, setIdx: Int, reps: Int) -> Unit,
    onAddSet: (exerciseIdx: Int) -> Unit,
    onAddRestSeconds: (Int) -> Unit,
    onSkipRestTimer: () -> Unit,
    onRequestAiAdvice: (String) -> Unit,
    onDismissAiAdvice: () -> Unit,
    onFinishWorkout: (notes: String) -> Unit,
    onExitWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var finishNotes by remember { mutableStateOf("") }

    // Handle system back button safely
    BackHandler {
        showExitConfirmDialog = true
    }

    if (activePlan == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No active workout selected", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onExitWorkout) {
                    Text("Return to Routines")
                }
            }
        }
        return
    }

    val exercises = activePlan.exercises
    val currentExercise = exercises.getOrNull(currentExerciseIndex) ?: return
    val currentSets = exerciseSets[currentExerciseIndex] ?: emptyList()

    val formattedTime = remember(elapsedSeconds) {
        val hrs = elapsedSeconds / 3600
        val mins = (elapsedSeconds % 3600) / 60
        val secs = elapsedSeconds % 60
        if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    Scaffold(
        modifier = modifier.testTag("active_workout_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activePlan.plan.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isPaused) PulseAmber else PulseGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPaused) "PAUSED ($formattedTime)" else formattedTime,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isPaused) PulseAmber else PulseGreen
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.testTag("toggle_pause_workout_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = { onFinishWorkout(finishNotes) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("finish_workout_top_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PulseGreen,
                            contentColor = Color(0xFF00381B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Finish", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp)
        ) {
            // Rest Timer Banner if active
            item {
                AnimatedVisibility(visible = isRestTimerActive) {
                    CircularRestTimerCountdown(
                        secondsRemaining = restSecondsRemaining,
                        totalSeconds = restDuration,
                        onAddSeconds = onAddRestSeconds,
                        onSkip = onSkipRestTimer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            // Exercise Header Card
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
                                text = "Exercise ${currentExerciseIndex + 1} of ${exercises.size}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PulseGreen
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = currentExercise.targetMuscle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PulseCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = currentExercise.exerciseName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (currentExercise.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Target: ${currentExercise.sets} sets × ${currentExercise.reps} reps • ${currentExercise.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // AI Coach Quick Form Cue Button
                        Button(
                            onClick = { onRequestAiAdvice(currentExercise.exerciseName) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("ask_coach_form_cue_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PulseCyan.copy(alpha = 0.18f),
                                contentColor = PulseCyan
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoadingAiAdvice) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PulseCyan, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Coach Pulse is analyzing form...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ask Coach Pulse Form Biomechanics", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Display AI Advice if present
                        aiAdvice?.let { advice ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PulseCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Coach Pulse Cues",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PulseCyan
                                        )
                                        IconButton(onClick = onDismissAiAdvice, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = advice,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Exercise Navigation Controls (Prev / Next)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledTonalButton(
                        onClick = onPrevExercise,
                        enabled = currentExerciseIndex > 0,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Previous")
                    }

                    FilledTonalButton(
                        onClick = onNextExercise,
                        enabled = currentExerciseIndex < exercises.size - 1,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next Exercise")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Sets Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workout Sets",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = { onAddSet(currentExerciseIndex) }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = PulseGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Set", color = PulseGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Table Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SET", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(44.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("WEIGHT (${if (userProfile.useMetric) "KG" else "LBS"})", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.5f), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Text("REPS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Text("DONE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(56.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }

            // Set Rows
            itemsIndexed(currentSets) { setIndex, activeSet ->
                SetRowItem(
                    set = activeSet,
                    useMetric = userProfile.useMetric,
                    onToggleComplete = { onToggleSetCompleted(currentExerciseIndex, setIndex) },
                    onWeightChange = { newW -> onUpdateSetWeight(currentExerciseIndex, setIndex, newW) },
                    onRepsChange = { newR -> onUpdateSetReps(currentExerciseIndex, setIndex, newR) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Exit Confirmation Dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("Quit Workout?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to exit? You can finish now to save your completed sets.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        onExitWorkout()
                    }
                ) {
                    Text("Discard & Exit", color = PulseRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Resume Workout")
                }
            }
        )
    }

    // Completed Summary Celebratory Dialog
    completedSummary?.let { summary ->
        WorkoutCompleteDialog(
            summary = summary,
            useMetric = userProfile.useMetric,
            onClose = onExitWorkout
        )
    }
}

@Composable
fun SetRowItem(
    set: ActiveSet,
    useMetric: Boolean,
    onToggleComplete: () -> Unit,
    onWeightChange: (Float) -> Unit,
    onRepsChange: (Int) -> Unit
) {
    var weightText by remember(set.weight) { mutableStateOf(if (set.weight > 0) set.weight.toString() else "0") }
    var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }

    val rowBg = if (set.isCompleted) PulseGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (set.isCompleted) PulseGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = rowBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set Number Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (set.isCompleted) PulseGreen else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${set.setNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (set.isCompleted) Color(0xFF00381B) else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Weight Input
            OutlinedTextField(
                value = weightText,
                onValueChange = {
                    weightText = it
                    it.toFloatOrNull()?.let(onWeightChange)
                },
                modifier = Modifier
                    .weight(1.5f)
                    .height(50.dp)
                    .testTag("set_${set.setNumber}_weight_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Reps Input
            OutlinedTextField(
                value = repsText,
                onValueChange = {
                    repsText = it
                    it.toIntOrNull()?.let(onRepsChange)
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(50.dp)
                    .testTag("set_${set.setNumber}_reps_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Checkmark button
            FilledIconToggleButton(
                checked = set.isCompleted,
                onCheckedChange = { onToggleComplete() },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("set_${set.setNumber}_check_button"),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    checkedContainerColor = PulseGreen,
                    checkedContentColor = Color(0xFF00381B),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (set.isCompleted) Icons.Filled.Check else Icons.Outlined.Check,
                    contentDescription = "Complete Set"
                )
            }
        }
    }
}

@Composable
fun WorkoutCompleteDialog(
    summary: WorkoutSessionSummary,
    useMetric: Boolean,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(PulseGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = PulseGreen,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Workout Crushed!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = summary.planTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseGreen,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Duration", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val mins = summary.durationSeconds / 60
                            val secs = summary.durationSeconds % 60
                            Text("${mins}m ${secs}s", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Lift Volume", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val unit = if (useMetric) "kg" else "lbs"
                            val vol = if (useMetric) summary.totalVolumeKg else (summary.totalVolumeKg * 2.20462f)
                            Text("%.0f %s".format(vol, unit), fontWeight = FontWeight.Bold, color = PulseCyan)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sets Completed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${summary.totalSetsCompleted} sets", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Exercises Mastered", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${summary.exercisesCompleted} exercises", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("workout_complete_done_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseGreen,
                        contentColor = Color(0xFF00381B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Return Home", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
