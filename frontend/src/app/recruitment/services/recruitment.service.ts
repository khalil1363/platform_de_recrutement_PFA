import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../models/api-response.model';
import {
  ApplicationRequest,
  ApplicationStatusUpdateRequest,
  Company,
  CompanyRequest,
  JobApplication,
  Recruitment,
  RecruitmentRequest,
  RhZoneAssignment,
  RhZoneAssignmentRequest,
  ApplicationStatus,
  Qcm,
  QcmRequest,
  Zone,
  ZoneRequest
} from '../models/recruitment.model';

@Injectable({ providedIn: 'root' })
export class RecruitmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/recruitment`;

  getPublishedRecruitments(): Observable<ApiResponse<Recruitment[]>> {
    return this.http.get<ApiResponse<Recruitment[]>>(`${this.apiUrl}/public/recruitments`);
  }

  getPublishedRecruitment(id: string): Observable<ApiResponse<Recruitment>> {
    return this.http.get<ApiResponse<Recruitment>>(`${this.apiUrl}/public/recruitments/${id}`);
  }

  uploadCv(file: File): Observable<ApiResponse<{ cvFileUrl: string }>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<{ cvFileUrl: string }>>(`${this.apiUrl}/upload/cv`, formData);
  }

  apply(request: ApplicationRequest): Observable<ApiResponse<JobApplication>> {
    return this.http.post<ApiResponse<JobApplication>>(`${this.apiUrl}/applications`, request);
  }

  getMyApplications(): Observable<ApiResponse<JobApplication[]>> {
    return this.http.get<ApiResponse<JobApplication[]>>(`${this.apiUrl}/applications/my`);
  }

  getZones(): Observable<ApiResponse<Zone[]>> {
    return this.http.get<ApiResponse<Zone[]>>(`${this.apiUrl}/zones`);
  }

  createZone(request: ZoneRequest): Observable<ApiResponse<Zone>> {
    return this.http.post<ApiResponse<Zone>>(`${this.apiUrl}/zones`, request);
  }

  assignRhToZone(request: RhZoneAssignmentRequest): Observable<ApiResponse<RhZoneAssignment[]>> {
    return this.http.post<ApiResponse<RhZoneAssignment[]>>(`${this.apiUrl}/rh-zone-assignments`, request);
  }

  getRhZoneAssignments(): Observable<ApiResponse<RhZoneAssignment[]>> {
    return this.http.get<ApiResponse<RhZoneAssignment[]>>(`${this.apiUrl}/rh-zone-assignments`);
  }

  getCompanies(): Observable<ApiResponse<Company[]>> {
    return this.http.get<ApiResponse<Company[]>>(`${this.apiUrl}/companies`);
  }

  createCompany(request: CompanyRequest): Observable<ApiResponse<Company>> {
    return this.http.post<ApiResponse<Company>>(`${this.apiUrl}/companies`, request);
  }

  getRecruitments(): Observable<ApiResponse<Recruitment[]>> {
    return this.http.get<ApiResponse<Recruitment[]>>(`${this.apiUrl}/recruitments`);
  }

  getRecruitment(id: string): Observable<ApiResponse<Recruitment>> {
    return this.http.get<ApiResponse<Recruitment>>(`${this.apiUrl}/recruitments/${id}`);
  }

  createRecruitment(request: RecruitmentRequest): Observable<ApiResponse<Recruitment>> {
    return this.http.post<ApiResponse<Recruitment>>(`${this.apiUrl}/recruitments`, request);
  }

  updateRecruitment(id: string, request: RecruitmentRequest): Observable<ApiResponse<Recruitment>> {
    return this.http.put<ApiResponse<Recruitment>>(`${this.apiUrl}/recruitments/${id}`, request);
  }

  getQcms(): Observable<ApiResponse<Qcm[]>> {
    return this.http.get<ApiResponse<Qcm[]>>(`${this.apiUrl}/qcm`);
  }

  getQcm(id: string): Observable<ApiResponse<Qcm>> {
    return this.http.get<ApiResponse<Qcm>>(`${this.apiUrl}/qcm/${id}`);
  }

  createQcm(request: QcmRequest): Observable<ApiResponse<Qcm>> {
    return this.http.post<ApiResponse<Qcm>>(`${this.apiUrl}/qcm`, request);
  }

  updateQcm(id: string, request: QcmRequest): Observable<ApiResponse<Qcm>> {
    return this.http.put<ApiResponse<Qcm>>(`${this.apiUrl}/qcm/${id}`, request);
  }

  deleteQcm(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/qcm/${id}`);
  }

  getApplications(recruitmentId: string): Observable<ApiResponse<JobApplication[]>> {
    return this.http.get<ApiResponse<JobApplication[]>>(`${this.apiUrl}/recruitments/${recruitmentId}/applications`);
  }

  getRhApplications(): Observable<ApiResponse<JobApplication[]>> {
    return this.http.get<ApiResponse<JobApplication[]>>(`${this.apiUrl}/applications`);
  }

  updateApplicationStatus(
    applicationId: string,
    request: ApplicationStatusUpdateRequest
  ): Observable<ApiResponse<JobApplication>> {
    return this.http.patch<ApiResponse<JobApplication>>(`${this.apiUrl}/applications/${applicationId}/status`, request);
  }

  analyzeApplicationCv(applicationId: string): Observable<ApiResponse<JobApplication>> {
    return this.http.post<ApiResponse<JobApplication>>(`${this.apiUrl}/applications/${applicationId}/analyze-cv`, {});
  }

  resolveFileUrl(path?: string | null): string | null {
    if (!path) {
      return null;
    }
    if (path.startsWith('http')) {
      return path;
    }
    return `${environment.apiUrl}${path}`;
  }
}
