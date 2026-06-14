export type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'default' | 'outline';
export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED' | 'CANCELLED';
export type TicketCategory = 'NETWORK' | 'HARDWARE' | 'SOFTWARE' | 'ACCESS' | 'PERIPHERALS';
export type TicketUrgency = 'HIGH' | 'MEDIUM' | 'LOW';

export function badgeVariantClass(variant: BadgeVariant): string {
  switch (variant) {
    case 'success':
      return 'bg-surface-secondary text-text-primary border border-border-default';
    case 'warning':
      return 'bg-surface-tertiary text-text-primary';
    case 'danger':
      return 'bg-accent-danger-muted text-accent-danger';
    case 'info':
      return 'bg-primary-600 text-white';
    case 'outline':
      return 'bg-surface-primary border border-border-default text-text-secondary';
    default:
      return 'bg-surface-tertiary text-text-secondary';
  }
}

export function statusBadgeClass(status: string): string {
  switch (status) {
    case 'NEW':
      return 'bg-primary-600 text-white';
    case 'IN_PROGRESS':
      return 'bg-accent-warning-muted text-accent-warning';
    case 'RESOLVED':
      return 'bg-accent-success-muted text-accent-success border border-accent-success';
    case 'CANCELLED':
      return 'bg-surface-tertiary text-text-tertiary';
    default:
      return 'bg-surface-tertiary text-text-secondary';
  }
}

export function categoryBadgeClass(category: string): string {
  switch (category) {
    case 'NETWORK':
      return 'bg-surface-primary border border-primary-600 text-primary-600';
    case 'HARDWARE':
      return 'bg-surface-tertiary text-text-primary';
    case 'SOFTWARE':
      return 'bg-accent-info-muted text-accent-info border border-accent-info';
    case 'ACCESS':
      return 'bg-surface-primary border border-border-default text-text-secondary';
    case 'PERIPHERALS':
      return 'bg-surface-tertiary text-text-secondary';
    default:
      return 'bg-surface-tertiary text-text-secondary';
  }
}

export function urgencyBadgeClass(urgency: string): string {
  switch (urgency) {
    case 'HIGH':
      return 'bg-accent-danger-muted text-accent-danger';
    case 'MEDIUM':
      return 'bg-accent-warning-muted text-accent-warning';
    case 'LOW':
      return 'bg-surface-secondary text-text-secondary';
    default:
      return 'bg-surface-tertiary text-text-secondary';
  }
}

export function articleStatusBadgeClass(status: string): string {
  switch (status) {
    case 'PUBLISHED':
      return 'bg-accent-success-muted text-accent-success border border-accent-success';
    case 'DRAFT':
      return 'bg-accent-warning-muted text-accent-warning';
    case 'ARCHIVED':
      return 'bg-surface-tertiary text-text-tertiary';
    default:
      return 'bg-surface-tertiary text-text-secondary';
  }
}
