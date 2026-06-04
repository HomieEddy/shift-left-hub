import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Ticket, CreateTicketRequest } from './ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {

  constructor(private http: HttpClient) {}

  createTicket(request: CreateTicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>('/api/tickets', request);
  }

  getTickets(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>('/api/tickets');
  }

  getTicket(id: string): Observable<Ticket> {
    return this.http.get<Ticket>(`/api/tickets/${id}`);
  }

  cancelTicket(id: string, cancelReason?: string): Observable<Ticket> {
    return this.http.post<Ticket>(`/api/tickets/${id}/cancel`, cancelReason ? { cancelReason } : {});
  }
}
