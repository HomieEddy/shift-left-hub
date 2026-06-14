import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ArticleService } from '../../services/article.service';
import { TagService } from '../../services/tag.service';
import { TagDto } from '../../models/tag.models';
import { CreateArticleRequest, UpdateArticleRequest } from '../../models/article.models';
import { MarkdownModule } from 'ngx-markdown';
import { CategoryService } from '../../../admin/taxonomy/category.service';
import { CategoryDto } from '../../../admin/taxonomy/category.model';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { ConfirmationDialogService } from '../../../../shared/ui/confirmation-dialog/confirmation-dialog.service';

@Component({
  selector: 'app-article-editor',
  standalone: true,
  imports: [FormsModule, MarkdownModule],
  templateUrl: './article-editor.component.html',
})
export class ArticleEditorComponent implements OnInit {
  private articleService = inject(ArticleService);
  private tagService = inject(TagService);
  private categoryService = inject(CategoryService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private confirmationDialog = inject(ConfirmationDialogService);
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
  excerptFr = '';
  featuredImage = '';

  private originalTitleEn = '';
  private originalContentEn = '';
  private originalTitleFr = '';
  private originalContentFr = '';
  private originalExcerpt = '';
  private originalExcerptFr = '';
  private originalFeaturedImage = '';

  allTags = signal<TagDto[]>([]);
  selectedTagIds = signal<Set<string>>(new Set());
  categories = signal<CategoryDto[]>([]);
  selectedCategoryId = signal<string | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id !== null) {
      this.isEdit.set(true);
      this.articleId.set(id);
      this.loadArticle(id);
    }
    this.loadTags();
    this.loadCategories();
  }

  loadCategories(): void {
    this.categoryService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cats) => this.categories.set(cats),
      });
  }

  loadArticle(id: string): void {
    this.isLoading.set(true);
    this.articleService
      .getArticleById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (article) => {
          this.titleEn = article.titleEn;
          this.contentEn = article.contentEn;
          this.titleFr = article.titleFr ?? '';
          this.contentFr = article.contentFr ?? '';
          this.excerpt = article.excerpt ?? '';
          this.excerptFr = article.excerptFr ?? '';
          this.featuredImage = article.featuredImage ?? '';
          this.originalTitleEn = this.titleEn;
          this.originalContentEn = this.contentEn;
          this.originalTitleFr = this.titleFr;
          this.originalContentFr = this.contentFr;
          this.originalExcerpt = this.excerpt;
          this.originalExcerptFr = this.excerptFr;
          this.originalFeaturedImage = this.featuredImage;
          this.selectedCategoryId.set(article.categoryId);
          this.selectedTagIds.set(new Set(article.tags.map((t) => t.id)));
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set(this.translationService.translate('kb.articles.error.load'));
          this.isLoading.set(false);
        },
      });
  }

  loadTags(): void {
    this.tagService
      .getTags()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (tags) => this.allTags.set(tags),
        error: () => this.errorMessage.set(this.translationService.translate('kb.tags.error.load')),
      });
  }

  toggleTag(tagId: string): void {
    this.selectedTagIds.update((ids) => {
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
      excerptFr: this.excerptFr || undefined,
      featuredImage: this.featuredImage || undefined,
      tagIds: Array.from(this.selectedTagIds()),
      categoryId: this.selectedCategoryId() ?? undefined,
    } satisfies CreateArticleRequest | UpdateArticleRequest;

    const id = this.articleId();
    const action =
      this.isEdit() && id !== null
        ? this.articleService.updateArticle(id, request)
        : this.articleService.createArticle(request);

    action.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.isSaving.set(false);
        void this.router.navigate(['/admin/articles']);
      },
      error: () => {
        this.errorMessage.set(this.translationService.translate('kb.articles.error.save'));
        this.isSaving.set(false);
      },
    });
  }

  cancel(): void {
    if (this.isDirty()) {
      this.confirmationDialog
        .confirm({
          title: this.translationService.translate('admin.articles.discard-title'),
          message: this.translationService.translate('admin.articles.discard-message'),
          confirmLabel: this.translationService.translate('admin.articles.discard-confirm'),
        })
        .subscribe((confirmed) => {
          if (confirmed) void this.router.navigate(['/admin/articles']);
        });
    } else {
      void this.router.navigate(['/admin/articles']);
    }
  }

  private isDirty(): boolean {
    return (
      this.titleEn !== this.originalTitleEn ||
      this.contentEn !== this.originalContentEn ||
      this.titleFr !== this.originalTitleFr ||
      this.contentFr !== this.originalContentFr ||
      this.excerpt !== this.originalExcerpt ||
      this.excerptFr !== this.originalExcerptFr ||
      this.featuredImage !== this.originalFeaturedImage
    );
  }

  protected getTagName(tag: { nameEn: string; nameFr: string }): string {
    return this.translationService.currentLang() === 'fr' ? tag.nameFr : tag.nameEn;
  }
}
