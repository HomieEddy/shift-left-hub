/** Represents a KCS auto-drafted article in the admin review queue. */
export interface KcsDraft {
  id: string;
  titleEn: string;
  titleFr: string | null;
  slug: string;
  excerpt: string | null;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  sourceTicketId: string;
  sourceTicketNumber: string | null;
  similarityWarnings: string[];
  tags: { id: string; nameEn: string; nameFr: string; color: string }[];
  createdAt: string;
}

/** Response shape for the pending-count endpoint used by the nav badge. */
export interface PendingCountResponse {
  pendingCount: number;
}
