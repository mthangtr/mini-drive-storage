export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: number;
  validationErrors?: Record<string, string>;
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  storageUsed: number;
  storageQuota: number;
}

export interface AuthResponse {
  token: string;
  type: string;
  userId: string;
  email: string;
  fullName: string;
  storageUsed: number;
  storageQuota: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export enum FileType {
  FILE = "FILE",
  FOLDER = "FOLDER"
}

export enum DownloadStatus {
  PENDING = "PENDING",
  PROCESSING = "PROCESSING",
  READY = "READY",
  FAILED = "FAILED"
}

export interface FileItem {
  id: string;
  name: string;
  type: FileType;
  mimeType: string | null;
  size: number;
  parentId: string | null;
  ownerId: string;
  ownerName: string;
  deleted: boolean;
  createdAt: string;
  updatedAt: string;
  canEdit: boolean;
}

export interface CreateFolderRequest {
  name: string;
  parentId?: string;
}

export interface UploadFileResponse {
  files: FileItem[];
  successCount: number;
  totalCount: number;
}

export interface DownloadStatusResponse {
  requestId: string;
  status: DownloadStatus;
  downloadUrl: string | null;
  message: string;
}
