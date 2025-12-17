"use client";

import React, { createContext, useContext, useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/lib/api/auth";
import type { User, LoginRequest, RegisterRequest } from "@/lib/types";

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    // Check if user is already logged in
    const currentUser = authService.getCurrentUser();
    if (currentUser) {
      setUser(currentUser);
    }
    setIsLoading(false);
  }, []);

  const login = async (data: LoginRequest) => {
    const response = await authService.login(data);
    setUser({
      id: response.userId,
      email: response.email,
      fullName: response.fullName,
      storageUsed: response.storageUsed,
      storageQuota: response.storageQuota,
    });
    router.push("/dashboard");
  };

  const register = async (data: RegisterRequest) => {
    const response = await authService.register(data);
    setUser({
      id: response.userId,
      email: response.email,
      fullName: response.fullName,
      storageUsed: response.storageUsed,
      storageQuota: response.storageQuota,
    });
    router.push("/dashboard");
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    router.push("/login");
  };

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
