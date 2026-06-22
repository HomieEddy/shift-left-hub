import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { CategoryService } from './category.service';
import { TranslationService } from '../../../core/i18n/translation.service';
import { TaxonomyTreeComponent } from './taxonomy-tree.component';
import { CategoryDto } from './category.model';

describe('TaxonomyTreeComponent', () => {
  let component: TaxonomyTreeComponent;
  let fixture: ComponentFixture<TaxonomyTreeComponent>;
  let categoryService: {
    getAll: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
    merge: ReturnType<typeof vi.fn>;
  };
  let translationService: { translate: ReturnType<typeof vi.fn> };

  const root1: CategoryDto = { id: 'c1', nameEn: 'Hardware', nameFr: 'Matériel', parentId: null, childCount: 2, createdAt: '2026-01-01T00:00:00Z' };
  const root2: CategoryDto = { id: 'c2', nameEn: 'Software', nameFr: 'Logiciel', parentId: null, childCount: 0, createdAt: '2026-01-02T00:00:00Z' };
  const child1: CategoryDto = { id: 'c3', nameEn: 'Laptops', nameFr: 'Portables', parentId: 'c1', childCount: 0, createdAt: '2026-01-03T00:00:00Z' };
  const child2: CategoryDto = { id: 'c4', nameEn: 'Printers', nameFr: 'Imprimantes', parentId: 'c1', childCount: 0, createdAt: '2026-01-04T00:00:00Z' };
  const grandchild: CategoryDto = { id: 'c5', nameEn: 'Inkjet', nameFr: 'Jet d\'encre', parentId: 'c4', childCount: 0, createdAt: '2026-01-05T00:00:00Z' };

  beforeEach(async () => {
    categoryService = {
      getAll: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
      delete: vi.fn(),
      merge: vi.fn(),
    };
    translationService = {
      translate: vi.fn((key: string) => key),
    };

    await TestBed.configureTestingModule({
      imports: [TaxonomyTreeComponent],
      providers: [
        { provide: CategoryService, useValue: categoryService },
        { provide: TranslationService, useValue: translationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TaxonomyTreeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    categoryService.getAll.mockReturnValue(of([]));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load and display hierarchical categories', () => {
    const categories = [root1, root2, child1, child2, grandchild];
    categoryService.getAll.mockReturnValue(of(categories));
    fixture.detectChanges();

    expect(categoryService.getAll).toHaveBeenCalled();
    expect(component.treeNodes().length).toBe(2);
    expect(component['rawCategories']().length).toBe(5);
  });

  it('should expand and collapse tree nodes', () => {
    const categories = [root1, root2, child1, child2];
    categoryService.getAll.mockReturnValue(of(categories));
    fixture.detectChanges();

    const hardwareNode = component.treeNodes()[0];
    expect(hardwareNode.expanded).toBe(false);

    component.toggleExpand(hardwareNode);
    expect(hardwareNode.expanded).toBe(true);

    component.toggleExpand(hardwareNode);
    expect(hardwareNode.expanded).toBe(false);
  });

  it('should add category at root level', () => {
    categoryService.getAll.mockReturnValue(of([]));
    fixture.detectChanges();

    component.openCreate();
    expect(component['showCreateDialog']()).toBe(true);
    expect(component.formParentId).toBeNull();

    component.formNameEn = 'New Category';
    component.formNameFr = 'Nouvelle Catégorie';
    categoryService.create.mockReturnValue(of({ id: 'new', nameEn: 'New Category', nameFr: 'Nouvelle Catégorie', parentId: null, childCount: 0, createdAt: '' }));
    categoryService.getAll.mockReturnValue(of([]));

    component.createCategory();

    expect(categoryService.create).toHaveBeenCalledWith({ nameEn: 'New Category', nameFr: 'Nouvelle Catégorie', parentId: null });
    expect(component['showCreateDialog']()).toBe(false);
  });

  it('should add child category', () => {
    categoryService.getAll.mockReturnValue(of([root1]));
    fixture.detectChanges();

    component.openCreate('c1');
    expect(component.formParentId).toBe('c1');
    expect(component['showCreateDialog']()).toBe(true);

    component.formNameEn = 'New Child';
    categoryService.create.mockReturnValue(of({ id: 'new', nameEn: 'New Child', nameFr: '', parentId: 'c1', childCount: 0, createdAt: '' }));
    categoryService.getAll.mockReturnValue(of([root1]));

    component.createCategory();

    expect(categoryService.create).toHaveBeenCalledWith({ nameEn: 'New Child', nameFr: '', parentId: 'c1' });
  });

  it('should rename category', () => {
    categoryService.getAll.mockReturnValue(of([root1]));
    fixture.detectChanges();

    component.openEdit(root1);
    expect(component['editingCategory']()).toEqual(root1);
    expect(component.formNameEn).toBe('Hardware');
    expect(component['showEditDialog']()).toBe(true);

    component.formNameEn = 'Hardware Updated';
    categoryService.update.mockReturnValue(of({ ...root1, nameEn: 'Hardware Updated' }));
    categoryService.getAll.mockReturnValue(of([{ ...root1, nameEn: 'Hardware Updated' }]));

    component.updateCategory();

    expect(categoryService.update).toHaveBeenCalledWith('c1', { nameEn: 'Hardware Updated', nameFr: 'Matériel', parentId: null });
    expect(component['showEditDialog']()).toBe(false);
  });

  it('should delete category', () => {
    categoryService.getAll.mockReturnValue(of([root1, child1]));
    fixture.detectChanges();

    component.confirmDelete(root1);
    expect(component['deletingCategory']()).toEqual(root1);
    expect(component['showDeleteDialog']()).toBe(true);

    categoryService.delete.mockReturnValue(of(undefined));
    categoryService.getAll.mockReturnValue(of([]));

    component.deleteCategory();

    expect(categoryService.delete).toHaveBeenCalledWith('c1', undefined);
    expect(component['showDeleteDialog']()).toBe(false);
  });

  it('should move category to new parent', () => {
    const categories = [root1, root2, child1];
    categoryService.getAll.mockReturnValue(of(categories));
    fixture.detectChanges();

    categoryService.update.mockReturnValue(of({ ...child1, parentId: 'c2' }));
    categoryService.getAll.mockReturnValue(of(categories));

    const dragEvent = new Event('dragstart') as DragEvent;
    component.onDragStart(dragEvent, 'c3');
    expect(component['draggedCategoryId']).toBe('c3');

    component.onDrop('c3', 'c2');
    expect(categoryService.update).toHaveBeenCalledWith('c3', { nameEn: 'Laptops', nameFr: 'Portables', parentId: 'c2' });
  });

  it('should handle empty category tree', () => {
    categoryService.getAll.mockReturnValue(of([]));
    fixture.detectChanges();

    expect(component.treeNodes().length).toBe(0);
    expect(component['rawCategories']().length).toBe(0);
    expect(component.isLoading()).toBe(false);
  });

  it('should handle errors on CRUD operations', () => {
    const errorSubject = new Subject<unknown>();
    categoryService.getAll.mockReturnValue(errorSubject.asObservable());

    fixture.detectChanges();
    errorSubject.error(new Error('Failed to load'));

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('admin.taxonomy.error.load');
  });
});
