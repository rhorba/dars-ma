export type GigStatus = 'OPEN' | 'MATCHED' | 'CLOSED';

export interface GigRequest {
  id: string;
  studentUserId: string;
  subject: string;
  level: string;
  description: string;
  budgetMinMad: number | null;
  budgetMaxMad: number | null;
  status: GigStatus;
  createdAt: string;
}

export interface GigRequestCreateRequest {
  subject: string;
  level: string;
  description: string;
  budgetMinMad: number | null;
  budgetMaxMad: number | null;
}

export interface MatchSuggestion {
  tutorUserId: string;
  similarityScore: number;
}
