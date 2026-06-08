import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (authService.isAuthenticated()) {
      <div class="min-h-[calc(100vh-3.5rem)] flex flex-col items-center justify-center">
        <p class="text-lg text-slate-600 mb-4" i18n="@@landing.signedInAs">You're signed in as <strong>{{ authService.user()?.displayName }}</strong></p>
        <a routerLink="/chat" class="rounded-lg bg-blue-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-blue-700 transition-colors" i18n="@@landing.goToAiAssistant">
          Go to AI Assistant
        </a>
      </div>
    } @else {
      <div class="min-h-[calc(100vh-3.5rem)] flex flex-col">
        <!-- Hero -->
        <section class="flex-1 flex items-center justify-center px-4 py-16">
          <div class="max-w-2xl text-center">
            <h1 class="text-4xl sm:text-5xl font-bold text-slate-800 mb-4 tracking-tight" i18n="@@landing.hero.title">
              Shift-Left Knowledge Hub
            </h1>
            <p class="text-lg text-slate-500 mb-3 max-w-xl mx-auto" i18n="@@landing.hero.subtitle">
              Resolve issues before they reach the queue.
            </p>
            <p class="text-slate-400 mb-10 max-w-md mx-auto leading-relaxed" i18n="@@landing.hero.description">
              AI-powered knowledge base that intercepts Level 0/1 tickets and
              eliminates the documentation burden on IT agents.
            </p>
            <div class="flex items-center justify-center gap-4">
              <a routerLink="/login" class="rounded-lg bg-blue-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-blue-700 transition-colors" i18n="@@landing.cta.signin">
                Sign in
              </a>
              <a routerLink="/register" class="rounded-lg border border-slate-300 px-6 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors" i18n="@@landing.cta.register">
                Create account
              </a>
            </div>
          </div>
        </section>

        <!-- Features bar -->
        <section class="border-t border-slate-200 bg-slate-50">
          <div class="max-w-4xl mx-auto px-4 py-10">
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-8 text-center text-sm">
              <div>
                <div class="text-blue-600 font-semibold text-base mb-1" i18n="@@landing.feature.searchTitle">Smart Search</div>
                <p class="text-slate-500" i18n="@@landing.feature.searchDesc">Hybrid FTS + vector search finds answers instantly.</p>
              </div>
              <div>
                <div class="text-blue-600 font-semibold text-base mb-1" i18n="@@landing.feature.aiTitle">AI Assistant</div>
                <p class="text-slate-500" i18n="@@landing.feature.aiDesc">Natural language queries with context-aware replies.</p>
              </div>
              <div>
                <div class="text-blue-600 font-semibold text-base mb-1" i18n="@@landing.feature.docsTitle">Auto-Docs</div>
                <p class="text-slate-500" i18n="@@landing.feature.docsDesc">Every resolution becomes a knowledge article.</p>
              </div>
            </div>
          </div>
        </section>
      </div>
    }
  `,
})
export class LandingComponent {
  protected authService = inject(AuthService);
}
