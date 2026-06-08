import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { $localize } from '@angular/localize/init';
import { ArticleService } from '../../services/article.service';
import { ArticleDto, ArticleStatus } from '../../models/article.models';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { ConfirmationDialogService } from '../../../../shared/ui/confirmation-dialog/confirmation-dialog.service';

@Component({
  selector: 'app-article-list',
  standalone: true,
  imports: [NgClass, DatePipe, RouterLink],
  templateUrl: './article-list.component.html',
})
export class ArticleListComponent implements OnInit {
  private articleService = inject(ArticleService);
  private destroyRef = inject(DestroyRef);
  private confirmationDialog = inject(ConfirmationDialogService);
  protected translationService = inject(TranslationService);

  articles = signal<ArticleDto[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  currentPage = signal(0);
  totalPages = signal(0);
  statusFilter = signal<ArticleStatus | ''>('');
  searchQuery = signal('');

  ngOnInit(): void {
    this.loadArticles();
  }

  loadArticles(): void {
    this.isLoading.set(true);
    this.articleService.getArticles(this.currentPage()).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (page) => {
        this.articles.set(page.content);
        this.totalPages.set(page.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set($localize`:@@kb.articles.error.load:Failed to load articles.`);
        this.isLoading.set(false);
      },
    });
  }

  publish(id: string): void {
    this.articleService.publishArticle(id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => this.loadArticles(),
      error: () => this.errorMessage.set($localize`:@@kb.articles.error.publish:Failed to publish article.`),
    });
  }

  archive(id: string): void {
    this.articleService.archiveArticle(id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => this.loadArticles(),
      error: () => this.errorMessage.set($localize`:@@kb.articles.error.archive:Failed to archive article.`),
    });
  }

  deleteArticle(id: string): void {
    this.confirmationDialog.confirm({
      title: $localize`:@@confirm.title.delete:Delete Confirmation`,
      message: $localize`:@@confirm.message.delete-article:Delete this article? This action cannot be undone.`,
      confirmLabel: $localize`:@@confirm.label.delete:Delete`,
    }).subscribe((confirmed) => {
      if (confirmed) {
        this.articleService.deleteArticle(id).pipe(
          takeUntilDestroyed(this.destroyRef)
        ).subscribe({
          next: () => this.loadArticles(),
          error: () => this.errorMessage.set($localize`:@@kb.articles.error.delete:Failed to delete article.`),
        });
      }
    });
  }

  statusBadgeVariant(status: ArticleStatus): string {
    switch (status) {
      case 'PUBLISHED': return 'success';
      case 'DRAFT': return 'warning';
      case 'ARCHIVED': return 'danger';
      default: return 'default';
    }
  }

  changePage(page: number): void {
    this.currentPage.set(page);
    this.loadArticles();
  }
}
