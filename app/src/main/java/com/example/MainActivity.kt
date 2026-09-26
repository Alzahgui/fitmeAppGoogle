package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FitPulseBottomNav
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AiCoachViewModel
import com.example.ui.viewmodel.FitPulseViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.WorkoutSessionViewModel

class MainActivity : ComponentActivity() {
    private val fitPulseViewModel: FitPulseViewModel by viewModels()
    private val sessionViewModel: WorkoutSessionViewModel by viewModels()
    private val aiCoachViewModel: AiCoachViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FitPulseApp(
                    fitPulseViewModel = fitPulseViewModel,
                    sessionViewModel = sessionViewModel,
                    aiCoachViewModel = aiCoachViewModel
                )
            }
        }
    }
}

@Composable
fun FitPulseApp(
    fitPulseViewModel: FitPulseViewModel,
    sessionViewModel: WorkoutSessionViewModel,
    aiCoachViewModel: AiCoachViewModel
) {
    val currentScreen by fitPulseViewModel.currentScreen.collectAsStateWithLifecycle()
    val userProfile by fitPulseViewModel.userProfile.collectAsStateWithLifecycle()
    val plans by fitPulseViewModel.allPlans.collectAsStateWithLifecycle()
    val planGenState by fitPulseViewModel.planGenState.collectAsStateWithLifecycle()
    val exercises by fitPulseViewModel.allExercises.collectAsStateWithLifecycle()
    val searchQuery by fitPulseViewModel.exerciseSearchQuery.collectAsStateWithLifecycle()
    val selectedMuscle by fitPulseViewModel.selectedMuscleFilter.collectAsStateWithLifecycle()
    val workoutLogs by fitPulseViewModel.workoutLogs.collectAsStateWithLifecycle()
    val totalVolume by fitPulseViewModel.totalVolume.collectAsStateWithLifecycle()
    val totalDuration by fitPulseViewModel.totalDurationSeconds.collectAsStateWithLifecycle()

    // Active session state
    val activePlan by sessionViewModel.activePlan.collectAsStateWithLifecycle()
    val currentExerciseIdx by sessionViewModel.currentExerciseIndex.collectAsStateWithLifecycle()
    val elapsedSeconds by sessionViewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val isPaused by sessionViewModel.isPaused.collectAsStateWithLifecycle()
    val exerciseSets by sessionViewModel.exerciseSets.collectAsStateWithLifecycle()
    val isRestTimerActive by sessionViewModel.isRestTimerActive.collectAsStateWithLifecycle()
    val restSecondsRemaining by sessionViewModel.restSecondsRemaining.collectAsStateWithLifecycle()
    val restDuration by sessionViewModel.restDuration.collectAsStateWithLifecycle()
    val completedSummary by sessionViewModel.completedSummary.collectAsStateWithLifecycle()
    val aiAdvice by sessionViewModel.exerciseAiAdvice.collectAsStateWithLifecycle()
    val isLoadingAiAdvice by sessionViewModel.isLoadingAiAdvice.collectAsStateWithLifecycle()

    // AI Coach Chat state
    val chatMessages by aiCoachViewModel.messages.collectAsStateWithLifecycle()
    val isChatLoading by aiCoachViewModel.isLoading.collectAsStateWithLifecycle()
    var coachPreFillQuery by remember { mutableStateOf<String?>(null) }

    // BackHandler for sub-screens
    if (currentScreen != ScreenTab.HOME && currentScreen != ScreenTab.ACTIVE_WORKOUT) {
        BackHandler {
            fitPulseViewModel.navigateTo(ScreenTab.HOME)
        }
    }

    val showBottomBar = currentScreen != ScreenTab.ACTIVE_WORKOUT && currentScreen != ScreenTab.PROFILE

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                FitPulseBottomNav(
                    currentTab = currentScreen,
                    onTabSelected = { tab ->
                        fitPulseViewModel.navigateTo(tab)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ScreenTab.HOME -> {
                    HomeScreen(
                        userProfile = userProfile,
                        plans = plans,
                        totalWorkouts = workoutLogs.size,
                        totalVolumeKg = totalVolume,
                        onStartWorkout = { plan ->
                            sessionViewModel.startWorkout(plan)
                            fitPulseViewModel.navigateTo(ScreenTab.ACTIVE_WORKOUT)
                        },
                        onNavigateTab = { tab -> fitPulseViewModel.navigateTo(tab) },
                        onOpenAiPlanner = { fitPulseViewModel.navigateTo(ScreenTab.PLANS) },
                        onOpenProfile = { fitPulseViewModel.navigateTo(ScreenTab.PROFILE) }
                    )
                }

                ScreenTab.PLANS -> {
                    WorkoutPlannerScreen(
                        plans = plans,
                        userProfile = userProfile,
                        planGenState = planGenState,
                        onStartWorkout = { plan ->
                            sessionViewModel.startWorkout(plan)
                            fitPulseViewModel.navigateTo(ScreenTab.ACTIVE_WORKOUT)
                        },
                        onGenerateAiPlan = { goal, level, days, dur, eq, mus, notes ->
                            fitPulseViewModel.generateAiPlan(goal, level, days, dur, eq, mus, notes)
                        },
                        onSaveGeneratedPlan = {
                            fitPulseViewModel.saveGeneratedPlan()
                        },
                        onDismissGeneratedPlan = {
                            fitPulseViewModel.dismissGeneratedPlan()
                        },
                        onDeletePlan = { planId ->
                            fitPulseViewModel.deletePlan(planId)
                        },
                        onCreateCustomPlan = { title, desc, cat, exList ->
                            fitPulseViewModel.createCustomPlan(title, desc, cat, exList)
                        }
                    )
                }

                ScreenTab.ACTIVE_WORKOUT -> {
                    ActiveWorkoutScreen(
                        activePlan = activePlan,
                        currentExerciseIndex = currentExerciseIdx,
                        elapsedSeconds = elapsedSeconds,
                        isPaused = isPaused,
                        exerciseSets = exerciseSets,
                        isRestTimerActive = isRestTimerActive,
                        restSecondsRemaining = restSecondsRemaining,
                        restDuration = restDuration,
                        completedSummary = completedSummary,
                        aiAdvice = aiAdvice,
                        isLoadingAiAdvice = isLoadingAiAdvice,
                        userProfile = userProfile,
                        onTogglePause = { sessionViewModel.togglePause() },
                        onSetExerciseIndex = { idx -> sessionViewModel.setExerciseIndex(idx) },
                        onNextExercise = { sessionViewModel.nextExercise() },
                        onPrevExercise = { sessionViewModel.prevExercise() },
                        onToggleSetCompleted = { exIdx, setIdx -> sessionViewModel.toggleSetCompletion(exIdx, setIdx) },
                        onUpdateSetWeight = { exIdx, setIdx, w -> sessionViewModel.updateSetWeight(exIdx, setIdx, w) },
                        onUpdateSetReps = { exIdx, setIdx, r -> sessionViewModel.updateSetReps(exIdx, setIdx, r) },
                        onAddSet = { exIdx -> sessionViewModel.addSet(exIdx) },
                        onAddRestSeconds = { sec -> sessionViewModel.addRestSeconds(sec) },
                        onSkipRestTimer = { sessionViewModel.skipRestTimer() },
                        onRequestAiAdvice = { exName -> sessionViewModel.requestAiCoachFormAdvice(exName) },
                        onDismissAiAdvice = { sessionViewModel.dismissAiAdvice() },
                        onFinishWorkout = { notes ->
                            sessionViewModel.finishWorkout(notes)
                        },
                        onExitWorkout = {
                            sessionViewModel.exitWorkout()
                            fitPulseViewModel.navigateTo(ScreenTab.HOME)
                        }
                    )
                }

                ScreenTab.EXERCISES -> {
                    ExerciseLibraryScreen(
                        exercises = exercises,
                        searchQuery = searchQuery,
                        selectedMuscle = selectedMuscle,
                        onSearchQueryChange = { q -> fitPulseViewModel.setExerciseSearchQuery(q) },
                        onSelectMuscle = { m -> fitPulseViewModel.setSelectedMuscleFilter(m) },
                        onAskCoachAboutExercise = { exName ->
                            coachPreFillQuery = exName
                            fitPulseViewModel.navigateTo(ScreenTab.AI_COACH)
                        }
                    )
                }

                ScreenTab.AI_COACH -> {
                    AiCoachScreen(
                        messages = chatMessages,
                        isLoading = isChatLoading,
                        quickChips = aiCoachViewModel.quickChips,
                        onSendMessage = { text -> aiCoachViewModel.sendMessage(text) },
                        onClearChat = { aiCoachViewModel.clearChat() },
                        initialPreFillQuery = coachPreFillQuery
                    )
                }

                ScreenTab.HISTORY -> {
                    HistoryScreen(
                        logs = workoutLogs,
                        totalVolumeKg = totalVolume,
                        totalDurationSeconds = totalDuration,
                        userProfile = userProfile,
                        onDeleteLog = { logId -> fitPulseViewModel.deleteLog(logId) }
                    )
                }

                ScreenTab.PROFILE -> {
                    ProfileScreen(
                        userProfile = userProfile,
                        onSaveProfile = { updated -> fitPulseViewModel.updateUserProfile(updated) },
                        onAskCoachForAudit = { profile ->
                            coachPreFillQuery = "Based on my profile (${profile.goal}, ${profile.experienceLevel}, ${profile.weightKg}kg), perform a strategic training audit."
                            fitPulseViewModel.navigateTo(ScreenTab.AI_COACH)
                        },
                        onBack = { fitPulseViewModel.navigateTo(ScreenTab.HOME) }
                    )
                }
            }
        }
    }
}
