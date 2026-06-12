export type WorkspaceRole = 'ADMIN' | 'MEMBER' | 'READ_ONLY';

export interface WorkspaceDto {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  logoUrl: string | null;
  icon: string | null;
  memberCount: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWorkspaceRequest {
  name: string;
  description?: string;
  logoUrl?: string;
}

export interface WorkspaceMemberDto {
  userId: string;
  email: string;
  displayName: string;
  role: string;
  joinedAt: string | null;
}

export interface AssignUserRequest {
  userId: string;
  role: 'ADMIN' | 'MEMBER' | 'READ_ONLY';
}

export interface CreateInvitationRequest {
  userId: string;
  role: 'ADMIN' | 'MEMBER' | 'READ_ONLY';
}

export interface InvitationDto {
  id: string;
  workspaceId: string;
  invitedUserId: string;
  invitedUserEmail: string;
  invitedUserDisplayName: string;
  invitedBy: string;
  role: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}

export interface ChangeRoleRequest {
  role: 'ADMIN' | 'MEMBER' | 'READ_ONLY';
}

export interface UpdateWorkspaceRequest {
  name?: string;
  description?: string;
  icon?: string;
}

export interface WorkspaceRoleResponse {
  role: string;
}
