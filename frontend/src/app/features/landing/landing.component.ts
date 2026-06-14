import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { WorkspaceService } from '../admin/workspaces/workspace.service';
import { WorkspaceDto } from '../admin/workspaces/workspace.model';
import {
  LucideMessageSquare,
  LucideBookOpen,
  LucideTicket,
  LucideSearch,
  LucideZap,
  LucideFileText,
  LucideSparkles,
  LucideArrowRight,
  LucideLayoutDashboard,
  LucideInbox,
} from '@lucide/angular';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    RouterLink,
    LucideMessageSquare,
    LucideBookOpen,
    LucideTicket,
    LucideSearch,
    LucideZap,
    LucideFileText,
    LucideSparkles,
    LucideArrowRight,
    LucideLayoutDashboard,
    LucideInbox,
  ],
  templateUrl: './landing.component.html',
})
export class LandingComponent implements OnInit {
  protected authService = inject(AuthService);
  protected translationService = inject(TranslationService);
  private workspaceService = inject(WorkspaceService);
  private destroyRef = inject(DestroyRef);

  workspaces = signal<WorkspaceDto[]>([]);

  currentWorkspace = computed(() => {
    const user = this.authService.user();
    const list = this.workspaces();
    if (!user) { return null; }
    return list.find(ws => ws.id === user.workspaceId) ?? list[0] ?? null;
  });

  ngOnInit() {
    this.workspaceService.getMyWorkspaces()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => {
          this.workspaces.set([]);
          return [];
        }),
      )
      .subscribe(ws => this.workspaces.set(ws));
  }

  protected get firstLetter(): string {
    const name = this.authService.user()?.displayName ?? '';
    return name.length > 0 ? name.charAt(0).toUpperCase() : '?';
  }
}
