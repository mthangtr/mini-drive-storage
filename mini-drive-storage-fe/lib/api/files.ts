import { api } from "./client";
import type {
  FileItem,
  CreateFolderRequest,
  UploadFileResponse,
  DownloadStatusResponse,
  ShareFileRequest,
  ShareFileResponse,
} from "@/lib/types";

export const fileService = {
  async listFiles(params?: {
    q?: string;
    type?: string;
    parentId?: string;
    fromSize?: number;
    toSize?: number;
  }): Promise<FileItem[]> {
    const queryParams = new URLSearchParams();
    if (params?.q) queryParams.append("q", params.q);
    if (params?.type) queryParams.append("type", params.type);
    if (params?.parentId) queryParams.append("parentId", params.parentId);
    if (params?.fromSize) queryParams.append("fromSize", params.fromSize.toString());
    if (params?.toSize) queryParams.append("toSize", params.toSize.toString());

    const query = queryParams.toString();
    return api.get<FileItem[]>(`/api/v1/files${query ? `?${query}` : ""}`);
  },

  async getFileDetails(fileId: string): Promise<FileItem> {
    return api.get<FileItem>(`/api/v1/files/${fileId}`);
  },

  async createFolder(data: CreateFolderRequest): Promise<FileItem> {
    return api.post<FileItem>("/api/v1/files", data);
  },

  async uploadFiles(files: File[], parentId?: string): Promise<UploadFileResponse> {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append("files", file);
    });
    if (parentId) {
      formData.append("parentId", parentId);
    }

    return api.post<UploadFileResponse>("/api/v1/files", formData);
  },

  async downloadFile(fileId: string, fileName: string): Promise<void> {
    const token = document.cookie.split('; ').find(row => row.startsWith('token='))?.split('=')[1];
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/v1/files/${fileId}/download`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {
      throw new Error("Download failed");
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  },

  async initiateDownloadFolder(folderId: string): Promise<DownloadStatusResponse> {
    return api.post<DownloadStatusResponse>(`/api/v1/files/${folderId}/download`);
  },

  async getDownloadStatus(requestId: string): Promise<DownloadStatusResponse> {
    return api.get<DownloadStatusResponse>(`/api/v1/files/downloads/${requestId}`);
  },

  async downloadZipFile(requestId: string, fileName: string): Promise<void> {
    const token = document.cookie.split('; ').find(row => row.startsWith('token='))?.split('=')[1];
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/v1/files/downloads/${requestId}/file`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {
      throw new Error("Download failed");
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName + ".zip";
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  },

  async deleteFile(fileId: string): Promise<void> {
    return api.delete<void>(`/api/v1/files/${fileId}`);
  },

  async shareFile(fileId: string, data: ShareFileRequest): Promise<ShareFileResponse> {
    return api.post<ShareFileResponse>(`/api/v1/files/${fileId}/share`, data);
  },

  async getSharedWithMe(): Promise<FileItem[]> {
    return api.get<FileItem[]>("/api/v1/files/shared-with-me");
  },

  async getFileShares(fileId: string): Promise<ShareFileResponse[]> {
    return api.get<ShareFileResponse[]>(`/api/v1/files/${fileId}/shares`);
  },

  async removeShare(fileId: string, email: string): Promise<void> {
    return api.delete<void>(`/api/v1/files/${fileId}/share/${encodeURIComponent(email)}`);
  },
};
