package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // Strength, Cardio, Mobility, Core
    val targetMuscle: String, // Chest, Back, Quads, Hamstrings, Shoulders, Biceps, Triceps, Core, Glutes, Calves, Full Body
    val secondaryMuscles: String = "",
    val equipment: String, // Barbell, Dumbbell, Bodyweight, Cable, Machine, Kettlebell
    val difficulty: String = "Intermediate", // Beginner, Intermediate, Advanced
    val instructions: String,
    val tips: String = ""
)

@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // Hypertrophy, Strength, HIIT, Fat Loss, Home Fitness
    val level: String = "Intermediate", // Beginner, Intermediate, Advanced
    val daysPerWeek: Int = 3,
    val estimatedDurationMinutes: Int = 45,
    val isCustom: Boolean = false,
    val isAiGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class PlanExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long,
    val exerciseName: String,
    val targetMuscle: String,
    val sets: Int = 3,
    val reps: String = "10-12",
    val defaultWeightKg: Float = 0f,
    val restSeconds: Int = 60,
    val orderIndex: Int = 0,
    val notes: String = ""
)

data class WorkoutPlanWithExercises(
    @Embedded val plan: WorkoutPlan,
    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val exercises: List<PlanExercise>
)

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planTitle: String,
    val startedAt: Long,
    val completedAt: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val totalVolumeKg: Float = 0f,
    val exercisesCompleted: Int = 0,
    val setsCompleted: Int = 0,
    val userNotes: String = ""
)

@Entity(
    tableName = "workout_log_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLog::class,
            parentColumns = ["id"],
            childColumns = ["workoutLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutLogId")]
)
data class WorkoutLogSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutLogId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val isCompleted: Boolean = true
)

data class WorkoutLogWithSets(
    @Embedded val log: WorkoutLog,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutLogId"
    )
    val sets: List<WorkoutLogSet>
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "Athlete",
    val goal: String = "Build Muscle & Strength", // Build Muscle & Strength, Fat Loss & Shred, Athletic Performance, General Health
    val experienceLevel: String = "Intermediate", // Beginner, Intermediate, Advanced
    val weightKg: Float = 75f,
    val targetWeightKg: Float = 80f,
    val heightCm: Float = 178f,
    val useMetric: Boolean = true, // true = kg, false = lbs
    val workoutsPerWeekGoal: Int = 4,
    val workoutLocation: String = "Gym", // Gym, Home Gym, Bodyweight Only
    val aiStrategySummary: String = "Focus on progressive overload across compound lifts, maintaining a slight caloric surplus, and aiming for 4 targeted weekly sessions."
)
