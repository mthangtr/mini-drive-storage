"use client";

import { useState, useEffect } from "react";
import { X, Mail, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
  const [error, setError] = useState("");

  useEffect(() => {
    loadShares();
  }, [fileId]);

  const loadShares = async () => {
    try {
      setIsLoadingShares(true);
      const data = await fileService.getFileShares(fileId);
      setShares(data);
    } catch (err) {
      if (err instanceof ApiError) {
        console.error("Failed to load shares:", err.message);
      }
    } finally {
      setIsLoadingShares(false);
    }
  };

  const handleShare = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!email.trim()) {
      setError("Email is required");
      return;
    }

    try {
      setIsLoading(true);
      setError("");
      await fileService.shareFile(fileId, { email: email.trim(), permission });
      setEmail("");
      await loadShares();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to share file");
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleRemoveShare = async (shareEmail: string) => {
    if (!confirm(`Remove access for ${shareEmail}?`)) return;

    try {
      await fileService.removeShare(fileId, shareEmail);
      await loadShares();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to remove share");
      }
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md max-h-[80vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold">Share "{fileName}"</h2>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-100 rounded transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
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

            {error && (
              <div className="text-sm text-red-600 bg-red-50 p-2 rounded">
                {error}
              </div>
            )}

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
                      <div className="text-xs text-gray-500">
                        {share.permission === PermissionLevel.VIEW ? "View only" : "Can edit"}
                      </div>
                    </div>
                    <button
                      onClick={() => handleRemoveShare(share.sharedWithEmail)}
                      className="p-1 hover:bg-red-50 rounded text-red-600 transition"
                      title="Remove access"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
