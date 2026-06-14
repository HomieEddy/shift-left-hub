import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { provideMarkdown } from 'ngx-markdown';
import { ArticleService } from '../../services/article.service';
import { TagService } from '../../services/tag.service';
import { ArticleEditorComponent } from './article-editor.component';

describe('ArticleEditorComponent', () => {
  let component: ArticleEditorComponent;
  let fixture: ComponentFixture<ArticleEditorComponent>;
  let articleService: {
    getArticleById: ReturnType<typeof vi.fn>;
    createArticle: ReturnType<typeof vi.fn>;
    updateArticle: ReturnType<typeof vi.fn>;
  };
  let tagService: {
    getTags: ReturnType<typeof vi.fn>;
  };
  let paramMap: { get: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  const mockArticle = {
    id: '123',
    titleEn: 'Test Article EN',
    contentEn: 'Test content EN',
    titleFr: 'Test Article FR',
    contentFr: 'Test content FR',
    slug: 'test-article',
    excerpt: 'A test article for testing',
    featuredImage: null,
    status: 'PUBLISHED',
    viewCount: 10,
    publishedAt: '2026-01-01T00:00:00Z',
    authorId: 'u1',
    authorName: 'Admin',
    lastEditorId: null,
    lastEditorName: null,
    tags: [
      {
        id: 't1',
        nameEn: 'Networking',
        nameFr: 'Réseau',
        color: 'blue',
        articleCount: 1,
        createdAt: '2026-01-01T00:00:00Z',
      },
    ],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    articleService = {
      getArticleById: vi.fn(),
      createArticle: vi.fn(),
      updateArticle: vi.fn(),
    };
    tagService = {
      getTags: vi.fn().mockReturnValue(of([])),
    };
    paramMap = {
      get: vi.fn(),
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    await TestBed.configureTestingModule({
      imports: [ArticleEditorComponent],
      providers: [
        { provide: ArticleService, useValue: articleService },
        { provide: TagService, useValue: tagService },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap } } },
        { provide: Router, useValue: router },
        provideMarkdown(),
      ],
    }).compileComponents();
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(ArticleEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe('create mode', () => {
    beforeEach(() => {
      paramMap.get.mockReturnValue(null);
      createComponent();
    });

    it('should start in create mode when no id param', () => {
      expect(component.isEdit()).toBe(false);
      expect(component.articleId()).toBeNull();
    });

    it('should load tags on init', () => {
      expect(tagService.getTags).toHaveBeenCalled();
    });

    it('should not load article when no id param', () => {
      expect(articleService.getArticleById).not.toHaveBeenCalled();
    });

    it('should call createArticle on save in create mode', () => {
      articleService.createArticle.mockReturnValue(of(mockArticle));

      component.titleEn = 'New Article';
      component.contentEn = 'New content';
      component.save();

      expect(articleService.createArticle).toHaveBeenCalledWith({
        titleEn: 'New Article',
        contentEn: 'New content',
        titleFr: undefined,
        contentFr: undefined,
        excerpt: undefined,
        featuredImage: undefined,
        tagIds: [],
      });
      expect(router.navigate).toHaveBeenCalledWith(['/admin/articles']);
    });

    it('should handle save error', () => {
      const errorSubject = new Subject<unknown>();
      articleService.createArticle.mockReturnValue(errorSubject.asObservable());

      component.titleEn = 'New Article';
      component.contentEn = 'New content';
      component.save();
      errorSubject.error(new Error('Save failed'));

      expect(component.isSaving()).toBe(false);
      expect(component.errorMessage()).toBe('Failed to save article. Please try again.');
    });
  });

  describe('edit mode', () => {
    beforeEach(() => {
      paramMap.get.mockReturnValue('123');
      articleService.getArticleById.mockReturnValue(of(mockArticle));
      createComponent();
    });

    it('should start in edit mode with id param', () => {
      expect(component.isEdit()).toBe(true);
      expect(component.articleId()).toBe('123');
    });

    it('should load existing article by id', () => {
      expect(articleService.getArticleById).toHaveBeenCalledWith('123');
    });

    it('should populate form fields from loaded article', () => {
      expect(component.titleEn).toBe('Test Article EN');
      expect(component.contentEn).toBe('Test content EN');
      expect(component.titleFr).toBe('Test Article FR');
      expect(component.contentFr).toBe('Test content FR');
      expect(component.excerpt).toBe('A test article for testing');
    });

    it('should call updateArticle on save in edit mode', () => {
      articleService.updateArticle.mockReturnValue(of(mockArticle));

      component.titleEn = 'Updated Title';
      component.save();

      expect(articleService.updateArticle).toHaveBeenCalledWith('123', {
        titleEn: 'Updated Title',
        contentEn: 'Test content EN',
        titleFr: 'Test Article FR',
        contentFr: 'Test content FR',
        excerpt: 'A test article for testing',
        featuredImage: undefined,
        tagIds: ['t1'],
      });
      expect(router.navigate).toHaveBeenCalledWith(['/admin/articles']);
    });

    it('should handle load article error', () => {
      const errorSubject = new Subject<unknown>();
      articleService.getArticleById.mockReturnValue(errorSubject.asObservable());

      component.loadArticle('123');
      errorSubject.error(new Error('Load failed'));

      expect(component.isLoading()).toBe(false);
      expect(component.errorMessage()).toBe('Failed to load articles.');
    });
  });
});
