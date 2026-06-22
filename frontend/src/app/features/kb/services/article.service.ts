import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ArticleDto,
  CreateArticleRequest,
  UpdateArticleRequest,
  PaginatedResponse,
} from '../models/article.models';

@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly http = inject(HttpClient);

  getArticles(page = 0, size = 20): Observable<PaginatedResponse<ArticleDto>> {
    return this.http.get<PaginatedResponse<ArticleDto>>('/api/admin/articles', {
      params: { page, size },
    });
  }

  getArticleById(id: string): Observable<ArticleDto> {
    return this.http.get<ArticleDto>(`/api/admin/articles/${id}`, {});
  }

  createArticle(request: CreateArticleRequest): Observable<ArticleDto> {
    return this.http.post<ArticleDto>('/api/admin/articles', request, {});
  }

  updateArticle(id: string, request: UpdateArticleRequest): Observable<ArticleDto> {
    return this.http.put<ArticleDto>(`/api/admin/articles/${id}`, request, {
      withCredentials: true,
    });
  }

  publishArticle(id: string): Observable<ArticleDto> {
    return this.http.put<ArticleDto>(
      `/api/admin/articles/${id}/publish`,
      {},
      {},
    );
  }

  archiveArticle(id: string): Observable<ArticleDto> {
    return this.http.put<ArticleDto>(
      `/api/admin/articles/${id}/archive`,
      {},
      {},
    );
  }

  deleteArticle(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/articles/${id}`, {});
  }
}
