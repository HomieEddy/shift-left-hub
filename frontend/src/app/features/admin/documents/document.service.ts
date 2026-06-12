import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DocumentDto, DocumentUploadResponse } from './document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private http = inject(HttpClient);

  uploadFile(file: File): Observable<DocumentUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<DocumentUploadResponse>('/api/admin/documents/upload', formData, {
      withCredentials: true,
    });
  }

  getDocuments(): Observable<DocumentDto[]> {
    return this.http.get<DocumentDto[]>('/api/admin/documents', { withCredentials: true });
  }

  getDocument(id: string): Observable<DocumentDto> {
    return this.http.get<DocumentDto>(`/api/admin/documents/${id}`, { withCredentials: true });
  }

  deleteDocument(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/documents/${id}`, { withCredentials: true });
  }

  reprocessDocument(id: string): Observable<DocumentUploadResponse> {
    return this.http.post<DocumentUploadResponse>(`/api/admin/documents/${id}/reprocess`, {}, { withCredentials: true });
  }
}
