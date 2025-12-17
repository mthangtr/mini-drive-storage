"use client";

import { useState, useEffect } from "react";
import { Mail, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { toast } from "sonner";
import { fileService } from "@/lib/api/files";
import { PermissionLevel, ShareFileResponse } from "@/lib/types";
import { ApiError } from "@/lib/api/client";

interface ShareDialogProps {
  fileId: string;
  fileName: string;
  onClose: () => void;
}

export default function ShareDialog({ fileId, fileName, onClose }: ShareDialogProps) {
  const [email, setEmail] = useState("");
  const [permission, setPermission] = useState<PermissionLevel>(PermissionLevel.VIEW);
  const [shares, setShares] = useState<ShareFileResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingShares, setIsLoadingShares] = useState(true);
  const [removeShareEmail, setRemoveShareEmail] = useState<string | null>(null);

  useEffect(() => {
    loadShares();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fileId]);

  const loadShares = async () => {
    try {
      setIsLoadingShares(true);
      const data = await fileService.getFileShares(fileId);
      setShares(data);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error("Failed to load shares: " + err.message);
      }
    } finally {
      setIsLoadingShares(false);
    }
  };

  const handleShare = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!email.trim()) {
      toast.error("Email is required");
      return;
    }

    try {
      setIsLoading(true);
      await fileService.shareFile(fileId, { email: email.trim(), permission });
      setEmail("");
      await loadShares();
      toast.success(`File shared with ${email.trim()}`);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to share file");
      }
    } finally {
      setIsLoading(false);
    }
  };

  const confirmRemoveShare = async () => {
    if (!removeShareEmail) return;

    try {
      await fileService.removeShare(fileId, removeShareEmail);
      await loadShares();
      toast.success(`Access removed for ${removeShareEmail}`);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("Failed to remove share");
      }
    } finally {
      setRemoveShareEmail(null);
    }
  };

  return (
    <Dialog open={true} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Share &ldquo;{fileName}&rdquo;</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          {/* Share Form */}
          <form onSubmit={handleShare} className="space-y-3">
            <div>
              <Label htmlFor="email">Share with</Label>
              <div className="flex gap-2 mt-1">
                <div className="relative flex-1">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <Input
                    id="email"
                    type="email"
                    placeholder="Enter email address"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="pl-9"
                  />
                </div>
              </div>
            </div>

            <div>
              <Label htmlFor="permission">Permission</Label>
              <Select
                value={permission}
                onValueChange={(value) => setPermission(value as PermissionLevel)}
              >
                <SelectTrigger id="permission" className="mt-1">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={PermissionLevel.VIEW}>View only</SelectItem>
                  <SelectItem value={PermissionLevel.EDIT}>Can edit</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Button type="submit" disabled={isLoading} className="w-full">
              {isLoading ? "Sharing..." : "Share"}
            </Button>
          </form>

          {/* Existing Shares */}
          <div className="pt-2">
            <h3 className="text-sm font-medium text-gray-700 mb-2">
              People with access
            </h3>

            {isLoadingShares ? (
              <div className="text-sm text-gray-500 py-4 text-center">
                Loading...
              </div>
            ) : shares.length === 0 ? (
              <div className="text-sm text-gray-500 py-4 text-center">
                Not shared with anyone yet
              </div>
            ) : (
              <div className="space-y-2">
                {shares.map((share) => (
                  <div
                    key={share.id}
                    className="flex items-center justify-between p-2 rounded hover:bg-gray-50"
                  >
                    <div className="flex-1">
                      <div className="text-sm font-medium">{share.sharedWithEmail}</div>
                      <Badge variant="outline" className="mt-1">
                        {share.permission === PermissionLevel.VIEW ? "View only" : "Can edit"}
                      </Badge>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setRemoveShareEmail(share.sharedWithEmail)}
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                      title="Remove access"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </DialogContent>

      {/* Remove Share Confirmation */}
      <AlertDialog open={!!removeShareEmail} onOpenChange={(open) => !open && setRemoveShareEmail(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Remove access?</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to remove access for {removeShareEmail}? They will no longer be able to view or edit this file.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={confirmRemoveShare}>Remove</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Dialog>
  );
}
