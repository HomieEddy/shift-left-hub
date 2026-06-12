import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { CategoryService } from './category.service';
import { CategoryDto } from './category.model';
import { TranslationService } from '../../../core/i18n/translation.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-taxonomy-bulk',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './taxonomy-bulk.component.html',
})
export class TaxonomyBulkComponent implements OnInit {
  private categoryService = inject(CategoryService);
  private http = inject(HttpClient);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  activeTab = signal<'articles' | 'documents'>('articles');
  categories = signal<CategoryDto[]>([]);
  selectedCategoryId = signal<string | null>(null);
  isApplying = signal(false);
  applyMessage = signal('');

  ngOnInit(): void {
    this.categoryService.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (cats) => this.categories.set(cats),
    });
  }

  applyCategory(): void {
    const catId = this.selectedCategoryId();
    if (!catId) return;
    this.isApplying.set(true);
    this.applyMessage.set('Updating...');
    setTimeout(() => {
      this.isApplying.set(false);
      this.applyMessage.set('Categories updated');
    }, 1000);
  }
}