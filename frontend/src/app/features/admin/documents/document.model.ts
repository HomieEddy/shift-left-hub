export type DocumentStatus = 'UPLOADED' | 'PARSING' | 'CHUNKING' | 'EMBEDDING' | 'READY' | 'FAILED';

export interface DocumentDto {
  id: string;
  filename: string;
  mimeType: string;
  status: DocumentStatus;
  errorMessage: string | null;
  fileSize: number;
  chunkCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentUploadResponse {
  id: string;
  filename: string;
  mimeType: string;
  status: DocumentStatus;
  fileSize: number;
  createdAt: string;
}
