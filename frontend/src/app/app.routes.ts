import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './core/auth/admin.guard';
import { agentGuard } from './core/auth/agent.guard';
import { redirectIfAuthenticatedGuard } from './core/auth/redirect-if-authenticated.guard';

/** Application route configuration with lazy-loaded feature modules. */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    canActivate: [redirectIfAuthenticatedGuard],
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    canActivate: [redirectIfAuthenticatedGuard],
  },
  {
    path: 'admin/users',
    loadComponent: () => import('./features/admin/user-list/user-list.component').then(m => m.UserListComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/articles',
    loadComponent: () => import('./features/kb/admin/article-list/article-list.component').then(m => m.ArticleListComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/articles/new',
    loadComponent: () => import('./features/kb/admin/article-editor/article-editor.component').then(m => m.ArticleEditorComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/articles/:id/edit',
    loadComponent: () => import('./features/kb/admin/article-editor/article-editor.component').then(m => m.ArticleEditorComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/kcs-drafts',
    loadComponent: () => import('./features/admin/kcs-draft-list/kcs-draft-list.component').then(m => m.KcsDraftListComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/kcs-drafts/:id/edit',
    loadComponent: () => import('./features/kb/admin/article-editor/article-editor.component').then(m => m.ArticleEditorComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/tags',
    loadComponent: () => import('./features/kb/admin/tag-manager/tag-manager.component').then(m => m.TagManagerComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/workspaces',
    loadComponent: () => import('./features/admin/workspaces/workspace-list.component').then(m => m.WorkspaceListComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/documents',
    loadComponent: () => import('./features/admin/documents/document-list.component').then(m => m.DocumentListComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/settings/llm',
    loadComponent: () => import('./features/admin/llm-settings/llm-settings.component').then(m => m.LlmSettingsComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'agent/tickets',
    loadComponent: () => import('./features/agent/agent-ticket-list/agent-ticket-list.component').then(m => m.AgentTicketListComponent),
    canActivate: [agentGuard],
  },
  {
    path: 'agent/tickets/:id',
    loadComponent: () => import('./features/agent/agent-ticket-detail/agent-ticket-detail.component').then(m => m.AgentTicketDetailComponent),
    canActivate: [agentGuard],
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
