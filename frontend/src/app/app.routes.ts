import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

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
    path: 'admin/tags',
    loadComponent: () => import('./features/kb/admin/tag-manager/tag-manager.component').then(m => m.TagManagerComponent),
    canActivate: [authGuard],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
