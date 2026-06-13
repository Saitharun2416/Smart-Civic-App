import { initializeApp, getApps, getApp } from "firebase/app";
import { 
  getAuth, 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  signOut as firebaseSignOut, 
  onAuthStateChanged,
  User as FirebaseUser
} from "firebase/auth";
import { 
  getFirestore, 
  collection, 
  doc, 
  getDoc, 
  setDoc, 
  updateDoc, 
  onSnapshot, 
  increment,
  runTransaction,
  deleteDoc
} from "firebase/firestore";
import { getStorage, ref as storageRef, uploadBytes, getDownloadURL } from "firebase/storage";
import { UserData, Complaint } from "./models";

// Firebase Configuration from google-services.json
const firebaseConfig = {
  apiKey: "AIzaSyAU4LG0rDp_da_sAaox5nUL46oUVGnZlyE",
  authDomain: "smart-civic-governance.firebaseapp.com",
  projectId: "smart-civic-governance",
  storageBucket: "smart-civic-governance.firebasestorage.app",
  messagingSenderId: "1084976579634"
};

let isFirebaseAvailable = false;
let auth: any = null;
let db: any = null;
let storage: any = null;

try {
  const isMockParam = typeof window !== "undefined" && window.location.search.includes("mock=true");
  if (isMockParam) {
    console.log("Forcing mock database mode via url query parameter.");
  } else {
    const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();
    auth = getAuth(app);
    db = getFirestore(app);
    storage = getStorage(app);
    isFirebaseAvailable = true;
    console.log("Firebase initialized successfully for web client.");
  }
} catch (e) {
  console.warn("Firebase failed to initialize. Falling back to localStorage mock database.", e);
}

// Helper to generate UUIDs
function generateUUID() {
  return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
}

// ---------------- LOCAL DATABASE SEEDING ----------------
const initialMockUsers: UserData[] = [
  { uid: "admin1", name: "Admin Officer", email: "admin@civic.gov", role: "ADMIN", points: 0, issuesSolved: 0, activeTasks: 0, totalRating: 0, ratingCount: 0, rank: 1, approved: true },
  { uid: "worker1", name: "Rajesh Kumar", email: "rajesh@civic.gov", role: "WORKER", points: 45, issuesSolved: 4, activeTasks: 1, totalRating: 19.2, ratingCount: 4, rank: 1, approved: true },
  { uid: "worker2", name: "Priya Singh", email: "priya@civic.gov", role: "WORKER", points: 30, issuesSolved: 3, activeTasks: 0, totalRating: 13.5, ratingCount: 3, rank: 2, approved: true },
  { uid: "worker3", name: "Amit Patel", email: "amit@civic.gov", role: "WORKER", points: 10, issuesSolved: 1, activeTasks: 0, totalRating: 4.0, ratingCount: 1, rank: 3, approved: true },
  { uid: "citizen1", name: "Suresh Sharma", email: "suresh@gmail.com", role: "CITIZEN", points: 10, issuesSolved: 0, activeTasks: 0, totalRating: 0, ratingCount: 0, rank: 1, approved: true },
  { uid: "citizen2", name: "Anjali Verma", email: "anjali@gmail.com", role: "CITIZEN", points: 5, issuesSolved: 0, activeTasks: 0, totalRating: 0, ratingCount: 0, rank: 1, approved: true },
  { uid: "worker_pending", name: "Vijay Kumar", email: "vijay@civic.gov", role: "WORKER", points: 0, issuesSolved: 0, activeTasks: 0, totalRating: 0, ratingCount: 0, rank: 4, approved: false }
];

