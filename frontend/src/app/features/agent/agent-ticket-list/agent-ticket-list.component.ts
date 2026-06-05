import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgIf, NgFor, DatePipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { AgentTicketService } from '../agent-ticket.service';
import { AgentTicket } from '../agent-ticket.model';

@Component({
  selector: 'app-agent-ticket-list',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, NgClass, RouterLink, FormsModule],
  templateUrl: './agent-ticket-list.component.html',
})
export class AgentTicketListComponent implements OnInit {
  private agentTicketService = inject(AgentTicketService);
  private destroyRef = inject(DestroyRef);

  tickets = signal<AgentTicket[]>([]);
  filteredTickets = signal<AgentTicket[]>([]);
  isLoading = signal(true);
  isError = signal(false);
  activeStatus = signal('ALL');
  selectedCategory = signal('');
  selectedUrgency = signal('');
  searchQuery = signal('');
  searchSubject = new Subject<string>();

  claimConfirmOpen = signal(false);
  claimingTicketId = signal<string | null>(null);

  readonly categories = ['', 'NETWORK', 'HARDWARE', 'SOFTWARE', 'ACCESS', 'PERIPHERALS'];
  readonly urgencies = ['', 'LOW', 'MEDIUM', 'HIGH'];

  constructor() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(query => {
      this.searchQuery.set(query);
      this.loadTickets();
    });
  }

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.isLoading.set(true);
    this.isError.set(false);
    this.agentTicketService.getTickets({
      status: this.activeStatus() !== 'ALL' ? this.activeStatus() : undefined,
      category: this.selectedCategory() || undefined,
      urgency: this.selectedUrgency() || undefined,
      search: this.searchQuery() || undefined,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (tickets) => {
        this.tickets.set(tickets);
        this.filteredTickets.set(tickets);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.isError.set(true);
      },
    });
  }

  onSearchInput(value: string): void {
    this.searchSubject.next(value);
  }

  changeStatusTab(status: string): void {
    this.activeStatus.set(status);
    this.loadTickets();
  }

  onCategoryChange(): void {
    this.loadTickets();
  }

  onUrgencyChange(): void {
    this.loadTickets();
  }

  openClaimConfirm(id: string): void {
    this.claimingTicketId.set(id);
    this.claimConfirmOpen.set(true);
  }

  confirmClaim(): void {
    const id = this.claimingTicketId();
    if (!id) return;
    this.claimConfirmOpen.set(false);
    this.claimingTicketId.set(null);
    this.agentTicketService.claimTicket(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loadTickets(),
      error: () => alert('Failed to claim ticket.'),
    });
  }

  cancelClaim(): void {
    this.claimConfirmOpen.set(false);
    this.claimingTicketId.set(null);
  }

  statusLabels: Record<string, string> = {
    'NEW': 'New',
    'IN_PROGRESS': 'In Progress',
    'RESOLVED': 'Resolved',
    'CANCELLED': 'Cancelled',
  };

  urgencyLabels: Record<string, string> = {
    'LOW': 'Low',
    'MEDIUM': 'Medium',
    'HIGH': 'High',
  };

  categoryLabels: Record<string, string> = {
    'NETWORK': 'Network',
    'HARDWARE': 'Hardware',
    'SOFTWARE': 'Software',
    'ACCESS': 'Access',
    'PERIPHERALS': 'Peripherals',
  };

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'NEW': return 'bg-blue-100 text-blue-700';
      case 'IN_PROGRESS': return 'bg-amber-100 text-amber-700';
      case 'RESOLVED': return 'bg-green-100 text-green-700';
      case 'CANCELLED': return 'bg-gray-100 text-gray-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  categoryBadgeClass(category: string): string {
    switch (category) {
      case 'NETWORK': return 'bg-purple-100 text-purple-700';
      case 'HARDWARE': return 'bg-cyan-100 text-cyan-700';
      case 'SOFTWARE': return 'bg-indigo-100 text-indigo-700';
      case 'ACCESS': return 'bg-teal-100 text-teal-700';
      case 'PERIPHERALS': return 'bg-pink-100 text-pink-700';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  urgencyBadgeClass(urgency: string): string {
    switch (urgency) {
      case 'HIGH': return 'bg-red-100 text-red-700';
      case 'MEDIUM': return 'bg-amber-100 text-amber-700';
      case 'LOW': return 'bg-gray-100 text-gray-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  }
}
