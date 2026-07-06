import { DocType } from '../profile/profile.models';

export interface VerificationQueueItem {
  documentId: string;
  tutorUserId: string;
  tutorFullName: string;
  docType: DocType;
  originalFilename: string | null;
  submittedAt: string;
}
