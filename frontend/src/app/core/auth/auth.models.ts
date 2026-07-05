export type Role = 'STUDENT' | 'TUTOR' | 'ADMIN';

export interface RegisterRequest {
  email: string;
  password: string;
  role: Role;
  fullName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  accessTokenExpiresInSeconds: number;
}

export interface DecodedAccessToken {
  sub: string;
  role: Role;
  exp: number;
}
