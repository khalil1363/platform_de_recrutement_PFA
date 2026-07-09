export type RecruitmentStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';
export type ApplicationStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'ACCEPTED' | 'REJECTED';

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
  interviewAt?: string;
  appliedAt?: string;
  answers?: ApplicationAnswer[];
}

export interface ApplicationStatusUpdateRequest {
  status: ApplicationStatus;
  interviewAt?: string | null;
}

export interface ZoneRequest {
  name: string;
  description?: string;
}

export interface CompanyRequest {
  name: string;
  zoneId: string;
  address?: string;
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
  questions?: QcmQuestion[];
}

export interface ApplicationRequest {
  recruitmentId: string;
  cvFileUrl: string;
  answers: QcmAnswer[];
}
