import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { AgentTicketService } from '../agent-ticket.service';
import { TranslationService } from '../../../core/i18n/translation.service';
import { AgentTicket } from '../agent-ticket.model';

@Component({
  selector: 'app-agent-ticket-list',
  standalone: true,
  imports: [DatePipe, RouterLink, FormsModule],
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
  protected translationService = inject(TranslationService);

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
  claimError = signal<string | null>(null);

  readonly categories = ['', 'NETWORK', 'HARDWARE', 'SOFTWARE', 'ACCESS', 'PERIPHERALS'];
  readonly urgencies = ['', 'LOW', 'MEDIUM', 'HIGH'];

  get allLabel(): string { return this.translationService.translate('agent.filter.all'); }
  get allCategoriesLabel(): string { return this.translationService.translate('agent.filter.allCategories'); }
  get allUrgenciesLabel(): string { return this.translationService.translate('agent.filter.allUrgencies'); }
  get searchPlaceholder(): string { return this.translationService.translate('agent.search.placeholder'); }
  get loadingLabel(): string { return this.translationService.translate('agent.loading'); }
  get errorLabel(): string { return this.translationService.translate('agent.error.load'); }
  get retryLabel(): string { return this.translationService.translate('agent.retry'); }
  get emptyLabel(): string { return this.translationService.translate('agent.empty'); }
  get claimLabel(): string { return this.translationService.translate('agent.claim'); }
  get resolveLabel(): string { return this.translationService.translate('agent.resolve'); }
  get viewLabel(): string { return this.translationService.translate('agent.view'); }
  get unassignedLabel(): string { return this.translationService.translate('agent.unassigned'); }
  get claimConfirmLabel(): string { return this.translationService.translate('agent.claim.confirm'); }
  get cancelLabel(): string { return this.translationService.translate('agent.cancel'); }
  get confirmLabel(): string { return this.translationService.translate('agent.confirm'); }
  get claimFailedAlert(): string { return this.translationService.translate('agent.claim.error'); }

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
    if (id == null) return;
    this.claimConfirmOpen.set(false);
    this.claimingTicketId.set(null);
    this.agentTicketService.claimTicket(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { void this.router.navigate(['/agent/tickets', id]); },
      error: () => {
        console.error('Failed to claim ticket');
        this.claimError.set(this.translationService.translate('agent.claim.error.detail'));
      },
    });
  }

  /** Cancels the claim confirmation dialog. */
  cancelClaim(): void {
    this.claimConfirmOpen.set(false);
    this.claimingTicketId.set(null);
  }

  get statusLabels(): Record<string, string> {
    return {
      'NEW': this.translationService.translate('tickets.status.new'),
      'IN_PROGRESS': this.translationService.translate('tickets.status.in_progress'),
      'RESOLVED': this.translationService.translate('tickets.status.resolved'),
      'CANCELLED': this.translationService.translate('tickets.status.cancelled'),
    };
  }

  get urgencyLabels(): Record<string, string> {
    return {
      'LOW': this.translationService.translate('agent.urgency.low'),
      'MEDIUM': this.translationService.translate('agent.urgency.medium'),
      'HIGH': this.translationService.translate('agent.urgency.high'),
    };
  }

  get categoryLabels(): Record<string, string> {
    return {
      'NETWORK': this.translationService.translate('tickets.category.network'),
      'HARDWARE': this.translationService.translate('tickets.category.hardware'),
      'SOFTWARE': this.translationService.translate('tickets.category.software'),
      'ACCESS': this.translationService.translate('tickets.category.access'),
      'PERIPHERALS': this.translationService.translate('tickets.category.peripherals'),
    };
  }

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
