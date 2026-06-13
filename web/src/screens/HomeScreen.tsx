import React, { useState } from "react";
import { UserData, Complaint } from "../data/models";
import { CivicRepository } from "../data/repository";
import { MapPin, Award, ShieldAlert, Star, ShieldCheck, Clock, Plus, HelpCircle, Lock, BookOpen } from "lucide-react";

interface HomeScreenProps {
  currentUser: UserData | null;
  complaints: Complaint[];
  onNavigateToTab: (tabIndex: number) => void;
  onNavigateToAuth: (role: string, isRegister: boolean) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  currentUser,
  complaints,
  onNavigateToTab,
  onNavigateToAuth
}) => {
  const pendingCount = complaints.filter(c => c.status === "Pending").length;
  const resolvedCount = 1852 + complaints.filter(c => c.status === "Resolved").length;

  return (
    <div className="fade-in">
      {/* Welcome Header */}
      <h1 className="screen-title">
        {currentUser ? `Hi, ${currentUser.name.split(" ")[0]}!` : "Welcome to smart civic"}
      </h1>
      <p className="screen-subtitle">Together, let's keep our neighborhoods pristine.</p>

      {/* Identity & Bulletins Grid */}
      <div className="desktop-split" style={{ marginBottom: "24px" }}>
        {/* SpotCivic ID Card (Chime Style) */}
        <div className="identity-card" style={{ marginBottom: 0 }}>
          <div className="identity-card-header">
            <span className="identity-card-title">smart civic ID</span>
            <span className="identity-card-badge">
              {currentUser ? currentUser.role.toUpperCase() : "GUEST"}
            </span>
          </div>
          <div>
            <div className="identity-card-score">
              {currentUser ? `${currentUser.points} PTS` : "0 PTS"}
            </div>
            <div className="identity-card-subtitle">
              {currentUser ? `Level ${currentUser.rank} Citizen` : "Sign in to earn rewards"}
            </div>
          </div>
          <div className="identity-card-footer">
            <span className="identity-card-name">
              {currentUser ? currentUser.name.toUpperCase() : "GUEST CITIZEN"}
            </span>
            <span className="identity-card-title" style={{ fontSize: "22px" }}>VIZ</span>
          </div>
        </div>

        {/* Recent Updates Bulletins */}
        <div className="premium-card" style={{ padding: "20px", marginBottom: 0, height: "190px", overflowY: "auto" }}>
          <h3 style={{ fontSize: "14px", fontWeight: "800", color: "var(--primary)", marginBottom: "14px", textTransform: "uppercase", letterSpacing: "0.5px" }}>
            Recent Bulletins
          </h3>
          <div style={{ marginBottom: "14px" }}>
            <h4 style={{ fontWeight: "700", fontSize: "13px", display: "flex", gap: "6px", alignItems: "center" }}>
              <BookOpen size={16} style={{ color: "var(--primary)" }} />
              Sector 4 Road Repair Complete
            </h4>
            <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginTop: "4px", lineHeight: "1.4" }}>
              Workers resolved the heavy pothole cluster reported by citizens yesterday. Thanks to municipal worker Rajesh Kumar!
            </p>
          </div>
          <hr style={{ border: "none", borderTop: "1px solid var(--border-color)", margin: "14px 0" }} />
          <div>
            <h4 style={{ fontWeight: "700", fontSize: "13px", display: "flex", gap: "6px", alignItems: "center" }}>
              <BookOpen size={16} style={{ color: "var(--primary)" }} />
              Garbage Free Drive in Sector 8
            </h4>
            <p style={{ fontSize: "12px", color: "var(--text-secondary)", marginTop: "4px", lineHeight: "1.4" }}>
              A major garbage overflow has been resolved. Let's keep our environment clean. Citizen reviews rated this 5 stars.
            </p>
          </div>
        </div>
      </div>

      {/* Quick Opportunities Carousel */}
      <h2 className="section-title">Quick Opportunities</h2>
      <div className="carousel-row">
        <div 
          className="carousel-card" 
          style={{ backgroundColor: "var(--primary-light)", color: "var(--text-primary)" }}
          onClick={() => onNavigateToTab(2)} // Navigate to Report Tab
        >
          <Plus className="carousel-card-icon" style={{ color: "var(--primary)" }} />
          <h3 className="carousel-card-title">Spot & Earn</h3>
          <p className="carousel-card-desc">Get +10 points for reporting local municipal issues</p>
        </div>

        {!currentUser && (
          <div 
            className="carousel-card" 
            style={{ backgroundColor: "#fff3cd", color: "#856404" }}
            onClick={() => onNavigateToAuth("CITIZEN", true)}
          >
            <ShieldCheck className="carousel-card-icon" />
            <h3 className="carousel-card-title">Join SpotCivic</h3>
            <p className="carousel-card-desc">Register an account to log issues and save progress</p>
          </div>
        )}

        <div 
          className="carousel-card" 
          style={{ backgroundColor: "#cce5ff", color: "#004085" }}
          onClick={() => onNavigateToTab(3)} // Navigate to Leaderboard Tab
        >
          <Award className="carousel-card-icon" />
          <h3 className="carousel-card-title">Top Performers</h3>
          <p className="carousel-card-desc">See top performing workers and active neighbors</p>
        </div>
      </div>

      {/* City Analytics Grid */}
      <h2 className="section-title">City Analytics</h2>
      <div className="stats-grid">
        <div className="metric-card">
          <ShieldCheck className="metric-icon" style={{ color: "var(--primary)" }} />
          <span className="metric-value">{resolvedCount}+</span>
          <span className="metric-label">Issues Solved</span>
        </div>
        <div className="metric-card">
          <Star className="metric-icon" style={{ color: "#fbbf24" }} />
          <span className="metric-value">98.4%</span>
          <span className="metric-label">Satisfaction Rate</span>
        </div>
        <div className="metric-card">
          <Clock className="metric-icon" style={{ color: "#059669" }} />
          <span className="metric-value">&lt; 24h</span>
          <span className="metric-label">Avg Response</span>
        </div>
        <div className="metric-card">
          <ShieldAlert className="metric-icon" style={{ color: "#ef4444" }} />
          <span className="metric-value">{pendingCount}</span>
          <span className="metric-label">Active Complaints</span>
        </div>
      </div>
    </div>
  );
};
