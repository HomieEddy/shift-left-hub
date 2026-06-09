import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { TranslationService } from '../../core/i18n/translation.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (authService.isAuthenticated()) {
      <div class="min-h-[calc(100vh-4rem)] flex flex-col items-center justify-center">
        <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-2xl font-bold shadow-lg mb-6">SL</div>
        <p class="text-lg text-slate-600 mb-6">{{ translationService.translate('landing.signedInAs') }} <strong class="text-slate-800">{{ authService.user()?.displayName }}</strong></p>
        <a routerLink="/chat" class="rounded-xl bg-blue-600 px-8 py-3 text-sm font-semibold text-white hover:bg-blue-700 transition-all shadow-md hover:shadow-lg">
          {{ translationService.translate('landing.goToAiAssistant') }}
        </a>
      </div>
    } @else {
      <div class="-mx-4 sm:-mx-6 lg:-mx-8">
        <!-- Hero -->
        <section class="relative overflow-hidden bg-gradient-to-b from-slate-900 via-slate-800 to-slate-900 px-4 sm:px-6 lg:px-8 py-24 sm:py-32">
          <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-blue-500/10 via-transparent to-transparent"></div>
          <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_bottom_left,_var(--tw-gradient-stops))] from-indigo-500/10 via-transparent to-transparent"></div>
          <div class="relative max-w-4xl mx-auto text-center">
            <div class="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/10 border border-white/20 text-white/80 text-xs font-medium mb-8">
              <span class="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse"></span>
              {{ translationService.translate('landing.hero.badge') }}
            </div>
            <h1 class="text-4xl sm:text-5xl lg:text-6xl font-bold text-white mb-6 tracking-tight leading-tight">
              {{ translationService.translate('landing.hero.title') }}
            </h1>
            <p class="text-lg sm:text-xl text-slate-300 mb-4 max-w-2xl mx-auto leading-relaxed">
              {{ translationService.translate('landing.hero.subtitle') }}
            </p>
            <p class="text-sm sm:text-base text-slate-400 mb-10 max-w-xl mx-auto leading-relaxed">
              {{ translationService.translate('landing.hero.description') }}
            </p>
            <div class="flex items-center justify-center gap-4">
              <a routerLink="/register" class="rounded-xl bg-blue-600 px-8 py-3.5 text-sm font-semibold text-white hover:bg-blue-700 transition-all shadow-lg shadow-blue-600/25 hover:shadow-blue-600/40">
                {{ translationService.translate('landing.cta.register') }}
              </a>
              <a routerLink="/login" class="rounded-xl border border-slate-600 bg-white/5 px-8 py-3.5 text-sm font-semibold text-slate-200 hover:bg-white/10 hover:text-white transition-all">
                {{ translationService.translate('landing.cta.signin') }}
              </a>
            </div>
          </div>
          <div class="absolute -bottom-12 left-1/2 -translate-x-1/2 w-[800px] h-[200px] bg-gradient-to-t from-blue-500/20 to-transparent blur-3xl rounded-full"></div>
        </section>

        <!-- Stats -->
        <section class="bg-white border-y border-slate-200">
          <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-8 text-center">
              <div>
                <div class="text-3xl font-bold text-blue-600 mb-1">99.9%</div>
                <div class="text-sm text-slate-500 font-medium">{{ translationService.translate('landing.stats.accuracy') }}</div>
              </div>
              <div>
                <div class="text-3xl font-bold text-blue-600 mb-1">90%</div>
                <div class="text-sm text-slate-500 font-medium">{{ translationService.translate('landing.stats.intercepted') }}</div>
              </div>
              <div>
                <div class="text-3xl font-bold text-blue-600 mb-1">&lt;15s</div>
                <div class="text-sm text-slate-500 font-medium">{{ translationService.translate('landing.stats.response') }}</div>
              </div>
            </div>
          </div>
        </section>

        <!-- How It Works -->
        <section class="bg-slate-50 px-4 sm:px-6 lg:px-8 py-16 sm:py-20">
          <div class="max-w-5xl mx-auto">
            <h2 class="text-2xl sm:text-3xl font-bold text-slate-800 text-center mb-3">{{ translationService.translate('landing.how.title') }}</h2>
            <p class="text-slate-500 text-center mb-12 max-w-lg mx-auto">{{ translationService.translate('landing.how.subtitle') }}</p>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
              <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm">
                <div class="w-10 h-10 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm mb-4">1</div>
                <h3 class="font-semibold text-slate-800 mb-2">{{ translationService.translate('landing.how.step1.title') }}</h3>
                <p class="text-sm text-slate-500 leading-relaxed">{{ translationService.translate('landing.how.step1.desc') }}</p>
              </div>
              <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm">
                <div class="w-10 h-10 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm mb-4">2</div>
                <h3 class="font-semibold text-slate-800 mb-2">{{ translationService.translate('landing.how.step2.title') }}</h3>
                <p class="text-sm text-slate-500 leading-relaxed">{{ translationService.translate('landing.how.step2.desc') }}</p>
              </div>
              <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm">
                <div class="w-10 h-10 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm mb-4">3</div>
                <h3 class="font-semibold text-slate-800 mb-2">{{ translationService.translate('landing.how.step3.title') }}</h3>
                <p class="text-sm text-slate-500 leading-relaxed">{{ translationService.translate('landing.how.step3.desc') }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- Features -->
        <section class="bg-white px-4 sm:px-6 lg:px-8 py-16 sm:py-20">
          <div class="max-w-5xl mx-auto">
            <h2 class="text-2xl sm:text-3xl font-bold text-slate-800 text-center mb-3">{{ translationService.translate('landing.features.title') }}</h2>
            <p class="text-slate-500 text-center mb-12 max-w-lg mx-auto">{{ translationService.translate('landing.features.subtitle') }}</p>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div class="p-6 rounded-2xl border border-slate-200 hover:border-blue-200 hover:shadow-lg transition-all group">
                <div class="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center mb-4 group-hover:bg-blue-100 transition-colors">
                  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
                </div>
                <h3 class="font-semibold text-slate-800 mb-2">{{ translationService.translate('landing.feature.searchTitle') }}</h3>
                <p class="text-sm text-slate-500 leading-relaxed">{{ translationService.translate('landing.feature.searchDesc') }}</p>
              </div>
              <div class="p-6 rounded-2xl border border-slate-200 hover:border-blue-200 hover:shadow-lg transition-all group">
                <div class="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center mb-4 group-hover:bg-blue-100 transition-colors">
                  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456zM16.894 20.567L16.5 21.75l-.394-1.183a2.25 2.25 0 00-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 001.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 001.423 1.423l1.183.394-1.183.394a2.25 2.25 0 00-1.423 1.423z"/></svg>
                </div>
                <h3 class="font-semibold text-slate-800 mb-2">{{ translationService.translate('landing.feature.aiTitle') }}</h3>
                <p class="text-sm text-slate-500 leading-relaxed">{{ translationService.translate('landing.feature.aiDesc') }}</p>
              </div>
              <div class="p-6 rounded-2xl border border-slate-200 hover:border-blue-200 hover:shadow-lg transition-all group">
                <div class="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center mb-4 group-hover:bg-blue-100 transition-colors">
                  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"/></svg>
                </div>
                <h3 class="font-semibold text-slate-800 mb-2">{{ translationService.translate('landing.feature.docsTitle') }}</h3>
                <p class="text-sm text-slate-500 leading-relaxed">{{ translationService.translate('landing.feature.docsDesc') }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- CTA -->
        <section class="bg-gradient-to-r from-blue-600 to-indigo-700 px-4 sm:px-6 lg:px-8 py-16 sm:py-20">
          <div class="max-w-3xl mx-auto text-center">
            <h2 class="text-2xl sm:text-3xl font-bold text-white mb-4">{{ translationService.translate('landing.cta.title') }}</h2>
            <p class="text-blue-100 mb-8 max-w-md mx-auto leading-relaxed">{{ translationService.translate('landing.cta.desc') }}</p>
            <div class="flex items-center justify-center gap-4">
              <a routerLink="/register" class="rounded-xl bg-white px-8 py-3.5 text-sm font-semibold text-blue-700 hover:bg-blue-50 transition-all shadow-lg">
                {{ translationService.translate('landing.cta.button') }}
              </a>
              <a routerLink="/login" class="rounded-xl border border-white/30 px-8 py-3.5 text-sm font-semibold text-white hover:bg-white/10 transition-all">
                {{ translationService.translate('landing.cta.signin') }}
              </a>
            </div>
          </div>
        </section>

        <!-- Footer simple -->
        <footer class="bg-slate-900 px-4 sm:px-6 lg:px-8 py-8">
          <div class="max-w-5xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="flex items-center gap-2">
              <div class="w-6 h-6 rounded-md bg-blue-600 flex items-center justify-center text-white font-bold text-[10px]">SL</div>
              <span class="text-sm text-slate-400 font-medium">{{ translationService.translate('app.title') }}</span>
            </div>
            <p class="text-xs text-slate-500">{{ translationService.translate('app.footer') }}</p>
          </div>
        </footer>
      </div>
    }
  `,
})
export class LandingComponent {
  protected authService = inject(AuthService);
  protected translationService = inject(TranslationService);
}
