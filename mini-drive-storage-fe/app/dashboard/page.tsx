"use client";

import { useState, useEffect, useRef } from "react";
import { FileText, Folder, MoreVertical, Upload, FolderPlus, Download, Trash2, Share2, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { fileService } from "@/lib/api/files";
import { FileItem, FileType } from "@/lib/types";
import { ApiError } from "@/lib/api/client";
import ShareDialog from "@/components/share-dialog";

type ViewMode = "my-drive" | "shared-with-me";

export default function DashboardPage() {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [viewMode, setViewMode] = useState<ViewMode>("my-drive");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [isUploading, setIsUploading] = useState(false);
  const [shareDialogFile, setShareDialogFile] = useState<FileItem | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadFiles = async () => {
    try {
      setIsLoading(true);
      const data = viewMode === "my-drive" 
        ? await fileService.listFiles()
        : await fileService.getSharedWithMe();
      setFiles(data);
      setError("");
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load files");
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadFiles();
  }, [viewMode]);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = e.target.files;
    if (!selectedFiles || selectedFiles.length === 0) return;

    try {
      setIsUploading(true);
      await fileService.uploadFiles(Array.from(selectedFiles));
      await loadFiles(); // Reload files
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to upload files");
      }
    } finally {
      setIsUploading(false);
    }
  };

  const handleCreateFolder = async () => {
    const folderName = prompt("Enter folder name:");
    if (!folderName) return;

    try {
      await fileService.createFolder({ name: folderName });
      await loadFiles();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to create folder");
      }
    }
  };

  const handleDownload = async (file: FileItem) => {
    try {
      if (file.type === FileType.FILE) {
        await fileService.downloadFile(file.id, file.name);
      } else {
        // Folder download
        const response = await fileService.initiateDownloadFolder(file.id);
        const requestId = response.requestId;
        
        // Poll for status
        let attempts = 0;
        const maxAttempts = 30;
        const pollInterval = setInterval(async () => {
          try {
            const status = await fileService.getDownloadStatus(requestId);
            if (status.status === "READY") {
              clearInterval(pollInterval);
              await fileService.downloadZipFile(requestId, file.name);
            } else if (status.status === "FAILED" || attempts >= maxAttempts) {
              clearInterval(pollInterval);
              setError("Download failed");
            }
            attempts++;
          } catch (err) {
            clearInterval(pollInterval);
            setError("Download failed");
          }
        }, 2000);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to download");
      }
    }
  };

  const handleDelete = async (fileId: string) => {
    if (!confirm("Are you sure you want to delete this item?")) return;

    try {
      await fileService.deleteFile(fileId);
      await loadFiles();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to delete");
      }
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  const formatSize = (bytes: number) => {
    if (bytes === 0) return "-";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + " " + sizes[i];
  };

  return (
    <div className="space-y-6">
      {/* Tabs */}
      <div className="border-b">
        <div className="flex gap-6">
          <button
            onClick={() => setViewMode("my-drive")}
            className={`pb-3 px-1 text-sm font-medium border-b-2 transition-colors ${
              viewMode === "my-drive"
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            <Folder className="inline h-4 w-4 mr-2" />
            My Drive
          </button>
          <button
            onClick={() => setViewMode("shared-with-me")}
            className={`pb-3 px-1 text-sm font-medium border-b-2 transition-colors ${
              viewMode === "shared-with-me"
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            <Users className="inline h-4 w-4 mr-2" />
            Shared with me
          </button>
        </div>
      </div>

      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          {viewMode === "my-drive" ? "My Drive" : "Shared with me"}
        </h1>
        {viewMode === "my-drive" && (
          <div className="flex items-center gap-2">
          <input
            ref={fileInputRef}
            type="file"
            multiple
            onChange={handleFileUpload}
            className="hidden"
            id="file-upload"
          />
          <Button
            variant="outline"
            size="sm"
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploading}
          >
            <Upload className="h-4 w-4 mr-2" />
            {isUploading ? "Uploading..." : "Upload"}
          </Button>
          <Button variant="outline" size="sm" onClick={handleCreateFolder}>
            <FolderPlus className="h-4 w-4 mr-2" />
            New Folder
          </Button>
        </div>
        )}
      </div>

      {error && (
        <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="text-muted-foreground">Loading...</div>
        </div>
      ) : files.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <Folder className="h-16 w-16 text-muted-foreground/50 mb-4" />
          <h3 className="text-lg font-medium mb-2">No files yet</h3>
          <p className="text-sm text-muted-foreground mb-4">Upload files or create folders to get started</p>
        </div>
      ) : (
        <div className="rounded-lg border bg-card shadow-sm">
          <div className="grid grid-cols-12 gap-4 p-4 border-b text-sm font-medium text-muted-foreground">
            <div className="col-span-6">Name</div>
            <div className="col-span-2">Owner</div>
            <div className="col-span-2">Last Modified</div>
            <div className="col-span-1">File Size</div>
            <div className="col-span-1"></div>
          </div>
          
          <div className="divide-y">
            {files.map((file) => (
              <div
                key={file.id}
                className="grid grid-cols-12 gap-4 p-4 items-center hover:bg-muted/50 transition-colors group"
              >
                <div className="col-span-6 flex items-center gap-3">
                  {file.type === FileType.FOLDER ? (
                    <Folder className="h-5 w-5 text-muted-foreground fill-muted-foreground/20" />
                  ) : (
                    <FileText className="h-5 w-5 text-muted-foreground" />
                  )}
                  <span className="text-sm font-medium text-foreground/70 group-hover:text-foreground">
                    {file.name}
                  </span>
                  {file.shared && (
                    <span className="ml-2 px-2 py-0.5 text-xs bg-blue-100 text-blue-700 rounded">
                      Shared
                    </span>
                  )}
                </div>
                <div className="col-span-2 text-sm text-muted-foreground">
                  {file.ownerName}
                </div>
                <div className="col-span-2 text-sm text-muted-foreground">
                  {formatDate(file.updatedAt)}
                </div>
                <div className="col-span-1 text-sm text-muted-foreground">
                  {formatSize(file.size)}
                </div>
                <div className="col-span-1 flex justify-end gap-1">
                  {viewMode === "my-drive" && (
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={() => setShareDialogFile(file)}
                      title="Share"
                    >
                      <Share2 className="h-4 w-4 text-muted-foreground" />
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
                    onClick={() => handleDownload(file)}
                    title="Download"
                  >
                    <Download className="h-4 w-4 text-muted-foreground" />
                  </Button>
                  {file.canEdit && (
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={() => handleDelete(file.id)}
                      title="Delete"
                    >
                      <Trash2 className="h-4 w-4 text-muted-foreground" />
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Share Dialog */}
      {shareDialogFile && (
        <ShareDialog
          fileId={shareDialogFile.id}
          fileName={shareDialogFile.name}
          onClose={() => setShareDialogFile(null)}
        />
      )}
    </div>
  );
}
