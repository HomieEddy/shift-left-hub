import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DocumentService } from './document.service';
import { DocumentDto, DocumentStatus } from './document.model';
import { CategoryService } from '../taxonomy/category.service';
import { CategoryDto } from '../taxonomy/category.model';
import { TranslationService } from '../../../core/i18n/translation.service';
import { ToastService } from '../../../shared/ui/toast/toast.service';
import { ModalComponent } from '../../../shared/ui/modal/modal.component';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [DatePipe, FormsModule, ModalComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './document-list.component.html',
})
export class DocumentListComponent implements OnInit {
  private documentService = inject(DocumentService);
  private categoryService = inject(CategoryService);
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);
  private toastService = inject(ToastService);
  protected translationService = inject(TranslationService);

  protected documents = signal<DocumentDto[]>([]);
  protected isLoading = signal(true);
  protected errorMessage = signal('');

  protected isDragging = signal(false);
  protected isUploading = signal(false);
  protected uploadQueue = signal<{ filename: string; status: string }[]>([]);
  protected categories = signal<CategoryDto[]>([]);
  protected selectedCategoryId = signal<string | null>(null);
  protected confirmDeleteOpen = signal(false);
  protected pendingDeleteId = signal<string | null>(null);
  protected pendingDeleteFilename = signal('');
  protected convertedDocIds = signal<Set<string>>(new Set());

  protected readonly acceptedTypes = '.md,.txt,.pdf,.html,.htm,.xhtml,.xml,.docx';

  ngOnInit(): void {
    this.loadDocuments();
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

  loadDocuments(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.documentService
      .getDocuments()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (docs) => {
          this.documents.set(docs);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set(this.translationService.translate('admin.documents.error.load'));
          this.isLoading.set(false);
        },
      });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
    if (event.dataTransfer?.files) {
      this.handleFiles(Array.from(event.dataTransfer.files));
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.handleFiles(Array.from(input.files));
      input.value = '';
    }
  }

  private handleFiles(files: File[]): void {
    const validFiles = files.filter((f) => {
      const valid = ['.md', '.txt', '.pdf', '.html', '.htm', '.xhtml', '.xml', '.docx'].some(
        (ext) => f.name.toLowerCase().endsWith(ext),
      );
      if (!valid) {
        this.uploadQueue.update((q) => [...q, { filename: f.name, status: 'unsupported' }]);
      }
      return valid;
    });

    this.isUploading.set(true);
    let completed = 0;

    validFiles.forEach((file) => {
      this.uploadQueue.update((q) => [...q, { filename: file.name, status: 'uploading' }]);
      this.documentService
        .uploadFile(file, this.selectedCategoryId())
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.uploadQueue.update((q) =>
              q.map((item) =>
                item.filename === file.name ? { ...item, status: 'uploaded' } : item,
              ),
            );
            completed++;
            if (completed === validFiles.length) {
              this.isUploading.set(false);
              setTimeout(() => {
                this.uploadQueue.set([]);
              }, 3000);
              this.loadDocuments();
            }
          },
          error: () => {
            this.uploadQueue.update((q) =>
              q.map((item) => (item.filename === file.name ? { ...item, status: 'failed' } : item)),
            );
            completed++;
            if (completed === validFiles.length) {
              this.isUploading.set(false);
            }
          },
        });
    });

    if (validFiles.length === 0) {
      this.isUploading.set(false);
    }
  }

  reprocessDocument(doc: DocumentDto): void {
    this.documentService
      .reprocessDocument(doc.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.loadDocuments(),
        error: (err: Error) => {
          this.errorMessage.set(
            this.translationService.translate('admin.documents.error.reprocess', { message: err.message }),
          );
        },
      });
  }

  requestDelete(doc: DocumentDto): void {
    this.pendingDeleteId.set(doc.id);
    this.pendingDeleteFilename.set(doc.filename);
    this.confirmDeleteOpen.set(true);
  }

  executeDelete(): void {
    const id = this.pendingDeleteId();
    if (id == null) return;
    this.confirmDeleteOpen.set(false);
    this.documentService
      .deleteDocument(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.pendingDeleteId.set(null);
          this.pendingDeleteFilename.set('');
          this.loadDocuments();
        },
        error: (err: Error) => {
          this.errorMessage.set(
            this.translationService.translate('admin.documents.error.delete', { message: err.message }),
          );
        },
      });
  }

  cancelDelete(): void {
    this.confirmDeleteOpen.set(false);
    this.pendingDeleteId.set(null);
    this.pendingDeleteFilename.set('');
  }

  convertToArticle(doc: DocumentDto): void {
    this.convertedDocIds.update((ids) => ids.add(doc.id));
    this.documentService
      .convertToArticle(doc.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.toastService.success(
            this.translationService.translate('admin.documents.article-created'),
          );
          void this.router.navigate(['/admin/articles', res.articleId, 'edit']);
        },
        error: (err: Error) => {
          this.convertedDocIds.update((ids) => {
            ids.delete(doc.id);
            return ids;
          });
          this.errorMessage.set(
            this.translationService.translate('admin.documents.error.convert', { message: err.message }),
          );
        },
      });
  }

  statusClass(status: DocumentStatus): string {
    switch (status) {
      case 'UPLOADED':
        return 'bg-surface-secondary text-text-secondary border border-border-default';
      case 'PARSING':
        return 'bg-primary-600 text-white';
      case 'CHUNKING':
        return 'bg-accent-warning-muted text-accent-warning';
      case 'EMBEDDING':
        return 'bg-accent-info-muted text-accent-info border border-accent-info';
      case 'READY':
        return 'bg-accent-success-muted text-accent-success border border-accent-success';
      case 'FAILED':
        return 'bg-accent-danger-muted text-accent-danger';
      default:
        return 'bg-surface-tertiary text-text-secondary';
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  statusLabel(status: DocumentStatus): string {
    const key = 'admin.documents.status.' + status.toLowerCase();
    return this.translationService.translate(key);
  }
}
