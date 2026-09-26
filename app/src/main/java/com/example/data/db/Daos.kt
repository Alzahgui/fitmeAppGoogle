package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.Exercise
import com.example.data.model.PlanExercise
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutLogSet
import com.example.data.model.WorkoutPlan
import com.example.data.model.WorkoutPlanWithExercises
import com.example.data.model.WorkoutLogWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE targetMuscle = :muscle ORDER BY name ASC")
    fun getExercisesByMuscle(muscle: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR targetMuscle LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercises(query: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int
}

@Dao
interface WorkoutPlanDao {
    @Transaction
    @Query("SELECT * FROM workout_plans ORDER BY createdAt DESC")
    fun getAllPlansWithExercises(): Flow<List<WorkoutPlanWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id = :planId")
    fun getPlanWithExercisesById(planId: Long): Flow<WorkoutPlanWithExercises?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercises(exercises: List<PlanExercise>)

    @Query("DELETE FROM workout_plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)

    @Query("SELECT COUNT(*) FROM workout_plans")
    suspend fun getPlanCount(): Int
}

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_logs ORDER BY completedAt DESC")
    fun getAllLogs(): Flow<List<WorkoutLog>>

    @Transaction
    @Query("SELECT * FROM workout_logs ORDER BY completedAt DESC")
    fun getAllLogsWithSets(): Flow<List<WorkoutLogWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_logs WHERE id = :id")
    fun getLogWithSetsById(id: Long): Flow<WorkoutLogWithSets?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WorkoutLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogSets(sets: List<WorkoutLogSet>)

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("SELECT SUM(totalVolumeKg) FROM workout_logs")
    fun getTotalVolume(): Flow<Float?>

    @Query("SELECT SUM(durationSeconds) FROM workout_logs")
    fun getTotalDuration(): Flow<Long?>
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
}
