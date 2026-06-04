export interface ShiftLeftContext {
  issue: string;
  category: string;
  urgency: string;
  transcript: { role: string; content: string }[];
  sources: { articleId: string; title: string; slug: string; score: number }[];
  aiSummary: string;
  confidenceScore: number;
}

export interface CreateTicketRequest {
  issue: string;
  category: string;
  urgency: string;
  shiftLeftContext?: string;
}

export interface Ticket {
  id: string;
  ticketNumber: string;
  status: 'NEW' | 'IN_PROGRESS' | 'RESOLVED' | 'CANCELLED';
  category: 'NETWORK' | 'HARDWARE' | 'SOFTWARE' | 'ACCESS' | 'PERIPHERALS';
  urgency: 'LOW' | 'MEDIUM' | 'HIGH';
  issue: string;
  shiftLeftContext?: string;
  userId: string;
  userDisplayName: string;
  resolvedAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EscalationPayload {
  issue: string;
  transcript: { role: string; content: string }[];
  sources: { articleId: string; title: string; slug: string; score: number }[];
}
