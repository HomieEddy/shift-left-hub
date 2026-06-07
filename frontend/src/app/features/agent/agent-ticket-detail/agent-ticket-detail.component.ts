import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgIf, NgFor, DatePipe, NgClass } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { $localize } from '@angular/localize/init';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AgentTicketService } from '../agent-ticket.service';
import { AgentTicket, WorkNote } from '../agent-ticket.model';

@Component({
  selector: 'app-agent-ticket-detail',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, NgClass, RouterLink, FormsModule],
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

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
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
      error: (err) => console.error('Failed to load work notes:', err),
    });
  }

  /** Submits a new work note and refreshes the work notes list. */
  addWorkNote(): void {
    const content = this.newWorkNote().trim();
    if (!content) return;
    const ticketId = this.ticket()?.id;
    if (!ticketId) return;

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
      },
    });
  }

  /** Claims the current ticket for the authenticated agent. */
  claimTicket(): void {
    const ticketId = this.ticket()?.id;
    if (!ticketId) return;

    this.isClaiming.set(true);
    this.agentTicketService.claimTicket(ticketId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (ticket) => {
        this.ticket.set(ticket);
        this.isClaiming.set(false);
      },
      error: () => {
        this.isClaiming.set(false);
        console.error('Failed to claim ticket');
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
    if (!ticketId || !this.resolutionNotes().trim()) return;

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
      },
    });
  }

  /** Cancels the resolve confirmation dialog. */
  cancelResolveConfirm(): void {
    this.resolveConfirmOpen.set(false);
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

  loadingLabel = $localize`:@@agent.detail.loading:Loading ticket details...`;
  errorLabel = $localize`:@@agent.detail.error:Failed to load ticket details.`;
  retryLabel = $localize`:@@agent.detail.retry:Retry`;
  backToQueueLabel = $localize`:@@agent.detail.backToQueue:\u2190 Back to Queue`;
  shiftLeftContextLabel = $localize`:@@agent.detail.shiftLeftContext:Shift-Left Context`;
  issueLabel = $localize`:@@agent.detail.issue:Issue`;
  chatTranscriptLabel = $localize`:@@agent.detail.chatTranscript:Chat Transcript`;
  unclaimedLabel = $localize`:@@agent.detail.unclaimed:This ticket has not been claimed yet.`;
  claimingLabel = $localize`:@@agent.detail.claiming:Claiming...`;
  claimTicketLabel = $localize`:@@agent.detail.claimTicket:Claim Ticket`;
  workNotesLabel = $localize`:@@agent.detail.workNotes:Work Notes`;
  noWorkNotesLabel = $localize`:@@agent.detail.noWorkNotes:No work notes yet.`;
  addNotePlaceholder = $localize`:@@agent.detail.addNotePlaceholder:Add a work note...`;
  addingLabel = $localize`:@@agent.detail.adding:Adding...`;
  addNoteLabel = $localize`:@@agent.detail.addNote:Add Note`;
  resolutionLabel = $localize`:@@agent.detail.resolution:Resolution`;
  resolutionPlaceholder = $localize`:@@agent.detail.resolutionPlaceholder:Describe the resolution steps...`;
  flagKnowledgeGapLabel = $localize`:@@agent.detail.flagKnowledgeGap:Flag as Knowledge Gap`;
  resolvingLabel = $localize`:@@agent.detail.resolving:Resolving...`;
  resolveTicketLabel = $localize`:@@agent.detail.resolveTicket:Resolve Ticket`;
  resolvedByLabel = $localize`:@@agent.detail.resolvedBy:Resolved by`;
  unknownLabel = $localize`:@@agent.detail.unknown:Unknown`;
  flaggedKnowledgeGapLabel = $localize`:@@agent.detail.flaggedKnowledgeGap:Flagged as Knowledge Gap`;
  cancelledByUserLabel = $localize`:@@agent.detail.cancelledByUser:Cancelled by user`;
  unassignedLabel = $localize`:@@agent.detail.unassigned:Unassigned`;
  confirmResolutionLabel = $localize`:@@agent.detail.confirmResolution:Confirm resolution for`;
  cancelLabel = $localize`:@@agent.detail.cancel:Cancel`;
  confirmLabel = $localize`:@@agent.detail.confirm:Confirm`;
  assignedToLabel = $localize`:@@agent.detail.assignedTo:Assigned to`;
  openedByLabel = $localize`:@@agent.detail.openedBy:Opened by`;

  addNoteErrorLabel = $localize`:@@agent.detail.addNoteError:Failed to add work note.`;
  claimErrorLabel = $localize`:@@agent.detail.claimError:Failed to claim ticket.`;
  resolveErrorLabel = $localize`:@@agent.detail.resolveError:Failed to resolve ticket.`;

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

  /** Returns the appropriate Tailwind badge classes for an urgency level. */
  urgencyBadgeClass(urgency: string): string {
    switch (urgency) {
      case 'HIGH': return 'bg-red-100 text-red-700';
      case 'MEDIUM': return 'bg-amber-100 text-amber-700';
      case 'LOW': return 'bg-gray-100 text-gray-600';
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
}
