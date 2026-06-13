package com.example.smartcivic.data.models

enum class UserRole {
    CITIZEN,
    WORKER,
    ADMIN
}

data class UserData(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "CITIZEN",
    val createdAt: Long = System.currentTimeMillis(),
    // Worker specific fields
    val points: Int = 0,
    val issuesSolved: Int = 0,
    val activeTasks: Int = 0,
    val totalRating: Float = 0f,
    val ratingCount: Int = 0,
    val rank: Int = 0,
    val approved: Boolean = true
) {
    val averageRating: Float
        get() = if (ratingCount > 0) totalRating / ratingCount else 0f
}

data class Complaint(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val reportedBy: String = "",
    val reportedByName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "Pending", // Pending, In Progress, Resolved
    val assignedWorkerId: String? = null,
    val assignedWorkerName: String? = null,
    val completionAcceptedAt: Long? = null,
    val workerProofImageUrl: String? = null,
    val workerNotes: String? = null,
    val resolvedAt: Long? = null,
    val citizenRating: Int? = null,
    val citizenReview: String? = null,
    val adminVerified: Boolean = false,
    val adminStatus: String = "Pending", // Pending, Approved, Rejected
    val priority: String = "Medium" // Low, Medium, High
)

data class LeaderboardEntry(
    val uid: String = "",
    val name: String = "",
    val points: Int = 0,
    val issuesSolved: Int = 0,
    val averageRating: Float = 0f,
    val rank: Int = 0
)
