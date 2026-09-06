export type ReclamationStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export interface Reclamation {
  reclamationId: string;
  title: string;
  description: string;
  status: ReclamationStatus;
  createdByUserId: string;
  createdByName?: string;
  handledByUserId?: string;
  handledByName?: string;
  rhResponse?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReclamationRequest {
  title: string;
  description: string;
}

export interface ReclamationUpdateRequest {
  title?: string;
  description?: string;
  rhResponse?: string;
  status?: ReclamationStatus;
}

export interface StatusUpdateRequest {
  status: ReclamationStatus;
  rhResponse?: string;
}