const initialMockComplaints: Complaint[] = [
  {
    id: "c_1",
    title: "Garbage Overflow at Sector 4",
    description: "Main garbage bin is overflowing since last 3 days. Foul smell spreading in the market area.",
    category: "Garbage Overflow",
    imageUrl: "https://images.unsplash.com/photo-1611284446314-60a58ac0deb9?q=80&w=400",
    latitude: 12.97259,
    longitude: 77.59556,
    address: "Sector 4 Market, near Central Bank",
    reportedBy: "citizen1",
    reportedByName: "Suresh Sharma",
    createdAt: Date.now() - 86400000 * 2,
    status: "Pending",
    adminVerified: false,
    adminStatus: "Pending",
    priority: "High"
  },
  {
    id: "c_2",
    title: "Deep Pothole near Main Flyover",
    description: "A very deep pothole causing traffic jam and accidents during night. Needs urgent filling.",
    category: "Pothole",
    imageUrl: "https://images.unsplash.com/photo-1515162305285-0293e4767cc2?q=80&w=400",
    latitude: 12.97359,
    longitude: 77.59656,
    address: "Service Lane, Main Flyover Entrance",
    reportedBy: "citizen2",
    reportedByName: "Anjali Verma",
    createdAt: Date.now() - 86400000 * 3,
    status: "In Progress",
    assignedWorkerId: "worker1",
    assignedWorkerName: "Rajesh Kumar",
    adminVerified: false,
    adminStatus: "Pending",
    priority: "High"
  },
  {
    id: "c_3",
    title: "Water Leakage on Church Road",
    description: "Main water pipeline broke. Clean drinking water wasting in huge amounts on the road.",
    category: "Water Leakage",
    imageUrl: "https://images.unsplash.com/photo-1541888946425-d81bb19240f5?q=80&w=400",
    latitude: 12.97059,
    longitude: 77.59356,
    address: "Near St. Mary Church, Church Road",
    reportedBy: "citizen1",
    reportedByName: "Suresh Sharma",
    createdAt: Date.now() - 86400000 * 5,
    status: "Resolved",
    assignedWorkerId: "worker2",
    assignedWorkerName: "Priya Singh",
    completionAcceptedAt: Date.now() - 3600000 * 4,
    resolvedAt: Date.now() - 86400000 * 4,
    workerNotes: "Leakage patched successfully with heavy-duty seal.",
    citizenRating: 5,
    citizenReview: "Excellent and fast work! Water wastage stopped.",
    adminVerified: true,
    adminStatus: "Approved",
    priority: "Medium"
  }
];

// Local state
let localUsers: UserData[] = JSON.parse(localStorage.getItem("smart_civic_users") || "null") || initialMockUsers;
let localComplaints: Complaint[] = JSON.parse(localStorage.getItem("smart_civic_complaints") || "null") || initialMockComplaints;
let localCurrentUser: UserData | null = JSON.parse(localStorage.getItem("smart_civic_current_user") || "null");

const listeners: { [key: string]: Set<(data: any) => void> } = {
  users: new Set(),
  complaints: new Set(),
  currentUser: new Set()
};

function saveLocalState() {
  localStorage.setItem("smart_civic_users", JSON.stringify(localUsers));
  localStorage.setItem("smart_civic_complaints", JSON.stringify(localComplaints));
  localStorage.setItem("smart_civic_current_user", JSON.stringify(localCurrentUser));
  notifyListeners();
}

function notifyListeners() {
  listeners.users.forEach(cb => cb([...localUsers]));
  listeners.complaints.forEach(cb => cb([...localComplaints]));
  listeners.currentUser.forEach(cb => cb(localCurrentUser));
}

// ---------------- REPOSITORY INTERFACE ----------------
export class CivicRepository {
  static getIsFirebase() {
    return isFirebaseAvailable;
  }

  static async signIn(email: string, password?: string): Promise<UserData> {
    if (isFirebaseAvailable && auth) {
      try {
        const authResult = await signInWithEmailAndPassword(auth, email, password || "");
        const uid = authResult.user.uid;
        
        // Fetch from firestore
        const docRef = doc(db, "users", uid);
        const userDoc = await getDoc(docRef);
        
        if (!userDoc.exists()) {
          await firebaseSignOut(auth);
          throw new Error("Account not found. Please register first.");
        }
        
        const userData = userDoc.data() as UserData;
        if (userData.role.toUpperCase() === "WORKER" && !userData.approved) {
          await firebaseSignOut(auth);
          throw new Error("Your worker registration is pending administrator approval.");
        }
        
        localCurrentUser = userData;
        saveLocalState();
        return userData;
      } catch (err: any) {
        console.warn("Firebase login failed, trying mock fallback...", err.message);
        return this.mockSignIn(email);
      }
    } else {
      return this.mockSignIn(email);
    }
  }

