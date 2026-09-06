import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('verifies the ADMIN role before storing a login session', () => {
    service.login('owner@example.com', 'secret-password').subscribe();

    const login = http.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(login.request.method).toBe('POST');
    login.flush({ accessToken: 'signed-token', expiresInSeconds: 3600 });

    const me = http.expectOne(`${environment.apiBaseUrl}/auth/me`);
    expect(me.request.headers.get('Authorization')).toBe('Bearer signed-token');
    me.flush({ userId: 'U-1', email: 'owner@example.com', roles: ['USER', 'ADMIN'] });

    expect(service.user()?.email).toBe('owner@example.com');
    expect(service.token()).toBe('signed-token');
  });
});
