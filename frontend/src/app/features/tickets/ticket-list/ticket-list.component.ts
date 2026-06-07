import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgIf, NgFor, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { $localize } from '@angular/localize/init';
import { TicketService } from '../ticket.service';
import { Ticket } from '../ticket.model';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, RouterLink],
  templateUrl: './ticket-list.component.html',
})
export class TicketListComponent implements OnInit {
  private ticketService = inject(TicketService);
  private destroyRef = inject(DestroyRef);

  tickets = signal<Ticket[]>([]);
  filteredTickets = signal<Ticket[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  activeFilter = signal<string>('ALL');

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.ticketService.getTickets().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (tickets) => {
        this.tickets.set(tickets);
        this.applyFilter(this.activeFilter());
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set($localize`:@@tickets.error.load:Failed to load tickets.`);
      },
    });
  }

  applyFilter(status: string): void {
    this.activeFilter.set(status);
    if (status === 'ALL') {
      this.filteredTickets.set(this.tickets());
    } else {
      this.filteredTickets.set(this.tickets().filter(t => t.status === status));
    }
  }

  allLabel: string = $localize`:@@tickets.filter.all:All`;

  statusLabels: Record<string, string> = {
    'NEW': $localize`:@@tickets.status.new:New`,
    'IN_PROGRESS': $localize`:@@tickets.status.in_progress:In Progress`,
    'RESOLVED': $localize`:@@tickets.status.resolved:Resolved`,
    'CANCELLED': $localize`:@@tickets.status.cancelled:Cancelled`,
  };

  categoryLabels: Record<string, string> = {
    'NETWORK': $localize`:@@tickets.category.network:Network`,
    'HARDWARE': $localize`:@@tickets.category.hardware:Hardware`,
    'SOFTWARE': $localize`:@@tickets.category.software:Software`,
    'ACCESS': $localize`:@@tickets.category.access:Access`,
    'PERIPHERALS': $localize`:@@tickets.category.peripherals:Peripherals`,
  };

  statusBadgeClass = (status: string): string => {
    switch (status) {
      case 'NEW': return 'bg-blue-100 text-blue-700';
      case 'IN_PROGRESS': return 'bg-amber-100 text-amber-700';
      case 'RESOLVED': return 'bg-green-100 text-green-700';
      case 'CANCELLED': return 'bg-gray-100 text-gray-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  };

  categoryBadgeClass = (category: string): string => {
    switch (category) {
      case 'NETWORK': return 'bg-purple-100 text-purple-700';
      case 'HARDWARE': return 'bg-cyan-100 text-cyan-700';
      case 'SOFTWARE': return 'bg-indigo-100 text-indigo-700';
      case 'ACCESS': return 'bg-teal-100 text-teal-700';
      case 'PERIPHERALS': return 'bg-pink-100 text-pink-700';
      default: return 'bg-slate-100 text-slate-600';
    }
  };
}
