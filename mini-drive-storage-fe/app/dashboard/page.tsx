"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { FileText, Folder, Upload, FolderPlus, Download, Trash2, Share2, Search, Filter, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { toast } from "sonner";
import { fileService } from "@/lib/api/files";
import { FileItem, FileType } from "@/lib/types";
import { ApiError } from "@/lib/api/client";
import ShareDialog from "@/components/share-dialog";

export default function DashboardPage() {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [shareDialogFile, setShareDialogFile] = useState<FileItem | null>(null);
  const [deleteFileId, setDeleteFileId] = useState<string | null>(null);
  const [currentFolderId, setCurrentFolderId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterType, setFilterType] = useState<string | null>(null);
  const [filterSizeMin, setFilterSizeMin] = useState<number | null>(null);
  const [filterSizeMax, setFilterSizeMax] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadFiles = async () => {
    try {
      setIsLoading(true);
      const params: {
        q?: string;
        parentId?: string;
        type?: string;
        fromSize?: number;
        toSize?: number;
      } = {};
      
      if (searchQuery) {
        params.q = searchQuery;
      } else if (currentFolderId) {
        params.parentId = currentFolderId;
      }
      if (filterType) params.type = filterType;
      if (filterSizeMin !== null) params.fromSize = filterSizeMin;
      if (filterSizeMax !== null) params.toSize = filterSizeMax;
      
      const data = await fileService.listFiles(params);
      setFiles(data);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to load files");
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadFiles();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentFolderId, searchQuery, filterType, filterSizeMin, filterSizeMax]);

  const navigateToFolder = useCallback((folder: FileItem) => {
    if (folder.type !== FileType.FOLDER) return;
    setCurrentFolderId(folder.id);
  }, []);

  const clearFilters = () => {
    setSearchQuery("");
    setFilterType(null);
    setFilterSizeMin(null);
    setFilterSizeMax(null);
  };

  const hasActiveFilters = searchQuery || filterType || filterSizeMin !== null || filterSizeMax !== null;

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = e.target.files;
    if (!selectedFiles || selectedFiles.length === 0) return;

    try {
      setIsUploading(true);
      await fileService.uploadFiles(Array.from(selectedFiles), currentFolderId || undefined);
      await loadFiles();
      toast.success("Files uploaded successfully");
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to upload files");
      }
    } finally {
      setIsUploading(false);
    }
  };

  const handleCreateFolder = async () => {
    const folderName = prompt("Enter folder name:");
    if (!folderName) return;

    try {
      await fileService.createFolder({ 
        name: folderName, 
        parentId: currentFolderId || undefined 
      });
      await loadFiles();
      toast.success("Folder created successfully");
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to create folder");
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
              toast.error("Download failed");
            }
            attempts++;
          } catch {
            clearInterval(pollInterval);
            toast.error("Download failed");
          }
        }, 2000);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to download");
      }
    }
  };

  const confirmDelete = async () => {
    if (!deleteFileId) return;

    try {
      await fileService.deleteFile(deleteFileId);
      await loadFiles();
      toast.success("Item deleted successfully");
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to delete");
      }
    } finally {
      setDeleteFileId(null);
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
      <div className="space-y-4 mb-6">
            {/* Search and Actions Row */}
            <div className="flex items-center gap-3">
              {/* Search Bar */}
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search files and folders..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10 pr-10"
                />
                {searchQuery && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-7"
                    onClick={() => setSearchQuery("")}
                  >
                    <X className="h-4 w-4" />
                  </Button>
                )}
              </div>

              {/* Filter Dropdown */}
              <DropdownMenu>
                <DropdownMenuTrigger className="inline-flex items-center justify-center gap-2 rounded-md border border-input bg-background px-3 h-9 text-sm font-medium shadow-sm hover:bg-accent hover:text-accent-foreground transition-colors">
                  <Filter className="h-4 w-4" />
                  Filters
                  {hasActiveFilters && (
                    <Badge variant="secondary" className="ml-1 h-5 px-1.5 text-xs">
                      {[filterType, filterSizeMin !== null, filterSizeMax !== null].filter(Boolean).length}
                    </Badge>
                  )}
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-64">
                  <DropdownMenuLabel>Filter by</DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  
                  <div className="p-2 space-y-3">
                    {/* Type Filter */}
                    <div>
                      <label className="text-xs font-medium text-muted-foreground mb-2 block">Type</label>
                      <div className="grid grid-cols-2 gap-2">
                        <Button
                          variant={filterType === "FILE" ? "default" : "outline"}
                          size="sm"
                          className="w-full text-xs"
                          onClick={() => setFilterType(filterType === "FILE" ? null : "FILE")}
                        >
                          Files
                        </Button>
                        <Button
                          variant={filterType === "FOLDER" ? "default" : "outline"}
                          size="sm"
                          className="w-full text-xs"
                          onClick={() => setFilterType(filterType === "FOLDER" ? null : "FOLDER")}
                        >
                          Folders
                        </Button>
                      </div>
                    </div>

                    {/* Size Filter */}
                    <div>
                      <label className="text-xs font-medium text-muted-foreground mb-2 block">Size (MB)</label>
                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <Input
                            type="number"
                            placeholder="Min"
                            value={filterSizeMin !== null ? filterSizeMin / (1024 * 1024) : ""}
                            onChange={(e) => setFilterSizeMin(e.target.value ? parseFloat(e.target.value) * 1024 * 1024 : null)}
                            className="h-8 text-xs"
                          />
                        </div>
                        <div>
                          <Input
                            type="number"
                            placeholder="Max"
                            value={filterSizeMax !== null ? filterSizeMax / (1024 * 1024) : ""}
                            onChange={(e) => setFilterSizeMax(e.target.value ? parseFloat(e.target.value) * 1024 * 1024 : null)}
                            className="h-8 text-xs"
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  {hasActiveFilters && (
                    <>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem onClick={clearFilters} className="text-xs">
                        <X className="h-3 w-3 mr-2" />
                        Clear all filters
                      </DropdownMenuItem>
                    </>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>

              {/* Upload/Create Actions */}
              {!searchQuery && (
                <>
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
                </>
              )}
            </div>

            {/* Active Filters Display */}
            {hasActiveFilters && (
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-xs text-muted-foreground">Active filters:</span>
                {searchQuery && (
                  <Badge variant="secondary" className="gap-1">
                    Search: {searchQuery}
                    <button onClick={() => setSearchQuery("")} className="ml-1 hover:text-foreground">
                      <X className="h-3 w-3" />
                    </button>
                  </Badge>
                )}
                {filterType && (
                  <Badge variant="secondary" className="gap-1">
                    Type: {filterType}
                    <button onClick={() => setFilterType(null)} className="ml-1 hover:text-foreground">
                      <X className="h-3 w-3" />
                    </button>
                  </Badge>
                )}
                {(filterSizeMin !== null || filterSizeMax !== null) && (
                  <Badge variant="secondary" className="gap-1">
                    Size: {filterSizeMin !== null ? `≥${(filterSizeMin / (1024 * 1024)).toFixed(1)}MB` : ""} 
                    {filterSizeMin !== null && filterSizeMax !== null ? " & " : ""}
                    {filterSizeMax !== null ? `≤${(filterSizeMax / (1024 * 1024)).toFixed(1)}MB` : ""}
                    <button onClick={() => { setFilterSizeMin(null); setFilterSizeMax(null); }} className="ml-1 hover:text-foreground">
                      <X className="h-3 w-3" />
                    </button>
                  </Badge>
                )}
              </div>
            )}
          </div>

          <div className="flex items-center justify-between mb-6">
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              {searchQuery ? `Search results for "${searchQuery}"` : "My Drive"}
            </h1>
          </div>


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
                className="grid grid-cols-12 gap-4 p-4 items-center hover:bg-muted/50 transition-colors group cursor-pointer"
                onClick={() => file.type === FileType.FOLDER && navigateToFolder(file)}
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
                    <Badge variant="secondary" className="ml-2">
                      Shared
                    </Badge>
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
                <div className="col-span-1 flex justify-end gap-1" onClick={(e) => e.stopPropagation()}>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
                    onClick={() => setShareDialogFile(file)}
                    title="Share"
                  >
                    <Share2 className="h-4 w-4 text-muted-foreground" />
                  </Button>
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
                      onClick={() => setDeleteFileId(file.id)}
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

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={!!deleteFileId} onOpenChange={(open) => !open && setDeleteFileId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the item.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={confirmDelete}>Delete</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
