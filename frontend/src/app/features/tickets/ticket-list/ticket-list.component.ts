import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TicketService } from '../ticket.service';
import { Ticket } from '../ticket.model';
import { statusBadgeClass, categoryBadgeClass } from '../../../shared/ui/badge/badge-utils';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
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

  readonly statusBadgeClass = statusBadgeClass;
  readonly categoryBadgeClass = categoryBadgeClass;

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.ticketService
      .getTickets()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
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
      this.filteredTickets.set(this.tickets().filter((t) => t.status === status));
    }
  }

  allLabel = computed(() => this.translationService.translate('tickets.filter.all'));

  statusLabels = computed<Record<string, string>>(() => ({
    NEW: this.translationService.translate('tickets.status.new'),
    IN_PROGRESS: this.translationService.translate('tickets.status.in_progress'),
    RESOLVED: this.translationService.translate('tickets.status.resolved'),
    CANCELLED: this.translationService.translate('tickets.status.cancelled'),
  }));

  categoryLabels = computed<Record<string, string>>(() => ({
    NETWORK: this.translationService.translate('tickets.category.network'),
    HARDWARE: this.translationService.translate('tickets.category.hardware'),
    SOFTWARE: this.translationService.translate('tickets.category.software'),
    ACCESS: this.translationService.translate('tickets.category.access'),
    PERIPHERALS: this.translationService.translate('tickets.category.peripherals'),
  }));
}
