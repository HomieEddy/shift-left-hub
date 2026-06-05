import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AgentTicket, AgentTicketFilters, ResolveTicketRequest, WorkNote } from './agent-ticket.model';

@Injectable({ providedIn: 'root' })
export class AgentTicketService {

  constructor(private http: HttpClient) {}

  getTickets(filters?: AgentTicketFilters): Observable<AgentTicket[]> {
    let params = new HttpParams();
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.category) params = params.set('category', filters.category);
    if (filters?.urgency) params = params.set('urgency', filters.urgency);
    if (filters?.search) params = params.set('search', filters.search);
    return this.http.get<AgentTicket[]>('/api/agent/tickets', { params, withCredentials: true });
  }

  getTicket(id: string): Observable<AgentTicket> {
    return this.http.get<AgentTicket>(`/api/agent/tickets/${id}`, { withCredentials: true });
  }

  claimTicket(id: string): Observable<AgentTicket> {
    return this.http.post<AgentTicket>(`/api/agent/tickets/${id}/claim`, {}, { withCredentials: true });
  }

  getWorkNotes(id: string): Observable<WorkNote[]> {
    return this.http.get<WorkNote[]>(`/api/agent/tickets/${id}/work-notes`, { withCredentials: true });
  }

  addWorkNote(id: string, content: string): Observable<WorkNote> {
    return this.http.post<WorkNote>(`/api/agent/tickets/${id}/work-notes`, { content }, { withCredentials: true });
  }

  resolveTicket(id: string, request: ResolveTicketRequest): Observable<AgentTicket> {
    return this.http.post<AgentTicket>(`/api/agent/tickets/${id}/resolve`, request, { withCredentials: true });
  }
}