  private static mockSignIn(email: string): UserData {
    const foundUser = localUsers.find(u => u.email.toLowerCase() === email.toLowerCase());
    if (foundUser) {
      if (foundUser.role.toUpperCase() === "WORKER" && !foundUser.approved) {
        throw new Error("Your worker registration is pending administrator approval.");
      }
      localCurrentUser = foundUser;
      saveLocalState();
      return foundUser;
    } else {
      throw new Error("Account not found. Please register first.");
    }
  }

  static async signUp(name: string, email: string, password?: string, role: string = "CITIZEN"): Promise<UserData> {
    const approved = role.toUpperCase() !== "WORKER";
    
    if (isFirebaseAvailable && auth) {
      try {
        const authResult = await createUserWithEmailAndPassword(auth, email, password || "");
        const uid = authResult.user.uid;
        const userData: UserData = {
          uid,
          name,
          email,
          role,
          points: 0,
          issuesSolved: 0,
          activeTasks: 0,
          totalRating: 0,
          ratingCount: 0,
          rank: 1,
          approved
        };
        
        await setDoc(doc(db, "users", uid), userData);
        localCurrentUser = userData;
        
        // Add to local list just in case
        if (!localUsers.some(u => u.uid === uid)) {
          localUsers.push(userData);
        }
        saveLocalState();
        return userData;
      } catch (err: any) {
        console.warn("Firebase sign up failed, trying mock fallback...", err.message);
        return this.mockSignUp(name, email, role, approved);
      }
    } else {
      return this.mockSignUp(name, email, role, approved);
    }
  }

  private static mockSignUp(name: string, email: string, role: string, approved: boolean): UserData {
    const uid = "mock_" + generateUUID();
    const userData: UserData = {
      uid,
      name,
      email,
      role,
      points: 0,
      issuesSolved: 0,
      activeTasks: 0,
      totalRating: 0,
      ratingCount: 0,
      rank: 1,
      approved
    };
    localUsers.push(userData);
    localCurrentUser = userData;
    saveLocalState();
    return userData;
  }

  static async signOut(): Promise<void> {
    if (isFirebaseAvailable && auth) {
      try {
        await firebaseSignOut(auth);
      } catch (e) {
        console.warn("Firebase sign out error:", e);
      }
    }
    localCurrentUser = null;
    saveLocalState();
  }

  static getCurrentUser(): UserData | null {
    return localCurrentUser;
  }

  static observeCurrentUser(callback: (user: UserData | null) => void): () => void {
    listeners.currentUser.add(callback);
    callback(localCurrentUser);
    
    let unsubFirebase: any = null;
    if (isFirebaseAvailable && auth) {
      unsubFirebase = onAuthStateChanged(auth, async (firebaseUser: FirebaseUser | null) => {
        if (firebaseUser) {
          const docRef = doc(db, "users", firebaseUser.uid);
          const snap = await getDoc(docRef);
          if (snap.exists()) {
            localCurrentUser = snap.data() as UserData;
            saveLocalState();
          }
        }
      });
    }

    return () => {
      listeners.currentUser.delete(callback);
      if (unsubFirebase) unsubFirebase();
    };
  }

  static observeComplaints(callback: (list: Complaint[]) => void): () => void {
    listeners.complaints.add(callback);
    callback([...localComplaints].sort((a, b) => b.createdAt - a.createdAt));

    let unsubFirebase: any = null;
    if (isFirebaseAvailable && db) {
      unsubFirebase = onSnapshot(collection(db, "complaints"), (snap) => {
        const list: Complaint[] = [];
        snap.forEach(d => list.push(d.data() as Complaint));
        localComplaints = list;
        saveLocalState();
      });
    }

    return () => {
      listeners.complaints.delete(callback);
      if (unsubFirebase) unsubFirebase();
    };
  }

  private static async uploadImage(file: File, folder: string): Promise<string> {
    if (!isFirebaseAvailable || !storage) {
      return "";
    }
    try {
      const fileRef = storageRef(storage, `${folder}/${generateUUID()}.jpg`);
      await uploadBytes(fileRef, file);
      const url = await getDownloadURL(fileRef);
      return url;
    } catch (e) {
      console.error("Storage upload failed", e);
      return "";
    }
  }

