import { Component, DestroyRef, inject, isDevMode, signal } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { WorkspaceRoleService } from './core/auth/workspace-role.service';
import { TranslationService, SupportedLanguage } from './core/i18n/translation.service';
import { KcsDraftService } from './features/admin/kcs-draft.service';
import { interval, switchMap, filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  LucideMenu,
  LucideLogOut,
  LucideBookOpen,
  LucideMessageSquare,
  LucideTicket,
  LucideLayoutList,
  LucideUsers,
  LucideFileText,
  LucideClipboardList,
  LucideTag,
  LucideSettings,
  LucideLayoutDashboard,
  LucideX,
  LucideUpload,
} from '@lucide/angular';
import { ToastContainer } from './shared/ui/toast/toast-container.component';
import { WorkspaceSwitcherComponent } from './features/workspace-switcher/workspace-switcher.component';
import { InvitationBadgeComponent } from './features/workspace-switcher/invitation-badge.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    LucideMenu,
    LucideLogOut,
    LucideBookOpen,
    LucideMessageSquare,
    LucideTicket,
    LucideLayoutList,
    LucideUsers,
    LucideFileText,
    LucideClipboardList,
    LucideTag,
    LucideSettings,
    LucideLayoutDashboard,
    LucideX,
    LucideUpload,
    ToastContainer,
    WorkspaceSwitcherComponent,
    InvitationBadgeComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected authService = inject(AuthService);
  protected workspaceRoleService = inject(WorkspaceRoleService);
  protected translationService = inject(TranslationService);
  private router = inject(Router);
  private kcsDraftService = inject(KcsDraftService);
  private destroyRef = inject(DestroyRef);

  pendingKcsCount = signal(0);
  isMobileMenuOpen = signal(false);
  showLogoutModal = signal(false);
  private readonly hideRoutes = ['/login', '/register'];
  hideSidebar = signal(this.shouldHideSidebar(this.router.url));

  private shouldHideSidebar(url: string): boolean {
    return this.hideRoutes.some((r) => url === r || url.startsWith(r + '?'));
  }

  constructor() {
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((e) => this.hideSidebar.set(this.shouldHideSidebar(e.url)));

    interval(60000)
      .pipe(
        filter(() => this.authService.isAdmin()),
        switchMap(() => this.kcsDraftService.getPendingCount()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => this.pendingKcsCount.set(response.pendingCount),
        error: (err) => {
          if (isDevMode()) {
            console.warn('KCS pending-count poll failed:', err);
          }
        },
      });
  }

  switchLanguage(lang: SupportedLanguage): void {
    this.translationService.switchLanguage(lang);
  }

  confirmLogout(): void {
    this.showLogoutModal.set(true);
  }

  cancelLogout(): void {
    this.showLogoutModal.set(false);
  }

  logout(): void {
    this.showLogoutModal.set(false);
    this.authService
      .logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        void this.router.navigate(['/']);
      });
  }
}
