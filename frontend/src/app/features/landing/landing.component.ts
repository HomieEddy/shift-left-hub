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
  LucideLayoutDashboard,
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
    LucideLayoutDashboard,
  ],
  template: `
    @if (authService.isAuthenticated()) {
      <div class="min-h-[calc(100vh-3.5rem)] bg-surface-secondary">
        <div class="max-w-5xl mx-auto px-4 py-10 sm:py-14">

          <!-- Welcome -->
          <div class="flex flex-col sm:flex-row items-start sm:items-center gap-4 sm:gap-6 mb-10">
            <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary-500 to-indigo-600 flex items-center justify-center text-white font-bold text-xl shadow-sm flex-shrink-0">
              {{ firstLetter }}
            </div>
            <div>
              <h1 class="text-2xl font-bold text-text-primary">{{ translationService.translate('landing.signedInAs') }} {{ authService.user()?.displayName }}</h1>
              <p class="text-text-secondary mt-1">{{ translationService.translate('landing.hero.description') }}</p>
            </div>
          </div>

          <!-- Quick actions -->
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-10">
            <a routerLink="/chat" class="group bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-primary-200 transition-all">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-blue-700 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                <lucide-message-square class="w-5 h-5 text-white"></lucide-message-square>
              </div>
              <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('nav.ai-assistant') }}</h3>
              <p class="text-sm text-text-secondary">{{ translationService.translate('landing.feature.aiDesc') }}</p>
            </a>
            <a routerLink="/articles" class="group bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-emerald-200 transition-all">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-700 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                <lucide-book-open class="w-5 h-5 text-white"></lucide-book-open>
              </div>
              <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('nav.knowledge-base') }}</h3>
              <p class="text-sm text-text-secondary">{{ translationService.translate('landing.feature.searchDesc') }}</p>
            </a>
            <a routerLink="/tickets" class="group bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-amber-200 transition-all">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500 to-amber-700 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                <lucide-ticket class="w-5 h-5 text-white"></lucide-ticket>
              </div>
              <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('nav.my-tickets') }}</h3>
              <p class="text-sm text-text-secondary">{{ translationService.translate('landing.feature.docsDesc') }}</p>
            </a>
          </div>

          <!-- How it works (abbreviated) -->
          <div class="mb-10">
            <h2 class="text-lg font-semibold text-text-primary mb-5">{{ translationService.translate('landing.how.title') }}</h2>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
              <div class="flex gap-3">
                <div class="w-8 h-8 rounded-lg bg-surface-tertiary flex items-center justify-center flex-shrink-0">
                  <span class="text-sm font-bold text-primary-600">1</span>
                </div>
                <div>
                  <h3 class="font-medium text-sm text-text-primary mb-1">{{ translationService.translate('landing.how.step1.title') }}</h3>
                  <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.how.step1.desc') }}</p>
                </div>
              </div>
              <div class="flex gap-3">
                <div class="w-8 h-8 rounded-lg bg-surface-tertiary flex items-center justify-center flex-shrink-0">
                  <span class="text-sm font-bold text-primary-600">2</span>
                </div>
                <div>
                  <h3 class="font-medium text-sm text-text-primary mb-1">{{ translationService.translate('landing.how.step2.title') }}</h3>
                  <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.how.step2.desc') }}</p>
                </div>
              </div>
              <div class="flex gap-3">
                <div class="w-8 h-8 rounded-lg bg-surface-tertiary flex items-center justify-center flex-shrink-0">
                  <span class="text-sm font-bold text-primary-600">3</span>
                </div>
                <div>
                  <h3 class="font-medium text-sm text-text-primary mb-1">{{ translationService.translate('landing.how.step3.title') }}</h3>
                  <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.how.step3.desc') }}</p>
                </div>
              </div>
            </div>
          </div>

          @if (authService.isAdmin() || authService.isAgent()) {
            <div>
              <h2 class="text-lg font-semibold text-text-primary mb-4">
                {{ authService.isAdmin() ? translationService.translate('nav.section.admin') : translationService.translate('nav.section.agent') }}
              </h2>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <a routerLink="/agent/tickets" class="flex items-center gap-3 px-4 py-3 bg-surface-primary border border-border-default rounded-xl hover:bg-surface-secondary transition-colors shadow-sm">
                  <lucide-layout-dashboard class="w-5 h-5 text-amber-600"></lucide-layout-dashboard>
                  <span class="text-sm font-medium text-text-primary">{{ translationService.translate('nav.ticket-queue') }}</span>
                  <lucide-arrow-right class="w-4 h-4 text-text-tertiary ml-auto"></lucide-arrow-right>
                </a>
                @if (authService.isAdmin()) {
                  <a routerLink="/admin/users" class="flex items-center gap-3 px-4 py-3 bg-surface-primary border border-border-default rounded-xl hover:bg-surface-secondary transition-colors shadow-sm">
                    <lucide-users class="w-5 h-5 text-indigo-600"></lucide-users>
                    <span class="text-sm font-medium text-text-primary">{{ translationService.translate('nav.users') }}</span>
                    <lucide-arrow-right class="w-4 h-4 text-text-tertiary ml-auto"></lucide-arrow-right>
                  </a>
                  <a routerLink="/admin/articles" class="flex items-center gap-3 px-4 py-3 bg-surface-primary border border-border-default rounded-xl hover:bg-surface-secondary transition-colors shadow-sm">
                    <lucide-file-text class="w-5 h-5 text-indigo-600"></lucide-file-text>
                    <span class="text-sm font-medium text-text-primary">{{ translationService.translate('nav.articles') }}</span>
                    <lucide-arrow-right class="w-4 h-4 text-text-tertiary ml-auto"></lucide-arrow-right>
                  </a>
                  <a routerLink="/admin/settings/llm" class="flex items-center gap-3 px-4 py-3 bg-surface-primary border border-border-default rounded-xl hover:bg-surface-secondary transition-colors shadow-sm">
                    <lucide-zap class="w-5 h-5 text-indigo-600"></lucide-zap>
                    <span class="text-sm font-medium text-text-primary">{{ translationService.translate('nav.ai-settings') }}</span>
                    <lucide-arrow-right class="w-4 h-4 text-text-tertiary ml-auto"></lucide-arrow-right>
                  </a>
                }
              </div>
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
