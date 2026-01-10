import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Project, ProjectRequest } from '../../models/project.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private apiUrl = `${environment.apiUrl}/api/projects`;

  constructor(private http: HttpClient) {}

  createProject(request: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.apiUrl, request);
  }

  getAllProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(this.apiUrl);
  }

  getProjectById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.apiUrl}/${id}`);
  }

  updateProject(id: number, request: ProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.apiUrl}/${id}`, request);
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  generateCode(id: number): Observable<Project> {
    return this.http.post<Project>(`${this.apiUrl}/${id}/generate`, {});
  }

  generateBackendCode(id: number): Observable<Project> {
    return this.http.post<Project>(`${this.apiUrl}/${id}/generate/backend`, {});
  }

  generateFrontendCode(id: number): Observable<Project> {
    return this.http.post<Project>(`${this.apiUrl}/${id}/generate/frontend`, {});
  }

  streamBackendCode(id: number): EventSource {
    const token = localStorage.getItem('token');
    // EventSource doesn't support custom headers, so we'll use query param
    // Note: For production, consider using WebSocket or fetch with ReadableStream
    const url = `${environment.apiUrl}/api/projects/${id}/generate/backend/stream?token=${encodeURIComponent(token || '')}`;
    return new EventSource(url);
  }

  streamFrontendCode(id: number): EventSource {
    const token = localStorage.getItem('token');
    const url = `${environment.apiUrl}/api/projects/${id}/generate/frontend/stream?token=${encodeURIComponent(token || '')}`;
    return new EventSource(url);
  }
}

