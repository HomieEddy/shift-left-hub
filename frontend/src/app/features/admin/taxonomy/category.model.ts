export interface CategoryDto {
  id: string;
  nameEn: string;
  nameFr: string;
  parentId: string | null;
  childCount: number;
  createdAt: string;
}

export interface CategoryRequest {
  nameEn: string;
  nameFr: string;
  parentId: string | null;
}

export interface TreeNode {
  category: CategoryDto;
  children: TreeNode[];
  expanded: boolean;
}

export interface MergeRequest {
  sourceCategoryId: string;
  targetCategoryId: string;
}
