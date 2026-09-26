package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.GeneratedPlanData
import com.example.data.model.PlanExercise
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutPlanWithExercises
import com.example.ui.theme.PulseAmber
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.PulseRose
import com.example.ui.viewmodel.PlanGenerationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlannerScreen(
    plans: List<WorkoutPlanWithExercises>,
    userProfile: UserProfile,
    planGenState: PlanGenerationState,
    onStartWorkout: (WorkoutPlanWithExercises) -> Unit,
    onGenerateAiPlan: (goal: String, level: String, days: Int, duration: Int, equipment: String, muscles: String, notes: String) -> Unit,
    onSaveGeneratedPlan: () -> Unit,
    onDismissGeneratedPlan: () -> Unit,
    onDeletePlan: (Long) -> Unit,
    onCreateCustomPlan: (title: String, desc: String, category: String, exercises: List<PlanExercise>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAiGeneratorModal by remember { mutableStateOf(false) }
    var showCustomBuilderModal by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val categories = listOf("All", "AI Generated", "Hypertrophy", "Strength", "HIIT")

    val filteredPlans = remember(plans, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "All" -> plans
            "AI Generated" -> plans.filter { it.plan.isAiGenerated }
            else -> plans.filter { it.plan.category.equals(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier.testTag("workout_planner_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Workout Plans & Routines",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showAiGeneratorModal = true },
                        modifier = Modifier.testTag("open_ai_generator_top_bar")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI Planner",
                            tint = PulseGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAiGeneratorModal = true },
                icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                text = { Text("AI Plan Generator", fontWeight = FontWeight.Bold) },
                containerColor = PulseGreen,
                contentColor = Color(0xFF00381B),
                modifier = Modifier.testTag("ai_plan_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Action banner buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showAiGeneratorModal = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("button_open_ai_generator"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Generator", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { showCustomBuilderModal = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("button_open_custom_builder"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Custom Plan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategoryFilter == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = category },
                        label = { Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PulseGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PulseGreen
                        )
                    )
                }
            }

            // Plan List
            if (filteredPlans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No plans found in this category",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAiGeneratorModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PulseGreen, contentColor = Color(0xFF00381B))
                        ) {
                            Text("Generate One with AI", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredPlans, key = { it.plan.id }) { planWithExercises ->
                        WorkoutPlanItemCard(
                            planWithExercises = planWithExercises,
                            onStartWorkout = { onStartWorkout(planWithExercises) },
                            onDelete = { onDeletePlan(planWithExercises.plan.id) }
                        )
                    }
                }
            }
        }
    }

    // AI Generation Dialog
    if (showAiGeneratorModal) {
        AiPlanGeneratorModal(
            userProfile = userProfile,
            isGenerating = planGenState.isGenerating,
            onDismiss = {
                showAiGeneratorModal = false
                onDismissGeneratedPlan()
            },
            onGenerate = { goal, level, days, dur, eq, mus, notes ->
                onGenerateAiPlan(goal, level, days, dur, eq, mus, notes)
            }
        )
    }

    // AI Generated Result Preview Dialog
    planGenState.generatedPlan?.let { genPlan ->
        GeneratedPlanPreviewDialog(
            plan = genPlan,
            onSave = {
                onSaveGeneratedPlan()
                showAiGeneratorModal = false
            },
            onDismiss = onDismissGeneratedPlan
        )
    }

    // Custom Plan Builder Dialog
    if (showCustomBuilderModal) {
        CustomPlanBuilderDialog(
            onDismiss = { showCustomBuilderModal = false },
            onSave = { title, desc, category, exercises ->
                onCreateCustomPlan(title, desc, category, exercises)
                showCustomBuilderModal = false
            }
        )
    }
}

