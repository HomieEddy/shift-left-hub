import { Injectable, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { Observable } from 'rxjs';
import { ConfirmationData } from './confirmation-dialog.model';
import { ConfirmDialogComponent } from './confirmation-dialog.component';

@Injectable({ providedIn: 'root' })
export class ConfirmationDialogService {
  private dialog = inject(Dialog);

  confirm(data: ConfirmationData): Observable<boolean | undefined> {
    const dialogRef = this.dialog.open<boolean>(ConfirmDialogComponent, {
      data,
      width: '420px',
      disableClose: false,
      autoFocus: '[data-confirm-btn]',
    });
    return dialogRef.closed;
  }
}