  static async reportComplaint(
    title: string,
    description: string,
    category: string,
    imageFile: File | null,
    latitude: number,
    longitude: number,
    address: string
  ): Promise<Complaint> {
    const user = localCurrentUser;
    if (!user) throw new Error("No user logged in");
    
    let imageUrl = "https://images.unsplash.com/photo-1515162305285-0293e4767cc2?q=80&w=400";
    if (imageFile) {
      const uploadedUrl = await this.uploadImage(imageFile, "complaints");
      if (uploadedUrl) imageUrl = uploadedUrl;
    }

    const complaint: Complaint = {
      id: generateUUID(),
      title,
      description,
      category,
      imageUrl,
      latitude,
      longitude,
      address,
      reportedBy: user.uid,
      reportedByName: user.name,
      createdAt: Date.now(),
      status: "Pending",
      adminVerified: false,
      adminStatus: "Pending",
      priority: "Medium"
    };

    if (isFirebaseAvailable && db) {
      try {
        await setDoc(doc(db, "complaints", complaint.id), complaint);
        return complaint;
      } catch (e) {
        console.warn("Failed to save to Firestore, saving locally", e);
      }
    }

    localComplaints.push(complaint);
    saveLocalState();
    return complaint;
  }

  static async acceptComplaint(complaintId: string): Promise<void> {
    const user = localCurrentUser;
    if (!user) throw new Error("No worker logged in");

    if (isFirebaseAvailable && db) {
      try {
        await runTransaction(db, async (transaction) => {
          const complaintRef = doc(db, "complaints", complaintId);
          const workerRef = doc(db, "users", user.uid);
          
          const complaintSnap = await transaction.get(complaintRef);
          if (!complaintSnap.exists()) throw new Error("Complaint not found");
          
          const c = complaintSnap.data() as Complaint;
          if (c.status !== "Pending") throw new Error("Already accepted");

          transaction.update(complaintRef, {
            status: "In Progress",
            assignedWorkerId: user.uid,
            assignedWorkerName: user.name,
            completionAcceptedAt: Date.now()
          });

          transaction.update(workerRef, {
            activeTasks: increment(1)
          });
        });
        return;
      } catch (e) {
        console.warn("Firebase transaction failed, running mock accept", e);
      }
    }

    // Mock implementation
    localComplaints = localComplaints.map(c => {
      if (c.id === complaintId) {
        return {
          ...c,
          status: "In Progress",
          assignedWorkerId: user.uid,
          assignedWorkerName: user.name,
          completionAcceptedAt: Date.now()
        };
      }
      return c;
    });

    localUsers = localUsers.map(u => {
      if (u.uid === user.uid) {
        const updated = { ...u, activeTasks: u.activeTasks + 1 };
        if (localCurrentUser?.uid === user.uid) localCurrentUser = updated;
        return updated;
      }
      return u;
    });

    saveLocalState();
  }

  static async submitCompletionProof(complaintId: string, imageFile: File | null, notes: string): Promise<void> {
    let proofUrl = "https://images.unsplash.com/photo-1541888946425-d81bb19240f5?q=80&w=400";
    if (imageFile) {
      const uploadedUrl = await this.uploadImage(imageFile, "proofs");
      if (uploadedUrl) proofUrl = uploadedUrl;
    }

    if (isFirebaseAvailable && db) {
      try {
        const complaintRef = doc(db, "complaints", complaintId);
        await updateDoc(complaintRef, {
          status: "Resolved",
          adminStatus: "Pending",
          workerProofImageUrl: proofUrl,
          workerNotes: notes,
          resolvedAt: Date.now()
        });
        return;
      } catch (e) {
        console.warn("Failed to update on Firestore, using mock fallback", e);
      }
    }

    localComplaints = localComplaints.map(c => {
      if (c.id === complaintId) {
        return {
          ...c,
          status: "Resolved",
          adminStatus: "Pending",
          workerProofImageUrl: proofUrl,
          workerNotes: notes,
          resolvedAt: Date.now()
        };
      }
      return c;
    });
    saveLocalState();
  }

