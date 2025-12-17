import { api } from "./client";
import type { AuthResponse, LoginRequest, RegisterRequest } from "@/lib/types";

export const authService = {
  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>("/api/v1/auth/register", data);
    if (response.token) {
      localStorage.setItem("token", response.token);
      localStorage.setItem("user", JSON.stringify({
        id: response.userId,
        email: response.email,
        fullName: response.fullName,
        storageUsed: response.storageUsed,
        storageQuota: response.storageQuota,
      }));
    }
    return response;
  },

  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>("/api/v1/auth/login", data);
    if (response.token) {
      localStorage.setItem("token", response.token);
      localStorage.setItem("user", JSON.stringify({
        id: response.userId,
        email: response.email,
        fullName: response.fullName,
        storageUsed: response.storageUsed,
        storageQuota: response.storageQuota,
      }));
    }
    return response;
  },

  logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  },

  getToken(): string | null {
    return localStorage.getItem("token");
  },

  isAuthenticated(): boolean {
    return !!this.getToken();
  },

  getCurrentUser() {
    const userStr = localStorage.getItem("user");
    return userStr ? JSON.parse(userStr) : null;
  },
};
