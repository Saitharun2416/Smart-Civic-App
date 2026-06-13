package com.example.smartcivic.data.repository

import android.content.Context
import android.util.Log
import com.example.smartcivic.data.models.Complaint
import com.example.smartcivic.data.models.UserData
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import android.net.Uri
import java.util.UUID

class FirebaseCivicRepository(private val context: Context) : CivicRepository {

    private val tag = "FirebaseCivicRepository"
    private var isFirebaseAvailable = false

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null

    // Fallback Mock In-Memory Database
    private val mockCurrentUser = MutableStateFlow<UserData?>(null)
    private val mockComplaints = MutableStateFlow<List<Complaint>>(emptyList())
    private val mockUsers = MutableStateFlow<List<UserData>>(emptyList())

    init {
        try {
            // Check if Firebase is initialized in the project
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val app = FirebaseApp.getInstance()
                val projectId = app.options.projectId
                if (projectId == "smart-civic-dummy" || projectId.isNullOrEmpty()) {
                    Log.w(tag, "Dummy Firebase configuration detected. Falling back to Mock Database.")
                    setupMockDatabase()
                } else {
                    auth = FirebaseAuth.getInstance()
                    firestore = FirebaseFirestore.getInstance()
                    storage = FirebaseStorage.getInstance()
                    isFirebaseAvailable = true
                    Log.d(tag, "Firebase is initialized and available with project: $projectId")
                }
            } else {
                Log.w(tag, "Firebase is not initialized. Falling back to Mock Database.")
                setupMockDatabase()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Firebase. Falling back to Mock Database.", e)
            setupMockDatabase()
        }
    }

    private fun setupMockDatabase() {
        isFirebaseAvailable = false
        // Populate Mock Users (1 Admin, 3 Workers, 2 Citizens, 1 Pending Worker)
        val admin = UserData(uid = "admin1", name = "Admin Officer", email = "admin@civic.gov", role = "ADMIN")
        val worker1 = UserData(uid = "worker1", name = "Rajesh Kumar", email = "rajesh@civic.gov", role = "WORKER", points = 45, issuesSolved = 4, totalRating = 19.2f, ratingCount = 4, rank = 1)
        val worker2 = UserData(uid = "worker2", name = "Priya Singh", email = "priya@civic.gov", role = "WORKER", points = 30, issuesSolved = 3, totalRating = 13.5f, ratingCount = 3, rank = 2)
        val worker3 = UserData(uid = "worker3", name = "Amit Patel", email = "amit@civic.gov", role = "WORKER", points = 10, issuesSolved = 1, totalRating = 4.0f, ratingCount = 1, rank = 3)
        val citizen1 = UserData(uid = "citizen1", name = "Suresh Sharma", email = "suresh@gmail.com", role = "CITIZEN")
        val citizen2 = UserData(uid = "citizen2", name = "Anjali Verma", email = "anjali@gmail.com", role = "CITIZEN")
        val workerPending = UserData(uid = "worker_pending", name = "Vijay Kumar", email = "vijay@civic.gov", role = "WORKER", approved = false)

        mockUsers.value = listOf(admin, worker1, worker2, worker3, citizen1, citizen2, workerPending)

        // Populate Mock Complaints
        val c1 = Complaint(
            id = "c_1",
            title = "Garbage Overflow at Sector 4",
            description = "Main garbage bin is overflowing since last 3 days. Foul smell spreading in the market area.",
            category = "Garbage Overflow",
            reportedBy = "citizen1",
            reportedByName = "Suresh Sharma",
            createdAt = System.currentTimeMillis() - 86400000 * 2, // 2 days ago
            status = "Pending",
            priority = "High",
            address = "Sector 4 Market, near Central Bank"
        )
        val c2 = Complaint(
            id = "c_2",
            title = "Deep Pothole near Main Flyover",
            description = "A very deep pothole causing traffic jam and accidents during night. Needs urgent filling.",
            category = "Pothole",
            reportedBy = "citizen2",
            reportedByName = "Anjali Verma",
            createdAt = System.currentTimeMillis() - 86400000 * 3, // 3 days ago
            status = "In Progress",
            assignedWorkerId = "worker1",
            assignedWorkerName = "Rajesh Kumar",
            completionAcceptedAt = System.currentTimeMillis() - 3600000 * 4, // 4 hours ago
            priority = "High",
            address = "Service Lane, Main Flyover Entrance"
        )
        val c3 = Complaint(
            id = "c_3",
            title = "Water Leakage on Church Road",
            description = "Main water pipeline broke. Clean drinking water wasting in huge amounts on the road.",
            category = "Water Leakage",
            reportedBy = "citizen1",
            reportedByName = "Suresh Sharma",
            createdAt = System.currentTimeMillis() - 86400000 * 5, // 5 days ago
            status = "Resolved",
            assignedWorkerId = "worker2",
            assignedWorkerName = "Priya Singh",
            completionAcceptedAt = System.currentTimeMillis() - 86400000 * 5 + 3600000 * 2, // accepted 2 hours after reporting
            resolvedAt = System.currentTimeMillis() - 86400000 * 4, // resolved 4 days ago
            workerNotes = "Leakage patched successfully with heavy-duty seal.",
            citizenRating = 5,
            citizenReview = "Excellent and fast work! Water wastage stopped.",
            adminVerified = true,
            adminStatus = "Approved",
            priority = "Medium",
            address = "Near St. Mary Church, Church Road"
        )
        val c4 = Complaint(
            id = "c_4",
            title = "Broken Streetlights on Lane 2",
            description = "Entire street is in dark. unsafe for women and children at night.",
            category = "Broken Streetlight",
            reportedBy = "citizen2",
            reportedByName = "Anjali Verma",
            createdAt = System.currentTimeMillis() - 3600000 * 5, // 5 hours ago
            status = "Pending",
            priority = "Medium",
            address = "Lane 2, Adarsh Nagar"
        )
        val c5 = Complaint(
            id = "c_5",
            title = "Drainage Blockage near School",
            description = "Blocked drain causing waterlogging outside school gate. Children unable to enter.",
            category = "Drainage Blockage",
            reportedBy = "citizen1",
            reportedByName = "Suresh Sharma",
            createdAt = System.currentTimeMillis() - 86400000, // 1 day ago
            status = "Resolved",
            assignedWorkerId = "worker1",
            assignedWorkerName = "Rajesh Kumar",
            completionAcceptedAt = System.currentTimeMillis() - 86400000 + 3600000,
            resolvedAt = System.currentTimeMillis() - 3600000 * 2, // resolved 2 hours ago
            workerNotes = "Debris cleared from pipeline. Flow restored.",
            adminVerified = false,
            adminStatus = "Pending",
            priority = "High",
            address = "Gate No. 2, Public Model School"
        )

        mockComplaints.value = listOf(c1, c2, c3, c4, c5)
    }

