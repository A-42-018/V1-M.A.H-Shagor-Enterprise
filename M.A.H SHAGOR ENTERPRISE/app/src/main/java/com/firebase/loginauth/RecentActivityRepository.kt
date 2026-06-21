package com.firebase.loginauth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecentActivityRepository private constructor(private val context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("recent_activities", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()
    
    private val _recentActivities = MutableStateFlow<List<RecentActivityEntry>>(emptyList())
    val recentActivities: StateFlow<List<RecentActivityEntry>> = _recentActivities.asStateFlow()
    
    companion object {
        private const val ACTIVITIES_KEY = "activities_list"
        private const val MAX_ACTIVITIES = 50 // Keep last 50 activities
        private const val TAG = "RecentActivityRepo"
        
        @Volatile
        private var INSTANCE: RecentActivityRepository? = null
        
        fun getInstance(context: Context): RecentActivityRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecentActivityRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Get user-specific key for SharedPreferences
    private fun getUserSpecificKey(baseKey: String): String {
        val user = auth.currentUser
        return if (user != null) {
            "${baseKey}_${user.uid}"
        } else {
            baseKey // Fallback to base key if no user
        }
    }
    
    init {
        loadLocalActivities()
        CoroutineScope(Dispatchers.IO).launch {
            syncWithFirestore()
        }
    }
    
    // Add new activity
    suspend fun addActivity(activity: RecentActivityEntry) {
        try {
            val currentActivities = _recentActivities.value.toMutableList()
            
            // Add new activity at the beginning
            currentActivities.add(0, activity)
            
            // Keep only the latest MAX_ACTIVITIES
            if (currentActivities.size > MAX_ACTIVITIES) {
                currentActivities.removeAt(currentActivities.size - 1)
            }
            
            // Update StateFlow
            _recentActivities.value = currentActivities
            
            // Save locally
            saveLocalActivities(currentActivities)
            
            // Save to Firestore if user is authenticated
            saveToFirestore(activity)
            
            Log.d(TAG, "Activity added: ${activity.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding activity: ${e.message}")
        }
    }
    
    // Load activities from SharedPreferences
    private fun loadLocalActivities() {
        try {
            val activitiesJson = sharedPreferences.getString(getUserSpecificKey(ACTIVITIES_KEY), "[]")
            val type = object : TypeToken<List<RecentActivityEntry>>() {}.type
            val activities: List<RecentActivityEntry> = gson.fromJson(activitiesJson, type) ?: emptyList()
            
            // Sort by timestamp (newest first)
            val sortedActivities = activities.sortedByDescending { it.timestamp }
            _recentActivities.value = sortedActivities
            
            Log.d(TAG, "Loaded ${activities.size} activities from local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local activities: ${e.message}")
            _recentActivities.value = emptyList()
        }
    }
    
    // Save activities to SharedPreferences
    private fun saveLocalActivities(activities: List<RecentActivityEntry>) {
        try {
            val activitiesJson = gson.toJson(activities)
            sharedPreferences.edit()
                .putString(getUserSpecificKey(ACTIVITIES_KEY), activitiesJson)
                .apply()
            
            Log.d(TAG, "Saved ${activities.size} activities to local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local activities: ${e.message}")
        }
    }
    
    // Save activity to Firestore
    private suspend fun saveToFirestore(activity: RecentActivityEntry) {
        try {
            val user = auth.currentUser
            if (user != null) {
                firestore.collection("users")
                    .document(user.uid)
                    .collection("recent_activities")
                    .document(activity.id)
                    .set(activity)
                    .await()
                
                Log.d(TAG, "Activity saved to Firestore: ${activity.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to Firestore: ${e.message}")
        }
    }
    
    // Sync with Firestore
    internal suspend fun syncWithFirestore() {
        try {
            val user = auth.currentUser
            if (user != null) {
                val snapshot = firestore.collection("users")
                    .document(user.uid)
                    .collection("recent_activities")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(MAX_ACTIVITIES.toLong())
                    .get()
                    .await()
                
                val firestoreActivities = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(RecentActivityEntry::class.java)
                }
                
                // Merge with local activities
                val mergedActivities = mergeActivities(firestoreActivities)
                
                _recentActivities.value = mergedActivities
                saveLocalActivities(mergedActivities)
                
                Log.d(TAG, "Synced ${firestoreActivities.size} activities from Firestore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with Firestore: ${e.message}")
        }
    }
    
    // Merge local and Firestore activities
    private fun mergeActivities(firestoreActivities: List<RecentActivityEntry>): List<RecentActivityEntry> {
        val localActivities = _recentActivities.value
        val allActivities = (localActivities + firestoreActivities).distinctBy { it.id }
        
        return allActivities
            .sortedByDescending { it.timestamp }
            .take(MAX_ACTIVITIES)
    }
    
    // Get recent activities for dashboard (last 5)
    fun getRecentActivitiesForDashboard(): List<RecentActivityEntry> {
        return _recentActivities.value.take(5)
    }
    
    // Convert to Transaction list for existing UI
    fun getTransactionsForDashboard(): List<Transaction> {
        return getRecentActivitiesForDashboard().map { it.toTransaction() }
    }
    
    // Clear all activities (for testing/reset)
    suspend fun clearAllActivities() {
        try {
            _recentActivities.value = emptyList()
            saveLocalActivities(emptyList())
            
            val user = auth.currentUser
            if (user != null) {
                val batch = firestore.batch()
                val snapshot = firestore.collection("users")
                    .document(user.uid)
                    .collection("recent_activities")
                    .get()
                    .await()
                
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                
                batch.commit().await()
            }
            
            Log.d(TAG, "All activities cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing activities: ${e.message}")
        }
    }
}
