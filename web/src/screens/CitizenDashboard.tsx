import React, { useState, useEffect } from "react";
import { UserData, Complaint } from "../data/models";
import { CivicRepository } from "../data/repository";
import { MapView } from "../components/MapView";
import { 
  Plus, List, Award, CheckCircle, Info, Star, Trash2, Camera, X, MapPin 
} from "lucide-react";

interface CitizenDashboardProps {
  currentUser: UserData;
  complaints: Complaint[];
  leaderboard: UserData[];
  initialTab?: number;
}

export const CitizenDashboard: React.FC<CitizenDashboardProps> = ({
  currentUser,
  complaints,
  leaderboard,
  initialTab = 0
}) => {
  const [activeTab, setActiveTab] = useState(initialTab); // 0: Report, 1: My Issues, 2: Leaders

  // Form states
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("Pothole");
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [latitude, setLatitude] = useState(12.97159);
  const [longitude, setLongitude] = useState(77.59456);
  const [address, setAddress] = useState("Click map below to place marker pin");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Rating Dialog states
  const [ratingComplaintId, setRatingComplaintId] = useState<string | null>(null);
  const [ratingVal, setRatingVal] = useState(5);
  const [reviewText, setReviewText] = useState("");

  const categories = ["Pothole", "Garbage Overflow", "Water Leakage", "Drainage Blockage", "Broken Streetlight", "Traffic Problem"];

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setImageFile(file);
      setImagePreview(URL.createObjectURL(file));
    }
  };

  const removeImage = () => {
    setImageFile(null);
    setImagePreview(null);
  };

  const handleReportSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title || !description) {
      setSubmitError("Please fill in title and description");
      return;
    }
    
    setIsSubmitting(true);
    setSubmitError(null);

    try {
      await CivicRepository.reportComplaint(
        title,
        description,
        category,
        imageFile,
        latitude,
        longitude,
        address
      );
      setSubmitSuccess(true);
      // Reset form
      setTitle("");
      setDescription("");
      setImageFile(null);
      setImagePreview(null);
      setAddress("Click map below to place marker pin");
      setLatitude(12.97159);
      setLongitude(77.59456);
    } catch (err: any) {
      setSubmitError(err.message || "Failed to submit complaint");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRatingSubmit = async () => {
    if (!ratingComplaintId) return;
    try {
      await CivicRepository.rateResolution(ratingComplaintId, ratingVal, reviewText);
      setRatingComplaintId(null);
      setRatingVal(5);
      setReviewText("");
    } catch (e) {
      console.error("Failed to rate", e);
    }
  };

  const myComplaints = complaints.filter(c => c.reportedBy === currentUser.uid);

  return (
    <div className="fade-in">
      {/* Tab Headers */}
      <div className="tab-headers">
        <button 
          className={`tab-header-btn ${activeTab === 0 ? "active" : ""}`}
          onClick={() => setActiveTab(0)}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
            <Plus size={16} /> Report Issue
          </div>
        </button>
        <button 
          className={`tab-header-btn ${activeTab === 1 ? "active" : ""}`}
          onClick={() => setActiveTab(1)}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
            <List size={16} /> Track My Issues
          </div>
        </button>
        <button 
          className={`tab-header-btn ${activeTab === 2 ? "active" : ""}`}
          onClick={() => setActiveTab(2)}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
            <Award size={16} /> Solvers Board
          </div>
        </button>
      </div>

      {/* TAB 0: REPORT FORM */}
      {activeTab === 0 && (
        <div className="fade-in">
          {submitSuccess ? (
            <div className="premium-card" style={{ textAlign: "center", padding: "40px 20px" }}>
              <div style={{ display: "flex", justifyContent: "center", color: "var(--primary)", marginBottom: "16px" }}>
                <CheckCircle size={56} />
              </div>
              <h3 style={{ fontSize: "20px", fontWeight: "800", marginBottom: "8px" }}>Issue Submitted Successfully!</h3>
              <p style={{ fontSize: "13px", color: "var(--text-secondary)", marginBottom: "24px" }}>
                Municipal officers and workers will review your complaint shortly. Thank you for your civic contribution.
              </p>
              <button onClick={() => setSubmitSuccess(false)} className="btn btn-primary">
                Report Another Issue
              </button>
            </div>
          ) : (
            <form onSubmit={handleReportSubmit} className="complaint-form">
              <h3 className="section-title">Report Civic Problem</h3>
              <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginBottom: "20px" }}>
                Submit accurate information and pin the exact location on the map to help workers locate the issue.
              </p>

              <div className="desktop-split" style={{ alignItems: "stretch" }}>
                <div>
                  <div className="form-group">
                    <label className="form-label">Issue Category</label>
                    <select 
                      className="form-input form-select"
                      value={category}
                      onChange={e => setCategory(e.target.value)}
                    >
                      {categories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Complaint Title</label>
                    <input 
                      type="text" 
                      className="form-input"
                      placeholder="e.g. Garbage piling outside market gate"
                      value={title}
                      onChange={e => setTitle(e.target.value)}
                      required
                    />
                  </div>

                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label">Describe the Problem</label>
                    <textarea 
                      className="form-input form-textarea"
                      style={{ minHeight: "150px" }}
                      placeholder="Provide any details about the issue..."
                      value={description}
                      onChange={e => setDescription(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div>
                  {/* Photo Picker */}
                  <div className="form-group">
                    <label className="form-label">Attach Photo of the Issue</label>
                    {!imagePreview ? (
                      <label className="file-picker-card" style={{ height: "90px" }}>
                        <Camera size={20} style={{ color: "var(--primary)", marginBottom: "4px" }} />
                        <span style={{ fontSize: "11px", fontWeight: "700" }}>Upload Image from Gallery</span>
                        <input 
                          type="file" 
                          accept="image/*" 
                          style={{ display: "none" }}
                          onChange={handleImageChange}
                        />
                      </label>
                    ) : (
                      <div className="preview-container" style={{ height: "90px" }}>
                        <img src={imagePreview} className="preview-image" alt="Complaint preview" />
                        <div className="preview-overlay" />
                        <button type="button" onClick={removeImage} className="remove-btn">
                          <X size={14} />
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Leaflet Map Pinning */}
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <div style={{ display: "flex", justifyItems: "center", gap: "6px", fontSize: "11px", fontWeight: "700", marginBottom: "6px" }}>
                      <MapPin size={14} style={{ color: "var(--primary)" }} />
                      <span style={{ textOverflow: "ellipsis", overflow: "hidden", whiteSpace: "nowrap", maxWidth: "100%" }}>
                        {address}
                      </span>
                    </div>
                    <MapView 
                      complaints={[]}
                      isHeatmapMode={false}
                      onLocationSelected={(lat, lng, addr) => {
                        setLatitude(lat);
                        setLongitude(lng);
                        setAddress(addr);
                      }}
                      height="175px"
                    />
                  </div>
                </div>
              </div>

              {submitError && (
                <div style={{ color: "#ef4444", fontSize: "12px", marginTop: "16px", marginBottom: "12px" }}>{submitError}</div>
              )}

              <button 
                type="submit" 
                className="btn btn-primary"
                disabled={isSubmitting}
                style={{ height: "46px", marginTop: "20px" }}
              >
                {isSubmitting ? "Submitting..." : "Submit Complaint"}
              </button>
            </form>
          )}
        </div>
      )}

      {/* TAB 1: MY ISSUES */}
      {activeTab === 1 && (
        <div className="fade-in">
          <h3 className="section-title">Track My Complaints</h3>
          <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginBottom: "20px" }}>
            Monitor the progress of your submitted complaints and rate resolved worker resolutions.
          </p>

          {myComplaints.length === 0 ? (
            <div style={{ textAlign: "center", padding: "40px", color: "var(--text-muted)" }}>
              No complaints submitted yet.
            </div>
          ) : (
            <div className="complaint-list">
              {myComplaints.map(c => {
                let statusColorClass = "status-pending";
                if (c.status === "In Progress") statusColorClass = "status-progress";
                else if (c.status === "Resolved") statusColorClass = "status-resolved";

                return (
                  <div key={c.id} className="premium-card">
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "8px" }}>
                      <div>
                        <span style={{ fontSize: "10px", fontWeight: "800", color: "var(--primary)", textTransform: "uppercase" }}>
                          {c.category}
                        </span>
                        <h4 style={{ fontWeight: "700", fontSize: "16px", marginTop: "2px" }}>{c.title}</h4>
                      </div>
                      <span className={`status-badge ${statusColorClass}`}>{c.status}</span>
                    </div>

                    <p style={{ fontSize: "13px", color: "var(--text-secondary)", marginBottom: "12px" }}>
                      {c.description}
                    </p>

                    <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "11px", color: "var(--text-muted)", marginBottom: "12px" }}>
                      <MapPin size={12} />
                      <span>{c.address}</span>
                    </div>

                    {c.imageUrl && (
                      <img 
                        src={c.imageUrl} 
                        alt="Issue" 
                        style={{ width: "100%", height: "130px", objectFit: "cover", borderRadius: "8px", marginBottom: "12px" }}
                      />
                    )}

                    {/* Rating buttons / Star display */}
                    {c.status === "Resolved" && (
                      <div style={{ marginTop: "12px" }}>
                        {c.adminStatus === "Approved" ? (
                          c.citizenRating === null || c.citizenRating === undefined ? (
                            <button 
                              onClick={() => setRatingComplaintId(c.id)}
                              className="btn btn-primary"
                              style={{ padding: "8px 16px", fontSize: "12px" }}
                            >
                              Rate Work Resolution
                            </button>
                          ) : (
                            <div style={{ 
                              display: "flex", 
                              alignItems: "center", 
                              gap: "8px", 
                              backgroundColor: "var(--primary-light)", 
                              padding: "10px", 
                              borderRadius: "8px" 
                            }}>
                              <span style={{ fontSize: "12px", fontWeight: "700" }}>Your Rating:</span>
                              <div className="stars-row">
                                {[1, 2, 3, 4, 5].map(star => (
                                  <Star 
                                    key={star} 
                                    size={14} 
                                    className={`star-icon ${star <= (c.citizenRating || 0) ? "filled" : ""}`} 
                                  />
                                ))}
                              </div>
                            </div>
                          )
                        ) : (
                          <div style={{ 
                            backgroundColor: "#fffbeb", 
                            border: "1px solid #fde68a", 
                            color: "#b45309", 
                            padding: "8px 12px", 
                            borderRadius: "6px", 
                            fontSize: "11px", 
                            fontWeight: "500",
                            textAlign: "center"
                          }}>
                            Work completed. Pending verification by Municipality Administrator.
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: LEADERS BOARD */}
      {activeTab === 2 && (
        <div className="fade-in">
          <h3 className="section-title">Municipality Leaderboard</h3>
          <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginBottom: "20px" }}>
            Top performing municipal workers resolving urban issues in your town.
          </p>

          {leaderboard.length === 0 ? (
            <div style={{ textAlign: "center", padding: "40px", color: "var(--text-muted)" }}>
              No solvers ranked yet.
            </div>
          ) : (
            <div className="worker-list-grid">
              {leaderboard.map((worker, i) => {
                const rank = i + 1;
                let rankBg = "var(--border-color)";
                let rankText = "var(--text-primary)";
                if (rank === 1) { rankBg = "#ffd700"; rankText = "#0f172a"; }
                else if (rank === 2) { rankBg = "#c0c0c0"; rankText = "#0f172a"; }
                else if (rank === 3) { rankBg = "#cd7f32"; rankText = "#0f172a"; }

                return (
                  <div key={worker.uid} className="premium-card" style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
                      <div style={{ 
                        width: "32px", 
                        height: "32px", 
                        borderRadius: "50%", 
                        backgroundColor: rankBg, 
                        color: rankText,
                        fontWeight: "900",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: "13px"
                      }}>
                        {rank}
                      </div>
                      <div>
                        <h4 style={{ fontWeight: "700", fontSize: "15px" }}>{worker.name}</h4>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "11px", color: "var(--text-muted)" }}>
                          <span>{worker.issuesSolved} Solved</span>
                          {worker.ratingCount > 0 && (
                            <>
                              <span>&bull;</span>
                              <div style={{ display: "flex", alignItems: "center", gap: "2px" }}>
                                <Star size={10} className="star-icon filled" />
                                <span>{(worker.totalRating / worker.ratingCount).toFixed(1)}</span>
                              </div>
                            </>
                          )}
                        </div>
                      </div>
                    </div>
                    <span style={{ fontSize: "15px", fontWeight: "900", color: "var(--primary)" }}>
                      {worker.points} pts
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Star Rating Dialog Modal */}
      {ratingComplaintId && (
        <div className="modal-overlay">
          <div className="modal-card">
            <h3 style={{ fontSize: "18px", fontWeight: "800", color: "var(--primary)", textAlign: "center", marginBottom: "16px" }}>
              Rate Work Resolution
            </h3>
            
            {/* Star selector */}
            <div style={{ display: "flex", justifyContent: "center", gap: "10px", marginBottom: "20px" }}>
              {[1, 2, 3, 4, 5].map(star => (
                <button 
                  key={star}
                  type="button" 
                  onClick={() => setRatingVal(star)} 
                  style={{ background: "none", border: "none", cursor: "pointer" }}
                >
                  <Star 
                    size={36} 
                    className={`star-icon ${star <= ratingVal ? "filled" : ""}`} 
                    style={{ transition: "transform var(--transition-fast)" }}
                  />
                </button>
              ))}
            </div>

            <div className="form-group" style={{ marginBottom: "24px" }}>
              <label className="form-label">Write Feedback (Optional)</label>
              <textarea 
                className="form-input"
                style={{ minHeight: "80px" }}
                placeholder="Was the issue resolved to your satisfaction?"
                value={reviewText}
                onChange={e => setReviewText(e.target.value)}
              />
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
              <button onClick={() => setRatingComplaintId(null)} className="btn btn-outline" style={{ width: "auto", padding: "10px 20px" }}>
                Cancel
              </button>
              <button onClick={handleRatingSubmit} className="btn btn-primary" style={{ width: "auto", padding: "10px 20px" }}>
                Submit Review
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
