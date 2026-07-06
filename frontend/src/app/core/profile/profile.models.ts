export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';
export type DocType = 'DIPLOMA' | 'CERTIFICATE' | 'ID';

export interface TutorProfile {
  userId: string;
  bio: string | null;
  subjects: string[];
  hourlyRateMad: number;
  verificationStatus: VerificationStatus;
  avgRating: number | null;
}

export interface TutorProfileRequest {
  bio: string | null;
  subjects: string[];
  hourlyRateMad: number;
}

export interface VerificationDocument {
  id: string;
  docType: DocType;
  originalFilename: string | null;
  reviewedAt: string | null;
  createdAt: string;
}
