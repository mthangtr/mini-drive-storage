import type { UsageStatsResponse } from "@/lib/types";
import { api } from "./client";

export const analyticsService = {
  async getUserUsage(): Promise<UsageStatsResponse> {
    return api.get<UsageStatsResponse>("/api/v1/analytics/usage");
  },
};
