import React, { useState, useEffect } from "react";
import { HomeScreen } from "./screens/HomeScreen";
import { AuthScreen } from "./screens/AuthScreen";
import { CitizenDashboard } from "./screens/CitizenDashboard";
import { WorkerDashboard } from "./screens/WorkerDashboard";
import { AdminDashboard } from "./screens/AdminDashboard";
import { CivicRepository } from "./data/repository";
import { UserData, Complaint } from "./data/models";
import { 
  Home, 
  User, 
  Briefcase, 
  ShieldAlert, 
  Sun, 
  Moon, 
  LogOut, 
  LogIn, 
  ShieldCheck, 
  Lock 
} from "lucide-react";
import "./App.css";

type ScreenType = "HOME" | "AUTH" | "CITIZEN" | "WORKER" | "ADMIN";

function App() {
  // Sync states
  const [currentUser, setCurrentUser] = useState<UserData | null>(null);
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [allUsers, setAllUsers] = useState<UserData[]>([]);
  const [leaderboard, setLeaderboard] = useState<UserData[]>([]);

  // Navigation states
  const [currentScreen, setCurrentScreen] = useState<ScreenType>("HOME");
  const [authInitialRole, setAuthInitialRole] = useState<string>("CITIZEN");
  const [authInitialRegister, setAuthInitialRegister] = useState<boolean>(false);
  
  // Tab states for resets/deep linking
  const [citizenTab, setCitizenTab] = useState<number>(0);
  const [workerTab, setWorkerTab] = useState<number>(0);
  const [adminTab, setAdminTab] = useState<number>(0);

  // Warning state
  const [accessDeniedWarning, setAccessDeniedWarning] = useState<string | null>(null);

  // Theme Mode
  const [theme, setTheme] = useState<"light" | "dark">(() => {
    const saved = localStorage.getItem("smart_civic_theme");
    if (saved === "dark") return "dark";
    if (saved === "light") return "light";
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  });

  // Observe repo data
  useEffect(() => {
    const unsubUser = CivicRepository.observeCurrentUser((user) => {
      setCurrentUser(user);
      // Auto-route on mount if logged in, but stay on dashboard/home
      if (user) {
        if (user.role.toUpperCase() === "ADMIN" && currentScreen === "AUTH") {
          setCurrentScreen("ADMIN");
        } else if (user.role.toUpperCase() === "WORKER" && currentScreen === "AUTH") {
          setCurrentScreen("WORKER");
        } else if (user.role.toUpperCase() === "CITIZEN" && currentScreen === "AUTH") {
          setCurrentScreen("CITIZEN");
        }
      }
    });

    const unsubComplaints = CivicRepository.observeComplaints(setComplaints);
    const unsubAllUsers = CivicRepository.observeAllUsers(setAllUsers);
    const unsubLeaderboard = CivicRepository.observeLeaderboard(setLeaderboard);

    return () => {
      unsubUser();
      unsubComplaints();
      unsubAllUsers();
      unsubLeaderboard();
    };
  }, [currentScreen]);

  // Handle Theme
  useEffect(() => {
    if (theme === "dark") {
      document.documentElement.classList.add("dark-theme");
    } else {
      document.documentElement.classList.remove("dark-theme");
    }
    localStorage.setItem("smart_civic_theme", theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === "light" ? "dark" : "light"));
  };

  const handleLogout = async () => {
    if (window.confirm("Are you sure you want to log out of Smart Civic?")) {
      await CivicRepository.signOut();
      setCurrentScreen("HOME");
    }
  };

  const handleAuthSuccess = (user: UserData) => {
    setCurrentUser(user);
    if (user.role.toUpperCase() === "ADMIN") {
      setCurrentScreen("ADMIN");
      setAdminTab(0);
    } else if (user.role.toUpperCase() === "WORKER") {
      setCurrentScreen("WORKER");
      setWorkerTab(0);
    } else {
      setCurrentScreen("CITIZEN");
      setCitizenTab(0);
    }
  };

  // Home Quick Navigations
  const handleHomeNavigateToTab = (tabIndex: number) => {
    if (tabIndex === 2) {
      // Report Issue
      if (currentUser) {
        setCitizenTab(0);
        setCurrentScreen("CITIZEN");
      } else {
        setAuthInitialRole("CITIZEN");
        setAuthInitialRegister(true);
        setCurrentScreen("AUTH");
      }
    } else if (tabIndex === 3) {
      // Leaderboard
      if (currentUser) {
        const role = currentUser.role.toUpperCase();
        if (role === "CITIZEN") {
          setCitizenTab(2);
          setCurrentScreen("CITIZEN");
        } else if (role === "WORKER") {
          setWorkerTab(2);
          setCurrentScreen("WORKER");
        } else if (role === "ADMIN") {
          setAdminTab(3);
          setCurrentScreen("ADMIN");
        }
      } else {
        // Let guests view citizen tab 2
        setAuthInitialRole("CITIZEN");
        setAuthInitialRegister(false);
        setCurrentScreen("AUTH");
      }
    }
  };

  const handleHomeNavigateToAuth = (role: string, isRegister: boolean) => {
    setAuthInitialRole(role);
    setAuthInitialRegister(isRegister);
    setCurrentScreen("AUTH");
  };

  // Bottom Nav click actions
  const handleTabClick = (target: "HOME" | "CITIZEN" | "WORKER" | "ADMIN") => {
    if (target === "HOME") {
      setCurrentScreen("HOME");
      return;
    }

    if (!currentUser) {
      setAuthInitialRole(target);
      setAuthInitialRegister(false);
      setCurrentScreen("AUTH");
      return;
    }

    const userRole = currentUser.role.toUpperCase();
    if (target === "CITIZEN") {
      // Anyone logged in can access Citizen features to report or track
      setCurrentScreen("CITIZEN");
      setCitizenTab(0);
    } else if (target === "WORKER") {
      if (userRole === "WORKER") {
        setCurrentScreen("WORKER");
        setWorkerTab(0);
      } else {
        setAccessDeniedWarning("This portal is restricted to Municipal Workers. Please register or sign in as a Municipal Worker.");
      }
    } else if (target === "ADMIN") {
      if (userRole === "ADMIN") {
        setCurrentScreen("ADMIN");
        setAdminTab(0);
      } else {
        setAccessDeniedWarning("This portal is restricted to Municipal Administrators. Please sign in with an Administrator profile.");
      }
    }
  };

  return (
    <div className="app-container">
      {/* Top Bar Header */}
      <header className="top-bar">
        <div className="top-bar-inner">
          <div className="top-bar-logo" onClick={() => setCurrentScreen("HOME")}>
            <div className="logo-icon">
              <ShieldCheck size={18} />
            </div>
            <span className="logo-text">
              smart civic<span className="logo-dot">.</span>
            </span>
          </div>

          {/* Desktop Navigation Links */}
          <nav className="top-bar-nav">
            <button 
              className={`top-nav-item ${currentScreen === "HOME" ? "active" : ""}`}
              onClick={() => handleTabClick("HOME")}
            >
              Home
            </button>
            <button 
              className={`top-nav-item ${currentScreen === "CITIZEN" ? "active" : ""}`}
              onClick={() => handleTabClick("CITIZEN")}
            >
              Citizen Portal
            </button>
            <button 
              className={`top-nav-item ${currentScreen === "WORKER" ? "active" : ""}`}
              onClick={() => handleTabClick("WORKER")}
            >
              Worker Portal
            </button>
            <button 
              className={`top-nav-item ${currentScreen === "ADMIN" ? "active" : ""}`}
              onClick={() => handleTabClick("ADMIN")}
            >
              Admin Console
            </button>
          </nav>

          <div className="top-bar-actions">
            {/* Theme Switcher */}
            <button onClick={toggleTheme} className="btn-icon" title="Toggle Theme" aria-label="Toggle Theme">
              {theme === "light" ? <Moon size={18} /> : <Sun size={18} />}
            </button>
            
            {/* Logout/Login Button */}
            {currentUser ? (
              <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <span style={{ fontSize: "11px", fontWeight: "800", color: "var(--primary)" }}>
                  {currentUser.points} PTS
                </span>
                <button onClick={handleLogout} className="btn-icon" title="Sign Out" aria-label="Sign Out" style={{ color: "#ef4444" }}>
                  <LogOut size={18} />
                </button>
              </div>
            ) : (
              currentScreen !== "AUTH" && (
                <button 
                  onClick={() => {
                    setAuthInitialRole("CITIZEN");
                    setAuthInitialRegister(false);
                    setCurrentScreen("AUTH");
                  }} 
                  className="btn-icon" 
                  title="Access Portal"
                  aria-label="Access Portal"
                >
                  <LogIn size={18} />
                </button>
              )
            )}
          </div>
        </div>
      </header>

      {/* Main Content Viewports */}
      <main className="main-content">
        <div className="main-content-inner">
          {currentScreen === "HOME" && (
            <HomeScreen 
              currentUser={currentUser} 
              complaints={complaints} 
              onNavigateToTab={handleHomeNavigateToTab}
              onNavigateToAuth={handleHomeNavigateToAuth}
            />
          )}
          
          {currentScreen === "AUTH" && (
            <AuthScreen 
              initialRole={authInitialRole} 
              initialRegister={authInitialRegister} 
              onAuthSuccess={handleAuthSuccess}
              onBackToHome={() => setCurrentScreen("HOME")}
            />
          )}

          {currentScreen === "CITIZEN" && currentUser && (
            <CitizenDashboard 
              key={`citizen-${citizenTab}`}
              currentUser={currentUser} 
              complaints={complaints}
              leaderboard={leaderboard}
              initialTab={citizenTab}
            />
          )}

          {currentScreen === "WORKER" && currentUser && (
            <WorkerDashboard 
              key={`worker-${workerTab}`}
              currentUser={currentUser} 
              complaints={complaints}
              leaderboard={leaderboard}
              initialTab={workerTab}
            />
          )}

          {currentScreen === "ADMIN" && currentUser && (
            <AdminDashboard 
              key={`admin-${adminTab}`}
              currentUser={currentUser} 
              complaints={complaints}
              workers={leaderboard}
              users={allUsers}
              initialTab={adminTab}
            />
          )}
        </div>
      </main>

      {/* Access Denied Warning Dialog */}
      {accessDeniedWarning && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ textAlign: "center", padding: "30px 20px" }}>
            <div style={{ display: "flex", justifyContent: "center", color: "#d97706", marginBottom: "16px" }}>
              <Lock size={48} />
            </div>
            <h3 style={{ fontSize: "18px", fontWeight: "800", marginBottom: "12px" }}>Access Restricted</h3>
            <p style={{ fontSize: "13px", color: "var(--text-secondary)", marginBottom: "24px", lineHeight: "1.5" }}>
              {accessDeniedWarning}
            </p>
            <button onClick={() => setAccessDeniedWarning(null)} className="btn btn-primary" style={{ height: "42px" }}>
              Dismiss
            </button>
          </div>
        </div>
      )}

      {/* Bottom Nav Bar */}
      <nav className="bottom-nav">
        <button 
          className={`nav-item ${currentScreen === "HOME" ? "active" : ""}`}
          onClick={() => handleTabClick("HOME")}
        >
          <Home className="nav-icon" />
          <span>Home</span>
        </button>
        <button 
          className={`nav-item ${currentScreen === "CITIZEN" ? "active" : ""}`}
          onClick={() => handleTabClick("CITIZEN")}
        >
          <User className="nav-icon" />
          <span>Citizen</span>
        </button>
        <button 
          className={`nav-item ${currentScreen === "WORKER" ? "active" : ""}`}
          onClick={() => handleTabClick("WORKER")}
        >
          <Briefcase className="nav-icon" />
          <span>Worker</span>
        </button>
        <button 
          className={`nav-item ${currentScreen === "ADMIN" ? "active" : ""}`}
          onClick={() => handleTabClick("ADMIN")}
        >
          <ShieldAlert className="nav-icon" />
          <span>Admin</span>
        </button>
      </nav>
    </div>
  );
}

export default App;
