package com.example.smartcivic.data.repository

import com.example.smartcivic.data.models.UserData
import com.example.smartcivic.data.models.Complaint
import kotlinx.coroutines.flow.Flow

interface CivicRepository {
    // Auth operations
    suspend fun signIn(email: String, password: String): Result<UserData>
    suspend fun signUp(name: String, email: String, password: String, role: String): Result<UserData>
    suspend fun signOut()
    fun getCurrentUser(): UserData?
    fun observeCurrentUser(): Flow<UserData?>

    // Complaint operations
    fun observeComplaints(): Flow<List<Complaint>>
    suspend fun reportComplaint(
        title: String,
        description: String,
        category: String,
        imageUrl: String,
        latitude: Double,
        longitude: Double,
        address: String
    ): Result<Complaint>
    suspend fun acceptComplaint(complaintId: String): Result<Unit>
    suspend fun submitCompletionProof(complaintId: String, proofImageUrl: String, notes: String): Result<Unit>
    suspend fun rateResolution(complaintId: String, rating: Int, review: String): Result<Unit>

    // Admin operations
    suspend fun verifyCompletion(complaintId: String, approve: Boolean): Result<Unit>
    suspend fun manualAssignWorker(complaintId: String, workerId: String, workerName: String): Result<Unit>
    suspend fun deleteComplaint(complaintId: String): Result<Unit>
    suspend fun approveWorker(workerId: String): Result<Unit>

    // Leaderboard operations
    fun observeLeaderboard(): Flow<List<UserData>>
    fun observeAllUsers(): Flow<List<UserData>>
}
