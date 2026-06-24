import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CategoryService } from './category.service';
import { CategoryDto, CategoryRequest, TreeNode, MergeRequest } from './category.model';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-taxonomy-tree',
  standalone: true,
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './taxonomy-tree.component.html',
})
export class TaxonomyTreeComponent implements OnInit {
  private categoryService = inject(CategoryService);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  protected rawCategories = signal<CategoryDto[]>([]);
  treeNodes = signal<TreeNode[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');

  showCreateDialog = signal(false);
  showEditDialog = signal(false);
  showDeleteDialog = signal(false);
  showMergeDialog = signal(false);

  editingCategory = signal<CategoryDto | null>(null);
  deletingCategory = signal<CategoryDto | null>(null);

  formNameEn = '';
  formNameFr = '';
  formParentId: string | null = null;

  mergeSourceId: string | null = null;
  mergeTargetId: string | null = null;
  reassignTargetId: string | null = null;

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.isLoading.set(true);
    this.categoryService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cats) => {
          this.rawCategories.set(cats);
          this.treeNodes.set(this.buildTree(cats));
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set(this.translationService.translate('admin.taxonomy.error.load'));
          this.isLoading.set(false);
        },
      });
  }

  private buildTree(categories: CategoryDto[]): TreeNode[] {
    const map = new Map<string, CategoryDto>();
    categories.forEach((c) => map.set(c.id, c));

    const roots = categories.filter((c) => c.parentId === null || !map.has(c.parentId));
    const buildChildren = (parentId: string): TreeNode[] =>
      categories
        .filter((c) => c.parentId === parentId)
        .map((c) => ({
          category: c,
          children: buildChildren(c.id),
          expanded: false,
        }));

    return roots.map((c) => ({
      category: c,
      children: buildChildren(c.id),
      expanded: false,
    }));
  }

  toggleExpand(node: TreeNode): void {
    node.expanded = !node.expanded;
    this.treeNodes.set([...this.treeNodes()]);
  }

  openCreate(parentId?: string): void {
    this.formNameEn = '';
    this.formNameFr = '';
    this.formParentId = parentId ?? null;
    this.editingCategory.set(null);
    this.showCreateDialog.set(true);
  }

  createCategory(): void {
    const request: CategoryRequest = {
      nameEn: this.formNameEn,
      nameFr: this.formNameFr,
      parentId: this.formParentId,
    };
    this.categoryService
      .create(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.showCreateDialog.set(false);
          this.loadCategories();
        },
      });
  }

  openEdit(cat: CategoryDto): void {
    this.editingCategory.set(cat);
    this.formNameEn = cat.nameEn;
    this.formNameFr = cat.nameFr;
    this.formParentId = cat.parentId;
    this.showEditDialog.set(true);
  }

  updateCategory(): void {
    const editing = this.editingCategory();
    if (!editing) return;
    const request: CategoryRequest = {
      nameEn: this.formNameEn,
      nameFr: this.formNameFr,
      parentId: this.formParentId,
    };
    this.categoryService
      .update(editing.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.showEditDialog.set(false);
          this.loadCategories();
        },
      });
  }

  confirmDelete(cat: CategoryDto): void {
    this.deletingCategory.set(cat);
    this.reassignTargetId = null;
    this.showDeleteDialog.set(true);
  }

  deleteCategory(): void {
    const cat = this.deletingCategory();
    if (!cat) return;
    this.categoryService
      .delete(cat.id, this.reassignTargetId ?? undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.showDeleteDialog.set(false);
          this.loadCategories();
        },
      });
  }

  openMerge(): void {
    this.mergeSourceId = null;
    this.mergeTargetId = null;
    this.showMergeDialog.set(true);
  }

  mergeCategories(): void {
    if (this.mergeSourceId == null || this.mergeTargetId == null) return;
    const request: MergeRequest = {
      sourceCategoryId: this.mergeSourceId,
      targetCategoryId: this.mergeTargetId,
    };
    this.categoryService
      .merge(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.showMergeDialog.set(false);
          this.loadCategories();
        },
      });
  }

  protected draggedCategoryId: string | null = null;

  onDragStart(event: DragEvent, categoryId: string): void {
    this.draggedCategoryId = categoryId;
  }

  onDrop(draggedId: string, targetId: string): void {
    const cat = this.rawCategories().find((c) => c.id === draggedId);
    if (cat == null) return;
    const request: CategoryRequest = { nameEn: cat.nameEn, nameFr: cat.nameFr, parentId: targetId };
    this.categoryService
      .update(draggedId, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.loadCategories(),
      });
  }

  getCategoryFullPath(cat: CategoryDto): string {
    const parts: string[] = [cat.nameEn];
    let current = cat;
    const map = new Map(this.rawCategories().map((c) => [c.id, c]));
    while (current.parentId != null && map.has(current.parentId)) {
      current = map.get(current.parentId)!;
      parts.unshift(current.nameEn);
    }
    return parts.join(' > ');
  }

  getOtherCategories(excludeId: string): CategoryDto[] {
    return this.rawCategories().filter((c) => c.id !== excludeId);
  }

  getCategoryDepth(catId: string): number {
    let depth = 0;
    let current = this.rawCategories().find((c) => c.id === catId);
    const map = new Map(this.rawCategories().map((c) => [c.id, c]));
    while (current != null && current.parentId != null && map.has(current.parentId)) {
      current = map.get(current.parentId)!;
      depth++;
    }
    return depth;
  }
}
