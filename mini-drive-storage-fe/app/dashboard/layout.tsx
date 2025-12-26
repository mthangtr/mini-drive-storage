"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { HardDrive, Share2, LogOut, BarChart3 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { analyticsService } from "@/lib/api/analytics";
import type { UsageStatsResponse } from "@/lib/types";
import { cn } from "@/lib/utils";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, logout, isLoading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [stats, setStats] = useState<UsageStatsResponse | null>(null);

  useEffect(() => {
    if (!isLoading && !user) {
      router.push("/login");
    }
  }, [user, isLoading, router]);

  useEffect(() => {
    const loadStats = async () => {
      try {
        const data = await analyticsService.getUserUsage();
        setStats(data);
      } catch (error) {
        console.error("Failed to load usage stats:", error);
      }
    };
    if (user) {
      loadStats();
    }
  }, [user]);

  if (isLoading || !user) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-muted-foreground">Loading...</div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-background">
      {/* Sidebar */}
      <aside className="w-64 border-r border-border bg-muted/50 hidden md:flex flex-col">
        <div className="p-6 flex items-center gap-2 font-bold text-xl tracking-tight">
          <div className="h-8 w-8 bg-primary rounded-lg flex items-center justify-center text-primary-foreground">
            <HardDrive className="h-5 w-5" />
          </div>
          MiniDrive
        </div>

        <nav className="flex-1 px-4 py-4 space-y-1">
          <Link
            href="/dashboard"
            className={cn(
              "flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-md transition-colors",
              pathname === "/dashboard"
                ? "bg-accent text-accent-foreground"
                : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
            )}
          >
            <HardDrive className="h-4 w-4" />
            My Drive
          </Link>
          <Link
            href="/dashboard/shared"
            className={cn(
              "flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-md transition-colors",
              pathname === "/dashboard/shared"
                ? "bg-accent text-accent-foreground"
                : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
            )}
          >
            <Share2 className="h-4 w-4" />
            Shared with me
          </Link>
        </nav>

        <div className="p-4 border-t border-border space-y-3">
          {/* Usage Stats */}
          {stats && (
            <div className="px-3 py-2 bg-muted/50 rounded-lg space-y-2">
              <div className="flex items-center gap-2 text-xs font-medium text-foreground">
                <BarChart3 className="h-3.5 w-3.5" />
                Storage Usage
              </div>
              <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                <div 
                  className="h-full bg-primary transition-all" 
                  style={{ width: `${Math.min(stats.usagePercentage, 100)}%` }}
                />
              </div>
              <div className="text-xs text-muted-foreground">
                {(stats.storageUsed / (1024 * 1024 * 1024)).toFixed(2)} GB of{" "}
                {(stats.storageQuota / (1024 * 1024 * 1024)).toFixed(0)} GB used
              </div>
              <div className="grid grid-cols-2 gap-2 pt-2 border-t border-border/50 text-xs">
                <div>
                  <div className="text-muted-foreground">Files</div>
                  <div className="font-medium">{stats.totalFiles}</div>
                </div>
                <div>
                  <div className="text-muted-foreground">Folders</div>
                  <div className="font-medium">{stats.totalFolders}</div>
                </div>
                <div>
                  <div className="text-muted-foreground">Shared with me</div>
                  <div className="font-medium">{stats.totalSharedWithMe}</div>
                </div>
                <div>
                  <div className="text-muted-foreground">Shared by me</div>
                  <div className="font-medium">{stats.totalSharedByMe}</div>
                </div>
              </div>
            </div>
          )}
          
          <div className="px-3 pt-2 border-t">
            <div className="text-xs font-medium text-foreground">{user.fullName}</div>
            <div className="text-xs text-muted-foreground">{user.email}</div>
          </div>
          <Button
            variant="ghost"
            size="sm"
            className="w-full justify-start gap-2"
            onClick={logout}
          >
            <LogOut className="h-4 w-4" />
            Sign out
          </Button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Page Content */}
        <main className="flex-1 overflow-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
