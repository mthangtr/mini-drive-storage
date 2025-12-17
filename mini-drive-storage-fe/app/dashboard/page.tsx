import { FileText, Folder, MoreVertical } from "lucide-react";
import { Button } from "@/components/ui/button";

const mockFiles = [
  { id: 1, name: "Design Assets", type: "folder", owner: "me", modified: "Dec 17, 2025", size: "-" },
  { id: 2, name: "Project Specs.pdf", type: "file", owner: "me", modified: "Dec 16, 2025", size: "2.4 MB" },
  { id: 3, name: "Q4 Report.docx", type: "file", owner: "me", modified: "Dec 15, 2025", size: "1.1 MB" },
  { id: 4, name: "Images", type: "folder", owner: "me", modified: "Dec 14, 2025", size: "-" },
  { id: 5, name: "Budget.xlsx", type: "file", owner: "me", modified: "Dec 10, 2025", size: "45 KB" },
];

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">My Drive</h1>
        <div className="flex items-center gap-2">
            {/* View toggle or sort options could go here */}
        </div>
      </div>

      <div className="rounded-lg border bg-card shadow-sm">
        <div className="grid grid-cols-12 gap-4 p-4 border-b text-sm font-medium text-muted-foreground">
          <div className="col-span-6">Name</div>
          <div className="col-span-2">Owner</div>
          <div className="col-span-2">Last Modified</div>
          <div className="col-span-1">File Size</div>
          <div className="col-span-1"></div>
        </div>
        
        <div className="divide-y">
          {mockFiles.map((file) => (
            <div
              key={file.id}
              className="grid grid-cols-12 gap-4 p-4 items-center hover:bg-muted/50 transition-colors cursor-pointer group"
            >
              <div className="col-span-6 flex items-center gap-3">
                {file.type === "folder" ? (
                  <Folder className="h-5 w-5 text-muted-foreground fill-muted-foreground/20" />
                ) : (
                  <FileText className="h-5 w-5 text-muted-foreground" />
                )}
                <span className="text-sm font-medium text-foreground/70 group-hover:text-foreground">
                  {file.name}
                </span>
              </div>
              <div className="col-span-2 text-sm text-muted-foreground">
                {file.owner}
              </div>
              <div className="col-span-2 text-sm text-muted-foreground">
                {file.modified}
              </div>
              <div className="col-span-1 text-sm text-muted-foreground">
                {file.size}
              </div>
              <div className="col-span-1 flex justify-end">
                <Button variant="ghost" size="icon" className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity">
                  <MoreVertical className="h-4 w-4 text-muted-foreground" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
