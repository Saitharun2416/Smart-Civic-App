import React, { useState } from "react";
import { UserData, Complaint } from "../data/models";
import { CivicRepository } from "../data/repository";
import { MapView } from "../components/MapView";
import { 
  Check, X, ClipboardSignature, UserCheck, BarChart2, Eye, Map, Trash2, ShieldAlert
} from "lucide-react";

interface AdminDashboardProps {
  currentUser: UserData;
  complaints: Complaint[];
  workers: UserData[];
  users: UserData[];
  initialTab?: number;
}

export const AdminDashboard: React.FC<AdminDashboardProps> = ({
  currentUser,
  complaints,
  workers,
  users,
  initialTab = 0
}) => {
  const [activeTab, setActiveTab] = useState(initialTab); // 0: Verifications, 1: Allocations, 2: Analytics, 3: Registry
  
  // Allocations Dropdowns states
  const [assigningId, setAssigningId] = useState<string | null>(null);
  
  // Analytics Map overlay toggle
  const [isHeatmap, setIsHeatmap] = useState(true);

  const handleVerify = async (complaintId: string, approve: boolean) => {
    try {
      await CivicRepository.verifyCompletion(complaintId, approve);
    } catch (e) {
      console.error("Failed to verify completion", e);
    }
  };

  const handleDelete = async (complaintId: string) => {
    if (window.confirm("Are you sure you want to delete this complaint?")) {
      try {
        await CivicRepository.deleteComplaint(complaintId);
      } catch (e) {
        console.error("Failed to delete", e);
      }
    }
  };

  const handleAssignWorker = async (complaintId: string, workerId: string, workerName: string) => {
    try {
      await CivicRepository.manualAssignWorker(complaintId, workerId, workerName);
      setAssigningId(null);
    } catch (e) {
      console.error("Failed to assign worker", e);
    }
  };

  const handleApproveWorker = async (workerId: string) => {
    try {
      await CivicRepository.approveWorker(workerId);
    } catch (e) {
      console.error("Failed to approve worker", e);
    }
  };

  // Filters
  const claims = complaints.filter(c => c.status === "Resolved" && c.adminStatus === "Pending");
  const pendingAllocations = complaints.filter(c => c.status === "Pending");
  
  const pendingWorkersList = users.filter(u => u.role.toUpperCase() === "WORKER" && !u.approved);
  const approvedWorkersList = users.filter(u => u.role.toUpperCase() === "WORKER" && u.approved);
  const citizensList = users.filter(u => u.role.toUpperCase() === "CITIZEN");

  // Category counts calculations
  const categoriesList = ["Pothole", "Garbage Overflow", "Water Leakage", "Drainage Blockage", "Broken Streetlight", "Traffic Problem"];
  const totalComplaintsCount = complaints.length || 1;

  return (
    <div className="fade-in">
      {/* Tab Headers */}
      <div className="tab-headers" style={{ marginBottom: "20px" }}>
        <button 
          className={`tab-header-btn ${activeTab === 0 ? "active" : ""}`}
          onClick={() => setActiveTab(0)}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
            <ClipboardSignature size={15} /> Verify
          </div>
        </button>
        <button 
          className={`tab-header-btn ${activeTab === 1 ? "active" : ""}`}
          onClick={() => setActiveTab(1)}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
            <ClipboardSignature size={15} /> Assign
          </div>
        </button>
        <button 
          className={`tab-header-btn ${activeTab === 2 ? "active" : ""}`}
          onClick={() => setActiveTab(2)}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
            <BarChart2 size={15} /> Analytics
          </div>
        </button>
        <button 
          className={`tab-header-btn ${activeTab === 3 ? "active" : ""}`}
          onClick={() => setActiveTab(3)}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
            <UserCheck size={15} /> Registry
          </div>
        </button>
      </div>

      {/* TAB 0: VERIFICATION CLAIMS */}
      {activeTab === 0 && (
        <div className="fade-in">
          <h3 className="section-title">Verify Work Completion</h3>
          <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginBottom: "20px" }}>
            Review submitted municipal worker proof images and notes before approving leaderboard points.
          </p>

          {claims.length === 0 ? (
            <div style={{ textAlign: "center", padding: "40px", color: "var(--text-muted)" }}>
              No verification claims pending.
            </div>
          ) : (
            <div className="complaint-list">
              {claims.map(claim => (
                <div key={claim.id} className="premium-card">
                  <span style={{ fontSize: "10px", fontWeight: "800", color: "var(--primary)", textTransform: "uppercase" }}>
                    {claim.category} &bull; Solved by {claim.assignedWorkerName}
                  </span>
                  
                  <h4 style={{ fontWeight: "700", fontSize: "16px", margin: "4px 0" }}>{claim.title}</h4>
                  <p style={{ fontSize: "12px", color: "var(--text-muted)", marginBottom: "12px" }}>
                    Original Desc: {claim.description}
                  </p>

                  <div style={{ 
                    backgroundColor: "var(--primary-light)", 
                    padding: "10px 14px", 
                    borderRadius: "8px", 
                    marginBottom: "12px",
                    fontSize: "12px"
                  }}>
                    <h5 style={{ fontWeight: "700", color: "var(--text-primary)" }}>Worker Notes:</h5>
                    <p style={{ color: "var(--text-secondary)", marginTop: "2px" }}>
                      {claim.workerNotes || "No notes provided."}
                    </p>
                  </div>

                  {claim.workerProofImageUrl && (
                    <div style={{ marginBottom: "16px" }}>
                      <h5 style={{ fontWeight: "700", fontSize: "12px", marginBottom: "6px" }}>Image Proof:</h5>
                      <img 
                        src={claim.workerProofImageUrl} 
                        alt="Completion Proof" 
                        style={{ width: "100%", height: "180px", objectFit: "cover", borderRadius: "8px" }}
                      />
                    </div>
                  )}

                  <div style={{ display: "flex", gap: "10px" }}>
                    <button 
                      onClick={() => handleVerify(claim.id, false)}
                      className="btn btn-danger"
                      style={{ padding: "8px 16px", height: "40px", fontSize: "12px" }}
                    >
                      <X size={16} /> Reject (-10 pts)
                    </button>
                    <button 
                      onClick={() => handleVerify(claim.id, true)}
                      className="btn btn-success"
                      style={{ padding: "8px 16px", height: "40px", fontSize: "12px" }}
                    >
                      <Check size={16} /> Approve (+10 pts)
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 1: ALLOCATIONS */}
      {activeTab === 1 && (
        <div className="fade-in">
          <h3 className="section-title">Task Allocation Panel</h3>
          <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginBottom: "20px" }}>
            Manually allocate active civic complaints directly to verified municipal workers, or delete spam reports.
          </p>

          {pendingAllocations.length === 0 ? (
            <div style={{ textAlign: "center", padding: "40px", color: "var(--text-muted)" }}>
              No pending complaints available for allocation.
            </div>
          ) : (
            <div className="complaint-list">
              {pendingAllocations.map(task => (
                <div key={task.id} className="premium-card">
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "8px" }}>
                    <span style={{ fontSize: "10px", fontWeight: "800", color: "var(--text-secondary)", textTransform: "uppercase" }}>
                      {task.category} &bull; Reported by {task.reportedByName}
                    </span>
                    <button 
                      onClick={() => handleDelete(task.id)}
                      style={{ background: "none", border: "none", color: "#ef4444", cursor: "pointer" }}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>

                  <h4 style={{ fontWeight: "700", fontSize: "16px", marginBottom: "4px" }}>{task.title}</h4>
                  <p style={{ fontSize: "13px", color: "var(--text-secondary)", marginBottom: "14px" }}>
                    {task.description}
                  </p>

                  <div className="form-group" style={{ margin: 0 }}>
                    <label className="form-label" style={{ fontSize: "11px" }}>Assign Verified Worker:</label>
                    <select
                      className="form-input form-select"
                      style={{ height: "40px", padding: "8px 12px" }}
                      defaultValue=""
                      onChange={e => {
                        const selectedWorker = approvedWorkersList.find(w => w.uid === e.target.value);
                        if (selectedWorker) {
                          handleAssignWorker(task.id, selectedWorker.uid, selectedWorker.name);
                        }
                      }}
                    >
                      <option value="" disabled>Select worker to assign...</option>
                      {approvedWorkersList.map(worker => (
                        <option key={worker.uid} value={worker.uid}>
                          {worker.name} ({worker.activeTasks} tasks active, {worker.points} pts)
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: ANALYTICS & HEATMAP */}
      {activeTab === 2 && (
        <div className="fade-in">
          <h3 className="section-title">Municipality Analytics</h3>
          <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginBottom: "16px" }}>
            Visual map overlay of complaint coordinates and counts sorted by categories.
          </p>

          <div className="desktop-split" style={{ alignItems: "stretch" }}>
            <div>
              {/* Heatmap Pin Toggle */}
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
                <span style={{ fontSize: "13px", fontWeight: "700" }}>City Map Overlays</span>
                <div className="pill-row" style={{ margin: 0 }}>
                  <button 
                    className={`pill-button ${!isHeatmap ? "active" : ""}`}
                    onClick={() => setIsHeatmap(false)}
                  >
                    Pin View
                  </button>
                  <button 
                    className={`pill-button ${isHeatmap ? "active" : ""}`}
                    onClick={() => setIsHeatmap(true)}
                  >
                    Heatmap View
                  </button>
                </div>
              </div>

              <MapView 
                complaints={complaints}
                isHeatmapMode={isHeatmap}
                height="300px"
              />
            </div>

            <div>
              {/* Category Statistics bars */}
              <div className="premium-card" style={{ height: "100%", marginBottom: 0 }}>
                <h4 style={{ fontWeight: "700", fontSize: "15px", marginBottom: "12px", color: "var(--primary)" }}>
                  Complaints by Category
                </h4>
                
                <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                  {categoriesList.map(cat => {
                    const count = complaints.filter(c => c.category === cat).length;
                    const progress = (count / totalComplaintsCount) * 100;
                    
                    return (
                      <div key={cat}>
                        <div style={{ display: "flex", justifyContent: "space-between", fontSize: "12px", fontWeight: "500", marginBottom: "4px" }}>
                          <span>{cat}</span>
                          <span style={{ color: "var(--text-secondary)" }}>{count}</span>
                        </div>
                        {/* Progress Bar Container */}
                        <div style={{ width: "100%", height: "6px", backgroundColor: "var(--border-color)", borderRadius: "3px", overflow: "hidden" }}>
                          <div style={{ 
                            width: `${progress}%`, 
                            height: "100%", 
                            backgroundColor: "var(--primary)", 
                            borderRadius: "3px",
                            transition: "width var(--transition-smooth)"
                          }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: USER REGISTRY */}
      {activeTab === 3 && (
        <div className="fade-in">
          <h3 className="section-title">User & Worker Registry</h3>
          <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginBottom: "20px" }}>
            Approve pending worker registrations and monitor active users list.
          </p>

          <div className="admin-registry-split">
            {/* Column 1: Pending Approvals */}
            <div>
              <h4 style={{ 
                fontSize: "15px", 
                fontWeight: "700", 
                marginBottom: "12px",
                color: pendingWorkersList.length > 0 ? "#d97706" : "var(--text-primary)" 
              }}>
                Pending Worker Approvals ({pendingWorkersList.length})
              </h4>

              {pendingWorkersList.length === 0 ? (
                <div className="premium-card" style={{ backgroundColor: "var(--primary-light)", borderStyle: "dashed", textAlign: "center", padding: "16px", color: "var(--text-muted)", fontSize: "13px" }}>
                  No pending worker registrations.
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                  {pendingWorkersList.map(worker => (
                    <div key={worker.uid} className="premium-card" style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px" }}>
                      <div>
                        <h5 style={{ fontWeight: "700", fontSize: "14px" }}>{worker.name}</h5>
                        <span style={{ fontSize: "11px", color: "var(--text-muted)" }}>{worker.email}</span>
                      </div>
                      <button 
                        onClick={() => handleApproveWorker(worker.uid)}
                        className="btn btn-success"
                        style={{ width: "auto", padding: "6px 12px", fontSize: "11px", height: "32px" }}
                      >
                        <Check size={12} /> Approve
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Column 2: Approved Workers */}
            <div>
              <h4 style={{ fontSize: "15px", fontWeight: "700", marginBottom: "12px" }}>
                Approved Workers ({approvedWorkersList.length})
              </h4>
              <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                {approvedWorkersList.map(worker => (
                  <div key={worker.uid} className="premium-card" style={{ display: "flex", alignItems: "center", gap: "12px", padding: "12px" }}>
                    <div style={{ 
                      width: "36px", 
                      height: "36px", 
                      borderRadius: "8px", 
                      backgroundColor: "var(--primary-light)", 
                      color: "var(--primary)",
                      fontWeight: "700",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center"
                    }}>
                      {worker.name.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <h5 style={{ fontWeight: "700", fontSize: "14px" }}>{worker.name}</h5>
                      <span style={{ fontSize: "11px", color: "var(--text-muted)" }}>
                        Rank #{worker.rank} &bull; {worker.points} pts
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Column 3: Registered Citizens */}
            <div>
              <h4 style={{ fontSize: "15px", fontWeight: "700", marginBottom: "12px" }}>
                Registered Citizens ({citizensList.length})
              </h4>
              <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                {citizensList.map(citizen => (
                  <div key={citizen.uid} className="premium-card" style={{ display: "flex", alignItems: "center", gap: "12px", padding: "12px" }}>
                    <div style={{ 
                      width: "36px", 
                      height: "36px", 
                      borderRadius: "8px", 
                      backgroundColor: "var(--border-color)", 
                      color: "var(--text-secondary)",
                      fontWeight: "700",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center"
                    }}>
                      {citizen.name.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <h5 style={{ fontWeight: "700", fontSize: "14px" }}>{citizen.name}</h5>
                      <span style={{ fontSize: "11px", color: "var(--text-muted)" }}>
                        {citizen.points} pts &bull; {citizen.email}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
