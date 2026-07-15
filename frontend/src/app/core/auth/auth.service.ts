import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, DecodedAccessToken, LoginRequest, RegisterRequest, Role } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly accessToken = signal<string | null>(null);
  private readonly decoded = computed<DecodedAccessToken | null>(() => {
    const token = this.accessToken();
    return token ? decodeJwtPayload(token) : null;
  });

  readonly isAuthenticated = computed(() => this.decoded() !== null);
  readonly role = computed<Role | null>(() => this.decoded()?.role ?? null);
  readonly userId = computed<string | null>(() => this.decoded()?.sub ?? null);

  constructor(private http: HttpClient) {}

  getAccessToken(): string | null {
    return this.accessToken();
  }

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>('/api/v1/auth/register', request);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/login', request)
      .pipe(tap((response) => this.accessToken.set(response.accessToken)));
  }

  refresh(): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/refresh', {})
      .pipe(tap((response) => this.accessToken.set(response.accessToken)));
  }

  logout(): void {
    this.accessToken.set(null);
  }
}

function decodeJwtPayload(token: string): DecodedAccessToken | null {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return null;
  }
}
