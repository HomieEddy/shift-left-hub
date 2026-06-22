import { Component, DestroyRef, inject, OnInit, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { AgentTicketService } from '../agent-ticket.service';
import { AgentTicket } from '../agent-ticket.model';
import {
  statusBadgeClass,
  categoryBadgeClass,
  urgencyBadgeClass,
} from '../../../shared/ui/badge/badge-utils';
import { TranslationService } from '../../../core/i18n/translation.service';
import { LoggerService } from '../../../core/logging/logger.service';

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
  private logger = inject(LoggerService);

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

  readonly statusBadgeClass = statusBadgeClass;
  readonly categoryBadgeClass = categoryBadgeClass;
  readonly urgencyBadgeClass = urgencyBadgeClass;

  allLabel = computed(() => this.translationService.translate('agent.filter.all'));
  allCategoriesLabel = computed(() =>
    this.translationService.translate('agent.filter.allCategories'),
  );
  allUrgenciesLabel = computed(() =>
    this.translationService.translate('agent.filter.allUrgencies'),
  );
  searchPlaceholder = computed(() => this.translationService.translate('agent.search.placeholder'));
  loadingLabel = computed(() => this.translationService.translate('agent.loading'));
  errorLabel = computed(() => this.translationService.translate('agent.error.load'));
  retryLabel = computed(() => this.translationService.translate('agent.retry'));
  emptyLabel = computed(() => this.translationService.translate('agent.empty'));
  claimLabel = computed(() => this.translationService.translate('agent.claim'));
  resolveLabel = computed(() => this.translationService.translate('agent.resolve'));
  viewLabel = computed(() => this.translationService.translate('agent.view'));
  unassignedLabel = computed(() => this.translationService.translate('agent.unassigned'));
  claimConfirmLabel = computed(() => this.translationService.translate('agent.claim.confirm'));
  cancelLabel = computed(() => this.translationService.translate('agent.cancel'));
  confirmLabel = computed(() => this.translationService.translate('agent.confirm'));
  claimFailedAlert = computed(() => this.translationService.translate('agent.claim.error'));

  constructor() {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((query) => {
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
    this.agentTicketService
      .getTickets({
        status: this.activeStatus() !== 'ALL' ? this.activeStatus() : undefined,
        category: this.selectedCategory() || undefined,
        urgency: this.selectedUrgency() || undefined,
        search: this.searchQuery() || undefined,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
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
    this.agentTicketService
      .claimTicket(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void this.router.navigate(['/agent/tickets', id]);
        },
        error: () => {
          this.logger.error('Failed to claim ticket');
          this.claimError.set('Failed to claim ticket. Please try again.');
        },
      });
  }

  /** Cancels the claim confirmation dialog. */
  cancelClaim(): void {
    this.claimConfirmOpen.set(false);
    this.claimingTicketId.set(null);
  }

  statusLabels = computed<Record<string, string>>(() => ({
    NEW: this.translationService.translate('tickets.status.new'),
    IN_PROGRESS: this.translationService.translate('tickets.status.in_progress'),
    RESOLVED: this.translationService.translate('tickets.status.resolved'),
    CANCELLED: this.translationService.translate('tickets.status.cancelled'),
  }));

  urgencyLabels = computed<Record<string, string>>(() => ({
    LOW: this.translationService.translate('agent.urgency.low'),
    MEDIUM: this.translationService.translate('agent.urgency.medium'),
    HIGH: this.translationService.translate('agent.urgency.high'),
  }));

  categoryLabels = computed<Record<string, string>>(() => ({
    NETWORK: this.translationService.translate('tickets.category.network'),
    HARDWARE: this.translationService.translate('tickets.category.hardware'),
    SOFTWARE: this.translationService.translate('tickets.category.software'),
    ACCESS: this.translationService.translate('tickets.category.access'),
    PERIPHERALS: this.translationService.translate('tickets.category.peripherals'),
  }));
}
