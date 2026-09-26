package com.example.data.repository

import com.example.data.api.ChatMessage
import com.example.data.api.GeminiApiService
import com.example.data.api.GeneratedPlanData
import com.example.data.db.ExerciseDao
import com.example.data.db.UserProfileDao
import com.example.data.db.WorkoutLogDao
import com.example.data.db.WorkoutPlanDao
import com.example.data.model.Exercise
import com.example.data.model.PlanExercise
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutLogSet
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutPlanWithExercises
import com.example.data.model.WorkoutLogWithSets
import kotlinx.coroutines.flow.Flow

class FitnessRepository(
    private val exerciseDao: ExerciseDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val workoutLogDao: WorkoutLogDao,
    private val userProfileDao: UserProfileDao
) {
    // Workout Plans
    val allPlans: Flow<List<WorkoutPlanWithExercises>> = workoutPlanDao.getAllPlansWithExercises()

    fun getPlanWithExercises(planId: Long): Flow<WorkoutPlanWithExercises?> =
        workoutPlanDao.getPlanWithExercisesById(planId)

    suspend fun saveCustomPlan(plan: WorkoutPlan, exercises: List<PlanExercise>): Long {
        val planId = workoutPlanDao.insertPlan(plan)
        val linkedExercises = exercises.mapIndexed { index, ex ->
            ex.copy(planId = planId, orderIndex = index)
        }
        workoutPlanDao.insertPlanExercises(linkedExercises)
        return planId
    }

    suspend fun deletePlan(id: Long) {
        workoutPlanDao.deletePlanById(id)
    }

    suspend fun generateAiPlan(
        goal: String,
        fitnessLevel: String,
        daysPerWeek: Int,
        durationMinutes: Int,
        equipment: String,
        targetMuscles: String,
        customNotes: String
    ): GeneratedPlanData {
        return GeminiApiService.generateWorkoutPlan(
            goal = goal,
            fitnessLevel = fitnessLevel,
            daysPerWeek = daysPerWeek,
            durationMinutes = durationMinutes,
            equipment = equipment,
            targetMuscles = targetMuscles,
            customNotes = customNotes
        )
    }

    suspend fun saveGeneratedPlan(planData: GeneratedPlanData): Long {
        val plan = WorkoutPlan(
            title = planData.title,
            description = planData.description,
            category = planData.category,
            level = planData.level,
            estimatedDurationMinutes = planData.durationMinutes,
            isCustom = true,
            isAiGenerated = true
        )
        val planId = workoutPlanDao.insertPlan(plan)
        val exercises = planData.exercises.mapIndexed { index, item ->
            PlanExercise(
                planId = planId,
                exerciseName = item.exerciseName,
                targetMuscle = item.targetMuscle,
                sets = item.sets,
                reps = item.reps,
                defaultWeightKg = 0f,
                restSeconds = item.restSeconds,
                orderIndex = index,
                notes = item.notes
            )
        }
        workoutPlanDao.insertPlanExercises(exercises)
        return planId
    }

    // Exercises
    val allExercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()

    fun searchExercises(query: String): Flow<List<Exercise>> =
        if (query.isBlank()) exerciseDao.getAllExercises() else exerciseDao.searchExercises(query)

    fun getExercisesByMuscle(muscle: String): Flow<List<Exercise>> =
        if (muscle == "All") exerciseDao.getAllExercises() else exerciseDao.getExercisesByMuscle(muscle)

    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)

    // Workout Logs & Analytics
    val allLogs: Flow<List<WorkoutLog>> = workoutLogDao.getAllLogs()
    val allLogsWithSets: Flow<List<WorkoutLogWithSets>> = workoutLogDao.getAllLogsWithSets()
    val totalVolume: Flow<Float?> = workoutLogDao.getTotalVolume()
    val totalDuration: Flow<Long?> = workoutLogDao.getTotalDuration()

    suspend fun saveWorkoutLog(log: WorkoutLog, sets: List<WorkoutLogSet>): Long {
        val logId = workoutLogDao.insertLog(log)
        val linkedSets = sets.map { it.copy(workoutLogId = logId) }
        workoutLogDao.insertLogSets(linkedSets)
        return logId
    }

    suspend fun deleteWorkoutLog(id: Long) {
        workoutLogDao.deleteLogById(id)
    }

    // User Profile
    val userProfile: Flow<UserProfile?> = userProfileDao.getProfile()

    suspend fun updateUserProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    // AI Coach Chat
    suspend fun chatWithCoach(history: List<ChatMessage>, message: String): String {
        return GeminiApiService.chatWithCoach(history, message)
    }
}
