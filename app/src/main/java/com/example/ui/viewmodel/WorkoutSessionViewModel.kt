package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.FitPulseDatabase
import com.example.data.model.PlanExercise
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutLogSet
import com.example.data.model.WorkoutPlanWithExercises
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ActiveSet(
    val setNumber: Int,
    var weight: Float,
    var reps: Int,
    var isCompleted: Boolean = false
)

data class WorkoutSessionSummary(
    val planTitle: String,
    val durationSeconds: Long,
    val totalVolumeKg: Float,
    val exercisesCompleted: Int,
    val totalSetsCompleted: Int
)

class WorkoutSessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FitnessRepository

    init {
        val database = FitPulseDatabase.getDatabase(application, viewModelScope)
        repository = FitnessRepository(
            database.exerciseDao(),
            database.workoutPlanDao(),
            database.workoutLogDao(),
            database.userProfileDao()
        )
    }

    // Active Workout State
    private val _activePlan = MutableStateFlow<WorkoutPlanWithExercises?>(null)
    val activePlan: StateFlow<WorkoutPlanWithExercises?> = _activePlan.asStateFlow()

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // Exercise index -> list of sets
    private val _exerciseSets = MutableStateFlow<Map<Int, List<ActiveSet>>>(emptyMap())
    val exerciseSets: StateFlow<Map<Int, List<ActiveSet>>> = _exerciseSets.asStateFlow()

    // Rest Timer State
    private val _restDuration = MutableStateFlow(60)
    val restDuration: StateFlow<Int> = _restDuration.asStateFlow()

    private val _restSecondsRemaining = MutableStateFlow(0)
    val restSecondsRemaining: StateFlow<Int> = _restSecondsRemaining.asStateFlow()

    private val _isRestTimerActive = MutableStateFlow(false)
    val isRestTimerActive: StateFlow<Boolean> = _isRestTimerActive.asStateFlow()

    // Workout completed summary dialog
    private val _completedSummary = MutableStateFlow<WorkoutSessionSummary?>(null)
    val completedSummary: StateFlow<WorkoutSessionSummary?> = _completedSummary.asStateFlow()

    // AI advice for active exercise
    private val _exerciseAiAdvice = MutableStateFlow<String?>(null)
    val exerciseAiAdvice: StateFlow<String?> = _exerciseAiAdvice.asStateFlow()
    private val _isLoadingAiAdvice = MutableStateFlow(false)
    val isLoadingAiAdvice: StateFlow<Boolean> = _isLoadingAiAdvice.asStateFlow()

    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private var workoutStartMillis: Long = 0L

    fun startWorkout(plan: WorkoutPlanWithExercises) {
        _activePlan.value = plan
        _currentExerciseIndex.value = 0
        _elapsedSeconds.value = 0L
        _isPaused.value = false
        _completedSummary.value = null
        _exerciseAiAdvice.value = null
        workoutStartMillis = System.currentTimeMillis()

        // Initialize sets for each exercise in plan
        val initialMap = mutableMapOf<Int, List<ActiveSet>>()
        plan.exercises.forEachIndexed { index, planEx ->
            val defaultReps = planEx.reps.split("-").lastOrNull()?.toIntOrNull() ?: 10
            val sets = (1..planEx.sets).map { setNum ->
                ActiveSet(
                    setNumber = setNum,
                    weight = planEx.defaultWeightKg,
                    reps = defaultReps,
                    isCompleted = false
                )
            }
            initialMap[index] = sets
        }
        _exerciseSets.value = initialMap

        startElapsedTimer()
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (!_isPaused.value && _activePlan.value != null) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun setExerciseIndex(index: Int) {
        val total = _activePlan.value?.exercises?.size ?: 0
        if (index in 0 until total) {
            _currentExerciseIndex.value = index
            _exerciseAiAdvice.value = null
        }
    }

    fun nextExercise() {
        val next = _currentExerciseIndex.value + 1
        setExerciseIndex(next)
    }

    fun prevExercise() {
        val prev = _currentExerciseIndex.value - 1
        setExerciseIndex(prev)
    }

    fun toggleSetCompletion(exerciseIndex: Int, setIndex: Int) {
        val currentMap = _exerciseSets.value.toMutableMap()
        val sets = currentMap[exerciseIndex]?.toMutableList() ?: return
        if (setIndex in sets.indices) {
            val set = sets[setIndex]
            val newCompleted = !set.isCompleted
            sets[setIndex] = set.copy(isCompleted = newCompleted)
            currentMap[exerciseIndex] = sets
            _exerciseSets.value = currentMap

            // If checked as completed, trigger rest timer!
            if (newCompleted) {
                val restSecs = _activePlan.value?.exercises?.getOrNull(exerciseIndex)?.restSeconds ?: 60
                startRestTimer(restSecs)
            }
        }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: Float) {
        val currentMap = _exerciseSets.value.toMutableMap()
        val sets = currentMap[exerciseIndex]?.toMutableList() ?: return
        if (setIndex in sets.indices) {
            sets[setIndex] = sets[setIndex].copy(weight = weight.coerceAtLeast(0f))
            currentMap[exerciseIndex] = sets
            _exerciseSets.value = currentMap
        }
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        val currentMap = _exerciseSets.value.toMutableMap()
        val sets = currentMap[exerciseIndex]?.toMutableList() ?: return
        if (setIndex in sets.indices) {
            sets[setIndex] = sets[setIndex].copy(reps = reps.coerceAtLeast(1))
            currentMap[exerciseIndex] = sets
            _exerciseSets.value = currentMap
        }
    }

    fun addSet(exerciseIndex: Int) {
        val currentMap = _exerciseSets.value.toMutableMap()
        val sets = currentMap[exerciseIndex]?.toMutableList() ?: mutableListOf()
        val lastSet = sets.lastOrNull()
        val newSet = ActiveSet(
            setNumber = sets.size + 1,
            weight = lastSet?.weight ?: 0f,
            reps = lastSet?.reps ?: 10,
            isCompleted = false
        )
        sets.add(newSet)
        currentMap[exerciseIndex] = sets
        _exerciseSets.value = currentMap
    }

    // Rest Timer
    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _restDuration.value = seconds
        _restSecondsRemaining.value = seconds
        _isRestTimerActive.value = true

        restTimerJob = viewModelScope.launch {
            while (_restSecondsRemaining.value > 0) {
                delay(1000)
                _restSecondsRemaining.value -= 1
            }
            _isRestTimerActive.value = false
            triggerRestCompleteHaptic()
        }
    }

    fun addRestSeconds(seconds: Int) {
        val newTime = (_restSecondsRemaining.value + seconds).coerceAtLeast(0)
        _restSecondsRemaining.value = newTime
        if (newTime > _restDuration.value) {
            _restDuration.value = newTime
        }
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        _isRestTimerActive.value = false
        _restSecondsRemaining.value = 0
    }

    private fun triggerRestCompleteHaptic() {
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(350)
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration fails
        }
    }

    // AI Coach Form Check
    fun requestAiCoachFormAdvice(exerciseName: String) {
        viewModelScope.launch {
            _isLoadingAiAdvice.value = true
            val prompt = "Give me 3 precise, high-priority biomechanical cues to master $exerciseName safely and maximize muscle activation. Keep it concise with bullet points."
            val advice = repository.chatWithCoach(emptyList(), prompt)
            _exerciseAiAdvice.value = advice
            _isLoadingAiAdvice.value = false
        }
    }

    fun dismissAiAdvice() {
        _exerciseAiAdvice.value = null
    }

    // Finish Workout Session
    fun finishWorkout(userNotes: String = "") {
        val plan = _activePlan.value ?: return
        val currentSets = _exerciseSets.value

        var totalVolume = 0f
        var totalCompletedSets = 0
        val logSetsToInsert = mutableListOf<WorkoutLogSet>()

        currentSets.forEach { (exIdx, sets) ->
            val exerciseName = plan.exercises.getOrNull(exIdx)?.exerciseName ?: "Exercise"
            sets.forEach { set ->
                if (set.isCompleted) {
                    totalVolume += (set.weight * set.reps)
                    totalCompletedSets++
                    logSetsToInsert.add(
                        WorkoutLogSet(
                            workoutLogId = 0,
                            exerciseName = exerciseName,
                            setNumber = set.setNumber,
                            weightKg = set.weight,
                            reps = set.reps,
                            isCompleted = true
                        )
                    )
                }
            }
        }

        val completedExercises = currentSets.count { (_, sets) -> sets.any { it.isCompleted } }
        val duration = _elapsedSeconds.value

        val log = WorkoutLog(
            planTitle = plan.plan.title,
            startedAt = workoutStartMillis,
            completedAt = System.currentTimeMillis(),
            durationSeconds = duration,
            totalVolumeKg = totalVolume,
            exercisesCompleted = completedExercises,
            setsCompleted = totalCompletedSets,
            userNotes = userNotes
        )

        viewModelScope.launch {
            repository.saveWorkoutLog(log, logSetsToInsert)
            _completedSummary.value = WorkoutSessionSummary(
                planTitle = plan.plan.title,
                durationSeconds = duration,
                totalVolumeKg = totalVolume,
                exercisesCompleted = completedExercises,
                totalSetsCompleted = totalCompletedSets
            )
            timerJob?.cancel()
            restTimerJob?.cancel()
        }
    }

    fun exitWorkout() {
        timerJob?.cancel()
        restTimerJob?.cancel()
        _activePlan.value = null
        _completedSummary.value = null
        _exerciseSets.value = emptyMap()
    }
}
