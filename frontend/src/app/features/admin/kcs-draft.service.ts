import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { KcsDraft, PendingCountResponse, PaginatedResponse } from './kcs-draft.model';

/** Service for interacting with the KCS draft admin API endpoints. */
@Injectable({ providedIn: 'root' })
export class KcsDraftService {
  private readonly http = inject(HttpClient);

  /**
   * Lists all KCS drafts with pagination.
   * @param page zero-indexed page number
   * @param size page size (default 20)
   */
  getDrafts(page = 0, size = 20): Observable<PaginatedResponse<KcsDraft>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PaginatedResponse<KcsDraft>>('/api/admin/kcs/drafts', {
      params,
    });
  }

  /** Gets a single KCS draft by article ID. */
  getDraftDetail(id: string): Observable<KcsDraft> {
    return this.http.get<KcsDraft>(`/api/admin/kcs/drafts/${id}`, {});
  }

  /** Approves a KCS draft (→ PUBLISHED). */
  approveDraft(id: string): Observable<KcsDraft> {
    return this.http.put<KcsDraft>(
      `/api/admin/kcs/drafts/${id}/approve`,
      {},
      {},
    );
  }

  /** Rejects a KCS draft (→ ARCHIVED). */
  rejectDraft(id: string): Observable<KcsDraft> {
    return this.http.put<KcsDraft>(
      `/api/admin/kcs/drafts/${id}/reject`,
      {},
      {},
    );
  }

  /** Gets the count of pending (DRAFT) KCS articles for the nav badge. */
  getPendingCount(): Observable<PendingCountResponse> {
    return this.http.get<PendingCountResponse>('/api/admin/kcs/drafts/pending-count', {
      });
  }
}
