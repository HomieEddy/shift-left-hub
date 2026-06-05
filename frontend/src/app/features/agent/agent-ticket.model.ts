/** Represents a support ticket from the agent's perspective. */
export interface AgentTicket {
  id: string;
  ticketNumber: string;
  status: 'NEW' | 'IN_PROGRESS' | 'RESOLVED' | 'CANCELLED';
  category: string;
  urgency: string;
  issue: string;
  shiftLeftContext?: string;
  userId: string;
  userDisplayName: string;
  userEmail: string;
  assignedToId: string | null;
  assignedToDisplayName: string | null;
  resolvedById: string | null;
  resolvedByDisplayName: string | null;
  resolutionNotes: string | null;
  isKnowledgeGap: boolean;
  resolvedAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Represents a work note attached to a ticket. */
export interface WorkNote {
  id: string;
  authorDisplayName: string;
  content: string;
  createdAt: string;
}

/** Request payload for resolving a ticket. */
export interface ResolveTicketRequest {
  resolutionNotes: string;
  isKnowledgeGap: boolean;
}

/** Filters for querying the agent ticket list. */
export interface AgentTicketFilters {
  status?: string;
  category?: string;
  urgency?: string;
  search?: string;
}
