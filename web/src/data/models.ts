export interface UserData {
  uid: string;
  name: string;
  email: string;
  role: string; // 'CITIZEN' | 'WORKER' | 'ADMIN'
  points: number;
  issuesSolved: number;
  activeTasks: number;
  totalRating: number;
  ratingCount: number;
  rank: number;
  approved: boolean;
}

export interface Complaint {
  id: string;
  title: string;
  description: string;
  category: string;
  imageUrl: string;
  latitude: number;
  longitude: number;
  address: string;
  reportedBy: string;
  reportedByName: string;
  createdAt: number;
  status: string; // 'Pending' | 'In Progress' | 'Resolved'
  assignedWorkerId?: string | null;
  assignedWorkerName?: string | null;
  completionAcceptedAt?: number | null;
  workerProofImageUrl?: string | null;
  workerNotes?: string | null;
  resolvedAt?: number | null;
  citizenRating?: number | null;
  citizenReview?: string | null;
  adminVerified: boolean;
  adminStatus: string; // 'Pending' | 'Approved' | 'Rejected'
  priority: string; // 'High' | 'Medium' | 'Low'
}
