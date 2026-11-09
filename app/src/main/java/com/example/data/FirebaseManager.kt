package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import java.util.UUID

data class FirebaseUserStats(
    val uid: String,
    val email: String,
    val isRealFirebase: Boolean
)

data class MultiplayerMatchStats(
    val matchId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val commanderName: String = "VS_Commander",
    val commanderFaction: String = "north_union",
    val victories: Int = 0,
    val defeats: Int = 0,
    val totalRounds: Int = 0,
    val averageLatencyMs: Double = 0.0,
    val outcome: String = "VICTORY"
)

data class FirebaseTacticalProgress(
    val slotId: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val commanderName: String = "",
    val commanderFaction: String = "",
    val credits: Int = 0,
    val alloys: Int = 0,
    val population: Long = 0,
    val regionsCount: Int = 0,
    val techsCount: Int = 0
)

class FirebaseManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "FirebaseManager"
        
        @Volatile
        private var instance: FirebaseManager? = null
        
        fun getInstance(context: Context): FirebaseManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var isFirebaseInitialized = false
    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseFirestore: FirebaseFirestore? = null

    // Local state variables for simulated mode
    private var simulatedUser: FirebaseUserStats? = null
    private val simulatedMatches = mutableListOf<MultiplayerMatchStats>()
    private val simulatedProgressList = mutableListOf<FirebaseTacticalProgress>()

    init {
        try {
            val apiKey = BuildConfig.FIREBASE_API_KEY
            val projectId = BuildConfig.FIREBASE_PROJECT_ID
            val appId = BuildConfig.FIREBASE_APP_ID

            // Check if user has populated their real secrets in .env
            if (!apiKey.isNullOrEmpty() && apiKey != "YOUR_FIREBASE_API_KEY" && 
                !projectId.isNullOrEmpty() && projectId != "YOUR_FIREBASE_PROJECT_ID" && 
                !appId.isNullOrEmpty() && appId != "YOUR_FIREBASE_APP_ID") {
                
                Log.d(TAG, "Initializing real Firebase dynamically with custom options...")
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .build()

                FirebaseApp.initializeApp(context, options)
                firebaseAuth = FirebaseAuth.getInstance()
                firebaseFirestore = FirebaseFirestore.getInstance()
                isFirebaseInitialized = true
                Log.d(TAG, "Real Firebase initialization succeeded!")
            } else {
                Log.d(TAG, "No valid Firebase credentials found. Running in low-latency simulated mode.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dynamic Firebase initialization failed, falling back to simulated mode.", e)
        }
        
        // Add some default mock logs/matches for a rich initial dashboard
        addMockInitialData()
    }

    fun isRealFirebaseActive(): Boolean = isFirebaseInitialized

    private fun addMockInitialData() {
        simulatedMatches.add(
            MultiplayerMatchStats(
                matchId = "MATCH-${UUID.randomUUID().toString().substring(0, 8).uppercase()}",
                timestamp = System.currentTimeMillis() - 86400000 * 2,
                commanderName = "VS_Commander",
                commanderFaction = "north_union",
                victories = 3,
                defeats = 1,
                totalRounds = 4,
                averageLatencyMs = 28.4,
                outcome = "VICTORY"
            )
        )
        simulatedMatches.add(
            MultiplayerMatchStats(
                matchId = "MATCH-${UUID.randomUUID().toString().substring(0, 8).uppercase()}",
                timestamp = System.currentTimeMillis() - 3600000 * 6,
                commanderName = "VS_Commander",
                commanderFaction = "north_union",
                victories = 1,
                defeats = 3,
                totalRounds = 4,
                averageLatencyMs = 32.1,
                outcome = "DEFEAT"
            )
        )
    }

    // --- AUTHENTICATION ACTIONS ---
    
    suspend fun registerUser(email: String, password: String): FirebaseUserStats {
        if (isFirebaseInitialized) {
            val result = firebaseAuth!!.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed: No user returned")
            return FirebaseUserStats(user.uid, user.email ?: email, true)
        } else {
            // Simulated user creation
            delay(800)
            val uid = "sim-uid-" + UUID.randomUUID().toString().substring(0, 10)
            val user = FirebaseUserStats(uid, email, false)
            simulatedUser = user
            return user
        }
    }

    suspend fun loginUser(email: String, password: String): FirebaseUserStats {
        if (isFirebaseInitialized) {
            val result = firebaseAuth!!.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Login failed: No user returned")
            return FirebaseUserStats(user.uid, user.email ?: email, true)
        } else {
            // Simulated login
            delay(600)
            val uid = "sim-uid-1948204"
            val user = FirebaseUserStats(uid, email, false)
            simulatedUser = user
            return user
        }
    }

    fun logoutUser() {
        if (isFirebaseInitialized) {
            firebaseAuth?.signOut()
        } else {
            simulatedUser = null
        }
    }

    fun getCurrentUser(): FirebaseUserStats? {
        if (isFirebaseInitialized) {
            val current = firebaseAuth?.currentUser
            return if (current != null) {
                FirebaseUserStats(current.uid, current.email ?: "", true)
            } else {
                null
            }
        } else {
            return simulatedUser
        }
    }

    // --- FIRESTORE ACTIONS ---

    suspend fun saveTacticalProgress(progress: FirebaseTacticalProgress) {
        if (isFirebaseInitialized) {
            val currentUser = getCurrentUser() ?: throw Exception("Authentication required to save progress.")
            firebaseFirestore!!.collection("users")
                .document(currentUser.uid)
                .collection("tactical_progress")
                .document("slot_${progress.slotId}")
                .set(progress)
                .await()
        } else {
            // Simulated save
            delay(500)
            simulatedProgressList.removeAll { it.slotId == progress.slotId }
            simulatedProgressList.add(progress)
        }
    }

    suspend fun loadAllTacticalProgress(): List<FirebaseTacticalProgress> {
        if (isFirebaseInitialized) {
            val currentUser = getCurrentUser() ?: return emptyList()
            val snapshot = firebaseFirestore!!.collection("users")
                .document(currentUser.uid)
                .collection("tactical_progress")
                .get()
                .await()
            return snapshot.toObjects(FirebaseTacticalProgress::class.java)
        } else {
            delay(400)
            return simulatedProgressList.toList()
        }
    }

    suspend fun saveMultiplayerMatchStats(stats: MultiplayerMatchStats) {
        val finalStats = if (stats.matchId.isEmpty()) {
            stats.copy(matchId = "MATCH-" + UUID.randomUUID().toString().substring(0, 8).uppercase())
        } else {
            stats
        }

        if (isFirebaseInitialized) {
            val currentUser = getCurrentUser()
            val docRef = if (currentUser != null) {
                firebaseFirestore!!.collection("users")
                    .document(currentUser.uid)
                    .collection("match_statistics")
                    .document(finalStats.matchId)
            } else {
                firebaseFirestore!!.collection("anonymous_match_statistics")
                    .document(finalStats.matchId)
            }
            docRef.set(finalStats).await()
        } else {
            // Simulated save
            delay(300)
            simulatedMatches.add(finalStats)
        }
    }

    suspend fun loadAllMultiplayerMatchStats(): List<MultiplayerMatchStats> {
        if (isFirebaseInitialized) {
            val currentUser = getCurrentUser()
            val query = if (currentUser != null) {
                firebaseFirestore!!.collection("users")
                    .document(currentUser.uid)
                    .collection("match_statistics")
            } else {
                firebaseFirestore!!.collection("anonymous_match_statistics")
            }
            val snapshot = query.get().await()
            return snapshot.toObjects(MultiplayerMatchStats::class.java).sortedByDescending { it.timestamp }
        } else {
            delay(300)
            return simulatedMatches.sortedByDescending { it.timestamp }
        }
    }
}
