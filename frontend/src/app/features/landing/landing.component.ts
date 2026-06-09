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
  LucideSettings,
  LucideClipboardList,
  LucideTag,
  LucideShield,
  LucideCheckCircle,
  LucideHelpCircle,
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
    LucideUsers,
    LucideArrowRight,
    LucideLayoutDashboard,
    LucideSettings,
    LucideClipboardList,
    LucideTag,
    LucideShield,
    LucideCheckCircle,
    LucideHelpCircle,
    LucideInbox,
  ],
  template: `
    @if (authService.isAuthenticated()) {

      @if (authService.isAdmin()) {
        <!-- ========== ADMIN DASHBOARD ========== -->
        <div class="min-h-[calc(100vh-3.5rem)] bg-surface-secondary">
          <div class="max-w-6xl mx-auto px-4 py-10 sm:py-14">

            <div class="flex flex-col sm:flex-row items-start sm:items-center gap-4 sm:gap-5 mb-10">
              <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white font-bold text-xl shadow-sm flex-shrink-0">
                {{ firstLetter }}
              </div>
              <div>
                <div class="flex items-center gap-2 mb-1">
                  <h1 class="text-2xl font-bold text-text-primary">{{ authService.user()?.displayName }}</h1>
                  <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-indigo-100 text-indigo-700">{{ translationService.translate('admin.users.role.admin') }}</span>
                </div>
                <p class="text-text-secondary">{{ translationService.translate('landing.hero.description') }}</p>
              </div>
            </div>

            <h2 class="text-lg font-semibold text-text-primary mb-4">{{ translationService.translate('nav.section.admin') }}</h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-10">
              <a routerLink="/admin/users" class="flex items-start gap-4 bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-indigo-200 transition-all group">
                <div class="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center flex-shrink-0">
                  <lucide-users class="w-5 h-5 text-indigo-600"></lucide-users>
                </div>
                <div class="flex-1 min-w-0">
                  <h3 class="font-semibold text-text-primary text-sm">{{ translationService.translate('nav.users') }}</h3>
                   <p class="text-xs text-text-secondary mt-1">{{ translationService.translate('landing.dashboard.admin.usersDesc') }}</p>
                </div>
                <lucide-arrow-right class="w-4 h-4 text-text-tertiary flex-shrink-0 group-hover:translate-x-0.5 transition-transform"></lucide-arrow-right>
              </a>

              <a routerLink="/admin/articles" class="flex items-start gap-4 bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-indigo-200 transition-all group">
                <div class="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center flex-shrink-0">
                  <lucide-file-text class="w-5 h-5 text-indigo-600"></lucide-file-text>
                </div>
                <div class="flex-1 min-w-0">
                  <h3 class="font-semibold text-text-primary text-sm">{{ translationService.translate('nav.articles') }}</h3>
                   <p class="text-xs text-text-secondary mt-1">{{ translationService.translate('landing.dashboard.admin.articlesDesc') }}</p>
                 </div>
                 <lucide-arrow-right class="w-4 h-4 text-text-tertiary flex-shrink-0 group-hover:translate-x-0.5 transition-transform"></lucide-arrow-right>
               </a>

               <a routerLink="/admin/tags" class="flex items-start gap-4 bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-indigo-200 transition-all group">
                 <div class="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center flex-shrink-0">
                   <lucide-tag class="w-5 h-5 text-indigo-600"></lucide-tag>
                 </div>
                 <div class="flex-1 min-w-0">
                   <h3 class="font-semibold text-text-primary text-sm">{{ translationService.translate('nav.tags') }}</h3>
                   <p class="text-xs text-text-secondary mt-1">{{ translationService.translate('landing.dashboard.admin.tagsDesc') }}</p>
                </div>
                <lucide-arrow-right class="w-4 h-4 text-text-tertiary flex-shrink-0 group-hover:translate-x-0.5 transition-transform"></lucide-arrow-right>
              </a>

              <a routerLink="/admin/kcs-drafts" class="flex items-start gap-4 bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-indigo-200 transition-all group">
                <div class="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center flex-shrink-0">
                  <lucide-clipboard-list class="w-5 h-5 text-indigo-600"></lucide-clipboard-list>
                </div>
                <div class="flex-1 min-w-0">
                  <h3 class="font-semibold text-text-primary text-sm">{{ translationService.translate('nav.kcs-drafts') }}</h3>
                   <p class="text-xs text-text-secondary mt-1">{{ translationService.translate('landing.dashboard.admin.draftsDesc') }}</p>
                </div>
                <lucide-arrow-right class="w-4 h-4 text-text-tertiary flex-shrink-0 group-hover:translate-x-0.5 transition-transform"></lucide-arrow-right>
              </a>

              <a routerLink="/admin/settings/llm" class="flex items-start gap-4 bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-indigo-200 transition-all group">
                <div class="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center flex-shrink-0">
                  <lucide-settings class="w-5 h-5 text-indigo-600"></lucide-settings>
                </div>
                <div class="flex-1 min-w-0">
                  <h3 class="font-semibold text-text-primary text-sm">{{ translationService.translate('nav.ai-settings') }}</h3>
                   <p class="text-xs text-text-secondary mt-1">{{ translationService.translate('landing.dashboard.admin.settingsDesc') }}</p>
                </div>
                <lucide-arrow-right class="w-4 h-4 text-text-tertiary flex-shrink-0 group-hover:translate-x-0.5 transition-transform"></lucide-arrow-right>
              </a>

              <a routerLink="/agent/tickets" class="flex items-start gap-4 bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-amber-200 transition-all group">
                <div class="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center flex-shrink-0">
                  <lucide-layout-dashboard class="w-5 h-5 text-amber-600"></lucide-layout-dashboard>
                </div>
                <div class="flex-1 min-w-0">
                  <h3 class="font-semibold text-text-primary text-sm">{{ translationService.translate('nav.ticket-queue') }}</h3>
                   <p class="text-xs text-text-secondary mt-1">{{ translationService.translate('landing.dashboard.admin.queueDesc') }}</p>
                </div>
                <lucide-arrow-right class="w-4 h-4 text-text-tertiary flex-shrink-0 group-hover:translate-x-0.5 transition-transform"></lucide-arrow-right>
              </a>
            </div>
          </div>
        </div>

      } @else if (authService.isAgent()) {
        <!-- ========== AGENT DASHBOARD ========== -->
        <div class="min-h-[calc(100vh-3.5rem)] bg-surface-secondary">
          <div class="max-w-5xl mx-auto px-4 py-10 sm:py-14">

            <div class="flex flex-col sm:flex-row items-start sm:items-center gap-4 sm:gap-5 mb-10">
              <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center text-white font-bold text-xl shadow-sm flex-shrink-0">
                {{ firstLetter }}
              </div>
              <div>
                <div class="flex items-center gap-2 mb-1">
                  <h1 class="text-2xl font-bold text-text-primary">{{ authService.user()?.displayName }}</h1>
                  <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-700">{{ translationService.translate('admin.users.role.agent') }}</span>
                </div>
                <p class="text-text-secondary">{{ translationService.translate('nav.section.agent') }} — {{ translationService.translate('landing.hero.subtitle') }}</p>
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-10">
              <a routerLink="/agent/tickets" class="relative overflow-hidden bg-gradient-to-br from-amber-500 to-orange-600 rounded-2xl p-6 text-white shadow-md hover:shadow-lg transition-shadow group">
                <div class="relative z-10">
                  <lucide-inbox class="w-8 h-8 mb-3 opacity-80"></lucide-inbox>
                  <h2 class="text-xl font-bold mb-1">{{ translationService.translate('nav.ticket-queue') }}</h2>
                   <p class="text-amber-100 text-sm mb-4">{{ translationService.translate('landing.dashboard.agent.ticketQueueDesc') }}</p>
                  <span class="inline-flex items-center gap-1 text-sm font-medium text-white group-hover:gap-2 transition-all">
                     {{ translationService.translate('landing.dashboard.agent.viewQueue') }} <lucide-arrow-right class="w-4 h-4"></lucide-arrow-right>
                  </span>
                </div>
                <div class="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-full -translate-y-8 translate-x-8"></div>
              </a>

              <div class="grid grid-cols-1 gap-4">
                <a routerLink="/chat" class="flex items-center gap-4 bg-surface-primary rounded-xl border border-border-default shadow-sm p-4 hover:shadow-md hover:border-primary-200 transition-all group">
                  <div class="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center flex-shrink-0">
                    <lucide-message-square class="w-5 h-5 text-blue-600"></lucide-message-square>
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="font-semibold text-text-primary text-sm">{{ translationService.translate('nav.ai-assistant') }}</h3>
                     <p class="text-xs text-text-secondary mt-0.5">{{ translationService.translate('landing.dashboard.agent.testAi') }}</p>
                  </div>
                  <lucide-arrow-right class="w-4 h-4 text-text-tertiary flex-shrink-0"></lucide-arrow-right>
                </a>
                <a routerLink="/articles" class="flex items-center gap-4 bg-surface-primary rounded-xl border border-border-default shadow-sm p-4 hover:shadow-md hover:border-emerald-200 transition-all group">
                  <div class="w-10 h-10 rounded-xl bg-emerald-100 flex items-center justify-center flex-shrink-0">
                    <lucide-book-open class="w-5 h-5 text-emerald-600"></lucide-book-open>
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="font-semibold text-text-primary text-sm">{{ translationService.translate('nav.knowledge-base') }}</h3>
                     <p class="text-xs text-text-secondary mt-0.5">{{ translationService.translate('landing.dashboard.agent.browseKb') }}</p>
                  </div>
                  <lucide-arrow-right class="w-4 h-4 text-text-tertiary flex-shrink-0"></lucide-arrow-right>
                </a>
              </div>
            </div>

             <h2 class="text-lg font-semibold text-text-primary mb-4">{{ translationService.translate('landing.dashboard.agent.workflow') }}</h2>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-5">
                <div class="flex items-center gap-3 mb-3">
                  <div class="w-8 h-8 rounded-lg bg-amber-100 flex items-center justify-center flex-shrink-0">
                    <lucide-inbox class="w-4 h-4 text-amber-600"></lucide-inbox>
                  </div>
                   <span class="text-xs font-bold text-text-tertiary">{{ translationService.translate('landing.dashboard.agent.step') }} 1</span>
                 </div>
                 <h3 class="font-semibold text-text-primary text-sm mb-1">{{ translationService.translate('landing.dashboard.agent.step1.title') }}</h3>
                 <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.dashboard.agent.step1.desc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-5">
                <div class="flex items-center gap-3 mb-3">
                  <div class="w-8 h-8 rounded-lg bg-primary-100 flex items-center justify-center flex-shrink-0">
                    <lucide-check-circle class="w-4 h-4 text-primary-600"></lucide-check-circle>
                  </div>
                   <span class="text-xs font-bold text-text-tertiary">{{ translationService.translate('landing.dashboard.agent.step') }} 2</span>
                 </div>
                 <h3 class="font-semibold text-text-primary text-sm mb-1">{{ translationService.translate('landing.dashboard.agent.step2.title') }}</h3>
                 <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.dashboard.agent.step2.desc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-5">
                <div class="flex items-center gap-3 mb-3">
                  <div class="w-8 h-8 rounded-lg bg-green-100 flex items-center justify-center flex-shrink-0">
                    <lucide-sparkles class="w-4 h-4 text-green-600"></lucide-sparkles>
                  </div>
                   <span class="text-xs font-bold text-text-tertiary">{{ translationService.translate('landing.dashboard.agent.step') }} 3</span>
                 </div>
                 <h3 class="font-semibold text-text-primary text-sm mb-1">{{ translationService.translate('landing.dashboard.agent.step3.title') }}</h3>
                 <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.dashboard.agent.step3.desc') }}</p>
              </div>
            </div>
          </div>
        </div>

      } @else {
        <!-- ========== USER DASHBOARD ========== -->
        <div class="min-h-[calc(100vh-3.5rem)] bg-surface-secondary">
          <div class="max-w-5xl mx-auto px-4 py-10 sm:py-14">

            <div class="flex flex-col sm:flex-row items-start sm:items-center gap-4 sm:gap-5 mb-10">
              <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary-500 to-indigo-600 flex items-center justify-center text-white font-bold text-xl shadow-sm flex-shrink-0">
                {{ firstLetter }}
              </div>
              <div>
                <h1 class="text-2xl font-bold text-text-primary mb-1">
                  {{ translationService.translate('landing.signedInAs') }}, {{ authService.user()?.displayName }}
                </h1>
                <p class="text-text-secondary">{{ translationService.translate('landing.hero.description') }}</p>
              </div>
            </div>

            <div class="bg-gradient-to-br from-primary-500 to-indigo-600 rounded-2xl p-6 sm:p-8 text-white shadow-md mb-10">
              <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div>
                   <h2 class="text-xl sm:text-2xl font-bold mb-2">{{ translationService.translate('landing.dashboard.user.heroTitle') }}</h2>
                   <p class="text-primary-100 text-sm max-w-md">{{ translationService.translate('landing.dashboard.user.heroDesc') }}</p>
                </div>
                <a routerLink="/chat" class="inline-flex items-center gap-2 rounded-xl bg-white px-6 py-2.5 text-sm font-semibold text-primary-700 hover:bg-primary-50 transition-colors shadow-sm whitespace-nowrap flex-shrink-0">
                  {{ translationService.translate('nav.ai-assistant') }}
                  <lucide-arrow-right class="w-4 h-4"></lucide-arrow-right>
                </a>
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-10">
              <a routerLink="/chat" class="group bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-primary-200 transition-all">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-blue-700 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                  <lucide-message-square class="w-5 h-5 text-white"></lucide-message-square>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('nav.ai-assistant') }}</h3>
                <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.feature.aiDesc') }}</p>
              </a>
              <a routerLink="/articles" class="group bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-emerald-200 transition-all">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-700 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                  <lucide-book-open class="w-5 h-5 text-white"></lucide-book-open>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('nav.knowledge-base') }}</h3>
                <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.feature.searchDesc') }}</p>
              </a>
              <a routerLink="/tickets" class="group bg-surface-primary rounded-xl border border-border-default shadow-sm p-5 hover:shadow-md hover:border-amber-200 transition-all">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500 to-amber-700 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                  <lucide-ticket class="w-5 h-5 text-white"></lucide-ticket>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('nav.my-tickets') }}</h3>
                <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.feature.docsDesc') }}</p>
              </a>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <h2 class="text-lg font-semibold text-text-primary mb-4">{{ translationService.translate('landing.how.title') }}</h2>
                <div class="space-y-4">
                  <div class="flex gap-3">
                    <div class="w-8 h-8 rounded-lg bg-surface-tertiary flex items-center justify-center flex-shrink-0">
                      <span class="text-sm font-bold text-primary-600">1</span>
                    </div>
                    <div>
                      <h3 class="font-medium text-sm text-text-primary mb-0.5">{{ translationService.translate('landing.how.step1.title') }}</h3>
                      <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.how.step1.desc') }}</p>
                    </div>
                  </div>
                  <div class="flex gap-3">
                    <div class="w-8 h-8 rounded-lg bg-surface-tertiary flex items-center justify-center flex-shrink-0">
                      <span class="text-sm font-bold text-primary-600">2</span>
                    </div>
                    <div>
                      <h3 class="font-medium text-sm text-text-primary mb-0.5">{{ translationService.translate('landing.how.step2.title') }}</h3>
                      <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.how.step2.desc') }}</p>
                    </div>
                  </div>
                  <div class="flex gap-3">
                    <div class="w-8 h-8 rounded-lg bg-surface-tertiary flex items-center justify-center flex-shrink-0">
                      <span class="text-sm font-bold text-primary-600">3</span>
                    </div>
                    <div>
                      <h3 class="font-medium text-sm text-text-primary mb-0.5">{{ translationService.translate('landing.how.step3.title') }}</h3>
                      <p class="text-xs text-text-secondary leading-relaxed">{{ translationService.translate('landing.how.step3.desc') }}</p>
                    </div>
                  </div>
                </div>
              </div>

              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-5">
                <div class="flex items-center gap-2 mb-3">
                  <lucide-help-circle class="w-5 h-5 text-primary-600"></lucide-help-circle>
                   <h3 class="font-semibold text-text-primary text-sm">{{ translationService.translate('landing.dashboard.user.tipsTitle') }}</h3>
                 </div>
                 <ul class="space-y-2 text-xs text-text-secondary">
                   <li class="flex items-start gap-2">
                     <lucide-check-circle class="w-3.5 h-3.5 text-emerald-500 flex-shrink-0 mt-0.5"></lucide-check-circle>
                     {{ translationService.translate('landing.dashboard.user.tip1') }}
                   </li>
                   <li class="flex items-start gap-2">
                     <lucide-check-circle class="w-3.5 h-3.5 text-emerald-500 flex-shrink-0 mt-0.5"></lucide-check-circle>
                     {{ translationService.translate('landing.dashboard.user.tip2') }}
                   </li>
                   <li class="flex items-start gap-2">
                     <lucide-check-circle class="w-3.5 h-3.5 text-emerald-500 flex-shrink-0 mt-0.5"></lucide-check-circle>
                     {{ translationService.translate('landing.dashboard.user.tip3') }}
                   </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      }

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
                {{ translationService.translate('landing.cta.register') }}
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
              {{ translationService.translate('landing.how.title') }}
            </h2>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
              <div class="text-center">
                <div class="w-14 h-14 mx-auto mb-4 rounded-xl bg-surface-secondary border border-border-default flex items-center justify-center">
                  <lucide-message-square class="w-6 h-6 text-primary-600"></lucide-message-square>
                </div>
                <h3 class="font-semibold text-text-primary mb-2">{{ translationService.translate('landing.how.step1.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.how.step1.desc') }}</p>
              </div>
              <div class="text-center">
                <div class="w-14 h-14 mx-auto mb-4 rounded-xl bg-surface-secondary border border-border-default flex items-center justify-center">
                  <lucide-search class="w-6 h-6 text-primary-600"></lucide-search>
                </div>
                <h3 class="font-semibold text-text-primary mb-2">{{ translationService.translate('landing.how.step2.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.how.step2.desc') }}</p>
              </div>
              <div class="text-center">
                <div class="w-14 h-14 mx-auto mb-4 rounded-xl bg-surface-secondary border border-border-default flex items-center justify-center">
                  <lucide-sparkles class="w-6 h-6 text-primary-600"></lucide-sparkles>
                </div>
                <h3 class="font-semibold text-text-primary mb-2">{{ translationService.translate('landing.how.step3.title') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.how.step3.desc') }}</p>
              </div>
            </div>
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
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.feature.searchTitle') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.feature.searchDesc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-purple-100 flex items-center justify-center mb-4">
                  <lucide-sparkles class="w-5 h-5 text-purple-700"></lucide-sparkles>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.feature.aiTitle') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.feature.aiDesc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-green-100 flex items-center justify-center mb-4">
                  <lucide-file-text class="w-5 h-5 text-green-600"></lucide-file-text>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.feature.docsTitle') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.feature.docsDesc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center mb-4">
                  <lucide-ticket class="w-5 h-5 text-amber-600"></lucide-ticket>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.feature.escalateTitle') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.feature.escalateDesc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-cyan-100 flex items-center justify-center mb-4">
                  <lucide-shield class="w-5 h-5 text-cyan-700"></lucide-shield>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.feature.rolesTitle') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.feature.rolesDesc') }}</p>
              </div>
              <div class="bg-surface-primary rounded-xl border border-border-default shadow-sm p-6 hover:shadow-md transition-shadow">
                <div class="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center mb-4">
                  <lucide-message-square class="w-5 h-5 text-indigo-700"></lucide-message-square>
                </div>
                <h3 class="font-semibold text-text-primary mb-1">{{ translationService.translate('landing.feature.bilingualTitle') }}</h3>
                <p class="text-sm text-text-secondary leading-relaxed">{{ translationService.translate('landing.feature.bilingualDesc') }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- CTA -->
        <section class="bg-gradient-to-r from-primary-600 to-indigo-700 px-4 py-16 sm:py-20">
          <div class="max-w-2xl mx-auto text-center">
            <h2 class="text-2xl sm:text-3xl font-bold text-white mb-4">
              {{ translationService.translate('landing.cta.title') }}
            </h2>
            <p class="text-blue-100 mb-8 leading-relaxed">
              {{ translationService.translate('landing.cta.desc') }}
            </p>
            <a routerLink="/register" class="inline-flex items-center gap-2 rounded-xl bg-white px-6 py-2.5 text-sm font-medium text-primary-600 hover:bg-blue-50 transition-colors shadow-sm">
              {{ translationService.translate('landing.cta.button') }}
              <lucide-arrow-right class="w-4 h-4"></lucide-arrow-right>
            </a>
          </div>
        </section>

        <!-- Footer -->
        <footer class="bg-slate-900 px-4 py-8 text-center text-sm text-slate-500">
          <p>&copy; {{ currentYear }} {{ translationService.translate('app.footer') }}</p>
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
