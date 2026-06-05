import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

/** Application route configuration with lazy-loaded feature modules. */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
  },
  {
    path: 'admin/users',
    loadComponent: () => import('./features/admin/user-list/user-list.component').then(m => m.UserListComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin/articles',
    loadComponent: () => import('./features/kb/admin/article-list/article-list.component').then(m => m.ArticleListComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin/articles/new',
    loadComponent: () => import('./features/kb/admin/article-editor/article-editor.component').then(m => m.ArticleEditorComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin/articles/:id/edit',
    loadComponent: () => import('./features/kb/admin/article-editor/article-editor.component').then(m => m.ArticleEditorComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin/kcs-drafts',
    loadComponent: () => import('./features/admin/kcs-draft-list/kcs-draft-list.component').then(m => m.KcsDraftListComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin/kcs-drafts/:id/edit',
    loadComponent: () => import('./features/kb/admin/article-editor/article-editor.component').then(m => m.ArticleEditorComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin/tags',
    loadComponent: () => import('./features/kb/admin/tag-manager/tag-manager.component').then(m => m.TagManagerComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin/settings/llm',
    loadComponent: () => import('./features/admin/llm-settings/llm-settings.component').then(m => m.LlmSettingsComponent),
    canActivate: [authGuard],
  },
  {
    path: 'agent/tickets',
    loadComponent: () => import('./features/agent/agent-ticket-list/agent-ticket-list.component').then(m => m.AgentTicketListComponent),
    canActivate: [authGuard],
  },
  {
    path: 'agent/tickets/:id',
    loadComponent: () => import('./features/agent/agent-ticket-detail/agent-ticket-detail.component').then(m => m.AgentTicketDetailComponent),
    canActivate: [authGuard],
  },
  {
    path: 'tickets',
    loadComponent: () => import('./features/tickets/ticket-list/ticket-list.component').then(m => m.TicketListComponent),
    canActivate: [authGuard],
  },
  {
    path: 'tickets/:id',
    loadComponent: () => import('./features/tickets/ticket-detail/ticket-detail.component').then(m => m.TicketDetailComponent),
    canActivate: [authGuard],
  },
  {
    path: 'chat',
    loadComponent: () => import('./features/chat/chat.component').then(m => m.ChatComponent),
    canActivate: [authGuard],
  },
  {
    path: 'articles',
    loadComponent: () => import('./features/kb/public/article-list/article-list.component').then(m => m.ArticleListComponent),
  },
  {
    path: 'articles/search',
    loadComponent: () => import('./features/kb/public/article-search/article-search.component').then(m => m.ArticleSearchComponent),
  },
  {
    path: 'articles/:id',
    loadComponent: () => import('./features/kb/public/article-viewer/article-viewer.component').then(m => m.ArticleViewerComponent),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
