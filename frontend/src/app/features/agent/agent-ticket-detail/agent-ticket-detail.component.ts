import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AgentTicketService } from '../agent-ticket.service';
import { TranslationService } from '../../../core/i18n/translation.service';
import { AgentTicket, WorkNote } from '../agent-ticket.model';
import { statusBadgeClass, categoryBadgeClass, urgencyBadgeClass } from '../../../shared/ui/badge/badge-utils';

@Component({
  selector: 'app-agent-ticket-detail',
  standalone: true,
  imports: [DatePipe, RouterLink, FormsModule],
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

  noteError = signal<string | null>(null);
  claimError = signal<string | null>(null);
  resolveError = signal<string | null>(null);
  workNoteError = signal<string | null>(null);

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
    this.agentTicketService.getTicket(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
    this.agentTicketService.getWorkNotes(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (notes) => this.workNotes.set(notes),
      error: (err) => {
        console.error('Failed to load work notes:', err);
        this.noteError.set(this.translationService.translate('agent.detail.loadNotesError'));
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
    this.agentTicketService.addWorkNote(ticketId, content).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.newWorkNote.set('');
        this.isSubmittingNote.set(false);
        this.loadWorkNotes(ticketId);
      },
      error: () => {
        this.isSubmittingNote.set(false);
        console.error('Failed to add work note');
        this.workNoteError.set(this.translationService.translate('agent.detail.addNoteError.detail'));
      },
    });
  }

  /** Claims the current ticket for the authenticated agent. */
  claimTicket(): void {
    const ticketId = this.ticket()?.id;
    if (ticketId == null) return;

    this.isClaiming.set(true);
    this.agentTicketService.claimTicket(ticketId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (ticket) => {
        this.ticket.set(ticket);
        this.isClaiming.set(false);
      },
      error: () => {
        this.isClaiming.set(false);
        console.error('Failed to claim ticket');
        this.claimError.set(this.translationService.translate('agent.detail.claimError.detail'));
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
    this.agentTicketService.resolveTicket(ticketId, {
      resolutionNotes: this.resolutionNotes().trim(),
      isKnowledgeGap: this.isKnowledgeGap(),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (ticket) => {
        this.ticket.set(ticket);
        this.isResolving.set(false);
      },
      error: () => {
        this.isResolving.set(false);
        console.error('Failed to resolve ticket');
        this.resolveError.set(this.translationService.translate('agent.detail.resolveError.detail'));
      },
    });
  }

  /** Cancels the resolve confirmation dialog. */
  cancelResolveConfirm(): void {
    this.resolveConfirmOpen.set(false);
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

  get loadingLabel(): string { return this.translationService.translate('agent.detail.loading'); }
  get errorLabel(): string { return this.translationService.translate('agent.detail.error'); }
  get retryLabel(): string { return this.translationService.translate('agent.detail.retry'); }
  get backToQueueLabel(): string { return this.translationService.translate('agent.detail.backToQueue'); }
  get shiftLeftContextLabel(): string { return this.translationService.translate('agent.detail.shiftLeftContext'); }
  get issueLabel(): string { return this.translationService.translate('agent.detail.issue'); }
  get chatTranscriptLabel(): string { return this.translationService.translate('agent.detail.chatTranscript'); }
  get unclaimedLabel(): string { return this.translationService.translate('agent.detail.unclaimed'); }
  get claimingLabel(): string { return this.translationService.translate('agent.detail.claiming'); }
  get claimTicketLabel(): string { return this.translationService.translate('agent.detail.claimTicket'); }
  get workNotesLabel(): string { return this.translationService.translate('agent.detail.workNotes'); }
  get noWorkNotesLabel(): string { return this.translationService.translate('agent.detail.noWorkNotes'); }
  get addNotePlaceholder(): string { return this.translationService.translate('agent.detail.addNotePlaceholder'); }
  get addingLabel(): string { return this.translationService.translate('agent.detail.adding'); }
  get addNoteLabel(): string { return this.translationService.translate('agent.detail.addNote'); }
  get resolutionLabel(): string { return this.translationService.translate('agent.detail.resolution'); }
  get resolutionPlaceholder(): string { return this.translationService.translate('agent.detail.resolutionPlaceholder'); }
  get flagKnowledgeGapLabel(): string { return this.translationService.translate('agent.detail.flagKnowledgeGap'); }
  get resolvingLabel(): string { return this.translationService.translate('agent.detail.resolving'); }
  get resolveTicketLabel(): string { return this.translationService.translate('agent.detail.resolveTicket'); }
  get resolvedByLabel(): string { return this.translationService.translate('agent.detail.resolvedBy'); }
  get unknownLabel(): string { return this.translationService.translate('agent.detail.unknown'); }
  get flaggedKnowledgeGapLabel(): string { return this.translationService.translate('agent.detail.flaggedKnowledgeGap'); }
  get cancelledByUserLabel(): string { return this.translationService.translate('agent.detail.cancelledByUser'); }
  get unassignedLabel(): string { return this.translationService.translate('agent.detail.unassigned'); }
  get confirmResolutionLabel(): string { return this.translationService.translate('agent.detail.confirmResolution'); }
  get cancelLabel(): string { return this.translationService.translate('agent.detail.cancel'); }
  get confirmLabel(): string { return this.translationService.translate('agent.detail.confirm'); }
  get assignedToLabel(): string { return this.translationService.translate('agent.detail.assignedTo'); }
  get openedByLabel(): string { return this.translationService.translate('agent.detail.openedBy'); }

  addNoteErrorLabel = $localize`:@@agent.detail.addNoteError:Failed to add work note.`;
  claimErrorLabel = $localize`:@@agent.detail.claimError:Failed to claim ticket.`;
  resolveErrorLabel = $localize`:@@agent.detail.resolveError:Failed to resolve ticket.`;
}
