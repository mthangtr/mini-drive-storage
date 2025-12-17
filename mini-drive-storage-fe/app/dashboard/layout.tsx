"use client";

import Link from "next/link";
import { HardDrive, Share2, Trash2, Search, Plus, User, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/lib/auth-context";
import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, logout, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !user) {
      router.push("/login");
    }
  }, [user, isLoading, router]);

  if (isLoading || !user) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-muted-foreground">Loading...</div>
      </div>
    );
  }

  const storagePercentage = (user.storageUsed / user.storageQuota) * 100;
  const storageUsedGB = (user.storageUsed / (1024 * 1024 * 1024)).toFixed(2);
  const storageQuotaGB = (user.storageQuota / (1024 * 1024 * 1024)).toFixed(0);

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
        
        <div className="px-4 py-2">
          <Button variant="outline" className="w-full justify-start gap-2 shadow-sm">
            <Plus className="h-4 w-4" />
            New
          </Button>
        </div>

        <nav className="flex-1 px-4 py-4 space-y-1">
          <Link
            href="/dashboard"
            className="flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-md bg-accent text-accent-foreground"
          >
            <HardDrive className="h-4 w-4" />
            My Drive
          </Link>
          <Link
            href="/dashboard/shared"
            className="flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground transition-colors"
          >
            <Share2 className="h-4 w-4" />
            Shared with me
          </Link>
          <Link
            href="/dashboard/trash"
            className="flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground transition-colors"
          >
            <Trash2 className="h-4 w-4" />
            Trash
          </Link>
        </nav>

        <div className="p-4 border-t border-border space-y-2">
          <div className="flex items-center gap-3 px-3 py-2 text-sm font-medium text-muted-foreground">
            <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
              <div 
                className="h-full bg-primary transition-all" 
                style={{ width: `${Math.min(storagePercentage, 100)}%` }}
              />
            </div>
          </div>
          <div className="px-3 text-xs text-muted-foreground">
            {storageUsedGB} GB of {storageQuotaGB} GB used
          </div>
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
        {/* Header */}
        <header className="h-16 border-b border-border flex items-center justify-between px-6 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 sticky top-0 z-10">
          <div className="flex-1 max-w-2xl">
            <div className="relative">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                placeholder="Search in Drive"
                className="w-full pl-9 bg-muted"
              />
            </div>
          </div>
          <div className="flex items-center gap-4 ml-4">
            <Button variant="ghost" size="icon" className="rounded-full">
              <User className="h-5 w-5 text-muted-foreground" />
            </Button>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
