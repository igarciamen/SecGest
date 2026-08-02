export interface DocumentMeta {
  id: number;
  taskId: number;
  uploaderUserId: number;
  uploaderRole: 'ROLE_USER' | 'ROLE_ADMIN';
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}
