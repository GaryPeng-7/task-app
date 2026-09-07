import { api } from "./client";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
}

export interface User {
  id: string;
  email: string;
}

export function login(req: LoginRequest) {
  return api<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function register(req: LoginRequest) {
  return api<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function getMe() {
  return api<User>("/api/me");
}
