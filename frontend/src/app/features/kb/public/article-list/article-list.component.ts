import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgFor, NgIf, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PublicArticleService } from '../../services/public-article.service';
import { ArticleDto } from '../../models/article.models';
import { TranslationService } from '../../../../core/i18n/translation.service';

@Component({
  selector: 'app-article-list',
  standalone: true,
  imports: [NgFor, NgIf, DatePipe, RouterLink],
  templateUrl: './article-list.component.html',
})
export class ArticleListComponent implements OnInit {
  private publicArticleService = inject(PublicArticleService);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  articles = signal<ArticleDto[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  currentPage = signal(0);
  totalPages = signal(0);

  ngOnInit(): void {
    this.loadArticles();
  }

  loadArticles(): void {
    this.isLoading.set(true);
    this.publicArticleService.getArticles(this.currentPage()).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (page) => {
        this.articles.set(page.content);
        this.totalPages.set(page.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load articles.');
        this.isLoading.set(false);
      },
    });
  }

  changePage(page: number): void {
    this.currentPage.set(page);
    this.loadArticles();
  }

  displayTitle(article: ArticleDto): string {
    if (this.translationService.currentLang() === 'fr' && article.titleFr) {
      return article.titleFr;
    }
    return article.titleEn;
  }

  displayExcerpt(article: ArticleDto): string {
    if (article.excerpt) return article.excerpt;
    const content = this.translationService.currentLang() === 'fr'
      ? (article.contentFr || article.contentEn)
      : article.contentEn;
    return content ? content.substring(0, 150) + '...' : '';
  }
}
