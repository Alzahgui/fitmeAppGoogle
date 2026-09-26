package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.PlanExercise
import com.example.data.model.WorkoutPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeneratedPlanData(
    val title: String,
    val description: String,
    val category: String,
    val level: String,
    val durationMinutes: Int,
    val exercises: List<PlanExerciseItem>
)

data class PlanExerciseItem(
    val exerciseName: String,
    val targetMuscle: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val notes: String = ""
)

object GeminiApiService {
    private const val TAG = "GeminiApiService"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    suspend fun generateWorkoutPlan(
        goal: String,
        fitnessLevel: String,
        daysPerWeek: Int,
        durationMinutes: Int,
        equipment: String,
        targetMuscles: String,
        customNotes: String
    ): GeneratedPlanData = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        val prompt = """
            You are an elite certified strength coach and exercise scientist.
            Create a custom, high-impact workout routine with these parameters:
            - Primary Goal: $goal
            - Fitness Level: $fitnessLevel
            - Target Workout Duration: $durationMinutes minutes
            - Frequency: $daysPerWeek days/week
            - Equipment Available: $equipment
            - Target Muscle Focus: $targetMuscles
            - Additional Notes/Preferences: $customNotes

            Respond ONLY with a valid JSON object matching this exact schema:
            {
              "title": "string (e.g. Iron Shred Full Body)",
              "description": "string (2-3 sentences explaining the stimulus and benefits)",
              "category": "string (one of: Hypertrophy, Strength, HIIT, Fat Loss, Endurance)",
              "level": "$fitnessLevel",
              "durationMinutes": $durationMinutes,
              "exercises": [
                {
                  "exerciseName": "string",
                  "targetMuscle": "string (e.g. Chest, Back, Quads, Hamstrings, Shoulders, Arms, Core)",
                  "sets": 3,
                  "reps": "string (e.g. 8-10, 12, or 45s)",
                  "restSeconds": 60,
                  "notes": "string (1 actionable form cue)"
                }
              ]
            }
            Include 4 to 6 top tier exercises matching the available equipment. Do not include markdown code fence formatting like ```json, just raw JSON.
        """.trimIndent()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "Using smart local fallback for workout generation (no active key)")
            return@withContext createFallbackPlan(goal, fitnessLevel, durationMinutes, equipment, targetMuscles)
        }

        try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contents)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful || bodyString == null) {
                    Log.w(TAG, "Gemini API error code: ${response.code}, falling back")
                    return@withContext createFallbackPlan(goal, fitnessLevel, durationMinutes, equipment, targetMuscles)
                }

                val jsonResponse = JSONObject(bodyString)
                val textCandidate = jsonResponse.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (textCandidate.isNullOrBlank()) {
                    return@withContext createFallbackPlan(goal, fitnessLevel, durationMinutes, equipment, targetMuscles)
                }

                parsePlanJson(textCandidate, goal, fitnessLevel, durationMinutes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini request failed: ${e.message}", e)
            createFallbackPlan(goal, fitnessLevel, durationMinutes, equipment, targetMuscles)
        }
    }

    suspend fun chatWithCoach(
        history: List<ChatMessage>,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalCoachResponse(userMessage)
        }

        try {
            val systemPrompt = "You are Coach Pulse, an elite personal trainer, kinesiologist, and sports nutritionist. You give actionable, motivating, science-backed workout and nutrition advice. Keep explanations crisp, encouraging, and focused on safe biomechanics. Format with clear bullet points where appropriate."

            val contentsArray = JSONArray()

            // Add previous recent messages (up to 6 turns for context efficiency)
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.role == "user") "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", msg.text)
                        })
                    })
                })
            }

            // Current message
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", userMessage)
                    })
                })
            })

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemPrompt)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    return@withContext getLocalCoachResponse(userMessage)
                }

                val jsonResponse = JSONObject(body)
                val textCandidate = jsonResponse.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                textCandidate?.trim() ?: getLocalCoachResponse(userMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Chat failed: ${e.message}", e)
            getLocalCoachResponse(userMessage)
        }
    }

    private fun parsePlanJson(
        jsonString: String,
        goal: String,
        fitnessLevel: String,
        durationMinutes: Int
    ): GeneratedPlanData {
        return try {
            val cleanJson = jsonString
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(cleanJson)
            val title = obj.optString("title", "AI Custom Workout")
            val description = obj.optString("description", "Personalized workout crafted for your specific goals.")
            val category = obj.optString("category", "Hypertrophy")
            val level = obj.optString("level", fitnessLevel)
            val duration = obj.optInt("durationMinutes", durationMinutes)

            val exercisesJson = obj.optJSONArray("exercises") ?: JSONArray()
            val exercises = mutableListOf<PlanExerciseItem>()

            for (i in 0 until exercisesJson.length()) {
                val exObj = exercisesJson.getJSONObject(i)
                exercises.add(
                    PlanExerciseItem(
                        exerciseName = exObj.optString("exerciseName", "Exercise ${i + 1}"),
                        targetMuscle = exObj.optString("targetMuscle", "Full Body"),
                        sets = exObj.optInt("sets", 3),
                        reps = exObj.optString("reps", "10-12"),
                        restSeconds = exObj.optInt("restSeconds", 60),
                        notes = exObj.optString("notes", "Maintain steady cadence.")
                    )
                )
            }

            if (exercises.isEmpty()) {
                createFallbackPlan(goal, fitnessLevel, durationMinutes, "Gym", "Full Body")
            } else {
                GeneratedPlanData(title, description, category, level, duration, exercises)
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error: ${e.message}")
            createFallbackPlan(goal, fitnessLevel, durationMinutes, "Gym", "Full Body")
        }
    }

    private fun createFallbackPlan(
        goal: String,
        fitnessLevel: String,
        durationMinutes: Int,
        equipment: String,
        targetMuscles: String
    ): GeneratedPlanData {
        val isHome = equipment.contains("Home", ignoreCase = true) || equipment.contains("Bodyweight", ignoreCase = true)

        val exercises = if (isHome) {
            listOf(
                PlanExerciseItem("Push-Ups", "Chest", 4, "15-20", 45, "Keep core clamped tight, full range of motion."),
                PlanExerciseItem("Walking Lunges", "Quads", 3, "12 each", 60, "Keep torso upright and knee tracking toes."),
                PlanExerciseItem("Plank Hold", "Core", 3, "45s", 45, "Squeeze glutes and press floor away with elbows."),
                PlanExerciseItem("Burpees", "Full Body", 3, "12", 60, "Pace yourself with smooth breathing transitions."),
                PlanExerciseItem("Bodyweight Squats", "Quads", 3, "20", 45, "Drive through midfoot and reach parallel depth.")
            )
        } else {
            listOf(
                PlanExerciseItem("Barbell Bench Press", "Chest", 4, "8-10", 90, "Retract scapulae and drive through heels."),
                PlanExerciseItem("Barbell Bent-Over Row", "Back", 4, "8-10", 75, "Hinge at 45 degrees, pull with your elbows."),
                PlanExerciseItem("Barbell Back Squat", "Quads", 4, "6-8", 120, "Brace core tightly before each descent."),
                PlanExerciseItem("Overhead Barbell Press", "Shoulders", 3, "8-10", 75, "Lockout overhead without arching lower back."),
                PlanExerciseItem("Romanian Deadlift (RDL)", "Hamstrings", 3, "10-12", 90, "Push hips back until you feel tension in hamstrings.")
            )
        }

        return GeneratedPlanData(
            title = "AI Adaptive: $targetMuscles Power Focus",
            description = "Scientifically balanced routine targeting $targetMuscles tuned for $goal at $fitnessLevel level.",
            category = if (goal.contains("Muscle", ignoreCase = true)) "Hypertrophy" else "Strength",
            level = fitnessLevel,
            durationMinutes = durationMinutes,
            exercises = exercises
        )
    }

    private fun getLocalCoachResponse(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("protein") || lower.contains("diet") || lower.contains("nutrition") -> {
                "💪 **Nutrition Guidance from Coach Pulse:**\n\n" +
                "• **Daily Intake:** Aim for 1.6 to 2.2 grams of protein per kilogram of bodyweight (0.8 - 1.0g per lb).\n" +
                "• **Distribution:** Space your protein across 3–4 meals (approx. 25-40g per meal with ~3g leucine for muscle protein synthesis).\n" +
                "• **Pre/Post Workout:** A moderate meal with 30g carbs and 25g protein 1–2 hours pre-workout ensures optimal glycogen stores and training intensity."
            }
            lower.contains("rest") || lower.contains("recovery") || lower.contains("sleep") -> {
                "⚡ **Recovery Architecture:**\n\n" +
                "• **Rest Between Sets:** 2–3 minutes for heavy compound lifts (Squat, Deadlift, Bench); 60–90 seconds for isolation or hypertrophy work.\n" +
                "• **Sleep:** 7–9 hours of deep sleep is where human growth hormone (HGH) peaks and muscle micro-tears repair.\n" +
                "• **Active Recovery:** On rest days, do a 20-30 min brisk walk or light mobility work to flush lactic accumulation."
            }
            lower.contains("form") || lower.contains("squat") || lower.contains("bench") || lower.contains("deadlift") -> {
                "🏋️‍♂️ **Lifting Biomechanics Essentials:**\n\n" +
                "• **Barbell Bench:** Pin shoulder blades down and back into the bench. Keep wrists straight over elbows. Arch upper back, keep glutes locked.\n" +
                "• **Squats:** Inhale into your diaphragm (Valsalva brace), push knees outward in line with toes, and hit parallel depth.\n" +
                "• **Deadlifts:** Pull the slack out of the bar before pushing the floor away. Bar must stay in contact with shins."
            }
            lower.contains("substitute") || lower.contains("alternative") || lower.contains("swap") -> {
                "🔄 **Smart Exercise Substitutions:**\n\n" +
                "• **No Barbell Bench?** Use Dumbbell Flat Press, Push-Ups with elevation, or Dips.\n" +
                "• **No Pull-Up Bar?** Inverted rows under a sturdy table, dumbbell chest-supported rows, or heavy resistance bands.\n" +
                "• **Lower Back Fatigue?** Swap conventional deadlifts for Romanian Deadlifts or Bulgarian Split Squats to target legs with minimal spinal shear."
            }
            else -> {
                "🔥 **Coach Pulse Strategy Check:**\n\n" +
                "Your consistency is key! For maximum results with your current plan:\n\n" +
                "1. **Track Every Rep & Set:** Progressive overload is king—try to add 1 rep or small weight increment each week.\n" +
                "2. **Mind-Muscle Connection:** Control the eccentric (lowering) phase for 2-3 seconds to recruit more fast-twitch fibers.\n" +
                "3. **Hydration:** Drink at least 3-4 liters of water with electrolytes on workout days.\n\n" +
                "What specific lift, muscle group, or routine question would you like to dial in today?"
            }
        }
    }
}
