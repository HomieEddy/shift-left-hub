import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { CategoryDto, CategoryRequest, MergeRequest } from './category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private http = inject(HttpClient);
  private baseUrl = '/api/admin/categories';

  getAll() { return this.http.get<CategoryDto[]>(this.baseUrl); }

  get(id: string) { return this.http.get<CategoryDto>(`${this.baseUrl}/${id}`); }

  create(request: CategoryRequest) { return this.http.post<CategoryDto>(this.baseUrl, request); }

  update(id: string, request: CategoryRequest) { return this.http.put<CategoryDto>(`${this.baseUrl}/${id}`, request); }

  delete(id: string, reassignTo?: string) {
    const params = reassignTo != null ? { reassignTo } : {};
    return this.http.delete(`${this.baseUrl}/${id}`, { params });
  }

  merge(request: MergeRequest) { return this.http.post<CategoryDto>(`${this.baseUrl}/merge`, request); }
}