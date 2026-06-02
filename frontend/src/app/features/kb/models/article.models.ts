export type ArticleStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface TagDto {
  id: string;
  nameEn: string;
  nameFr: string;
  color: string;
  articleCount: number;
  createdAt: string;
}

export interface ArticleDto {
  id: string;
  titleEn: string;
  contentEn: string;
  titleFr: string | null;
  contentFr: string | null;
  slug: string;
  excerpt: string | null;
  featuredImage: string | null;
  status: ArticleStatus;
  viewCount: number;
  publishedAt: string | null;
  authorId: string;
  authorName: string;
  lastEditorId: string | null;
  tags: TagDto[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateArticleRequest {
  titleEn: string;
  contentEn: string;
  titleFr?: string;
  contentFr?: string;
  excerpt?: string;
  featuredImage?: string;
  tagIds?: string[];
}

export interface UpdateArticleRequest {
  titleEn: string;
  contentEn: string;
  titleFr?: string;
  contentFr?: string;
  excerpt?: string;
  featuredImage?: string;
  tagIds?: string[];
}

export interface ArticleSearchResult {
  id: string;
  title: string;
  headline: string;
  slug: string;
  excerpt: string | null;
  publishedAt: string;
  tagNames: string[];
}
