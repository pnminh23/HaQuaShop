import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../config/api.config';
import { Profile, SessionUser, TokenResponse } from '../models/auth.model';
import { StorageService } from './storage.service';

const ACCESS = 'haqua.access';
const REFRESH = 'haqua.refresh';
const USER = 'haqua.user';
export const CART_TOKEN = 'haqua.cartToken';

export interface RegisterPayload {
  fullName: string;
  phone: string;
  email: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(StorageService);
  private readonly router = inject(Router);

  private readonly _user = signal<SessionUser | null>(this.restore());

  readonly user = this._user.asReadonly();
  readonly isLoggedIn = computed(() => this._user() !== null);
  readonly isStaff = computed(() => ['STAFF', 'ADMIN'].includes(this._user()?.role ?? ''));
  readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  get accessToken(): string | null {
    return this.storage.get(ACCESS);
  }

  get refreshToken(): string | null {
    return this.storage.get(REFRESH);
  }

  login(username: string, password: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API_URL}/auth/login`, { username, password, guestCartToken: this.storage.get(CART_TOKEN) })
      .pipe(tap((t) => this.accept(t)));
  }

  register(payload: RegisterPayload): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API_URL}/auth/register`, { ...payload, guestCartToken: this.storage.get(CART_TOKEN) })
      .pipe(tap((t) => this.accept(t)));
  }

  refresh(): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API_URL}/auth/refresh`, { refreshToken: this.refreshToken })
      .pipe(tap((t) => this.accept(t)));
  }

  /** redirect=false: dọn phiên hết hạn âm thầm, không đẩy người dùng khỏi trang đang xem. */
  logout(redirect = true): void {
    [ACCESS, REFRESH, USER].forEach((k) => this.storage.remove(k));
    this._user.set(null);
    if (redirect) void this.router.navigateByUrl('/');
  }

  profile(): Observable<Profile> {
    return this.http.get<Profile>(`${API_URL}/account`);
  }

  /** Trang đích sau khi đăng nhập: nhân viên/quản trị vào /admin, khách về trang trước. */
  homeFor(user: SessionUser): string {
    return user.role === 'CUSTOMER' ? '/' : '/admin';
  }

  private accept(t: TokenResponse): void {
    const user: SessionUser = { userId: t.userId, fullName: t.fullName, role: t.role };
    this.storage.set(ACCESS, t.accessToken);
    this.storage.set(REFRESH, t.refreshToken);
    this.storage.set(USER, JSON.stringify(user));
    this.storage.remove(CART_TOKEN); // giỏ vãng lai đã được gộp vào tài khoản
    this._user.set(user);
  }

  private restore(): SessionUser | null {
    const raw = this.storage.get(USER);
    try {
      return raw ? (JSON.parse(raw) as SessionUser) : null;
    } catch {
      return null;
    }
  }
}
