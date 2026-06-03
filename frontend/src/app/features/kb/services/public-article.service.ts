import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ArticleDto, ArticleSearchResult, ArticleSearchTag, PaginatedResponse } from '../models/article.models';

@Injectable({ providedIn: 'root' })
export class PublicArticleService {

  constructor(private http: HttpClient) {}

  getArticles(page: number = 0, size: number = 20): Observable<PaginatedResponse<ArticleDto>> {
    return this.http.get<PaginatedResponse<ArticleDto>>('/api/articles', {
      params: { page, size },
    });
  }

  search(query: string, page: number = 0, size: number = 20, tags: string[] = []): Observable<PaginatedResponse<ArticleSearchResult>> {
    let params = new HttpParams()
      .set('q', query)
      .set('page', String(page))
      .set('size', String(size));
    for (const tag of tags) {
      params = params.append('tags', tag);
    }

    return this.http.get<PaginatedResponse<ArticleSearchResult>>('/api/articles/search', { params });
  }

  getSearchTags(): Observable<ArticleSearchTag[]> {
    return this.http.get<ArticleSearchTag[]>('/api/articles/search/tags');
  }

  getArticleById(id: string): Observable<ArticleDto> {
    return this.http.get<ArticleDto>(`/api/articles/${id}`);
  }
}
