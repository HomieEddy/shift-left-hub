import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgIf, NgFor, DatePipe, NgClass } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { $localize } from '@angular/localize/init';
import { AgentTicketService } from '../agent-ticket.service';
import { AgentTicket } from '../agent-ticket.model';

@Component({
  selector: 'app-agent-ticket-list',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, NgClass, RouterLink, FormsModule],
  templateUrl: './agent-ticket-list.component.html',
})
/**
 * Smart component that displays the agent ticket list with filtering,
 * search, and claim capabilities.
 */
export class AgentTicketListComponent implements OnInit {
  private agentTicketService = inject(AgentTicketService);
  private router = inject(Router);
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

  allLabel = $localize`:@@agent.filter.all:All`;
  allCategoriesLabel = $localize`:@@agent.filter.allCategories:All Categories`;
  allUrgenciesLabel = $localize`:@@agent.filter.allUrgencies:All Urgencies`;
  searchPlaceholder = $localize`:@@agent.search.placeholder:Search by ticket # or user...`;
  loadingLabel = $localize`:@@agent.loading:Loading tickets...`;
  errorLabel = $localize`:@@agent.error.load:Failed to load tickets.`;
  retryLabel = $localize`:@@agent.retry:Retry`;
  emptyLabel = $localize`:@@agent.empty:No tickets found matching your filters`;
  claimLabel = $localize`:@@agent.claim:Claim`;
  resolveLabel = $localize`:@@agent.resolve:Resolve`;
  viewLabel = $localize`:@@agent.view:View`;
  unassignedLabel = $localize`:@@agent.unassigned:Unassigned`;
  claimConfirmLabel = $localize`:@@agent.claim.confirm:Claim this ticket?`;
  cancelLabel = $localize`:@@agent.cancel:Cancel`;
  confirmLabel = $localize`:@@agent.confirm:Confirm`;
  claimFailedAlert = $localize`:@@agent.claim.error:Failed to claim ticket.`;

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

  /** Loads the ticket list from the API with current filter values. */
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

  /** Handles debounced search input from the search field. */
  onSearchInput(value: string): void {
    this.searchSubject.next(value);
  }

  /** Changes the active status filter tab and reloads tickets. */
  changeStatusTab(status: string): void {
    this.activeStatus.set(status);
    this.loadTickets();
  }

  /** Handles category filter change and reloads tickets. */
  onCategoryChange(): void {
    this.loadTickets();
  }

  /** Handles urgency filter change and reloads tickets. */
  onUrgencyChange(): void {
    this.loadTickets();
  }

  /** Opens the claim confirmation dialog for a ticket. */
  openClaimConfirm(id: string): void {
    this.claimingTicketId.set(id);
    this.claimConfirmOpen.set(true);
  }

  /** Confirms claim and navigates to the ticket detail page. */
  confirmClaim(): void {
    const id = this.claimingTicketId();
    if (!id) return;
    this.claimConfirmOpen.set(false);
    this.claimingTicketId.set(null);
    this.agentTicketService.claimTicket(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.router.navigate(['/agent/tickets', id]),
      error: () => alert(this.claimFailedAlert),
    });
  }

  /** Cancels the claim confirmation dialog. */
  cancelClaim(): void {
    this.claimConfirmOpen.set(false);
    this.claimingTicketId.set(null);
  }

  statusLabels: Record<string, string> = {
    'NEW': $localize`:@@tickets.status.new:New`,
    'IN_PROGRESS': $localize`:@@tickets.status.in_progress:In Progress`,
    'RESOLVED': $localize`:@@tickets.status.resolved:Resolved`,
    'CANCELLED': $localize`:@@tickets.status.cancelled:Cancelled`,
  };

  urgencyLabels: Record<string, string> = {
    'LOW': $localize`:@@agent.urgency.low:Low`,
    'MEDIUM': $localize`:@@agent.urgency.medium:Medium`,
    'HIGH': $localize`:@@agent.urgency.high:High`,
  };

  categoryLabels: Record<string, string> = {
    'NETWORK': $localize`:@@tickets.category.network:Network`,
    'HARDWARE': $localize`:@@tickets.category.hardware:Hardware`,
    'SOFTWARE': $localize`:@@tickets.category.software:Software`,
    'ACCESS': $localize`:@@tickets.category.access:Access`,
    'PERIPHERALS': $localize`:@@tickets.category.peripherals:Peripherals`,
  };

  /** Returns the appropriate Tailwind badge classes for a ticket status. */
  statusBadgeClass(status: string): string {
    switch (status) {
      case 'NEW': return 'bg-blue-100 text-blue-700';
      case 'IN_PROGRESS': return 'bg-amber-100 text-amber-700';
      case 'RESOLVED': return 'bg-green-100 text-green-700';
      case 'CANCELLED': return 'bg-gray-100 text-gray-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  /** Returns the appropriate Tailwind badge classes for a ticket category. */
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

  /** Returns the appropriate Tailwind badge classes for an urgency level. */
  urgencyBadgeClass(urgency: string): string {
    switch (urgency) {
      case 'HIGH': return 'bg-red-100 text-red-700';
      case 'MEDIUM': return 'bg-amber-100 text-amber-700';
      case 'LOW': return 'bg-gray-100 text-gray-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  }
}
