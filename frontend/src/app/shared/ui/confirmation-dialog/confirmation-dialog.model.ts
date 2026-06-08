import { Observable } from 'rxjs';

export interface ConfirmationData {
  title: string;
  message: string;
  confirmLabel: string;
  itemIdentifier?: string;
  onConfirm?: () => Observable<void> | Promise<void>;
}
