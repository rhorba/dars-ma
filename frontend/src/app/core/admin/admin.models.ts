import { DocType } from '../profile/profile.models';

export interface VerificationQueueItem {
  documentId: string;
  tutorUserId: string;
  tutorFullName: string;
  docType: DocType;
  originalFilename: string | null;
  submittedAt: string;
}

export interface AdminAnalytics {
  studentSignups: number;
  tutorSignups: number;
  totalBookings: number;
  completedBookings: number;
  gmvMad: number;
  matchRatePercent: number;
}
