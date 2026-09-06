import { Injectable, signal } from '@angular/core';
import { catchError, map, of, switchMap, throwError } from 'rxjs';

import { ApiService } from './api.service';
import { AuthenticatedUser, AuthResponse } from './models';

interface StoredSession {
  token: string;
  expiresAt: number;
  user: AuthenticatedUser;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'flashsale_admin_session';
  private readonly sessionSignal = signal<StoredSession | null>(this.readSession());

  readonly user = () => this.sessionSignal()?.user ?? null;
  readonly token = () => this.sessionSignal()?.token ?? null;

  constructor(private api: ApiService) {}

  login(email: string, password: string) {
    return this.api.post<AuthResponse>('/auth/login', { email, password }).pipe(
      switchMap((response) =>
        this.api
          .get<AuthenticatedUser>('/auth/me', undefined, {
            Authorization: `Bearer ${response.accessToken}`,
          })
          .pipe(map((user) => ({ response, user }))),
      ),
      switchMap(({ response, user }) => {
        if (!user.roles.includes('ADMIN')) {
          return throwError(() => new Error('This account does not have the ADMIN role.'));
        }
        const session: StoredSession = {
          token: response.accessToken,
          expiresAt: Date.now() + response.expiresInSeconds * 1000,
          user,
        };
        this.saveSession(session);
        return of(user);
      }),
    );
  }

  restore() {
    const session = this.sessionSignal();
    if (!session) return of(null);

    return this.api
      .get<AuthenticatedUser>('/auth/me', undefined, {
        Authorization: `Bearer ${session.token}`,
      })
      .pipe(
        switchMap((user) => {
          if (!user.roles.includes('ADMIN')) {
            this.logout();
            return of(null);
          }
          this.saveSession({ ...session, user });
          return of(user);
        }),
        catchError(() => {
          this.logout();
          return of(null);
        }),
      );
  }

  logout() {
    localStorage.removeItem(this.storageKey);
    this.sessionSignal.set(null);
  }

  private readSession(): StoredSession | null {
    try {
      const raw = localStorage.getItem(this.storageKey);
      const session = raw ? (JSON.parse(raw) as StoredSession) : null;
      if (!session || session.expiresAt <= Date.now()) {
        localStorage.removeItem(this.storageKey);
        return null;
      }
      return session;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }

  private saveSession(session: StoredSession) {
    localStorage.setItem(this.storageKey, JSON.stringify(session));
    this.sessionSignal.set(session);
  }
}