@Composable
fun WorkoutPlanItemCard(
    planWithExercises: WorkoutPlanWithExercises,
    onStartWorkout: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val plan = planWithExercises.plan

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (plan.isAiGenerated) PulseCyan.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Category, Badges, and Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PulseGreen.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = plan.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PulseGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (plan.isAiGenerated) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PulseCyan.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI GENERATED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PulseCyan
                                )
                            }
                        }
                    }
                }

                if (plan.isCustom || plan.isAiGenerated) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete Plan",
                            tint = PulseRose,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("~${plan.estimatedDurationMinutes}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${planWithExercises.exercises.size} Exercises", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(plan.level, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Exercises expandable preview
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    planWithExercises.exercises.forEachIndexed { i, ex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${i + 1}. ${ex.exerciseName}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${ex.sets} sets × ${ex.reps}",
                                style = MaterialTheme.typography.labelMedium,
                                color = PulseGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Bottom action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (expanded) "Hide Details" else "View Exercises", fontSize = 12.sp)
                }

                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_plan_button_${plan.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseGreen,
                        contentColor = Color(0xFF00381B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPlanGeneratorModal(
    userProfile: UserProfile,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (goal: String, level: String, days: Int, duration: Int, equipment: String, muscles: String, notes: String) -> Unit
) {
    var selectedGoal by remember { mutableStateOf(userProfile.goal) }
    var selectedLevel by remember { mutableStateOf(userProfile.experienceLevel) }
    var selectedDays by remember { mutableStateOf(4) }
    var selectedDuration by remember { mutableStateOf(45) }
    var selectedEquipment by remember { mutableStateOf(userProfile.workoutLocation) }
    var selectedMuscleFocus by remember { mutableStateOf("Full Body Power") }
    var customNotes by remember { mutableStateOf("") }

    val goals = listOf("Build Muscle & Strength", "Fat Loss & Shred", "Athletic Performance", "Functional Mobility")
    val levels = listOf("Beginner", "Intermediate", "Advanced")
    val equipments = listOf("Gym", "Dumbbells Only", "Home / Bodyweight", "Kettlebell")
    val muscleFocuses = listOf("Full Body Power", "Push (Chest & Shoulders)", "Pull (Back & Biceps)", "Legs & Glutes", "Core & Cardio")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PulseGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = PulseGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI Workout Architect",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Powered by Coach Pulse",
                                style = MaterialTheme.typography.labelSmall,
                                color = PulseGreen
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = PulseGreen,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Crafting your routine...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Analyzing biomechanics, muscle balance, and progressive overload.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Goal
                        Text("Target Goal", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(goals) { goal ->
                                FilterChip(
                                    selected = selectedGoal == goal,
                                    onClick = { selectedGoal = goal },
                                    label = { Text(goal, fontSize = 12.sp) }
                                )
                            }
                        }

                        // Experience Level
                        Text("Fitness Level", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            levels.forEach { level ->
                                FilterChip(
                                    selected = selectedLevel == level,
                                    onClick = { selectedLevel = level },
                                    label = { Text(level, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Target Muscle Focus
                        Text("Target Muscle Focus", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(muscleFocuses) { focus ->
                                FilterChip(
                                    selected = selectedMuscleFocus == focus,
                                    onClick = { selectedMuscleFocus = focus },
                                    label = { Text(focus, fontSize = 12.sp) }
                                )
                            }
                        }

                        // Equipment
                        Text("Equipment Available", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(equipments) { eq ->
                                FilterChip(
                                    selected = selectedEquipment == eq,
                                    onClick = { selectedEquipment = eq },
                                    label = { Text(eq, fontSize = 12.sp) }
                                )
                            }
                        }

                        // Target Duration
                        Text("Workout Duration: $selectedDuration minutes", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30, 45, 60, 75).forEach { dur ->
                                FilterChip(
                                    selected = selectedDuration == dur,
                                    onClick = { selectedDuration = dur },
                                    label = { Text("${dur}m", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Additional Notes
                        OutlinedTextField(
                            value = customNotes,
                            onValueChange = { customNotes = it },
                            label = { Text("Special Requests / Injury Considerations") },
                            placeholder = { Text("e.g. Avoid shoulder impingement, focus on upper chest") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_planner_notes_input"),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onGenerate(
                                selectedGoal,
                                selectedLevel,
                                selectedDays,
                                selectedDuration,
                                selectedEquipment,
                                selectedMuscleFocus,
                                customNotes
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_generate_ai_plan_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PulseGreen,
                            contentColor = Color(0xFF00381B)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Custom Workout Routine", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratedPlanPreviewDialog(
    plan: GeneratedPlanData,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PulseGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "AI ROUTINE READY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PulseGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(plan.exercises) { ex ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.exerciseName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${ex.targetMuscle} • ${ex.notes}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PulseCyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${ex.sets} × ${ex.reps}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PulseCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_ai_plan_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseGreen,
                        contentColor = Color(0xFF00381B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to My Workout Plans", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomPlanBuilderDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, category: String, exercises: List<PlanExercise>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Strength") }
    val exercises = remember { mutableStateListOf<PlanExercise>() }

    var newExName by remember { mutableStateOf("") }
    var newExMuscle by remember { mutableStateOf("Chest") }
    var newExSets by remember { mutableStateOf("3") }
    var newExReps by remember { mutableStateOf("10") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Build Custom Routine",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine Name") },
                    placeholder = { Text("e.g. Upper Body Blast") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Add Exercise to Plan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newExName,
                        onValueChange = { newExName = it },
                        label = { Text("Exercise") },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newExSets,
                        onValueChange = { newExSets = it },
                        label = { Text("Sets") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newExReps,
                        onValueChange = { newExReps = it },
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (newExName.isNotBlank()) {
                            exercises.add(
                                PlanExercise(
                                    planId = 0,
                                    exerciseName = newExName.trim(),
                                    targetMuscle = newExMuscle,
                                    sets = newExSets.toIntOrNull() ?: 3,
                                    reps = newExReps.ifBlank { "10" }
                                )
                            )
                            newExName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to List")
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(exercises) { ex ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ex.exerciseName} (${ex.sets} × ${ex.reps})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(
                                    onClick = { exercises.remove(ex) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = PulseRose, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank() && exercises.isNotEmpty()) {
                            onSave(title, desc, category, exercises.toList())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PulseGreen, contentColor = Color(0xFF00381B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Routine", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
