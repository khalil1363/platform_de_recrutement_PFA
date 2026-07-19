export type RecruitmentStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';
export type ApplicationStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'ACCEPTED' | 'HIRED' | 'REJECTED';

export interface Zone {
  zoneId: string;
  name: string;
  description?: string;
  active: boolean;
}

export interface Company {
  companyId: string;
  name: string;
  zoneId: string;
  zoneName?: string;
  address?: string;
  latitude?: number | null;
  longitude?: number | null;
  googleMapsUrl?: string;
  active: boolean;
}

export interface QcmQuestion {
  questionId?: string;
  questionText: string;
  optionA: string;
  optionB: string;
  optionC?: string;
  optionD?: string;
  correctOption?: string;
  orderIndex?: number;
  dimensionCode?: string;
  scoreA?: number;
  scoreB?: number;
  scoreC?: number;
  scoreD?: number;
}

export interface Qcm {
  qcmId: string;
  title: string;
  description?: string;
  createdByRhUserId?: string;
  createdAt?: string;
  questionCount?: number;
  questions?: QcmQuestion[];
}

export interface QcmRequest {
  title: string;
  description?: string;
  questions: QcmQuestion[];
}

export interface QcmAnswer {
  questionId: string;
  selectedOption: string;
}

export interface Recruitment {
  recruitmentId: string;
  title: string;
  description?: string;
  responsibilities?: string;
  technicalSkills?: string;
  personalSkills?: string;
  educationRequirements?: string;
  experienceRequirements?: string;
  companyId: string;
  companyName?: string;
  zoneId?: string;
  zoneName?: string;
  jobType?: string;
  availability?: string;
  salaryMin?: number;
  salaryMax?: number;
  salaryPeriod?: string;
  languages?: string[];
  educationLevel?: string;
  experienceLevel?: string;
  drivingLicenseRequired?: boolean;
  country?: string;
  region?: string;
  city?: string;
  localTravel?: boolean;
  internationalTravel?: boolean;
  anonymousMode?: boolean;
  publicationDate?: string;
  responsibleName?: string;
  internalReference?: string;
  keejobReference?: string;
  status: RecruitmentStatus;
  createdAt?: string;
  qcmId?: string | null;
  qcmTitle?: string;
  questions?: QcmQuestion[];
}

export interface UserSummary {
  userId: string;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  phoneNumber?: string;
  profileImageUrl?: string;
}

export interface ApplicationAnswer {
  questionId: string;
  questionText: string;
  selectedOption: string;
  correctOption?: string;
  correct: boolean;
}

export interface JobApplication {
  applicationId: string;
  recruitmentId: string;
  recruitmentTitle?: string;
  zoneName?: string;
  region?: string;
  candidateUserId: string;
  candidate?: UserSummary;
  cvFileUrl?: string;
  status: ApplicationStatus;
  qcmScore?: number;
  qcmTotalQuestions?: number;
  cvMatchScore?: number | null;
  extractedSkills?: string;
  matchedSkills?: string;
  missingSkills?: string;
  cvAnalysisSummary?: string;
  cvAnalyzedAt?: string;
  interviewAt?: string;
  interviewEndAt?: string;
  googleMeetLink?: string;
  meetingProvider?: string;
  meetingId?: string;
  meetingWarning?: string;
  companyName?: string;
  companyAddress?: string;
  companyGoogleMapsUrl?: string;
  hiredAt?: string;
  hireStartDate?: string;
  hireContractType?: string;
  hireNetSalary?: string;
  hireWorkingHours?: string;
  hireBenefits?: string;
  hireIntegrationAddress?: string;
  hireIntegrationGpsUrl?: string;
  appliedAt?: string;
  answers?: ApplicationAnswer[];
}

export interface ApplicationStatusUpdateRequest {
  status: ApplicationStatus;
  interviewAt?: string | null;
  interviewEndAt?: string | null;
  durationMinutes?: number | null;
  /** ONLINE (default) or PHYSICAL */
  interviewType?: 'ONLINE' | 'PHYSICAL';
  /** Optional location override for physical interviews */
  interviewLocation?: string | null;
  hireStartDate?: string | null;
  hireContractType?: string | null;
  hireNetSalary?: string | null;
  hireWorkingHours?: string | null;
  hireBenefits?: string | null;
  hireIntegrationAddress?: string | null;
  hireIntegrationGpsUrl?: string | null;
}

export interface ZoneRequest {
  name: string;
  description?: string;
}

export interface CompanyRequest {
  name: string;
  zoneId: string;
  address?: string;
  latitude?: number | null;
  longitude?: number | null;
  googleMapsUrl?: string | null;
}

export interface RhZoneAssignmentRequest {
  rhUserId: string;
  zoneIds: string[];
}

export interface RhZoneAssignment {
  id: number;
  rhUserId: string;
  zoneId: string;
  zoneName: string;
  assignedAt?: string;
}

export interface RecruitmentRequest {
  title: string;
  description?: string;
  responsibilities?: string;
  technicalSkills?: string;
  personalSkills?: string;
  educationRequirements?: string;
  experienceRequirements?: string;
  companyId: string;
  jobType?: string;
  availability?: string;
  salaryMin?: number;
  salaryMax?: number;
  salaryPeriod?: string;
  languages?: string[];
  educationLevel?: string;
  experienceLevel?: string;
  drivingLicenseRequired?: boolean;
  country?: string;
  region?: string;
  city?: string;
  localTravel?: boolean;
  internationalTravel?: boolean;
  anonymousMode?: boolean;
  publicationDate?: string;
  responsibleName?: string;
  emailNotificationPerApplication?: boolean;
  internalReference?: string;
  keejobReference?: string;
  status?: RecruitmentStatus;
  qcmId?: string | null;
}

export interface ApplicationRequest {
  recruitmentId: string;
  cvFileUrl: string;
  answers: QcmAnswer[];
  qcmViolated?: boolean;
}

export type HiredQcmStatus = 'ASSIGNED' | 'COMPLETED' | 'VIOLATED';

export interface HiredQcmAssignment {
  assignmentId: string;
  applicationId: string;
  candidateUserId: string;
  recruitmentId: string;
  recruitmentTitle?: string;
  companyName?: string;
  qcmId: string;
  qcmTitle?: string;
  status: HiredQcmStatus;
  score?: number | null;
  totalQuestions?: number | null;
  overallFitPercent?: number | null;
  qcmViolated?: boolean | null;
  assignedAt?: string;
  completedAt?: string;
  candidate?: UserSummary;
  questions?: QcmQuestion[];
  answers?: ApplicationAnswer[];
  dimensionScores?: DimensionScore[];
}

export interface DimensionScore {
  dimensionCode: string;
  dimensionLabel: string;
  score: number;
  expectedScore: number;
  commentText?: string;
  sortOrder?: number;
}

export interface HiredQcmSubmitRequest {
  answers?: QcmAnswer[];
  qcmViolated?: boolean;
}
