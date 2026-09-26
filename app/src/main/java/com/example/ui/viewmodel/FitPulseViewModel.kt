package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeneratedPlanData
import com.example.data.db.FitPulseDatabase
import com.example.data.db.populateInitialData
import com.example.data.model.Exercise
import com.example.data.model.PlanExercise
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutLogWithSets
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutPlanWithExercises
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab {
    HOME,
    PLANS,
    ACTIVE_WORKOUT,
    EXERCISES,
    AI_COACH,
    HISTORY,
    PROFILE
}

data class PlanGenerationState(
    val isGenerating: Boolean = false,
    val generatedPlan: GeneratedPlanData? = null,
    val error: String? = null
)

data class WeeklyChartData(
    val dayLabel: String,
    val workoutsCount: Int,
    val volumeKg: Float
)

class FitPulseViewModel(application: Application) : AndroidViewModel(application) {
    val repository: FitnessRepository

    init {
        val database = FitPulseDatabase.getDatabase(application, viewModelScope)
        repository = FitnessRepository(
            database.exerciseDao(),
            database.workoutPlanDao(),
            database.workoutLogDao(),
            database.userProfileDao()
        )
        // Ensure default data exists even if database already existed
        viewModelScope.launch(Dispatchers.IO) {
            if (database.exerciseDao().getExerciseCount() == 0) {
                populateInitialData(database)
            }
        }
    }

    // Navigation & Screen State
    private val _currentScreen = MutableStateFlow(ScreenTab.HOME)
    val currentScreen: StateFlow<ScreenTab> = _currentScreen.asStateFlow()

    fun navigateTo(screen: ScreenTab) {
        _currentScreen.value = screen
    }

    // User Profile
    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .combine(MutableStateFlow(UserProfile())) { profile, defaultProfile ->
            profile ?: defaultProfile
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserProfile()
        )

    fun updateUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.updateUserProfile(profile)
        }
    }

    // Workout Plans
    val allPlans: StateFlow<List<WorkoutPlanWithExercises>> = repository.allPlans
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _planFilter = MutableStateFlow("All") // All, AI Generated, Custom, Pre-built
    val planFilter: StateFlow<String> = _planFilter.asStateFlow()

    fun setPlanFilter(filter: String) {
        _planFilter.value = filter
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            repository.deletePlan(planId)
        }
    }

    // AI Plan Generation
    private val _planGenState = MutableStateFlow(PlanGenerationState())
    val planGenState: StateFlow<PlanGenerationState> = _planGenState.asStateFlow()

    fun generateAiPlan(
        goal: String,
        level: String,
        daysPerWeek: Int,
        durationMinutes: Int,
        equipment: String,
        targetMuscles: String,
        notes: String
    ) {
        viewModelScope.launch {
            _planGenState.value = PlanGenerationState(isGenerating = true)
            try {
                val plan = repository.generateAiPlan(
                    goal = goal,
                    fitnessLevel = level,
                    daysPerWeek = daysPerWeek,
                    durationMinutes = durationMinutes,
                    equipment = equipment,
                    targetMuscles = targetMuscles,
                    customNotes = notes
                )
                _planGenState.value = PlanGenerationState(isGenerating = false, generatedPlan = plan)
            } catch (e: Exception) {
                _planGenState.value = PlanGenerationState(isGenerating = false, error = e.localizedMessage ?: "Generation failed")
            }
        }
    }

    fun saveGeneratedPlan() {
        val plan = _planGenState.value.generatedPlan ?: return
        viewModelScope.launch {
            repository.saveGeneratedPlan(plan)
            _planGenState.value = PlanGenerationState() // Reset
            _currentScreen.value = ScreenTab.PLANS
        }
    }

    fun dismissGeneratedPlan() {
        _planGenState.value = PlanGenerationState()
    }

    // Custom Plan Builder
    fun createCustomPlan(title: String, desc: String, category: String, exercises: List<PlanExercise>) {
        viewModelScope.launch {
            val plan = WorkoutPlan(
                title = title.ifBlank { "Custom Routine" },
                description = desc.ifBlank { "Custom tailored workout routine." },
                category = category,
                level = userProfile.value.experienceLevel,
                estimatedDurationMinutes = exercises.size * 10,
                isCustom = true,
                isAiGenerated = false
            )
            repository.saveCustomPlan(plan, exercises)
        }
    }

    // Exercise Library
    private val _exerciseSearchQuery = MutableStateFlow("")
    val exerciseSearchQuery: StateFlow<String> = _exerciseSearchQuery.asStateFlow()

    private val _selectedMuscleFilter = MutableStateFlow("All")
    val selectedMuscleFilter: StateFlow<String> = _selectedMuscleFilter.asStateFlow()

    val allExercises: StateFlow<List<Exercise>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setExerciseSearchQuery(query: String) {
        _exerciseSearchQuery.value = query
    }

    fun setSelectedMuscleFilter(muscle: String) {
        _selectedMuscleFilter.value = muscle
    }

    // Workout Logs & History
    val workoutLogs: StateFlow<List<WorkoutLogWithSets>> = repository.allLogsWithSets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalVolume: StateFlow<Float> = repository.totalVolume
        .combine(MutableStateFlow(0f)) { vol, defaultVal -> vol ?: defaultVal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val totalDurationSeconds: StateFlow<Long> = repository.totalDuration
        .combine(MutableStateFlow(0L)) { dur, defaultVal -> dur ?: defaultVal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            repository.deleteWorkoutLog(id)
        }
    }

    // Selected plan to launch into active workout
    private val _selectedPlanForWorkout = MutableStateFlow<WorkoutPlanWithExercises?>(null)
    val selectedPlanForWorkout: StateFlow<WorkoutPlanWithExercises?> = _selectedPlanForWorkout.asStateFlow()

    fun selectPlanForWorkout(plan: WorkoutPlanWithExercises) {
        _selectedPlanForWorkout.value = plan
        _currentScreen.value = ScreenTab.ACTIVE_WORKOUT
    }

    fun clearSelectedPlanForWorkout() {
        _selectedPlanForWorkout.value = null
    }
}
