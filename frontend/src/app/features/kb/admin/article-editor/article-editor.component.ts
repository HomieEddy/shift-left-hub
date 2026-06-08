import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ArticleService } from '../../services/article.service';
import { TagService } from '../../services/tag.service';
import { TagDto } from '../../models/tag.models';
import { CreateArticleRequest, UpdateArticleRequest } from '../../models/article.models';
import { MarkdownModule } from 'ngx-markdown';
import { TranslationService } from '../../../../core/i18n/translation.service';

@Component({
  selector: 'app-article-editor',
  standalone: true,
  imports: [FormsModule, MarkdownModule],
  templateUrl: './article-editor.component.html',
})
export class ArticleEditorComponent implements OnInit {
  private articleService = inject(ArticleService);
  private tagService = inject(TagService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  isEdit = signal(false);
  articleId = signal<string | null>(null);
  isLoading = signal(false);
  isSaving = signal(false);
  errorMessage = signal('');

  activeTab = signal<'en' | 'fr'>('en');

  titleEn = '';
  contentEn = '';
  titleFr = '';
  contentFr = '';
  excerpt = '';
  featuredImage = '';

  allTags = signal<TagDto[]>([]);
  selectedTagIds = signal<Set<string>>(new Set());

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id !== null) {
      this.isEdit.set(true);
      this.articleId.set(id);
      this.loadArticle(id);
    }
    this.loadTags();
  }

  loadArticle(id: string): void {
    this.isLoading.set(true);
    this.articleService.getArticleById(id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (article) => {
        this.titleEn = article.titleEn;
        this.contentEn = article.contentEn;
        this.titleFr = article.titleFr ?? '';
        this.contentFr = article.contentFr ?? '';
        this.excerpt = article.excerpt ?? '';
        this.featuredImage = article.featuredImage ?? '';
        this.selectedTagIds.set(new Set(article.tags.map(t => t.id)));
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load article.');
        this.isLoading.set(false);
      },
    });
  }

  loadTags(): void {
    this.tagService.getTags().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (tags) => this.allTags.set(tags),
      error: () => this.errorMessage.set('Failed to load tags.'),
    });
  }

  toggleTag(tagId: string): void {
    this.selectedTagIds.update(ids => {
      const newIds = new Set(ids);
      if (newIds.has(tagId)) {
        newIds.delete(tagId);
      } else {
        newIds.add(tagId);
      }
      return newIds;
    });
  }

  save(): void {
    this.isSaving.set(true);
    this.errorMessage.set('');

    const request = {
      titleEn: this.titleEn,
      contentEn: this.contentEn,
      titleFr: this.titleFr || undefined,
      contentFr: this.contentFr || undefined,
      excerpt: this.excerpt || undefined,
      featuredImage: this.featuredImage || undefined,
      tagIds: Array.from(this.selectedTagIds()),
    } satisfies CreateArticleRequest | UpdateArticleRequest;

    const id = this.articleId();
    const action = this.isEdit() && id !== null
      ? this.articleService.updateArticle(id, request)
      : this.articleService.createArticle(request);

    action.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.isSaving.set(false);
        void this.router.navigate(['/admin/articles']);
      },
      error: () => {
        this.errorMessage.set('Failed to save article. Please try again.');
        this.isSaving.set(false);
      },
    });
  }

  cancel(): void {
    void this.router.navigate(['/admin/articles']);
  }
}
