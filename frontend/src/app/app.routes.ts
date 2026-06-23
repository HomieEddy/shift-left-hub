import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './core/auth/admin.guard';
import { agentGuard } from './core/auth/agent.guard';
import { redirectIfAuthenticatedGuard } from './core/auth/redirect-if-authenticated.guard';

/** Application route configuration with lazy-loaded feature modules. */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/landing/landing.component').then((m) => m.LandingComponent),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
    canMatch: [redirectIfAuthenticatedGuard],
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
    canMatch: [redirectIfAuthenticatedGuard],
  },
  {
    path: 'admin/users',
    loadComponent: () =>
      import('./features/admin/user-list/user-list.component').then((m) => m.UserListComponent),
    canMatch: [adminGuard],
  },
  {
    path: 'admin/articles',
    loadComponent: () =>
      import('./features/kb/admin/article-list/article-list.component').then(
        (m) => m.ArticleListComponent,
      ),
    canMatch: [adminGuard],
  },
  {
    path: 'admin/articles/new',
    loadComponent: () =>
      import('./features/kb/admin/article-editor/article-editor.component').then(
        (m) => m.ArticleEditorComponent,
      ),
    canMatch: [adminGuard],
  },
  {
    path: 'admin/articles/:id/edit',
    loadComponent: () =>
      import('./features/kb/admin/article-editor/article-editor.component').then(
        (m) => m.ArticleEditorComponent,
      ),
    canMatch: [adminGuard],
  },
  {
    path: 'admin/kcs-drafts',
    loadComponent: () =>
      import('./features/admin/kcs-draft-list/kcs-draft-list.component').then(
        (m) => m.KcsDraftListComponent,
      ),
    canMatch: [adminGuard],
  },
  {
    path: 'admin/kcs-drafts/:id/edit',
    loadComponent: () =>
      import('./features/kb/admin/article-editor/article-editor.component').then(
        (m) => m.ArticleEditorComponent,
      ),
    canMatch: [adminGuard],
  },
  {
    path: 'admin/tags',
    loadComponent: () =>
      import('./features/kb/admin/tag-manager/tag-manager.component').then(
        (m) => m.TagManagerComponent,
      ),
    canMatch: [adminGuard],
  },
  {
    path: 'admin/workspaces',
    canMatch: [adminGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/admin/workspaces/workspace-list.component').then(
            (m) => m.WorkspaceListComponent,
          ),
      },
      {
        path: ':id',
        loadComponent: () =>
          import('./features/admin/workspaces/workspace-detail.component').then(
            (m) => m.WorkspaceDetailComponent,
          ),
      },
    ],
  },
  {
    path: 'admin/documents',
    loadComponent: () =>
      import('./features/admin/documents/document-list.component').then(
        (m) => m.DocumentListComponent,
      ),
    canMatch: [adminGuard],
  },
  {
    path: 'admin/taxonomy',
    loadComponent: () =>
      import('./features/admin/taxonomy/taxonomy-tree.component').then(
        (m) => m.TaxonomyTreeComponent,
      ),
    canMatch: [adminGuard],
  },
  {
    path: 'admin/settings/llm',
    loadComponent: () =>
      import('./features/admin/llm-settings/llm-settings.component').then(
        (m) => m.LlmSettingsComponent,
      ),
    canMatch: [adminGuard],
  },
  {
    path: 'agent/tickets',
    loadComponent: () =>
      import('./features/agent/agent-ticket-list/agent-ticket-list.component').then(
        (m) => m.AgentTicketListComponent,
      ),
    canMatch: [agentGuard],
  },
  {
    path: 'agent/tickets/:id',
    loadComponent: () =>
      import('./features/agent/agent-ticket-detail/agent-ticket-detail.component').then(
        (m) => m.AgentTicketDetailComponent,
      ),
    canMatch: [agentGuard],
  },
  {
    path: 'tickets',
    loadComponent: () =>
      import('./features/tickets/ticket-list/ticket-list.component').then(
        (m) => m.TicketListComponent,
      ),
    canMatch: [authGuard],
  },
  {
    path: 'tickets/:id',
    loadComponent: () =>
      import('./features/tickets/ticket-detail/ticket-detail.component').then(
        (m) => m.TicketDetailComponent,
      ),
    canMatch: [authGuard],
  },
  {
    path: 'chat',
    loadComponent: () => import('./features/chat/chat.component').then((m) => m.ChatComponent),
    canMatch: [authGuard],
  },
  {
    path: 'articles',
    loadComponent: () =>
      import('./features/kb/public/article-list/article-list.component').then(
        (m) => m.ArticleListComponent,
      ),
  },
  {
    path: 'articles/search',
    loadComponent: () =>
      import('./features/kb/public/article-search/article-search.component').then(
        (m) => m.ArticleSearchComponent,
      ),
  },
  {
    path: 'articles/:id',
    loadComponent: () =>
      import('./features/kb/public/article-viewer/article-viewer.component').then(
        (m) => m.ArticleViewerComponent,
      ),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
