import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ArticleDto, ArticleSearchResult } from '../models/article.models';

@Injectable({ providedIn: 'root' })
export class PublicArticleService {

  constructor(private http: HttpClient) {}

  getArticles(page: number = 0, size: number = 20): Observable<any> {
    return this.http.get<any>('/api/articles', {
      params: { page, size },
    });
  }

  search(query: string, page: number = 0, size: number = 20): Observable<any> {
    return this.http.get<any>('/api/articles/search', {
      params: { q: query, page, size },
    });
  }

  getArticleById(id: string): Observable<ArticleDto> {
    return this.http.get<ArticleDto>(`/api/articles/${id}`);
  }
}
