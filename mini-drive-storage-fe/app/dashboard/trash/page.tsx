"use client";

import { useState, useEffect } from "react";
import { FileText, Folder, RotateCcw, Trash2, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
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

export default function TrashPage() {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [permanentDeleteFileId, setPermanentDeleteFileId] = useState<string | null>(null);
  const [restoreFileId, setRestoreFileId] = useState<string | null>(null);

  const loadTrash = async () => {
    try {
      setIsLoading(true);
      // Note: Backend should provide a deleted=true filter, but for now we'll use this approach
      // This is a mock implementation - you may need to add a backend endpoint for trash
      const data = await fileService.listFiles({});
      // Filter deleted items on frontend (ideally backend should provide this)
      const deletedItems = data.filter(item => item.deleted);
      setFiles(deletedItems);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to load trash");
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadTrash();
  }, []);

  const handleRestore = async (fileId: string) => {
    try {
      // Note: This assumes there's a restore endpoint
      // You may need to add this to the backend API
      await fileService.restoreFile(fileId);
      await loadTrash();
      toast.success("Item restored successfully");
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to restore item");
      }
    } finally {
      setRestoreFileId(null);
    }
  };

  const confirmPermanentDelete = async () => {
    if (!permanentDeleteFileId) return;

    try {
      // This should call a permanent delete endpoint
      await fileService.permanentDelete(permanentDeleteFileId);
      await loadTrash();
      toast.success("Item permanently deleted");
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to delete permanently");
      }
    } finally {
      setPermanentDeleteFileId(null);
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

  const getDaysInTrash = (deletedAt: string) => {
    const deleted = new Date(deletedAt);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - deleted.getTime());
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Trash</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Items in trash will be automatically deleted after 30 days
          </p>
        </div>
      </div>

      {/* Warning Banner */}
      <div className="flex items-center gap-3 p-4 bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-900 rounded-lg">
        <AlertCircle className="h-5 w-5 text-amber-600 dark:text-amber-400 flex-shrink-0" />
        <div className="text-sm text-amber-900 dark:text-amber-200">
          Items in trash are deleted forever after 30 days. You can restore them before that time.
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="text-muted-foreground">Loading...</div>
        </div>
      ) : files.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <Trash2 className="h-16 w-16 text-muted-foreground/50 mb-4" />
          <h3 className="text-lg font-medium mb-2">Trash is empty</h3>
          <p className="text-sm text-muted-foreground">
            Items you delete will appear here
          </p>
        </div>
      ) : (
        <div className="rounded-lg border bg-card shadow-sm">
          <div className="grid grid-cols-12 gap-4 p-4 border-b text-sm font-medium text-muted-foreground">
            <div className="col-span-5">Name</div>
            <div className="col-span-2">Deleted</div>
            <div className="col-span-2">Days in Trash</div>
            <div className="col-span-1">Size</div>
            <div className="col-span-2"></div>
          </div>

          <div className="divide-y">
            {files.map((file) => {
              const daysInTrash = getDaysInTrash(file.deletedAt || file.updatedAt);
              const isExpiringSoon = daysInTrash >= 25;
              
              return (
                <div
                  key={file.id}
                  className="grid grid-cols-12 gap-4 p-4 items-center hover:bg-muted/50 transition-colors group"
                >
                  <div className="col-span-5 flex items-center gap-3">
                    {file.type === FileType.FOLDER ? (
                      <Folder className="h-5 w-5 text-muted-foreground fill-muted-foreground/20" />
                    ) : (
                      <FileText className="h-5 w-5 text-muted-foreground" />
                    )}
                    <span className="text-sm font-medium text-foreground/70 group-hover:text-foreground">
                      {file.name}
                    </span>
                  </div>
                  <div className="col-span-2 text-sm text-muted-foreground">
                    {formatDate(file.deletedAt || file.updatedAt)}
                  </div>
                  <div className="col-span-2">
                    <div className="flex items-center gap-2">
                      <span className="text-sm text-muted-foreground">{daysInTrash} days</span>
                      {isExpiringSoon && (
                        <Badge variant="destructive" className="text-xs">
                          Expiring soon
                        </Badge>
                      )}
                    </div>
                  </div>
                  <div className="col-span-1 text-sm text-muted-foreground">
                    {formatSize(file.size)}
                  </div>
                  <div className="col-span-2 flex justify-end gap-1">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="gap-1 opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={() => setRestoreFileId(file.id)}
                    >
                      <RotateCcw className="h-4 w-4" />
                      Restore
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="gap-1 text-destructive hover:text-destructive hover:bg-destructive/10 opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={() => setPermanentDeleteFileId(file.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                      Delete forever
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Restore Confirmation Dialog */}
      <AlertDialog open={!!restoreFileId} onOpenChange={(open) => !open && setRestoreFileId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Restore item?</AlertDialogTitle>
            <AlertDialogDescription>
              This item will be restored to its original location.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={() => restoreFileId && handleRestore(restoreFileId)}>
              Restore
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Permanent Delete Confirmation Dialog */}
      <AlertDialog open={!!permanentDeleteFileId} onOpenChange={(open) => !open && setPermanentDeleteFileId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete permanently?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the item and remove it from our servers.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={confirmPermanentDelete} className="bg-destructive hover:bg-destructive/90">
              Delete Forever
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
