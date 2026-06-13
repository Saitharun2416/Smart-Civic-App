import React, { useState } from "react";
import { CivicRepository } from "../data/repository";
import { UserData } from "../data/models";
import { ShieldCheck, User, LogIn, Lock, Info, Mail } from "lucide-react";

interface AuthScreenProps {
  initialRole?: string;
  initialRegister?: boolean;
  onAuthSuccess: (user: UserData) => void;
  onBackToHome: () => void;
}

export const AuthScreen: React.FC<AuthScreenProps> = ({
  initialRole = "CITIZEN",
  initialRegister = false,
  onAuthSuccess,
  onBackToHome
}) => {
  const [isRegister, setIsRegister] = useState(initialRegister);
  const [role, setRole] = useState(initialRole);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) {
      setErrorMsg("Please enter email address");
      return;
    }
    
    setIsLoading(true);
    setErrorMsg(null);
    setSuccessMsg(null);

    try {
      if (isRegister) {
        if (!name) {
          setErrorMsg("Please enter your name");
          setIsLoading(false);
          return;
        }
        const user = await CivicRepository.signUp(name, email, password, role);
        if (role.toUpperCase() === "WORKER") {
          setSuccessMsg("Registration successful! Your worker profile is pending administrator approval.");
          // Reset form fields
          setName("");
          setEmail("");
          setPassword("");
        } else {
          onAuthSuccess(user);
        }
      } else {
        const user = await CivicRepository.signIn(email, password);
        onAuthSuccess(user);
      }
    } catch (err: any) {
      setErrorMsg(err.message || "Authentication failed");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fade-in" style={{ padding: "10px 0" }}>
      <div style={{ display: "flex", justifyContent: "center", marginBottom: "24px" }}>
        <div className="logo-icon" style={{ width: "48px", height: "48px" }}>
          <ShieldCheck size={28} />
        </div>
      </div>
      
      <h2 style={{ textAlign: "center", fontSize: "24px", fontWeight: "800", marginBottom: "4px" }}>
        {isRegister ? "Create Smart Civic Account" : "Access Civic Portal"}
      </h2>
      <p style={{ textAlign: "center", fontSize: "13px", color: "var(--text-secondary)", marginBottom: "24px" }}>
        {isRegister ? "Register to start resolving city issues" : "Log in with your registered credentials"}
      </p>

      {/* Tab Selectors for Login vs Register */}
      <div className="tab-headers" style={{ marginBottom: "24px" }}>
        <button 
          className={`tab-header-btn ${!isRegister ? "active" : ""}`}
          onClick={() => { setIsRegister(false); setErrorMsg(null); setSuccessMsg(null); }}
        >
          Sign In
        </button>
        <button 
          className={`tab-header-btn ${isRegister ? "active" : ""}`}
          onClick={() => { setIsRegister(true); setErrorMsg(null); setSuccessMsg(null); }}
        >
          Register
        </button>
      </div>

      <form onSubmit={handleSubmit} className="premium-card" style={{ padding: "24px" }}>
        {/* Role Select Pills */}
        <div className="form-group">
          <label className="form-label">Select Profile Role</label>
          <div className="pill-row">
            <button
              type="button"
              className={`pill-button ${role === "CITIZEN" ? "active" : ""}`}
              onClick={() => setRole("CITIZEN")}
            >
              Citizen User
            </button>
            <button
              type="button"
              className={`pill-button ${role === "WORKER" ? "active" : ""}`}
              onClick={() => setRole("WORKER")}
            >
              Municipal Worker
            </button>
            <button
              type="button"
              className={`pill-button ${role === "ADMIN" ? "active" : ""}`}
              onClick={() => setRole("ADMIN")}
            >
              Administrator
            </button>
          </div>
        </div>

        {isRegister && (
          <div className="form-group">
            <label className="form-label">Full Name</label>
            <div style={{ position: "relative" }}>
              <input
                type="text"
                className="form-input"
                placeholder="Enter your full name"
                value={name}
                onChange={e => setName(e.target.value)}
                required
              />
            </div>
          </div>
        )}

        <div className="form-group">
          <label className="form-label">Email Address</label>
          <input
            type="email"
            className="form-input"
            placeholder="e.g. suresh@gmail.com"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label className="form-label">Password</label>
          <input
            type="password"
            className="form-input"
            placeholder="Enter account password"
            value={password}
            onChange={e => setPassword(e.target.value)}
          />
        </div>

        {errorMsg && (
          <div style={{ 
            backgroundColor: "#fef2f2", 
            border: "1px solid #fca5a5", 
            color: "#b91c1c", 
            padding: "12px", 
            borderRadius: "8px", 
            fontSize: "12px", 
            marginBottom: "16px",
            display: "flex",
            gap: "8px",
            alignItems: "center"
          }}>
            <Info size={16} />
            <span>{errorMsg}</span>
          </div>
        )}

        {successMsg && (
          <div style={{ 
            backgroundColor: "#ecfdf5", 
            border: "1px solid #6ee7b7", 
            color: "#047857", 
            padding: "12px", 
            borderRadius: "8px", 
            fontSize: "12px", 
            marginBottom: "16px",
            display: "flex",
            gap: "8px",
            alignItems: "center"
          }}>
            <Info size={16} />
            <span>{successMsg}</span>
          </div>
        )}

        <button 
          type="submit" 
          className="btn btn-primary"
          disabled={isLoading}
          style={{ height: "46px" }}
        >
          {isLoading ? "Please Wait..." : isRegister ? "Create Account" : "Access Account"}
        </button>
      </form>

      <button 
        onClick={onBackToHome}
        className="btn btn-outline"
        style={{ marginTop: "16px", height: "46px" }}
      >
        Browse Portal as Guest
      </button>
    </div>
  );
};
