package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Exercise
import com.example.data.model.PlanExercise
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutLogSet
import com.example.data.model.WorkoutPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Exercise::class,
        WorkoutPlan::class,
        PlanExercise::class,
        WorkoutLog::class,
        WorkoutLogSet::class,
        UserProfile::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FitPulseDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: FitPulseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FitPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitPulseDatabase::class.java,
                    "fitpulse_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }
    }
}

suspend fun populateInitialData(database: FitPulseDatabase) {
    val exerciseDao = database.exerciseDao()
    val planDao = database.workoutPlanDao()
    val profileDao = database.userProfileDao()

    // 1. Initial User Profile
    profileDao.insertOrUpdateProfile(
        UserProfile(
            id = 1,
            name = "Alex Stone",
            goal = "Build Muscle & Strength",
            experienceLevel = "Intermediate",
            weightKg = 76f,
            targetWeightKg = 80f,
            heightCm = 178f,
            useMetric = true,
            workoutsPerWeekGoal = 4,
            workoutLocation = "Gym",
            aiStrategySummary = "Focus on compound strength progression with 60-90s rest intervals, aiming for a 250 kcal surplus and consistent sleep."
        )
    )

    // 2. Curated Exercises Library
    val exercises = listOf(
        // Chest
        Exercise(
            name = "Barbell Bench Press",
            category = "Strength",
            targetMuscle = "Chest",
            secondaryMuscles = "Triceps, Front Shoulders",
            equipment = "Barbell",
            difficulty = "Intermediate",
            instructions = "Lie flat on the bench with feet firmly planted. Grip the barbell slightly wider than shoulder width. Unrack and lower bar smoothly to mid-chest while tucking elbows slightly. Press explosively upward.",
            tips = "Retract scapulae and keep wrist straight over elbows. Do not bounce the bar off your ribcage."
        ),
        Exercise(
            name = "Incline Dumbbell Press",
            category = "Hypertrophy",
            targetMuscle = "Chest",
            secondaryMuscles = "Front Delts, Triceps",
            equipment = "Dumbbell",
            difficulty = "Intermediate",
            instructions = "Set bench to 30-45 degree incline. Hold dumbbells with neutral or pronated grip at upper chest level. Press up smoothly, converging slightly at top without clanking.",
            tips = "Keep lower back gently arched without lifting hips off the bench."
        ),
        Exercise(
            name = "Cable Chest Flyes",
            category = "Hypertrophy",
            targetMuscle = "Chest",
            secondaryMuscles = "Front Delts",
            equipment = "Cable",
            difficulty = "Beginner",
            instructions = "Set pulleys to shoulder height. Step forward into a split stance. With a slight bend in elbows, sweep arms forward until hands meet in front of chest.",
            tips = "Squeeze chest hard at contraction peak. Control the eccentric stretch."
        ),
        Exercise(
            name = "Push-Ups",
            category = "Strength",
            targetMuscle = "Chest",
            secondaryMuscles = "Core, Triceps, Shoulders",
            equipment = "Bodyweight",
            difficulty = "Beginner",
            instructions = "Start in a high plank position with hands slightly wider than shoulders. Keep core braced and hips aligned. Lower chest until 1 inch off ground, then push back up.",
            tips = "Avoid sagging hips or flaring elbows 90 degrees outward."
        ),

        // Back
        Exercise(
            name = "Deadlift (Conventional)",
            category = "Strength",
            targetMuscle = "Back",
            secondaryMuscles = "Hamstrings, Glutes, Traps, Core",
            equipment = "Barbell",
            difficulty = "Advanced",
            instructions = "Stand with feet hip-width under the barbell, bar over mid-foot. Hinge hips back and grip bar just outside knees. Wedge chest tall, pull slack out of bar, and drive through the floor to stand upright.",
            tips = "Engage lats like squeezing oranges in armpits to protect the lower spine."
        ),
        Exercise(
            name = "Barbell Bent-Over Row",
            category = "Strength",
            targetMuscle = "Back",
            secondaryMuscles = "Biceps, Rear Delts, Core",
            equipment = "Barbell",
            difficulty = "Intermediate",
            instructions = "Hinge forward at the hips to a 45-degree angle with a flat back. Grip barbell overhand. Pull barbell toward your lower ribs, squeezing shoulder blades together.",
            tips = "Do not use body momentum or swing up and down."
        ),
        Exercise(
            name = "Lat Pulldown",
            category = "Hypertrophy",
            targetMuscle = "Back",
            secondaryMuscles = "Biceps, Rear Delts",
            equipment = "Cable",
            difficulty = "Beginner",
            instructions = "Sit upright with thighs locked under pads. Grip wide bar outside shoulder width. Pull bar down toward upper chest while driving elbows down and back.",
            tips = "Avoid leaning backward excessively. Emphasize full stretch at the top."
        ),
        Exercise(
            name = "Pull-Ups",
            category = "Strength",
            targetMuscle = "Back",
            secondaryMuscles = "Biceps, Forearms, Core",
            equipment = "Bodyweight",
            difficulty = "Intermediate",
            instructions = "Grip overhead pull-up bar slightly wider than shoulder width. From dead hang, depress shoulder blades and pull chest toward the bar until chin clears bar.",
            tips = "Control the descent back to a full dead hang; avoid swinging legs."
        ),
        Exercise(
            name = "Single-Arm Dumbbell Row",
            category = "Hypertrophy",
            targetMuscle = "Back",
            secondaryMuscles = "Biceps, Rhomboids",
            equipment = "Dumbbell",
            difficulty = "Beginner",
            instructions = "Place knee and same-side hand on flat bench. With free hand, hold dumbbell and pull toward hip pocket while maintaining flat neutral spine.",
            tips = "Pull with the elbow, not the wrist. Squeeze the lat at peak contraction."
        ),

        // Legs
        Exercise(
            name = "Barbell Back Squat",
            category = "Strength",
            targetMuscle = "Quads",
            secondaryMuscles = "Glutes, Hamstrings, Core",
            equipment = "Barbell",
            difficulty = "Advanced",
            instructions = "Place barbell across upper traps. Unrack and take 2 steps back. Set feet shoulder-width, toes slightly flared. Inhale, brace core, break at hips and knees simultaneously to squat to parallel or below. Drive up through midfoot.",
            tips = "Keep knees tracking in line with toes and chest proud throughout."
        ),
        Exercise(
            name = "Romanian Deadlift (RDL)",
            category = "Hypertrophy",
            targetMuscle = "Hamstrings",
            secondaryMuscles = "Glutes, Lower Back",
            equipment = "Barbell",
            difficulty = "Intermediate",
            instructions = "Stand holding barbell with overhand grip at thighs. With soft knees, push hips backward like closing a car door with glutes. Lower bar close to shins until deep hamstring stretch, then drive hips forward.",
            tips = "Movement is hip hinge, not knee bend. Keep spine strictly neutral."
        ),
        Exercise(
            name = "Leg Press",
            category = "Hypertrophy",
            targetMuscle = "Quads",
            secondaryMuscles = "Glutes, Calves",
            equipment = "Machine",
            difficulty = "Beginner",
            instructions = "Sit firmly in machine with back and hips pressed against pad. Place feet shoulder-width on carriage. Release safety locks, lower weight until knees form 90 degrees, then press smoothly up without locking knees.",
            tips = "Never lock knees at the top lockout position."
        ),
        Exercise(
            name = "Walking Dumbbell Lunges",
            category = "Strength",
            targetMuscle = "Quads",
            secondaryMuscles = "Glutes, Hamstrings, Calves",
            equipment = "Dumbbell",
            difficulty = "Intermediate",
            instructions = "Hold dumbbells at sides. Step forward into lunge until trailing knee gently hovers above floor. Drive off front heel into next forward step.",
            tips = "Maintain upright torso posture and keep core engaged."
        ),
        Exercise(
            name = "Lying Hamstring Curl",
            category = "Hypertrophy",
            targetMuscle = "Hamstrings",
            secondaryMuscles = "Calves",
            equipment = "Machine",
            difficulty = "Beginner",
            instructions = "Lie face down with pad positioned just below calf muscles. Curl heels toward glutes smoothly, pause briefly, then control descent.",
            tips = "Keep hips anchored to pad to prevent lower back hyperextension."
        ),
        Exercise(
            name = "Standing Calf Raises",
            category = "Hypertrophy",
            targetMuscle = "Calves",
            secondaryMuscles = "Feet",
            equipment = "Machine",
            difficulty = "Beginner",
            instructions = "Place balls of feet on platform edge with heels hanging. Lower heels into deep stretch, then elevate on toes as high as possible.",
            tips = "Hold peak top contraction for 1 full second for maximum hypertrophy stimulus."
        ),

        // Shoulders
        Exercise(
            name = "Overhead Barbell Press",
            category = "Strength",
            targetMuscle = "Shoulders",
            secondaryMuscles = "Triceps, Upper Chest, Core",
            equipment = "Barbell",
            difficulty = "Intermediate",
            instructions = "Hold barbell at clavicle level with pronated grip outside shoulders. Squeeze glutes and core. Press bar vertically overhead, moving head slightly back then pushing forward under the bar at lockout.",
            tips = "Avoid excessive lower back hyperextension."
        ),
        Exercise(
            name = "Dumbbell Lateral Raise",
            category = "Hypertrophy",
            targetMuscle = "Shoulders",
            secondaryMuscles = "Traps",
            equipment = "Dumbbell",
            difficulty = "Beginner",
            instructions = "Stand tall with dumbbells at sides. Raise arms outward to the sides with slight elbow bend until parallel with floor. Lower with controlled tempo.",
            tips = "Lead with the elbows, not hands. Avoid swinging torso."
        ),
        Exercise(
            name = "Face Pulls",
            category = "Mobility",
            targetMuscle = "Shoulders",
            secondaryMuscles = "Rear Delts, Rotator Cuff, Traps",
            equipment = "Cable",
            difficulty = "Beginner",
            instructions = "Attach rope to upper pulley. Grip rope with thumbs pointing back. Step back, pull rope toward eye level while externally rotating wrists so knuckles face behind you.",
            tips = "Terrific posture correction and shoulder longevity movement."
        ),

        // Arms (Biceps & Triceps)
        Exercise(
            name = "Barbell Bicep Curl",
            category = "Hypertrophy",
            targetMuscle = "Biceps",
            secondaryMuscles = "Forearms",
            equipment = "Barbell",
            difficulty = "Beginner",
            instructions = "Stand holding barbell underhand at hip level. Keep upper arms pinned by sides. Curl bar toward shoulders, squeezing biceps hard at the top.",
            tips = "Do not swing hips back and forth to hoist the weight."
        ),
        Exercise(
            name = "Incline Dumbbell Curl",
            category = "Hypertrophy",
            targetMuscle = "Biceps",
            secondaryMuscles = "Forearms",
            equipment = "Dumbbell",
            difficulty = "Intermediate",
            instructions = "Sit on incline bench (45-60 deg). Let arms hang down fully extended. Curl dumbbells up while supinating wrists at top.",
            tips = "Places maximum stretch on the long head of the bicep."
        ),
        Exercise(
            name = "Tricep Rope Pushdown",
            category = "Hypertrophy",
            targetMuscle = "Triceps",
            secondaryMuscles = "Forearms",
            equipment = "Cable",
            difficulty = "Beginner",
            instructions = "Attach rope to high pulley. Tuck elbows against ribs. Extend arms downward, flaring rope ends apart at bottom for full lockout.",
            tips = "Keep upper arms stationary throughout the entire rep."
        ),
        Exercise(
            name = "Skull Crushers (Lying Tricep Extension)",
            category = "Hypertrophy",
            targetMuscle = "Triceps",
            secondaryMuscles = "Forearms",
            equipment = "Barbell",
            difficulty = "Intermediate",
            instructions = "Lie on flat bench holding EZ bar with arms vertical. Keeping elbows pointed up, bend elbows to lower bar toward forehead or slightly behind head, then press back up.",
            tips = "Keep elbows tucked rather than flared outward."
        ),

        // Core
        Exercise(
            name = "Hanging Leg Raises",
            category = "Core",
            targetMuscle = "Core",
            secondaryMuscles = "Hip Flexors, Grip",
            equipment = "Bodyweight",
            difficulty = "Intermediate",
            instructions = "Hang from pull-up bar with arms straight. Keeping legs straight or slightly bent, raise feet upward toward the bar by rolling the pelvis up.",
            tips = "Focus on curling pelvis toward ribs rather than swinging legs."
        ),
        Exercise(
            name = "Plank Hold",
            category = "Core",
            targetMuscle = "Core",
            secondaryMuscles = "Glutes, Shoulders",
            equipment = "Bodyweight",
            difficulty = "Beginner",
            instructions = "Rest on forearms and toes. Form straight line from head to heels. Squeeze glutes, brace abs tightly as if preparing for a punch.",
            tips = "Breathe steadily; don't let lower back dip downward."
        ),
        Exercise(
            name = "Cable Woodchoppers",
            category = "Core",
            targetMuscle = "Core",
            secondaryMuscles = "Obliques, Shoulders",
            equipment = "Cable",
            difficulty = "Intermediate",
            instructions = "Stand sideways to high cable pulley. Grip handle with both hands. Rotate torso diagonally downward across body toward opposite hip.",
            tips = "Rotate from core and hips, keeping arms relatively straight."
        ),

        // Cardio & HIIT
        Exercise(
            name = "Kettlebell Swings",
            category = "Cardio",
            targetMuscle = "Full Body",
            secondaryMuscles = "Hamstrings, Glutes, Core, Back",
            equipment = "Kettlebell",
            difficulty = "Intermediate",
            instructions = "Stand over kettlebell with feet shoulder-width. Hinge at hips to swing kettlebell between legs, then snap hips forward explosively to swing bell to chest height.",
            tips = "Power comes from explosive hip drive, not arm lifting."
        ),
        Exercise(
            name = "Burpees",
            category = "Cardio",
            targetMuscle = "Full Body",
            secondaryMuscles = "Chest, Quads, Core",
            equipment = "Bodyweight",
            difficulty = "Beginner",
            instructions = "Drop from standing into a squat, kick feet back to plank, perform push-up, jump feet back under hips, and jump explosively with arms overhead.",
            tips = "Maintain rhythmic pacing during long sets."
        )
    )

    exerciseDao.insertExercises(exercises)

    // 3. Pre-built Curated Workout Plans
    // Plan 1: Push Power Hypertrophy
    val pushPlanId = planDao.insertPlan(
        WorkoutPlan(
            title = "Push Day Power & Hypertrophy",
            description = "Ultimate chest, front delt, and tricep builder with progressive compound overloads.",
            category = "Hypertrophy",
            level = "Intermediate",
            daysPerWeek = 4,
            estimatedDurationMinutes = 50,
            isCustom = false,
            isAiGenerated = false
        )
    )
    planDao.insertPlanExercises(
        listOf(
            PlanExercise(planId = pushPlanId, exerciseName = "Barbell Bench Press", targetMuscle = "Chest", sets = 4, reps = "6-8", defaultWeightKg = 70f, restSeconds = 90, orderIndex = 0),
            PlanExercise(planId = pushPlanId, exerciseName = "Incline Dumbbell Press", targetMuscle = "Chest", sets = 3, reps = "8-10", defaultWeightKg = 24f, restSeconds = 75, orderIndex = 1),
            PlanExercise(planId = pushPlanId, exerciseName = "Overhead Barbell Press", targetMuscle = "Shoulders", sets = 3, reps = "8-10", defaultWeightKg = 40f, restSeconds = 75, orderIndex = 2),
            PlanExercise(planId = pushPlanId, exerciseName = "Dumbbell Lateral Raise", targetMuscle = "Shoulders", sets = 4, reps = "12-15", defaultWeightKg = 10f, restSeconds = 60, orderIndex = 3),
            PlanExercise(planId = pushPlanId, exerciseName = "Tricep Rope Pushdown", targetMuscle = "Triceps", sets = 3, reps = "12-15", defaultWeightKg = 25f, restSeconds = 60, orderIndex = 4)
        )
    )

    // Plan 2: Pull Titan Strength
    val pullPlanId = planDao.insertPlan(
        WorkoutPlan(
            title = "Pull Day Titan Back & Arms",
            description = "Heavy back density and bicep peak development focusing on width and thickness.",
            category = "Strength",
            level = "Intermediate",
            daysPerWeek = 4,
            estimatedDurationMinutes = 55,
            isCustom = false,
            isAiGenerated = false
        )
    )
    planDao.insertPlanExercises(
        listOf(
            PlanExercise(planId = pullPlanId, exerciseName = "Deadlift (Conventional)", targetMuscle = "Back", sets = 3, reps = "5", defaultWeightKg = 110f, restSeconds = 120, orderIndex = 0),
            PlanExercise(planId = pullPlanId, exerciseName = "Pull-Ups", targetMuscle = "Back", sets = 3, reps = "8-10", defaultWeightKg = 0f, restSeconds = 90, orderIndex = 1),
            PlanExercise(planId = pullPlanId, exerciseName = "Barbell Bent-Over Row", targetMuscle = "Back", sets = 4, reps = "8-10", defaultWeightKg = 60f, restSeconds = 75, orderIndex = 2),
            PlanExercise(planId = pullPlanId, exerciseName = "Face Pulls", targetMuscle = "Shoulders", sets = 3, reps = "15", defaultWeightKg = 20f, restSeconds = 60, orderIndex = 3),
            PlanExercise(planId = pullPlanId, exerciseName = "Barbell Bicep Curl", targetMuscle = "Biceps", sets = 3, reps = "10-12", defaultWeightKg = 30f, restSeconds = 60, orderIndex = 4)
        )
    )

    // Plan 3: Legs & Core Powerhouse
    val legsPlanId = planDao.insertPlan(
        WorkoutPlan(
            title = "Legs & Core Powerhouse",
            description = "Quad drive, hamstring hinge strength, and locked core resilience.",
            category = "Strength",
            level = "Advanced",
            daysPerWeek = 3,
            estimatedDurationMinutes = 55,
            isCustom = false,
            isAiGenerated = false
        )
    )
    planDao.insertPlanExercises(
        listOf(
            PlanExercise(planId = legsPlanId, exerciseName = "Barbell Back Squat", targetMuscle = "Quads", sets = 4, reps = "6-8", defaultWeightKg = 90f, restSeconds = 120, orderIndex = 0),
            PlanExercise(planId = legsPlanId, exerciseName = "Romanian Deadlift (RDL)", targetMuscle = "Hamstrings", sets = 3, reps = "8-10", defaultWeightKg = 75f, restSeconds = 90, orderIndex = 1),
            PlanExercise(planId = legsPlanId, exerciseName = "Leg Press", targetMuscle = "Quads", sets = 3, reps = "10-12", defaultWeightKg = 140f, restSeconds = 75, orderIndex = 2),
            PlanExercise(planId = legsPlanId, exerciseName = "Standing Calf Raises", targetMuscle = "Calves", sets = 4, reps = "15", defaultWeightKg = 50f, restSeconds = 45, orderIndex = 3),
            PlanExercise(planId = legsPlanId, exerciseName = "Hanging Leg Raises", targetMuscle = "Core", sets = 3, reps = "12-15", defaultWeightKg = 0f, restSeconds = 60, orderIndex = 4)
        )
    )

    // Plan 4: Full Body Home HIIT & Mobility
    val homePlanId = planDao.insertPlan(
        WorkoutPlan(
            title = "Home Athlete HIIT & Core",
            description = "High intensity bodyweight and cardio circuit to burn fat and boost VO2 max anywhere.",
            category = "HIIT",
            level = "Beginner",
            daysPerWeek = 3,
            estimatedDurationMinutes = 30,
            isCustom = false,
            isAiGenerated = false
        )
    )
    planDao.insertPlanExercises(
        listOf(
            PlanExercise(planId = homePlanId, exerciseName = "Push-Ups", targetMuscle = "Chest", sets = 3, reps = "15-20", defaultWeightKg = 0f, restSeconds = 45, orderIndex = 0),
            PlanExercise(planId = homePlanId, exerciseName = "Burpees", targetMuscle = "Full Body", sets = 3, reps = "12", defaultWeightKg = 0f, restSeconds = 45, orderIndex = 1),
            PlanExercise(planId = homePlanId, exerciseName = "Plank Hold", targetMuscle = "Core", sets = 3, reps = "45s", defaultWeightKg = 0f, restSeconds = 45, orderIndex = 2),
            PlanExercise(planId = homePlanId, exerciseName = "Kettlebell Swings", targetMuscle = "Full Body", sets = 4, reps = "20", defaultWeightKg = 16f, restSeconds = 45, orderIndex = 3)
        )
    )

    // 4. Sample Completed Workout Log so user starts with realistic stats & charts
    val logDao = database.workoutLogDao()
    val now = System.currentTimeMillis()
    val dayMillis = 24 * 60 * 60 * 1000L

    val log1Id = logDao.insertLog(
        WorkoutLog(
            planTitle = "Push Day Power & Hypertrophy",
            startedAt = now - (dayMillis * 3) - 3000000,
            completedAt = now - (dayMillis * 3),
            durationSeconds = 2700,
            totalVolumeKg = 3850f,
            exercisesCompleted = 5,
            setsCompleted = 17,
            userNotes = "Great bench session, hit 70kg easily for 8 reps!"
        )
    )
    logDao.insertLogSets(
        listOf(
            WorkoutLogSet(workoutLogId = log1Id, exerciseName = "Barbell Bench Press", setNumber = 1, weightKg = 70f, reps = 8),
            WorkoutLogSet(workoutLogId = log1Id, exerciseName = "Barbell Bench Press", setNumber = 2, weightKg = 70f, reps = 8),
            WorkoutLogSet(workoutLogId = log1Id, exerciseName = "Barbell Bench Press", setNumber = 3, weightKg = 72.5f, reps = 6),
            WorkoutLogSet(workoutLogId = log1Id, exerciseName = "Incline Dumbbell Press", setNumber = 1, weightKg = 24f, reps = 10)
        )
    )

    val log2Id = logDao.insertLog(
        WorkoutLog(
            planTitle = "Pull Day Titan Back & Arms",
            startedAt = now - (dayMillis * 1) - 3200000,
            completedAt = now - (dayMillis * 1),
            durationSeconds = 3100,
            totalVolumeKg = 4600f,
            exercisesCompleted = 5,
            setsCompleted = 16,
            userNotes = "Deadlifts felt super clean. Grip was strong."
        )
    )
    logDao.insertLogSets(
        listOf(
            WorkoutLogSet(workoutLogId = log2Id, exerciseName = "Deadlift (Conventional)", setNumber = 1, weightKg = 110f, reps = 5),
            WorkoutLogSet(workoutLogId = log2Id, exerciseName = "Deadlift (Conventional)", setNumber = 2, weightKg = 115f, reps = 5),
            WorkoutLogSet(workoutLogId = log2Id, exerciseName = "Barbell Bent-Over Row", setNumber = 1, weightKg = 60f, reps = 10)
        )
    )
}
