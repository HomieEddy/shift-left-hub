import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgIf, NgFor, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslationService } from '../../../core/i18n/translation.service';
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
  protected translationService = inject(TranslationService);

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
        this.errorMessage.set(this.translationService.translate('tickets.error.load'));
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

  get allLabel(): string {
    return this.translationService.translate('tickets.filter.all');
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      'NEW': this.translationService.translate('tickets.status.new'),
      'IN_PROGRESS': this.translationService.translate('tickets.status.in_progress'),
      'RESOLVED': this.translationService.translate('tickets.status.resolved'),
      'CANCELLED': this.translationService.translate('tickets.status.cancelled'),
    };
    return labels[status] || status;
  }

  categoryLabel(category: string): string {
    const labels: Record<string, string> = {
      'NETWORK': this.translationService.translate('tickets.category.network'),
      'HARDWARE': this.translationService.translate('tickets.category.hardware'),
      'SOFTWARE': this.translationService.translate('tickets.category.software'),
      'ACCESS': this.translationService.translate('tickets.category.access'),
      'PERIPHERALS': this.translationService.translate('tickets.category.peripherals'),
    };
    return labels[category] || category;
  }

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