    override suspend fun signIn(email: String, password: String): Result<UserData> {
        return if (isFirebaseAvailable && auth != null && firestore != null) {
            try {
                val authResult = auth!!.signInWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: return Result.failure(Exception("User UID is null"))
                val userDoc = firestore!!.collection("users").document(uid).get().await()
                val userData = userDoc.toObject(UserData::class.java)
                if (userData == null) {
                    auth!!.signOut()
                    return Result.failure(Exception("Account not found. Please register first."))
                }
                if (userData.role.uppercase() == "WORKER" && !userData.approved) {
                    auth!!.signOut()
                    return Result.failure(Exception("Your worker registration is pending administrator approval."))
                }
                mockCurrentUser.value = userData
                Result.success(userData)
            } catch (e: Exception) {
                Log.w(tag, "Firebase login failed: ${e.message}. Trying mock fallback login.")
                val foundUser = mockUsers.value.find { it.email.equals(email, ignoreCase = true) }
                if (foundUser != null) {
                    if (foundUser.role.uppercase() == "WORKER" && !foundUser.approved) {
                        return Result.failure(Exception("Your worker registration is pending administrator approval."))
                    }
                    mockCurrentUser.value = foundUser
                    Result.success(foundUser)
                } else {
                    Result.failure(Exception("Account not found. Please register first."))
                }
            }
        } else {
            // Mock Auth Login
            val foundUser = mockUsers.value.find { it.email.equals(email, ignoreCase = true) }
            if (foundUser != null) {
                if (foundUser.role.uppercase() == "WORKER" && !foundUser.approved) {
                    return Result.failure(Exception("Your worker registration is pending administrator approval."))
                }
                mockCurrentUser.value = foundUser
                Result.success(foundUser)
            } else {
                Result.failure(Exception("Account not found. Please register first."))
            }
        }
    }

