"use client";

import { useState, useEffect, useMemo } from "react";
import { FileText, Folder, Download, Share2, Search, Filter, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
import { fileService } from "@/lib/api/files";
import { FileItem, FileType } from "@/lib/types";
import { ApiError } from "@/lib/api/client";

export default function SharedWithMePage() {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterType, setFilterType] = useState<string | null>(null);

  const loadFiles = async () => {
    try {
      setIsLoading(true);
      const data = await fileService.getSharedWithMe();
      setFiles(data);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to load shared files");
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadFiles();
  }, []);

  // Client-side filtering
  const filteredFiles = useMemo(() => {
    let result = files;

    // Search filter
    if (searchQuery) {
      result = result.filter(file => 
        file.name.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    // Type filter
    if (filterType) {
      result = result.filter(file => file.type === filterType);
    }

    return result;
  }, [files, searchQuery, filterType]);

  const clearFilters = () => {
    setSearchQuery("");
    setFilterType(null);
  };

  const hasActiveFilters = searchQuery || filterType;

  const handleDownload = async (file: FileItem) => {
    try {
      if (file.type === FileType.FILE) {
        await fileService.downloadFile(file.id, file.name);
      } else {
        const response = await fileService.initiateDownloadFolder(file.id);
        const requestId = response.requestId;
        
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
        toast.error("Download failed");
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
      <div className="space-y-4 mb-6">
        {/* Search and Filter Row */}
        <div className="flex items-center gap-3">
          {/* Search Bar */}
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search shared files..."
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
            <DropdownMenuTrigger>
              <Button variant="outline" size="sm" className="gap-2">
                <Filter className="h-4 w-4" />
                Filters
                {hasActiveFilters && (
                  <Badge variant="secondary" className="ml-1 h-5 px-1.5 text-xs">
                    {[filterType].filter(Boolean).length}
                  </Badge>
                )}
              </Button>
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
              </div>

              {hasActiveFilters && (
                <>
                  <DropdownMenuSeparator />
                  <div className="p-2">
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      className="w-full text-xs justify-start" 
                      onClick={clearFilters}
                    >
                      <X className="h-3 w-3 mr-2" />
                      Clear all filters
                    </Button>
                  </div>
                </>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
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
          </div>
        )}
      </div>

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          {searchQuery ? `Search results for "${searchQuery}"` : "Shared with me"}
        </h1>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="text-muted-foreground">Loading...</div>
        </div>
      ) : filteredFiles.length === 0 ? (
        hasActiveFilters ? (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <Search className="h-16 w-16 text-muted-foreground/50 mb-4" />
            <h3 className="text-lg font-medium mb-2">No results found</h3>
            <p className="text-sm text-muted-foreground mb-4">
              Try adjusting your search or filters
            </p>
            <Button variant="outline" onClick={clearFilters}>
              Clear filters
            </Button>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <Share2 className="h-16 w-16 text-muted-foreground/50 mb-4" />
            <h3 className="text-lg font-medium mb-2">No shared files</h3>
            <p className="text-sm text-muted-foreground">
              Files shared with you will appear here
            </p>
          </div>
        )
      ) : (
        <div className="rounded-lg border bg-card shadow-sm">
          <div className="grid grid-cols-12 gap-4 p-4 border-b text-sm font-medium text-muted-foreground">
            <div className="col-span-6">Name</div>
            <div className="col-span-2">Owner</div>
            <div className="col-span-2">Shared Date</div>
            <div className="col-span-1">Size</div>
            <div className="col-span-1"></div>
          </div>

          <div className="divide-y">
            {filteredFiles.map((file) => (
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
                  <Badge variant="secondary" className="ml-2">
                    {file.permissionLevel === "EDIT" ? "Can edit" : "View only"}
                  </Badge>
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
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
                    onClick={() => handleDownload(file)}
                    title="Download"
                  >
                    <Download className="h-4 w-4 text-muted-foreground" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
