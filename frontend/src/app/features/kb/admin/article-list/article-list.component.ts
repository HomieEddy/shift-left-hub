import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ArticleService } from '../../services/article.service';
import { ArticleDto, ArticleStatus } from '../../models/article.models';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { ConfirmationDialogService } from '../../../../shared/ui/confirmation-dialog/confirmation-dialog.service';

@Component({
  selector: 'app-article-list',
  standalone: true,
  imports: [NgClass, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
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
    this.articleService
      .getArticles(this.currentPage())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.articles.set(page.content);
          this.totalPages.set(page.totalPages);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set(this.translationService.translate('kb.articles.error.load'));
          this.isLoading.set(false);
        },
      });
  }

  publish(id: string): void {
    this.confirmationDialog
      .confirm({
        title: this.translationService.translate('confirm.title.publish'),
        message: this.translationService.translate('confirm.message.publish-article'),
        confirmLabel: this.translationService.translate('confirm.label.publish'),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.articleService
          .publishArticle(id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => this.loadArticles(),
            error: () =>
              this.errorMessage.set(this.translationService.translate('kb.articles.error.publish')),
          });
      });
  }

  archive(id: string): void {
    this.confirmationDialog
      .confirm({
        title: this.translationService.translate('confirm.title.archive'),
        message: this.translationService.translate('confirm.message.archive-article'),
        confirmLabel: this.translationService.translate('confirm.label.archive'),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.articleService
          .archiveArticle(id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => this.loadArticles(),
            error: () =>
              this.errorMessage.set(this.translationService.translate('kb.articles.error.archive')),
          });
      });
  }

  deleteArticle(id: string): void {
    this.confirmationDialog
      .confirm({
        title: this.translationService.translate('confirm.title.delete'),
        message: this.translationService.translate('confirm.message.delete-article'),
        confirmLabel: this.translationService.translate('confirm.label.delete'),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (confirmed) {
          this.articleService
            .deleteArticle(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: () => this.loadArticles(),
              error: () =>
                this.errorMessage.set(
                  this.translationService.translate('kb.articles.error.delete'),
                ),
            });
        }
      });
  }

  statusBadgeVariant(status: ArticleStatus): string {
    switch (status) {
      case 'PUBLISHED':
        return 'success';
      case 'DRAFT':
        return 'warning';
      case 'ARCHIVED':
        return 'danger';
      default:
        return 'default';
    }
  }

  statusLabel(status: ArticleStatus): string {
    return this.translationService.translate('admin.articles.status.' + status.toLowerCase());
  }

  protected getTagName(tag: { nameEn: string; nameFr: string }): string {
    return this.translationService.currentLang() === 'fr' ? tag.nameFr : tag.nameEn;
  }

  changePage(page: number): void {
    this.currentPage.set(page);
    this.loadArticles();
  }
}
