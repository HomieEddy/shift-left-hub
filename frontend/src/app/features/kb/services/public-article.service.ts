import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ArticleDto, ArticleSearchResult, PaginatedResponse } from '../models/article.models';

@Injectable({ providedIn: 'root' })
export class PublicArticleService {

  constructor(private http: HttpClient) {}

  getArticles(page: number = 0, size: number = 20): Observable<PaginatedResponse<ArticleDto>> {
    return this.http.get<PaginatedResponse<ArticleDto>>('/api/articles', {
      params: { page, size },
    });
  }

  search(query: string, page: number = 0, size: number = 20): Observable<PaginatedResponse<ArticleSearchResult>> {
    return this.http.get<PaginatedResponse<ArticleSearchResult>>('/api/articles/search', {
      params: { q: query, page, size },
    });
  }

  getArticleById(id: string): Observable<ArticleDto> {
    return this.http.get<ArticleDto>(`/api/articles/${id}`);
  }
}
