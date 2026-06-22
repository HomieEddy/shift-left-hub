import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { CategoryService } from './category.service';
import { TranslationService } from '../../../core/i18n/translation.service';
import { TaxonomyBulkComponent } from './taxonomy-bulk.component';

describe('TaxonomyBulkComponent', () => {
  let component: TaxonomyBulkComponent;
  let fixture: ComponentFixture<TaxonomyBulkComponent>;
  let categoryService: { getAll: ReturnType<typeof vi.fn> };
  let http: { post: ReturnType<typeof vi.fn> };
  let translationService: { translate: ReturnType<typeof vi.fn> };

  const mockCategories = [
    { id: 'c1', nameEn: 'Hardware', nameFr: 'Matériel', parentId: null, childCount: 0, createdAt: '' },
    { id: 'c2', nameEn: 'Software', nameFr: 'Logiciel', parentId: null, childCount: 0, createdAt: '' },
    { id: 'c3', nameEn: 'Network', nameFr: 'Réseau', parentId: null, childCount: 0, createdAt: '' },
  ];

  beforeEach(async () => {
    categoryService = {
      getAll: vi.fn(),
    };
    http = {
      post: vi.fn(),
    };
    translationService = {
      translate: vi.fn((key: string) => key),
    };

    await TestBed.configureTestingModule({
      imports: [TaxonomyBulkComponent],
      providers: [
        { provide: CategoryService, useValue: categoryService },
        { provide: HttpClient, useValue: http },
        { provide: TranslationService, useValue: translationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TaxonomyBulkComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    categoryService.getAll.mockReturnValue(of(mockCategories));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should select and deselect categories', () => {
    categoryService.getAll.mockReturnValue(of(mockCategories));
    fixture.detectChanges();

    component.selectedCategoryId.set('c1');
    expect(component.selectedCategoryId()).toBe('c1');

    component.selectedCategoryId.set(null);
    expect(component.selectedCategoryId()).toBeNull();
  });

  it('should select all categories', () => {
    categoryService.getAll.mockReturnValue(of(mockCategories));
    fixture.detectChanges();

    expect(component.categories().length).toBe(3);
  });

  it('should perform batch apply', () => {
    categoryService.getAll.mockReturnValue(of(mockCategories));
    fixture.detectChanges();

    component.selectedCategoryId.set('c1');
    http.post.mockReturnValue(of({}));

    component.applyCategory();

    expect(http.post).toHaveBeenCalledWith('/api/admin/articles/bulk-category', { categoryId: 'c1' });
    expect(component['isApplying']()).toBe(false);
    expect(component['applyMessage']()).toBe('admin.taxonomy.bulk.updated');
  });

  it('should perform batch apply to documents tab', () => {
    categoryService.getAll.mockReturnValue(of(mockCategories));
    fixture.detectChanges();

    component.activeTab.set('documents');
    component.selectedCategoryId.set('c2');
    http.post.mockReturnValue(of({}));

    component.applyCategory();

    expect(http.post).toHaveBeenCalledWith('/api/admin/documents/bulk-category', { categoryId: 'c2' });
  });

  it('should handle errors on batch operations', () => {
    categoryService.getAll.mockReturnValue(of(mockCategories));
    fixture.detectChanges();

    component.selectedCategoryId.set('c1');
    const errorSubject = new Subject<unknown>();
    http.post.mockReturnValue(errorSubject.asObservable());

    component.applyCategory();
    errorSubject.error(new Error('API error'));

    expect(component['isApplying']()).toBe(false);
    expect(component['applyMessage']()).toBe('admin.taxonomy.bulk.error.update');
  });
});
