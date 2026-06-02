export interface TagDto {
  id: string;
  nameEn: string;
  nameFr: string;
  color: string;
  articleCount: number;
  createdAt: string;
}

export interface CreateTagRequest {
  nameEn: string;
  nameFr: string;
  color: string;
}

export interface UpdateTagRequest {
  nameEn: string;
  nameFr: string;
  color: string;
}
