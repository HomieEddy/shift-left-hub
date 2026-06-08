import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AgentTicket, AgentTicketFilters, ResolveTicketRequest, WorkNote } from './agent-ticket.model';

/** Service for interacting with the agent ticket API endpoints. */
@Injectable({ providedIn: 'root' })
export class AgentTicketService {
  private http = inject(HttpClient);

  /**
   * Retrieves the list of agent tickets with optional filters.
   * @param filters optional status, category, urgency, and search filters
   */
  getTickets(filters?: AgentTicketFilters): Observable<AgentTicket[]> {
    let params = new HttpParams();
    if (filters?.status != null) params = params.set('status', filters.status);
    if (filters?.category != null) params = params.set('category', filters.category);
    if (filters?.urgency != null) params = params.set('urgency', filters.urgency);
    if (filters?.search != null) params = params.set('search', filters.search);
    return this.http.get<AgentTicket[]>('/api/agent/tickets', { params, withCredentials: true });
  }

  /**
   * Retrieves full detail for a single ticket.
   * @param id the ticket UUID
   */
  getTicket(id: string): Observable<AgentTicket> {
    return this.http.get<AgentTicket>(`/api/agent/tickets/${id}`, { withCredentials: true });
  }

  /**
   * Claims a NEW ticket for the authenticated agent.
   * @param id the ticket UUID
   */
  claimTicket(id: string): Observable<AgentTicket> {
    return this.http.post<AgentTicket>(`/api/agent/tickets/${id}/claim`, {}, { withCredentials: true });
  }

  /**
   * Retrieves work notes for a ticket, newest first.
   * @param id the ticket UUID
   */
  getWorkNotes(id: string): Observable<WorkNote[]> {
    return this.http.get<WorkNote[]>(`/api/agent/tickets/${id}/work-notes`, { withCredentials: true });
  }

  /**
   * Adds a work note to a ticket.
   * @param id the ticket UUID
   * @param content the note content
   */
  addWorkNote(id: string, content: string): Observable<WorkNote> {
    return this.http.post<WorkNote>(`/api/agent/tickets/${id}/work-notes`, { content }, { withCredentials: true });
  }

  /**
   * Resolves an IN_PROGRESS ticket with resolution notes.
   * @param id the ticket UUID
   * @param request the resolution payload
   */
  resolveTicket(id: string, request: ResolveTicketRequest): Observable<AgentTicket> {
    return this.http.post<AgentTicket>(`/api/agent/tickets/${id}/resolve`, request, { withCredentials: true });
  }
}
