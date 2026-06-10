export interface WorkspaceDto {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  logoUrl: string | null;
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