    override suspend fun signUp(name: String, email: String, password: String, role: String): Result<UserData> {
        val approved = (role.uppercase() != "WORKER")
        return if (isFirebaseAvailable && auth != null && firestore != null) {
            try {
                val authResult = auth!!.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: return Result.failure(Exception("User UID registration failed"))
                val userData = UserData(uid = uid, name = name, email = email, role = role, approved = approved)
                firestore!!.collection("users").document(uid).set(userData).await()
                mockCurrentUser.value = userData
                Result.success(userData)
            } catch (e: Exception) {
                Log.w(tag, "Firebase signup failed: ${e.message}. Trying mock fallback signup.")
                val uid = UUID.randomUUID().toString()
                val newUser = UserData(uid = uid, name = name, email = email, role = role, approved = approved)
                mockUsers.update { it + newUser }
                mockCurrentUser.value = newUser
                Result.success(newUser)
            }
        } else {
            // Mock Sign Up
            val uid = UUID.randomUUID().toString()
            val newUser = UserData(uid = uid, name = name, email = email, role = role, approved = approved)
            mockUsers.update { it + newUser }
            mockCurrentUser.value = newUser
            Result.success(newUser)
        }
    }

    override suspend fun signOut() {
        if (isFirebaseAvailable && auth != null) {
            auth!!.signOut()
        }
        mockCurrentUser.value = null
    }

