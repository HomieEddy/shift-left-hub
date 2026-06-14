import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TagDto, CreateTagRequest, UpdateTagRequest } from '../models/tag.models';

@Injectable({ providedIn: 'root' })
export class TagService {
  private readonly http = inject(HttpClient);

  getTags(): Observable<TagDto[]> {
    return this.http.get<TagDto[]>('/api/admin/tags', { withCredentials: true });
  }

  getTagById(id: string): Observable<TagDto> {
    return this.http.get<TagDto>(`/api/admin/tags/${id}`, { withCredentials: true });
  }

  createTag(request: CreateTagRequest): Observable<TagDto> {
    return this.http.post<TagDto>('/api/admin/tags', request, { withCredentials: true });
  }

  updateTag(id: string, request: UpdateTagRequest): Observable<TagDto> {
    return this.http.put<TagDto>(`/api/admin/tags/${id}`, request, { withCredentials: true });
  }

  deleteTag(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/tags/${id}`, { withCredentials: true });
  }
}
