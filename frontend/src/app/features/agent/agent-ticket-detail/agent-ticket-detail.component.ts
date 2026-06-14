import { Component, DestroyRef, computed, inject, isDevMode, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MarkdownModule } from 'ngx-markdown';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AgentTicketService } from '../agent-ticket.service';
import { AgentTicket, WorkNote } from '../agent-ticket.model';
import {
  statusBadgeClass,
  categoryBadgeClass,
  urgencyBadgeClass,
} from '../../../shared/ui/badge/badge-utils';
import { ShiftLeftContext } from '../../tickets/ticket.model';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-agent-ticket-detail',
  standalone: true,
  imports: [DatePipe, RouterLink, FormsModule, MarkdownModule],
  templateUrl: './agent-ticket-detail.component.html',
})
/**
 * Smart component that displays the detail view of a single agent ticket,
 * including work notes, context sections, and claim/resolve actions.
 */
export class AgentTicketDetailComponent implements OnInit {
  private agentTicketService = inject(AgentTicketService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  ticket = signal<AgentTicket | null>(null);
  workNotes = signal<WorkNote[]>([]);
  isLoading = signal(true);
  isError = signal(false);

  contextExpanded = signal(false);
  transcriptExpanded = signal(false);
  sourcesExpanded = signal(false);

  newWorkNote = signal('');
  resolutionNotes = signal('');
  isKnowledgeGap = signal(false);
  isSubmittingNote = signal(false);
  isResolving = signal(false);

  resolveConfirmOpen = signal(false);
  isClaiming = signal(false);

  claimError = signal<string | null>(null);
  resolveError = signal<string | null>(null);
  workNoteError = signal<string | null>(null);

  parsedContext = computed(() => {
    const raw = this.ticket()?.shiftLeftContext;
    if (raw == null) return null;
    try {
      return JSON.parse(raw) as ShiftLeftContext;
    } catch {
      return null;
    }
  });

  transcriptMessages = computed(() => this.parsedContext()?.transcript ?? []);

  readonly statusBadgeClass = statusBadgeClass;
  readonly categoryBadgeClass = categoryBadgeClass;
  readonly urgencyBadgeClass = urgencyBadgeClass;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id != null) {
      this.loadTicket(id);
      this.loadWorkNotes(id);
    }
  }

  /** Loads the full ticket detail from the API. */
  loadTicket(id: string): void {
    this.isLoading.set(true);
    this.isError.set(false);
    this.agentTicketService
      .getTicket(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ticket) => {
          this.ticket.set(ticket);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.isError.set(true);
        },
      });
  }

  /** Loads work notes for the ticket from the API. */
  loadWorkNotes(id: string): void {
    this.agentTicketService
      .getWorkNotes(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (notes) => this.workNotes.set(notes),
        error: (err) => {
          if (isDevMode()) {
            console.error('Failed to load work notes:', err);
          }
          this.workNoteError.set('Failed to load work notes.');
        },
      });
  }

  /** Submits a new work note and refreshes the work notes list. */
  addWorkNote(): void {
    const content = this.newWorkNote().trim();
    if (!content) return;
    const ticketId = this.ticket()?.id;
    if (ticketId == null) return;

    this.isSubmittingNote.set(true);
    this.agentTicketService
      .addWorkNote(ticketId, content)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.newWorkNote.set('');
          this.isSubmittingNote.set(false);
          this.loadWorkNotes(ticketId);
        },
        error: () => {
          this.isSubmittingNote.set(false);
          if (isDevMode()) {
            console.error('Failed to add work note');
          }
          this.workNoteError.set('Failed to add work note. Please try again.');
        },
      });
  }

  /** Claims the current ticket for the authenticated agent. */
  claimTicket(): void {
    const ticketId = this.ticket()?.id;
    if (ticketId == null) return;

    this.isClaiming.set(true);
    this.agentTicketService
      .claimTicket(ticketId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ticket) => {
          this.ticket.set(ticket);
          this.isClaiming.set(false);
        },
        error: () => {
          this.isClaiming.set(false);
          if (isDevMode()) {
            console.error('Failed to claim ticket');
          }
          this.claimError.set('Failed to claim ticket. Please try again.');
        },
      });
  }

  /** Opens the resolve confirmation dialog. */
  openResolveConfirm(): void {
    this.resolveConfirmOpen.set(true);
  }

  /** Confirms ticket resolution and submits to the API. */
  confirmResolve(): void {
    const ticketId = this.ticket()?.id;
    if (ticketId == null) return;
    if (this.resolutionNotes().trim() === '') return;

    this.isResolving.set(true);
    this.resolveConfirmOpen.set(false);
    this.agentTicketService
      .resolveTicket(ticketId, {
        resolutionNotes: this.resolutionNotes().trim(),
        isKnowledgeGap: this.isKnowledgeGap(),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ticket) => {
          this.ticket.set(ticket);
          this.isResolving.set(false);
        },
        error: () => {
          this.isResolving.set(false);
          if (isDevMode()) {
            console.error('Failed to resolve ticket');
          }
          this.resolveError.set('Failed to resolve ticket. Please try again.');
        },
      });
  }

  /** Cancels the resolve confirmation dialog. */
  cancelResolveConfirm(): void {
    this.resolveConfirmOpen.set(false);
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

  loadingLabel = computed(() => this.translationService.translate('agent.detail.loading'));
  errorLabel = computed(() => this.translationService.translate('agent.detail.error'));
  retryLabel = computed(() => this.translationService.translate('agent.detail.retry'));
  backToQueueLabel = computed(() => this.translationService.translate('agent.detail.backToQueue'));
  shiftLeftContextLabel = computed(() =>
    this.translationService.translate('agent.detail.shiftLeftContext'),
  );
  issueLabel = computed(() => this.translationService.translate('agent.detail.issue'));
  chatTranscriptLabel = computed(() =>
    this.translationService.translate('agent.detail.chatTranscript'),
  );
  unclaimedLabel = computed(() => this.translationService.translate('agent.detail.unclaimed'));
  claimingLabel = computed(() => this.translationService.translate('agent.detail.claiming'));
  claimTicketLabel = computed(() => this.translationService.translate('agent.detail.claimTicket'));
  workNotesLabel = computed(() => this.translationService.translate('agent.detail.workNotes'));
  noWorkNotesLabel = computed(() => this.translationService.translate('agent.detail.noWorkNotes'));
  addNotePlaceholder = computed(() =>
    this.translationService.translate('agent.detail.addNotePlaceholder'),
  );
  addingLabel = computed(() => this.translationService.translate('agent.detail.adding'));
  addNoteLabel = computed(() => this.translationService.translate('agent.detail.addNote'));
  resolutionLabel = computed(() => this.translationService.translate('agent.detail.resolution'));
  resolutionPlaceholder = computed(() =>
    this.translationService.translate('agent.detail.resolutionPlaceholder'),
  );
  flagKnowledgeGapLabel = computed(() =>
    this.translationService.translate('agent.detail.flagKnowledgeGap'),
  );
  resolvingLabel = computed(() => this.translationService.translate('agent.detail.resolving'));
  resolveTicketLabel = computed(() =>
    this.translationService.translate('agent.detail.resolveTicket'),
  );
  resolvedByLabel = computed(() => this.translationService.translate('agent.detail.resolvedBy'));
  unknownLabel = computed(() => this.translationService.translate('agent.detail.unknown'));
  flaggedKnowledgeGapLabel = computed(() =>
    this.translationService.translate('agent.detail.flaggedKnowledgeGap'),
  );
  cancelledByUserLabel = computed(() =>
    this.translationService.translate('agent.detail.cancelledByUser'),
  );
  unassignedLabel = computed(() => this.translationService.translate('agent.detail.unassigned'));
  confirmResolutionLabel = computed(() =>
    this.translationService.translate('agent.detail.confirmResolution'),
  );
  cancelLabel = computed(() => this.translationService.translate('agent.detail.cancel'));
  confirmLabel = computed(() => this.translationService.translate('agent.detail.confirm'));
  assignedToLabel = computed(() => this.translationService.translate('agent.detail.assignedTo'));
  openedByLabel = computed(() => this.translationService.translate('agent.detail.openedBy'));
}
