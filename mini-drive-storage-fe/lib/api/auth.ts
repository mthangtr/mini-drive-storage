import { api } from "./client";
import type { AuthResponse, LoginRequest, RegisterRequest } from "@/lib/types";

function setCookie(name: string, value: string, days: number = 7) {
  const expires = new Date();
  expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
  document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/;SameSite=Lax`;
}

export function getCookie(name: string): string | null {
  const nameEQ = name + "=";
  const ca = document.cookie.split(';');
  for (let i = 0; i < ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) === ' ') c = c.substring(1, c.length);
    if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
  }
  return null;
}

function deleteCookie(name: string) {
  document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;`;
}

export const authService = {
  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>("/api/v1/auth/register", data);
    if (response.token) {
      setCookie("token", response.token, 7);
      // Store user info for UI display
      if (typeof window !== "undefined") {
        localStorage.setItem("user", JSON.stringify({
          id: response.userId,
          email: response.email,
          fullName: response.fullName,
          storageUsed: response.storageUsed,
          storageQuota: response.storageQuota,
        }));
      }
    }
    return response;
  },

  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>("/api/v1/auth/login", data);
    if (response.token) {
      setCookie("token", response.token, 7);
      // Store user info for UI display
      if (typeof window !== "undefined") {
        localStorage.setItem("user", JSON.stringify({
          id: response.userId,
          email: response.email,
          fullName: response.fullName,
          storageUsed: response.storageUsed,
          storageQuota: response.storageQuota,
        }));
      }
    }
    return response;
  },

  logout() {
    deleteCookie("token");
    if (typeof window !== "undefined") {
      localStorage.removeItem("user");
    }
  },

  getToken(): string | null {
    return getCookie("token");
  },

  isAuthenticated(): boolean {
    return !!this.getToken();
  },

  getCurrentUser() {
    const userStr = localStorage.getItem("user");
    return userStr ? JSON.parse(userStr) : null;
  },
};
