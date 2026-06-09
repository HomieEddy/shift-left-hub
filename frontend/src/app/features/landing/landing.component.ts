import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { TranslationService } from '../../core/i18n/translation.service';
import {
  LucideMessageSquare,
  LucideBookOpen,
  LucideTicket,
  LucideSearch,
  LucideZap,
  LucideFileText,
  LucideSparkles,
  LucideUsers,
  LucideArrowRight,
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
    LucideUsers,
    LucideArrowRight,
  ],
  template: `
    @if (authService.isAuthenticated()) {
      <div class="min-h-[calc(100vh-3.5rem)] bg-surface-secondary">
        <div class="max-w-4xl mx-auto px-4 py-12">
          <div class="flex items-center gap-4 mb-8">
            <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-600 to-indigo-700 flex items-center justify-center text-white font-bold text-lg shadow-sm">
              {{ firstLetter }}
            </div>
            <div>
              <p class="text-lg font-semibold text-text-primary">{{ translationService.translate('landing.signedInAs') }} {{ authService.user()?.displayName }}</p>
              <p class="text-sm text-text-secondary">{{ translationService.translate('landing.welcome') }}</p>
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
            <a routerLink="/chat" class="group bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-blue-700 flex items-center justify-center mb-4">
                <lucide-message-square class="w-5 h-5 text-white"></lucide-message-square>
              </div>
              <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.dashboard.chat.title') }}</h3>
              <p class="text-sm text-text-secondary">{{ translationService.translate('landing.dashboard.chat.hint') }}</p>
            </a>
            <a routerLink="/articles" class="group bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-700 flex items-center justify-center mb-4">
                <lucide-book-open class="w-5 h-5 text-white"></lucide-book-open>
              </div>
              <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.dashboard.articles.title') }}</h3>
              <p class="text-sm text-text-secondary">{{ translationService.translate('landing.dashboard.articles.hint') }}</p>
            </a>
            <a routerLink="/tickets" class="group bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500 to-amber-700 flex items-center justify-center mb-4">
                <lucide-ticket class="w-5 h-5 text-white"></lucide-ticket>
              </div>
              <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.dashboard.tickets.title') }}</h3>
              <p class="text-sm text-text-secondary">{{ translationService.translate('landing.dashboard.tickets.hint') }}</p>
            </a>
          </div>

          @if (authService.isAgent()) {
            <div class="flex flex-wrap gap-3">
              <a routerLink="/agent/tickets" class="inline-flex items-center gap-2 px-4 py-2.5 bg-surface-primary border border-border-default rounded-xl text-sm font-medium text-text-primary hover:bg-surface-secondary transition-colors shadow-sm">
                <lucide-ticket class="w-4 h-4 text-text-tertiary"></lucide-ticket>
                {{ translationService.translate('landing.dashboard.agent.tickets') }}
                <lucide-arrow-right class="w-4 h-4 text-text-tertiary"></lucide-arrow-right>
              </a>
              <a routerLink="/admin/articles" class="inline-flex items-center gap-2 px-4 py-2.5 bg-surface-primary border border-border-default rounded-xl text-sm font-medium text-text-primary hover:bg-surface-secondary transition-colors shadow-sm">
                <lucide-file-text class="w-4 h-4 text-text-tertiary"></lucide-file-text>
                {{ translationService.translate('landing.dashboard.admin.articles') }}
                <lucide-arrow-right class="w-4 h-4 text-text-tertiary"></lucide-arrow-right>
              </a>
            </div>
          }
        </div>
      </div>
    } @else {
      <div class="min-h-[calc(100vh-3.5rem)]">
        <!-- Hero -->
        <section class="relative overflow-hidden bg-gradient-to-b from-slate-900 via-slate-800 to-slate-950 px-4 py-20 sm:py-28">
          <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,var(--color-primary-500)_0%,transparent_50%)] opacity-10"></div>
          <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_bottom_left,var(--color-primary-500)_0%,transparent_50%)] opacity-10"></div>

          <div class="relative max-w-3xl mx-auto text-center">
            <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-white/10 bg-white/5 text-sm text-slate-300 mb-6">
              <lucide-zap class="w-3.5 h-3.5 text-primary-400"></lucide-zap>
              {{ translationService.translate('landing.hero.badge') }}
            </div>

            <h1 class="text-4xl sm:text-5xl font-bold text-white mb-4 tracking-tight">
              {{ translationService.translate('landing.hero.title') }}
            </h1>
            <p class="text-lg text-slate-400 max-w-2xl mx-auto mb-10 leading-relaxed">
              {{ translationService.translate('landing.hero.subtitle') }}
            </p>

            <div class="flex items-center justify-center gap-4">
              <a routerLink="/register" class="rounded-xl bg-primary-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-primary-700 transition-colors shadow-sm">
                {{ translationService.translate('landing.cta.getStarted') }}
              </a>
              <a routerLink="/login" class="rounded-xl border border-slate-600 bg-white/5 px-6 py-2.5 text-sm font-medium text-white hover:bg-white/10 transition-colors">
                {{ translationService.translate('landing.cta.signin') }}
              </a>
            </div>
          </div>
        </section>

        <!-- How It Works -->
        <section class="bg-surface-primary px-4 py-16 sm:py-20">
          <div class="max-w-5xl mx-auto">
            <h2 class="text-2xl sm:text-3xl font-bold text-text-primary text-center mb-12">
              {{ translationService.translate('landing.howItWorks.title') }}
            </h2>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
              <div class="text-center">
                <div class="w-14 h-14 mx-auto mb-4 rounded-xl bg-surface-secondary border border-border-default flex items-center justify-center">
                  <lucide-message-square class="w-6 h-6 text-primary-600"></lucide-message-square>
                </div>
                <h3 class="font-semibold text-text-primary mb-2">{{ translationService.translate('landing.howItWorks.step1.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.howItWorks.step1.desc') }}</p>
              </div>
              <div class="text-center">
                <div class="w-14 h-14 mx-auto mb-4 rounded-xl bg-surface-secondary border border-border-default flex items-center justify-center">
                  <lucide-search class="w-6 h-6 text-primary-600"></lucide-search>
                </div>
                <h3 class="font-semibold text-text-primary mb-2">{{ translationService.translate('landing.howItWorks.step2.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.howItWorks.step2.desc') }}</p>
              </div>
              <div class="text-center">
                <div class="w-14 h-14 mx-auto mb-4 rounded-xl bg-surface-secondary border border-border-default flex items-center justify-center">
                  <lucide-sparkles class="w-6 h-6 text-primary-600"></lucide-sparkles>
                </div>
                <h3 class="font-semibold text-text-primary mb-2">{{ translationService.translate('landing.howItWorks.step3.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.howItWorks.step3.desc') }}</p>
              </div>
            </div>
            <p class="text-xs text-slate-500">{{ translationService.translate('app.footer') }}</p>
          </div>
        </section>

        <!-- Features -->
        <section class="bg-surface-secondary px-4 py-16 sm:py-20">
          <div class="max-w-5xl mx-auto">
            <h2 class="text-2xl sm:text-3xl font-bold text-text-primary text-center mb-12">
              {{ translationService.translate('landing.features.title') }}
            </h2>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center mb-4">
                  <lucide-search class="w-5 h-5 text-blue-600"></lucide-search>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.features.search.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.features.search.desc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-purple-100 flex items-center justify-center mb-4">
                  <lucide-sparkles class="w-5 h-5 text-purple-700"></lucide-sparkles>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.features.ai.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.features.ai.desc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-green-100 flex items-center justify-center mb-4">
                  <lucide-file-text class="w-5 h-5 text-green-600"></lucide-file-text>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.features.docs.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.features.docs.desc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center mb-4">
                  <lucide-ticket class="w-5 h-5 text-amber-600"></lucide-ticket>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.features.tickets.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.features.tickets.desc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-cyan-100 flex items-center justify-center mb-4">
                  <lucide-users class="w-5 h-5 text-cyan-700"></lucide-users>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.features.escalate.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.features.escalate.desc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center mb-4">
                  <lucide-message-square class="w-5 h-5 text-indigo-700"></lucide-message-square>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.features.bilingual.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.features.bilingual.desc') }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- CTA -->
        <section class="bg-gradient-to-r from-primary-600 to-indigo-700 px-4 py-16 sm:py-20">
          <div class="max-w-2xl mx-auto text-center">
            <h2 class="text-2xl sm:text-3xl font-bold text-white mb-4">
              {{ translationService.translate('landing.cta.section.title') }}
            </h2>
            <p class="text-blue-100 mb-8 leading-relaxed">
              {{ translationService.translate('landing.cta.section.subtitle') }}
            </p>
            <a routerLink="/register" class="inline-flex items-center gap-2 rounded-xl bg-white px-6 py-2.5 text-sm font-medium text-primary-600 hover:bg-blue-50 transition-colors shadow-sm">
              {{ translationService.translate('landing.cta.startToday') }}
              <lucide-arrow-right class="w-4 h-4"></lucide-arrow-right>
            </a>
          </div>
        </section>

        <!-- Footer -->
        <footer class="bg-slate-900 px-4 py-8 text-center text-sm text-slate-500">
          <p>&copy; {{ currentYear }} {{ translationService.translate('landing.footer.copyright') }}</p>
        </footer>
      </div>
    }
  `,
})
export class LandingComponent {
  protected authService = inject(AuthService);
  protected translationService = inject(TranslationService);

  protected get currentYear(): number {
    return new Date().getFullYear();
  }

  protected get firstLetter(): string {
    const name = this.authService.user()?.displayName ?? '';
    return name.length > 0 ? name.charAt(0).toUpperCase() : '?';
  }
}
