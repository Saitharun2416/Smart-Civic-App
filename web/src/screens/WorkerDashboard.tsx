import React, { useState } from "react";
import { UserData, Complaint } from "../data/models";
import { CivicRepository } from "../data/repository";
import { 
  ClipboardList, Play, Star, Award, CheckCircle, MapPin, Camera, X, Info 
} from "lucide-react";

interface WorkerDashboardProps {
  currentUser: UserData;
  complaints: Complaint[];
  leaderboard: UserData[];
  initialTab?: number;
}

export const WorkerDashboard: React.FC<WorkerDashboardProps> = ({
  currentUser,
  complaints,
  leaderboard,
  initialTab = 0
}) => {
  const [activeTab, setActiveTab] = useState(initialTab); // 0: Job Board, 1: My Tasks, 2: Performance

  // Active Task completion states
  const [activeCompletionId, setActiveCompletionId] = useState<string | null>(null);
  const [completionNotes, setCompletionNotes] = useState("");
  const [proofImageFile, setProofImageFile] = useState<File | null>(null);
  const [proofImagePreview, setProofImagePreview] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const workerProfile = leaderboard.find(w => w.uid === currentUser.uid) || currentUser;

  const handleProofImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setProofImageFile(file);
      setProofImagePreview(URL.createObjectURL(file));
    }
  };

  const removeProofImage = () => {
    setProofImageFile(null);
    setProofImagePreview(null);
  };

  const handleAcceptJob = async (complaintId: string) => {
    try {
      await CivicRepository.acceptComplaint(complaintId);
    } catch (e) {
      console.error("Failed to accept job", e);
    }
  };

  const handleProofSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeCompletionId || !completionNotes) return;
    
    setIsSubmitting(true);
    try {
      await CivicRepository.submitCompletionProof(
        activeCompletionId,
        proofImageFile,
        completionNotes
      );
      setActiveCompletionId(null);
      setCompletionNotes("");
      setProofImageFile(null);
      setProofImagePreview(null);
    } catch (e) {
      console.error("Failed to submit proof", e);
    } finally {
      setIsSubmitting(false);
    }
  };

  const availableJobs = complaints.filter(c => c.status === "Pending");
  const myTasks = complaints.filter(
    c => c.assignedWorkerId === currentUser.uid &&
    (c.status === "In Progress" || (c.status === "Resolved" && c.adminStatus === "Pending"))
  );

  const getRankBadgeColor = (rank: number) => {
    if (rank === 1) return "#ffd700";
    if (rank === 2) return "#c0c0c0";
    if (rank === 3) return "#cd7f32";
    return "var(--text-primary)";
  };

  return (
    <div className="fade-in">
      {/* Tab Headers */}
      <div className="tab-headers">
        <button 
          className={`tab-header-btn ${activeTab === 0 ? "active" : ""}`}
          onClick={() => setActiveTab(0)}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
            <ClipboardList size={16} /> Job Board
          </div>
        </button>
        <button 
          className={`tab-header-btn ${activeTab === 1 ? "active" : ""}`}
          onClick={() => setActiveTab(1)}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
            <Play size={16} /> My Tasks
          </div>
        </button>
        <button 
          className={`tab-header-btn ${activeTab === 2 ? "active" : ""}`}
          onClick={() => setActiveTab(2)}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
            <Star size={16} /> Performance
          </div>
        </button>
      </div>

      {/* TAB 0: JOB BOARD */}
      {activeTab === 0 && (
        <div className="fade-in">
          <h3 className="section-title">Available Civic Issues</h3>
          <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginBottom: "20px" }}>
            Accept pending tasks to resolve municipal problems and earn leadership points.
          </p>

          {availableJobs.length === 0 ? (
            <div style={{ textAlign: "center", padding: "40px", color: "var(--text-muted)" }}>
              No pending complaints available. Good job, town!
            </div>
          ) : (
            <div className="complaint-list">
              {availableJobs.map(job => {
                let priorityClass = "priority-low";
                if (job.priority === "High") priorityClass = "priority-high";
                else if (job.priority === "Medium") priorityClass = "priority-medium";

                return (
                  <div key={job.id} className="premium-card">
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                      <span style={{ fontSize: "10px", fontWeight: "800", color: "var(--primary)", textTransform: "uppercase" }}>
                        {job.category}
                      </span>
                      <span className={`priority-badge ${priorityClass}`}>{job.priority} Priority</span>
                    </div>

                    <h4 style={{ fontWeight: "700", fontSize: "16px", marginBottom: "4px" }}>{job.title}</h4>
                    <p style={{ fontSize: "13px", color: "var(--text-secondary)", marginBottom: "12px" }}>
                      {job.description}
                    </p>

                    <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "11px", color: "var(--text-muted)", marginBottom: "16px" }}>
                      <MapPin size={12} />
                      <span>{job.address}</span>
                    </div>

                    <button 
                      onClick={() => handleAcceptJob(job.id)}
                      className="btn btn-primary"
                    >
                      Accept Task & Start Resolution
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* TAB 1: MY ACTIVE TASKS */}
      {activeTab === 1 && (
        <div className="fade-in">
          <h3 className="section-title">Active Tasks</h3>
          <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginBottom: "20px" }}>
            Track your accepted jobs and submit proof of completion when resolved.
          </p>

          {myTasks.length === 0 ? (
            <div style={{ textAlign: "center", padding: "40px", color: "var(--text-muted)" }}>
              No active tasks. Check the Job Board!
            </div>
          ) : (
            <div className="complaint-list">
              {myTasks.map(task => (
                <div key={task.id} className="premium-card">
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                    <span style={{ fontSize: "10px", fontWeight: "800", color: "var(--primary)", textTransform: "uppercase" }}>
                      {task.category}
                    </span>
                    {task.status === "Resolved" && (
                      <span className="status-badge status-pending" style={{ fontSize: "9px" }}>
                        Pending Admin Verification
                      </span>
                    )}
                  </div>

                  <h4 style={{ fontWeight: "700", fontSize: "16px", marginBottom: "4px" }}>{task.title}</h4>
                  <p style={{ fontSize: "13px", color: "var(--text-secondary)", marginBottom: "12px" }}>
                    {task.description}
                  </p>

                  <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "11px", color: "var(--text-muted)", marginBottom: "16px" }}>
                    <MapPin size={12} />
                    <span>{task.address}</span>
                  </div>

                  {task.status === "In Progress" ? (
                    <button 
                      onClick={() => setActiveCompletionId(task.id)}
                      className="btn btn-success"
                    >
                      Submit Completion Proof
                    </button>
                  ) : (
                    <div style={{ 
                      backgroundColor: "var(--primary-light)", 
                      padding: "12px", 
                      borderRadius: "8px",
                      fontSize: "12px"
                    }}>
                      <h5 style={{ fontWeight: "700", color: "var(--text-primary)", marginBottom: "4px" }}>Submitted Notes:</h5>
                      <p style={{ color: "var(--text-secondary)" }}>{task.workerNotes || "No notes provided."}</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: PERFORMANCE */}
      {activeTab === 2 && (
        <div className="fade-in">
          <h3 className="section-title">My Performance</h3>
          <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginBottom: "20px" }}>
            Track your municipal rank, leaderboard score, and citizens ratings feedback.
          </p>

          <div className="desktop-split" style={{ alignItems: "stretch" }}>
            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              {/* Stats Header Box */}
              <div className="identity-card" style={{ height: "auto", padding: "24px", marginBottom: 0 }}>
                <div style={{ textAlign: "center", marginBottom: "16px" }}>
                  <span style={{ fontSize: "12px", opacity: 0.8 }}>Total Points Earned</span>
                  <div style={{ fontSize: "38px", fontWeight: "900", color: "var(--primary)", marginTop: "4px" }}>
                    {workerProfile.points} pts
                  </div>
                </div>

                <div style={{ display: "flex", justifyContent: "space-between", textAlign: "center", borderTop: "1px solid rgba(255,255,255,0.1)", paddingTop: "14px" }}>
                  <div>
                    <span style={{ fontSize: "10px", opacity: 0.8 }}>Leader Rank</span>
                    <div style={{ fontSize: "18px", fontWeight: "800", color: getRankBadgeColor(workerProfile.rank) }}>
                      #{workerProfile.rank}
                    </div>
                  </div>
                  <div>
                    <span style={{ fontSize: "10px", opacity: 0.8 }}>Active Tasks</span>
                    <div style={{ fontSize: "18px", fontWeight: "800", color: "white" }}>
                      {workerProfile.activeTasks}
                    </div>
                  </div>
                  <div>
                    <span style={{ fontSize: "10px", opacity: 0.8 }}>Issues Solved</span>
                    <div style={{ fontSize: "18px", fontWeight: "800", color: "white" }}>
                      {workerProfile.issuesSolved}
                    </div>
                  </div>
                </div>
              </div>

              {/* Ratings Box */}
              <div className="premium-card" style={{ display: "flex", alignItems: "center", gap: "16px", marginBottom: 0 }}>
                <div style={{ 
                  width: "48px", 
                  height: "48px", 
                  borderRadius: "50%", 
                  backgroundColor: "var(--primary-light)", 
                  display: "flex", 
                  alignItems: "center", 
                  justifyContent: "center",
                  color: "#fbbf24"
                }}>
                  <Star size={24} style={{ fill: "#fbbf24" }} />
                </div>
                <div>
                  <h4 style={{ fontWeight: "700", fontSize: "15px" }}>Citizen Ratings</h4>
                  <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                    <span style={{ fontSize: "18px", fontWeight: "800" }}>
                      {workerProfile.ratingCount > 0 ? (workerProfile.totalRating / workerProfile.ratingCount).toFixed(1) : "0.0"}
                    </span>
                    <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>
                      ({workerProfile.ratingCount} reviews)
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div>
              {/* Rules / Guide Box */}
              <div className="premium-card" style={{ backgroundColor: "var(--primary-light)", height: "100%", marginBottom: 0 }}>
                <h4 style={{ fontWeight: "700", fontSize: "14px", marginBottom: "12px", display: "flex", alignItems: "center", gap: "6px" }}>
                  <Info size={16} /> Leaderboard Point System:
                </h4>
                <ul style={{ fontSize: "12px", color: "var(--text-secondary)", paddingLeft: "16px", display: "flex", flexDirection: "column", gap: "10px", lineHeight: "1.6" }}>
                  <li>Resolve any assigned civic issue: <b>+10 points</b></li>
                  <li>Quick resolution bonus (&lt;24 hours): <b>+5 points</b></li>
                  <li>High citizen ratings feedback (4+ stars): <b>+5 points</b></li>
                  <li>Spam / False completion claims: <span style={{ color: "#ef4444", fontWeight: "700" }}>-10 points</span></li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Proof Submission Modal Dialog */}
      {activeCompletionId && (
        <div className="modal-overlay">
          <form onSubmit={handleProofSubmit} className="modal-card">
            <h3 style={{ fontSize: "18px", fontWeight: "800", color: "var(--primary)", textAlign: "center", marginBottom: "16px" }}>
              Submit Completion Proof
            </h3>

            <div className="form-group">
              <label className="form-label">Resolution Notes</label>
              <textarea 
                className="form-input"
                style={{ minHeight: "80px" }}
                placeholder="Describe how you resolved this issue..."
                value={completionNotes}
                onChange={e => setCompletionNotes(e.target.value)}
                required
              />
            </div>

            <div className="form-group" style={{ marginBottom: "20px" }}>
              <label className="form-label">Attach Completion Image Proof</label>
              {!proofImagePreview ? (
                <label className="file-picker-card">
                  <Camera size={24} style={{ color: "var(--primary)", marginBottom: "8px" }} />
                  <span style={{ fontSize: "12px", fontWeight: "700" }}>Choose Image Proof</span>
                  <input 
                    type="file" 
                    accept="image/*" 
                    style={{ display: "none" }}
                    onChange={handleProofImageChange}
                  />
                </label>
              ) : (
                <div className="preview-container" style={{ height: "130px" }}>
                  <img src={proofImagePreview} className="preview-image" alt="Proof preview" />
                  <div className="preview-overlay" />
                  <button type="button" onClick={removeProofImage} className="remove-btn">
                    <X size={16} />
                  </button>
                </div>
              )}
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
              <button 
                type="button" 
                onClick={() => {
                  setActiveCompletionId(null);
                  setCompletionNotes("");
                  setProofImageFile(null);
                  setProofImagePreview(null);
                }} 
                className="btn btn-outline" 
                style={{ width: "auto", padding: "10px 20px" }}
              >
                Cancel
              </button>
              <button 
                type="submit" 
                className="btn btn-success" 
                disabled={isSubmitting}
                style={{ width: "auto", padding: "10px 20px" }}
              >
                {isSubmitting ? "Uploading..." : "Submit Proof"}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
