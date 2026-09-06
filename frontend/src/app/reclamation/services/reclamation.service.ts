import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../models/api-response.model';
import {
  Reclamation,
  ReclamationRequest,
  ReclamationUpdateRequest,
  StatusUpdateRequest
} from '../models/reclamation.model';

@Injectable({ providedIn: 'root' })
export class ReclamationService {
  private readonly apiUrl = `${environment.apiUrl}/api/reclamation`;

  constructor(private readonly http: HttpClient) {}

  create(request: ReclamationRequest): Observable<ApiResponse<Reclamation>> {
    return this.http.post<ApiResponse<Reclamation>>(`${this.apiUrl}/reclamations`, request);
  }

  getMine(): Observable<ApiResponse<Reclamation[]>> {
    return this.http.get<ApiResponse<Reclamation[]>>(`${this.apiUrl}/reclamations/my`);
  }

  getAll(): Observable<ApiResponse<Reclamation[]>> {
    return this.http.get<ApiResponse<Reclamation[]>>(`${this.apiUrl}/reclamations`);
  }

  getById(id: string): Observable<ApiResponse<Reclamation>> {
    return this.http.get<ApiResponse<Reclamation>>(`${this.apiUrl}/reclamations/${id}`);
  }

  update(id: string, request: ReclamationUpdateRequest): Observable<ApiResponse<Reclamation>> {
    return this.http.put<ApiResponse<Reclamation>>(`${this.apiUrl}/reclamations/${id}`, request);
  }

  updateStatus(id: string, request: StatusUpdateRequest): Observable<ApiResponse<Reclamation>> {
    return this.http.patch<ApiResponse<Reclamation>>(`${this.apiUrl}/reclamations/${id}/status`, request);
  }

  delete(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/reclamations/${id}`);
  }
}
