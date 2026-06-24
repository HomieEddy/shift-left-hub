import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { WorkspaceService } from '../admin/workspaces/workspace.service';
import { InvitationDto } from '../admin/workspaces/workspace.model';
import { TranslationService } from '../../core/i18n/translation.service';
import { LucideBell, LucideCheck, LucideX, LucideUsers } from '@lucide/angular';

@Component({
  selector: 'app-invitation-badge',
  standalone: true,
  imports: [LucideBell, LucideCheck, LucideX, LucideUsers],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './invitation-badge.component.html',
})
export class InvitationBadgeComponent implements OnInit {
  private workspaceService = inject(WorkspaceService);
  protected translationService = inject(TranslationService);
  private destroyRef = inject(DestroyRef);

  invitations = signal<InvitationDto[]>([]);
  isOpen = signal(false);
  isLoading = signal(true);
  processingIds = signal<Set<string>>(new Set());

  pendingCount = computed(() => this.invitations().length);
  hasPending = computed(() => this.pendingCount() > 0);

  ngOnInit() {
    this.loadInvitations();
  }

  toggleDropdown() {
    this.isOpen.update((v) => !v);
  }

  loadInvitations() {
    this.isLoading.set(true);
    this.workspaceService
      .getMyInvitations()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (invs) => {
          this.invitations.set(invs);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        },
      });
  }

  acceptInvitation(inv: InvitationDto) {
    this.processingIds.update((set) => {
      set.add(inv.id);
      return new Set(set);
    });
    this.workspaceService
      .acceptInvitation(inv.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.invitations.update((list) => list.filter((i) => i.id !== inv.id));
          this.processingIds.update((set) => {
            set.delete(inv.id);
            return new Set(set);
          });
        },
        error: () =>
          this.processingIds.update((set) => {
            set.delete(inv.id);
            return new Set(set);
          }),
      });
  }

  rejectInvitation(inv: InvitationDto) {
    this.processingIds.update((set) => {
      set.add(inv.id);
      return new Set(set);
    });
    this.workspaceService
      .rejectInvitation(inv.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.invitations.update((list) => list.filter((i) => i.id !== inv.id));
          this.processingIds.update((set) => {
            set.delete(inv.id);
            return new Set(set);
          });
        },
        error: () =>
          this.processingIds.update((set) => {
            set.delete(inv.id);
            return new Set(set);
          }),
      });
  }
}
