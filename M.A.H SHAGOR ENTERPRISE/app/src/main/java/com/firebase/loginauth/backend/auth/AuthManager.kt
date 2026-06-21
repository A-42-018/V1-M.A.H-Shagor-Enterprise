package com.firebase.loginauth.backend.auth

import com.firebase.loginauth.backend.sync.BackgroundSyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val syncManager: BackgroundSyncManager
) {
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated
    
    init {
        // Initialize with current auth state
        _currentUser.value = auth.currentUser
        _isAuthenticated.value = auth.currentUser != null
        
        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            _isAuthenticated.value = user != null
            
            if (user != null) {
                // User signed in, start background sync
                syncManager.startPeriodicSync()
            } else {
                // User signed out, stop background sync
                syncManager.stopPeriodicSync()
            }
        }
    }
    
    // **USER AUTHENTICATION**
    
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Authentication failed")
            
            // Start background sync after successful login
            syncManager.startPeriodicSync()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createUserWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")
            
            // Start background sync after successful registration
            syncManager.startPeriodicSync()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut(): Result<Boolean> {
        return try {
            // Stop background sync before signing out
            syncManager.stopPeriodicSync()
            
            auth.signOut()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePassword(newPassword: String): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: throw Exception("No authenticated user")
            user.updatePassword(newPassword).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateProfile(displayName: String): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: throw Exception("No authenticated user")
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            
            user.updateProfile(profileUpdates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **USER DATA ISOLATION HELPERS**
    
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }
    
    fun getCurrentUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }
    
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    // **BUSINESS-SPECIFIC USER MANAGEMENT**
    
    suspend fun initializeUserBusinessData(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("No authenticated user")
            
            // Trigger initial sync from Firebase to populate local database
            val syncResult = syncManager.performFullSync()
            
            if (syncResult.isSuccess) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to initialize user business data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun clearUserLocalData(): Result<Boolean> {
        return try {
            // This would clear all local database data for the current user
            // Implementation depends on your specific requirements
            // For now, we'll rely on the sync manager to handle data isolation
            
            syncManager.stopPeriodicSync()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
