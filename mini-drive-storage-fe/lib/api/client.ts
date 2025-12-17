import type { ApiResponse, ErrorResponse } from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public status: number,
    public error: string,
    message: string,
    public validationErrors?: Record<string, string>
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;

  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  // Don't set Content-Type for FormData (browser will set it with boundary)
  if (!(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const errorData: ErrorResponse = await response.json().catch(() => ({
      status: response.status,
      error: response.statusText,
      message: "An unexpected error occurred",
      path: endpoint,
      timestamp: Date.now(),
    }));

    throw new ApiError(
      errorData.status,
      errorData.error,
      errorData.message,
      errorData.validationErrors
    );
  }

  // For blob responses (file downloads)
  if (options.headers && (options.headers as Record<string, string>).Accept === "application/octet-stream") {
    return response.blob() as Promise<T>;
  }

  const data: ApiResponse<T> = await response.json();
  return data.data;
}

export const api = {
  get: <T>(endpoint: string, options?: RequestInit) =>
    apiRequest<T>(endpoint, { ...options, method: "GET" }),

  post: <T>(endpoint: string, body?: unknown, options?: RequestInit) =>
    apiRequest<T>(endpoint, {
      ...options,
      method: "POST",
      body: body instanceof FormData ? body : JSON.stringify(body),
    }),

  put: <T>(endpoint: string, body?: unknown, options?: RequestInit) =>
    apiRequest<T>(endpoint, {
      ...options,
      method: "PUT",
      body: JSON.stringify(body),
    }),

  delete: <T>(endpoint: string, options?: RequestInit) =>
    apiRequest<T>(endpoint, { ...options, method: "DELETE" }),
};
