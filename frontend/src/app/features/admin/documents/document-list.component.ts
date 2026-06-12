import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { DocumentService } from './document.service';
import { DocumentDto, DocumentStatus } from './document.model';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './document-list.component.html',
})
export class DocumentListComponent implements OnInit {
  private documentService = inject(DocumentService);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  protected documents = signal<DocumentDto[]>([]);
  protected isLoading = signal(true);
  protected errorMessage = signal('');

  protected isDragging = signal(false);
  protected isUploading = signal(false);
  protected uploadQueue = signal<{ filename: string; status: string }[]>([]);

  protected readonly acceptedTypes = '.md,.txt,.pdf';

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.documentService.getDocuments().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (docs) => { this.documents.set(docs); this.isLoading.set(false); },
      error: () => { this.errorMessage.set('Failed to load documents'); this.isLoading.set(false); },
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
    const validFiles = files.filter(f => {
      const valid = ['.md', '.txt', '.pdf'].some(ext => f.name.toLowerCase().endsWith(ext));
      if (!valid) {
        this.uploadQueue.update(q => [...q, { filename: f.name, status: 'unsupported' }]);
      }
      return valid;
    });

    this.isUploading.set(true);
    let completed = 0;

    validFiles.forEach(file => {
      this.uploadQueue.update(q => [...q, { filename: file.name, status: 'uploading' }]);
      this.documentService.uploadFile(file).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          this.uploadQueue.update(q => q.map(item =>
            item.filename === file.name ? { ...item, status: 'uploaded' } : item
          ));
          completed++;
          if (completed === validFiles.length) {
            this.isUploading.set(false);
            setTimeout(() => { this.uploadQueue.set([]); }, 3000);
            this.loadDocuments();
          }
        },
        error: () => {
          this.uploadQueue.update(q => q.map(item =>
            item.filename === file.name ? { ...item, status: 'failed' } : item
          ));
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
    this.documentService.reprocessDocument(doc.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loadDocuments(),
      error: (err: Error) => {
        this.errorMessage.set(`Failed to reprocess document: ${err.message}`);
      },
    });
  }

  deleteDocument(doc: DocumentDto): void {
    if (!confirm(this.translationService.translate('admin.documents.confirm-delete'))) return;
    this.documentService.deleteDocument(doc.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loadDocuments(),
      error: (err: Error) => {
        this.errorMessage.set(`Failed to delete document: ${err.message}`);
      },
    });
  }

  statusClass(status: DocumentStatus): string {
    switch (status) {
      case 'UPLOADED':   return 'bg-surface-secondary text-text-secondary border border-border-default';
      case 'PARSING':    return 'bg-primary-600 text-white';
      case 'CHUNKING':   return 'bg-accent-warning-muted text-accent-warning';
      case 'EMBEDDING':  return 'bg-accent-info-muted text-accent-info border border-accent-info';
      case 'READY':      return 'bg-accent-success-muted text-accent-success border border-accent-success';
      case 'FAILED':     return 'bg-accent-danger-muted text-accent-danger';
      default:           return 'bg-surface-tertiary text-text-secondary';
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