  static async verifyCompletion(complaintId: string, approve: boolean): Promise<void> {
    if (isFirebaseAvailable && db) {
      try {
        await runTransaction(db, async (transaction) => {
          const complaintRef = doc(db, "complaints", complaintId);
          const complaintSnap = await transaction.get(complaintRef);
          if (!complaintSnap.exists()) throw new Error("Complaint not found");
          
          const c = complaintSnap.data() as Complaint;
          const workerId = c.assignedWorkerId;
          if (!workerId) throw new Error("No worker assigned");
          
          const workerRef = doc(db, "users", workerId);
          const workerSnap = await transaction.get(workerRef);
          if (!workerSnap.exists()) throw new Error("Worker not found");
          
          const w = workerSnap.data() as UserData;
          let pointsDelta = 0;
          let issuesSolvedDelta = 0;

          if (approve) {
            pointsDelta += 10;
            issuesSolvedDelta = 1;
            // Speed bonus
            const acceptTime = c.completionAcceptedAt || c.createdAt;
            const resolveTime = c.resolvedAt || Date.now();
            if (resolveTime - acceptTime < 86400000) {
              pointsDelta += 5;
            }

            transaction.update(complaintRef, {
              adminVerified: true,
              adminStatus: "Approved"
            });
          } else {
            pointsDelta -= 10;
            transaction.update(complaintRef, {
              status: "Pending",
              assignedWorkerId: null,
              assignedWorkerName: null,
              completionAcceptedAt: null,
              workerProofImageUrl: null,
              workerNotes: null,
              resolvedAt: null,
              adminVerified: false,
              adminStatus: "Rejected"
            });
          }

          transaction.update(workerRef, {
            points: Math.max(0, w.points + pointsDelta),
            issuesSolved: w.issuesSolved + issuesSolvedDelta,
            activeTasks: Math.max(0, w.activeTasks - 1)
          });
        });
        this.recalculateMockLeaderboardRanks();
        return;
      } catch (e) {
        console.warn("Firebase verify transaction failed, using mock", e);
      }
    }

    // Mock Verification
    const claim = localComplaints.find(c => c.id === complaintId);
    if (!claim || !claim.assignedWorkerId) return;
    
    const workerId = claim.assignedWorkerId;
    let pointsDelta = 0;
    let issuesSolvedDelta = 0;

    localComplaints = localComplaints.map(c => {
      if (c.id === complaintId) {
        if (approve) {
          pointsDelta += 10;
          issuesSolvedDelta = 1;
          const acceptTime = c.completionAcceptedAt || c.createdAt;
          const resolveTime = c.resolvedAt || Date.now();
          if (resolveTime - acceptTime < 86400000) {
            pointsDelta += 5;
          }
          return { ...c, adminVerified: true, adminStatus: "Approved" };
        } else {
          pointsDelta -= 10;
          return {
            ...c,
            status: "Pending",
            assignedWorkerId: null,
            assignedWorkerName: null,
            completionAcceptedAt: null,
            workerProofImageUrl: null,
            workerNotes: null,
            resolvedAt: null,
            adminVerified: false,
            adminStatus: "Rejected"
          };
        }
      }
      return c;
    });

    localUsers = localUsers.map(u => {
      if (u.uid === workerId) {
        const updated = {
          ...u,
          points: Math.max(0, u.points + pointsDelta),
          issuesSolved: u.issuesSolved + issuesSolvedDelta,
          activeTasks: Math.max(0, u.activeTasks - 1)
        };
        if (localCurrentUser?.uid === workerId) localCurrentUser = updated;
        return updated;
      }
      return u;
    });

    this.recalculateMockLeaderboardRanks();
    saveLocalState();
  }

  private static recalculateMockLeaderboardRanks() {
    const workers = localUsers.filter(u => u.role.toUpperCase() === "WORKER");
    workers.sort((a, b) => b.points - a.points);
    localUsers = localUsers.map(u => {
      if (u.role.toUpperCase() === "WORKER") {
        const idx = workers.findIndex(w => w.uid === u.uid);
        return { ...u, rank: idx + 1 };
      }
      return u;
    });
  }