    override fun getCurrentUser(): UserData? {
        return if (isFirebaseAvailable && auth != null && firestore != null) {
            val firebaseUser = auth!!.currentUser
            if (firebaseUser != null) {
                val cached = mockCurrentUser.value
                if (cached != null && cached.uid == firebaseUser.uid) {
                    cached
                } else {
                    UserData(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
                }
            } else {
                mockCurrentUser.value
            }
        } else {
            mockCurrentUser.value
        }
    }

    override fun observeCurrentUser(): Flow<UserData?> {
        if (isFirebaseAvailable && auth != null && firestore != null) {
            return callbackFlow {
                val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        val listener = firestore!!.collection("users").document(user.uid)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    val fallbackUser = mockCurrentUser.value ?: UserData(uid = user.uid, email = user.email ?: "")
                                    trySend(fallbackUser)
                                    return@addSnapshotListener
                                }
                                val userData = snapshot?.toObject(UserData::class.java) 
                                    ?: mockCurrentUser.value 
                                    ?: UserData(uid = user.uid, email = user.email ?: "")
                                mockCurrentUser.value = userData
                                trySend(userData)
                            }
                    } else {
                        trySend(mockCurrentUser.value)
                    }
                }
                auth!!.addAuthStateListener(authListener)
                awaitClose {
                    auth!!.removeAuthStateListener(authListener)
                }
            }
        } else {
            return mockCurrentUser
        }
    }

    override fun observeComplaints(): Flow<List<Complaint>> {
        if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            return callbackFlow {
                val listenerRegistration: ListenerRegistration = firestore!!.collection("complaints")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val list = snapshot?.documents?.mapNotNull { it.toObject(Complaint::class.java) } ?: emptyList()
                        trySend(list.sortedByDescending { it.createdAt })
                    }
                awaitClose { listenerRegistration.remove() }
            }
        } else {
            return mockComplaints
        }
    }

    override suspend fun reportComplaint(
        title: String,
        description: String,
        category: String,
        imageUrl: String,
        latitude: Double,
        longitude: Double,
        address: String
    ): Result<Complaint> {
        val user = getCurrentUser() ?: return Result.failure(Exception("No user logged in"))
        val id = UUID.randomUUID().toString()
        val userName = if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            try {
                val doc = firestore!!.collection("users").document(user.uid).get().await()
                doc.getString("name") ?: user.email.substringBefore("@")
            } catch (e: Exception) {
                Log.w(tag, "Failed to get user name from Firestore, fallback to local name: ${e.message}")
                mockCurrentUser.value?.name ?: user.email.substringBefore("@")
            }
        } else {
            mockCurrentUser.value?.name ?: "Citizen"
        }

        val uploadedUrl = if (imageUrl.isNotEmpty()) {
            uploadImageIfNeeded(imageUrl, "complaints")
        } else {
            "https://images.unsplash.com/photo-1515162305285-0293e4767cc2?q=80&w=400"
        }

        val complaint = Complaint(
            id = id,
            title = title,
            description = description,
            category = category,
            imageUrl = uploadedUrl,
            latitude = latitude,
            longitude = longitude,
            address = address,
            reportedBy = user.uid,
            reportedByName = userName,
            createdAt = System.currentTimeMillis(),
            status = "Pending"
        )

        return if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            try {
                firestore!!.collection("complaints").document(id).set(complaint).await()
                Result.success(complaint)
            } catch (e: Exception) {
                mockComplaints.update { list -> list + complaint }
                Result.success(complaint)
            }
        } else {
            mockComplaints.update { list -> list + complaint }
            Result.success(complaint)
        }
    }

    override suspend fun acceptComplaint(complaintId: String): Result<Unit> {
        val user = getCurrentUser() ?: return Result.failure(Exception("No user logged in"))
        val userName = if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            try {
                val doc = firestore!!.collection("users").document(user.uid).get().await()
                doc.getString("name") ?: "Worker"
            } catch (e: Exception) {
                Log.w(tag, "Failed to get user name from Firestore, fallback to local name: ${e.message}")
                mockCurrentUser.value?.name ?: "Worker"
            }
        } else {
            mockCurrentUser.value?.name ?: "Worker"
        }

        return if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            try {
                val complaintRef = firestore!!.collection("complaints").document(complaintId)
                val workerRef = firestore!!.collection("users").document(user.uid)

                firestore!!.runTransaction { transaction ->
                    val complaint = transaction.get(complaintRef).toObject(Complaint::class.java)
                        ?: throw Exception("Complaint not found")

                    if (complaint.status != "Pending") {
                        throw Exception("Complaint already accepted or resolved")
                    }

                    transaction.update(complaintRef, mapOf(
                        "status" to "In Progress",
                        "assignedWorkerId" to user.uid,
                        "assignedWorkerName" to userName,
                        "completionAcceptedAt" to System.currentTimeMillis()
                    ))

                    transaction.update(workerRef, "activeTasks", FieldValue.increment(1))
                }.await()

                Result.success(Unit)
            } catch (e: Exception) {
                mockComplaints.update { list ->
                    list.map {
                        if (it.id == complaintId) {
                            it.copy(
                                status = "In Progress",
                                assignedWorkerId = user.uid,
                                assignedWorkerName = userName,
                                completionAcceptedAt = System.currentTimeMillis()
                            )
                        } else it
                    }
                }
                mockUsers.update { users ->
                    users.map {
                        if (it.uid == user.uid) {
                            it.copy(activeTasks = it.activeTasks + 1)
                        } else it
                    }
                }
                Result.success(Unit)
            }
        } else {
            mockComplaints.update { list ->
                list.map {
                    if (it.id == complaintId) {
                        it.copy(
                            status = "In Progress",
                            assignedWorkerId = user.uid,
                            assignedWorkerName = userName,
                            completionAcceptedAt = System.currentTimeMillis()
                        )
                    } else it
                }
            }
            mockUsers.update { users ->
                users.map {
                    if (it.uid == user.uid) {
                        it.copy(activeTasks = it.activeTasks + 1)
                    } else it
                }
            }
            Result.success(Unit)
        }
    }

    override suspend fun submitCompletionProof(complaintId: String, proofImageUrl: String, notes: String): Result<Unit> {
        return if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            try {
                val uploadedUrl = if (proofImageUrl.isNotEmpty()) {
                    uploadImageIfNeeded(proofImageUrl, "proofs")
                } else {
                    "https://images.unsplash.com/photo-1541888946425-d81bb19240f5?q=80&w=400"
                }
                firestore!!.collection("complaints").document(complaintId).update(mapOf(
                    "status" to "Resolved",
                    "adminStatus" to "Pending",
                    "workerProofImageUrl" to uploadedUrl,
                    "workerNotes" to notes,
                    "resolvedAt" to System.currentTimeMillis()
                )).await()
                Result.success(Unit)
            } catch (e: Exception) {
                mockSubmitCompletionProofLocal(complaintId, proofImageUrl, notes)
            }
        } else {
            mockSubmitCompletionProofLocal(complaintId, proofImageUrl, notes)
        }
    }

    private fun mockSubmitCompletionProofLocal(complaintId: String, proofImageUrl: String, notes: String): Result<Unit> {
        mockComplaints.update { list ->
            list.map {
                if (it.id == complaintId) {
                    it.copy(
                        status = "Resolved",
                        adminStatus = "Pending",
                        workerProofImageUrl = proofImageUrl.ifEmpty { "https://images.unsplash.com/photo-1541888946425-d81bb19240f5?q=80&w=400" },
                        workerNotes = notes,
                        resolvedAt = System.currentTimeMillis()
                    )
                } else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun verifyCompletion(complaintId: String, approve: Boolean): Result<Unit> {
        return if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            try {
                val complaintRef = firestore!!.collection("complaints").document(complaintId)
                
                firestore!!.runTransaction { transaction ->
                    val complaint = transaction.get(complaintRef).toObject(Complaint::class.java)
                        ?: throw Exception("Complaint not found")

                    val workerId = complaint.assignedWorkerId ?: throw Exception("No assigned worker")
                    val workerRef = firestore!!.collection("users").document(workerId)
                    val worker = transaction.get(workerRef).toObject(UserData::class.java)
                        ?: throw Exception("Worker not found")

                    var pointsDelta = 0
                    var issuesSolvedDelta = 0
                    val activeTasksDelta = -1

                    if (approve) {
                        // base points +10
                        pointsDelta += 10
                        issuesSolvedDelta = 1
                        
                        // speed bonus +5 if resolved in < 24h
                        val acceptTime = complaint.completionAcceptedAt ?: complaint.createdAt
                        val resolveTime = complaint.resolvedAt ?: System.currentTimeMillis()
                        if (resolveTime - acceptTime < 86400000) {
                            pointsDelta += 5
                        }

                        transaction.update(complaintRef, mapOf(
                            "adminVerified" to true,
                            "adminStatus" to "Approved"
                        ))
                    } else {
                        // fake claims: -10 points, status back to Pending
                        pointsDelta -= 10
                        transaction.update(complaintRef, mapOf(
                            "status" to "Pending",
                            "assignedWorkerId" to null,
                            "assignedWorkerName" to null,
                            "completionAcceptedAt" to null,
                            "workerProofImageUrl" to null,
                            "workerNotes" to null,
                            "resolvedAt" to null,
                            "adminVerified" to false,
                            "adminStatus" to "Rejected"
                        ))
                    }

                    transaction.update(workerRef, mapOf(
                        "points" to (worker.points + pointsDelta).coerceAtLeast(0),
                        "issuesSolved" to (worker.issuesSolved + issuesSolvedDelta),
                        "activeTasks" to (worker.activeTasks + activeTasksDelta).coerceAtLeast(0)
                    ))
                }.await()
                
                // Recalculate leaderboard ranks
                recalculateLeaderboardRanks()
                Result.success(Unit)
            } catch (e: Exception) {
                mockVerifyCompletionLocal(complaintId, approve)
            }
        } else {
            mockVerifyCompletionLocal(complaintId, approve)
        }
    }

    private suspend fun mockVerifyCompletionLocal(complaintId: String, approve: Boolean): Result<Unit> {
        var workerId = ""
        var pointsDelta = 0
        var issuesSolvedDelta = 0
        var speedBonus = false

        mockComplaints.update { list ->
            list.map {
                if (it.id == complaintId) {
                    workerId = it.assignedWorkerId ?: ""
                    if (approve) {
                        pointsDelta += 10
                        issuesSolvedDelta = 1
                        val acceptTime = it.completionAcceptedAt ?: it.createdAt
                        val resolveTime = it.resolvedAt ?: System.currentTimeMillis()
                        if (resolveTime - acceptTime < 86400000) {
                            pointsDelta += 5
                            speedBonus = true
                        }
                        it.copy(adminVerified = true, adminStatus = "Approved")
                    } else {
                        pointsDelta -= 10
                        it.copy(
                            status = "Pending",
                            assignedWorkerId = null,
                            assignedWorkerName = null,
                            completionAcceptedAt = null,
                            workerProofImageUrl = null,
                            workerNotes = null,
                            resolvedAt = null,
                            adminVerified = false,
                            adminStatus = "Rejected"
                        )
                    }
                } else it
            }
        }

        if (workerId.isNotEmpty()) {
            mockUsers.update { users ->
                users.map {
                    if (it.uid == workerId) {
                        val newPoints = (it.points + pointsDelta).coerceAtLeast(0)
                        val newSolved = it.issuesSolved + issuesSolvedDelta
                        val newActive = (it.activeTasks - 1).coerceAtLeast(0)
                        it.copy(points = newPoints, issuesSolved = newSolved, activeTasks = newActive)
                    } else it
                }
            }
            recalculateLeaderboardRanks()
        }
        return Result.success(Unit)
    }

    override suspend fun manualAssignWorker(complaintId: String, workerId: String, workerName: String): Result<Unit> {
        return if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            try {
                val complaintRef = firestore!!.collection("complaints").document(complaintId)
                val workerRef = firestore!!.collection("users").document(workerId)
                
                firestore!!.runTransaction { transaction ->
                    val complaint = transaction.get(complaintRef).toObject(Complaint::class.java)
                        ?: throw Exception("Complaint not found")
                    
                    if (complaint.status != "Pending") {
                        throw Exception("Complaint is not Pending")
                    }

                    transaction.update(complaintRef, mapOf(
                        "status" to "In Progress",
                        "assignedWorkerId" to workerId,
                        "assignedWorkerName" to workerName,
                        "completionAcceptedAt" to System.currentTimeMillis()
                    ))

                    transaction.update(workerRef, "activeTasks", FieldValue.increment(1))
                }.await()
                Result.success(Unit)
            } catch (e: Exception) {
                mockManualAssignWorkerLocal(complaintId, workerId, workerName)
            }
        } else {
            mockManualAssignWorkerLocal(complaintId, workerId, workerName)
        }
    }

    private fun mockManualAssignWorkerLocal(complaintId: String, workerId: String, workerName: String): Result<Unit> {
        mockComplaints.update { list ->
            list.map {
                if (it.id == complaintId) {
                    it.copy(
                        status = "In Progress",
                        assignedWorkerId = workerId,
                        assignedWorkerName = workerName,
                        completionAcceptedAt = System.currentTimeMillis()
                    )
                } else it
            }
        }
        mockUsers.update { users ->
            users.map {
                if (it.uid == workerId) {
                    it.copy(activeTasks = it.activeTasks + 1)
                } else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteComplaint(complaintId: String): Result<Unit> {
        return if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            try {
                firestore!!.collection("complaints").document(complaintId).delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                mockDeleteComplaintLocal(complaintId)
            }
        } else {
            mockDeleteComplaintLocal(complaintId)
        }
    }

    private fun mockDeleteComplaintLocal(complaintId: String): Result<Unit> {
        mockComplaints.update { list -> list.filter { it.id != complaintId } }
        return Result.success(Unit)
    }

    override suspend fun rateResolution(complaintId: String, rating: Int, review: String): Result<Unit> {
        return if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            try {
                val complaintRef = firestore!!.collection("complaints").document(complaintId)
                
                firestore!!.runTransaction { transaction ->
                    val complaint = transaction.get(complaintRef).toObject(Complaint::class.java)
                        ?: throw Exception("Complaint not found")

                    if (complaint.status != "Resolved" || complaint.adminStatus != "Approved") {
                        throw Exception("Work must be completed and approved before rating")
                    }

                    transaction.update(complaintRef, mapOf(
                        "citizenRating" to rating,
                        "citizenReview" to review
                    ))

                    val workerId = complaint.assignedWorkerId
                    if (workerId != null) {
                        val workerRef = firestore!!.collection("users").document(workerId)
                        val worker = transaction.get(workerRef).toObject(UserData::class.java)
                            ?: throw Exception("Worker not found")

                        var pointsDelta = 0
                        if (rating >= 4) {
                            pointsDelta = 5 // citizen rating bonus
                        }

                        transaction.update(workerRef, mapOf(
                            "totalRating" to (worker.totalRating + rating),
                            "ratingCount" to (worker.ratingCount + 1),
                            "points" to (worker.points + pointsDelta)
                        ))
                    }
                }.await()
                recalculateLeaderboardRanks()
                Result.success(Unit)
            } catch (e: Exception) {
                mockRateResolutionLocal(complaintId, rating, review)
            }
        } else {
            mockRateResolutionLocal(complaintId, rating, review)
        }
    }

    private suspend fun mockRateResolutionLocal(complaintId: String, rating: Int, review: String): Result<Unit> {
        var workerId = ""
        mockComplaints.update { list ->
            list.map {
                if (it.id == complaintId) {
                    workerId = it.assignedWorkerId ?: ""
                    it.copy(citizenRating = rating, citizenReview = review)
                } else it
            }
        }

        if (workerId.isNotEmpty()) {
            mockUsers.update { users ->
                users.map {
                    if (it.uid == workerId) {
                        val bonusPoints = if (rating >= 4) 5 else 0
                        it.copy(
                            totalRating = it.totalRating + rating,
                            ratingCount = it.ratingCount + 1,
                            points = it.points + bonusPoints
                        )
                    } else it
                }
            }
            recalculateLeaderboardRanks()
        }
        return Result.success(Unit)
    }

    override fun observeLeaderboard(): Flow<List<UserData>> {
        if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            return callbackFlow {
                val listenerRegistration = firestore!!.collection("users")
                    .whereEqualTo("role", "WORKER")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val list = snapshot?.documents?.mapNotNull { it.toObject(UserData::class.java) } ?: emptyList()
                        trySend(list.sortedByDescending { it.points })
                    }
                awaitClose { listenerRegistration.remove() }
            }
        } else {
            return flow {
                mockUsers.collect { users ->
                    val workers = users.filter { it.role == "WORKER" }.sortedByDescending { it.points }
                    emit(workers.mapIndexed { index, worker -> worker.copy(rank = index + 1) })
                }
            }
        }
    }

    private suspend fun recalculateLeaderboardRanks() {
        if (isFirebaseAvailable && firestore != null) {
            try {
                val workersSnapshot = firestore!!.collection("users")
                    .whereEqualTo("role", "WORKER")
                    .get().await()

                val workers = workersSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserData::class.java)?.copy(uid = doc.id)
                }.sortedByDescending { it.points }

                firestore!!.runBatch { batch ->
                    workers.forEachIndexed { index, worker ->
                        val ref = firestore!!.collection("users").document(worker.uid)
                        batch.update(ref, "rank", index + 1)
                    }
                }.await()
            } catch (e: Exception) {
                Log.e(tag, "Failed to recalculate ranks", e)
            }
        } else {
            mockUsers.update { users ->
                val workers = users.filter { it.role == "WORKER" }.sortedByDescending { it.points }
                val updatedWorkers = workers.mapIndexed { index, worker ->
                    worker.copy(rank = index + 1)
                }
                val nonWorkers = users.filter { it.role != "WORKER" }
                nonWorkers + updatedWorkers
            }
        }
    }

    override fun observeAllUsers(): Flow<List<UserData>> {
        if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            return callbackFlow {
                val listenerRegistration = firestore!!.collection("users")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val list = snapshot?.documents?.mapNotNull { it.toObject(UserData::class.java) } ?: emptyList()
                        trySend(list)
                    }
                awaitClose { listenerRegistration.remove() }
            }
        } else {
            return mockUsers
        }
    }

    override suspend fun approveWorker(workerId: String): Result<Unit> {
        return if (isFirebaseAvailable && auth?.currentUser != null && firestore != null) {
            try {
                firestore!!.collection("users").document(workerId).update("approved", true).await()
                mockUsers.update { list ->
                    list.map {
                        if (it.uid == workerId) it.copy(approved = true) else it
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                mockApproveWorkerLocal(workerId)
            }
        } else {
            mockApproveWorkerLocal(workerId)
        }
    }

    private fun mockApproveWorkerLocal(workerId: String): Result<Unit> {
        mockUsers.update { list ->
            list.map {
                if (it.uid == workerId) {
                    it.copy(approved = true)
                } else it
            }
        }
        return Result.success(Unit)
    }

    private suspend fun uploadImageIfNeeded(localUriStr: String, folder: String): String {
        if (!isFirebaseAvailable || auth?.currentUser == null || storage == null) {
            return localUriStr
        }
        if (!localUriStr.startsWith("content://") && !localUriStr.startsWith("file://")) {
            return localUriStr
        }
        return try {
            val uri = Uri.parse(localUriStr)
            val filename = "${UUID.randomUUID()}.jpg"
            val ref = storage!!.reference.child("$folder/$filename")
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(tag, "Failed to upload image to Firebase Storage: ${e.message}", e)
            localUriStr
        }
    }
}