  static async rateResolution(complaintId: string, rating: number, review: string): Promise<void> {
    if (isFirebaseAvailable && db) {
      try {
        await runTransaction(db, async (transaction) => {
          const complaintRef = doc(db, "complaints", complaintId);
          const complaintSnap = await transaction.get(complaintRef);
          if (!complaintSnap.exists()) throw new Error("Complaint not found");
          
          const c = complaintSnap.data() as Complaint;
          const workerId = c.assignedWorkerId;
          
          transaction.update(complaintRef, {
            citizenRating: rating,
            citizenReview: review
          });

          if (workerId) {
            const workerRef = doc(db, "users", workerId);
            const wSnap = await transaction.get(workerRef);
            if (wSnap.exists()) {
              const w = wSnap.data() as UserData;
              const bonus = rating >= 4 ? 5 : 0;
              transaction.update(workerRef, {
                totalRating: w.totalRating + rating,
                ratingCount: w.ratingCount + 1,
                points: w.points + bonus
              });
            }
          }
        });
        return;
      } catch (e) {
        console.warn("Firebase rating transaction failed, using mock", e);
      }
    }

    let workerId = "";
    localComplaints = localComplaints.map(c => {
      if (c.id === complaintId) {
        workerId = c.assignedWorkerId || "";
        return { ...c, citizenRating: rating, citizenReview: review };
      }
      return c;
    });

    if (workerId) {
      localUsers = localUsers.map(u => {
        if (u.uid === workerId) {
          const bonus = rating >= 4 ? 5 : 0;
          return {
            ...u,
            totalRating: u.totalRating + rating,
            ratingCount: u.ratingCount + 1,
            points: u.points + bonus
          };
        }
        return u;
      });
    }

    saveLocalState();
  }

  static async manualAssignWorker(complaintId: string, workerId: string, workerName: string): Promise<void> {
    if (isFirebaseAvailable && db) {
      try {
        await runTransaction(db, async (transaction) => {
          const cRef = doc(db, "complaints", complaintId);
          const wRef = doc(db, "users", workerId);
          
          transaction.update(cRef, {
            status: "In Progress",
            assignedWorkerId: workerId,
            assignedWorkerName: workerName,
            completionAcceptedAt: Date.now()
          });
          transaction.update(wRef, {
            activeTasks: increment(1)
          });
        });
        return;
      } catch (e) {
        console.warn("Failed to manually assign worker in Firestore, using mock", e);
      }
    }

    localComplaints = localComplaints.map(c => {
      if (c.id === complaintId) {
        return {
          ...c,
          status: "In Progress",
          assignedWorkerId: workerId,
          assignedWorkerName: workerName,
          completionAcceptedAt: Date.now()
        };
      }
      return c;
    });

    localUsers = localUsers.map(u => {
      if (u.uid === workerId) {
        return { ...u, activeTasks: u.activeTasks + 1 };
      }
      return u;
    });

    saveLocalState();
  }

  static async deleteComplaint(complaintId: string): Promise<void> {
    if (isFirebaseAvailable && db) {
      try {
        await deleteDoc(doc(db, "complaints", complaintId));
        return;
      } catch (e) {
        console.warn("Failed to delete from Firestore, using mock", e);
      }
    }
    localComplaints = localComplaints.filter(c => c.id !== complaintId);
    saveLocalState();
  }

  static async approveWorker(workerId: string): Promise<void> {
    if (isFirebaseAvailable && db) {
      try {
        await updateDoc(doc(db, "users", workerId), { approved: true });
      } catch (e) {
        console.warn("Failed to approve worker on Firestore, using mock", e);
      }
    }

    localUsers = localUsers.map(u => {
      if (u.uid === workerId) {
        return { ...u, approved: true };
      }
      return u;
    });
    saveLocalState();
  }

  static observeLeaderboard(callback: (list: UserData[]) => void): () => void {
    const trigger = (usersList: UserData[]) => {
      const workers = usersList.filter(u => u.role.toUpperCase() === "WORKER");
      workers.sort((a, b) => b.points - a.points);
      callback(workers.map((w, i) => ({ ...w, rank: i + 1 })));
    };

    listeners.users.add(trigger);
    trigger(localUsers);

    return () => {
      listeners.users.delete(trigger);
    };
  }

  static observeAllUsers(callback: (list: UserData[]) => void): () => void {
    listeners.users.add(callback);
    callback([...localUsers]);

    let unsubFirebase: any = null;
    if (isFirebaseAvailable && db) {
      unsubFirebase = onSnapshot(collection(db, "users"), (snap) => {
        const list: UserData[] = [];
        snap.forEach(d => list.push(d.data() as UserData));
        localUsers = list;
        saveLocalState();
      });
    }

    return () => {
      listeners.users.delete(callback);
      if (unsubFirebase) unsubFirebase();
    };
  }
}
