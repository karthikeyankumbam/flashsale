import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  get<T>(
    path: string,
    params?: Record<string, string | number | boolean | null | undefined>,
    headers?: Record<string, string>,
  ) {
    let httpParams = new HttpParams();
    Object.entries(params ?? {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return this.http.get<T>(`${this.base}${path}`, {
      params: httpParams,
      headers: new HttpHeaders(headers ?? {}),
    });
  }

  post<T>(path: string, body: unknown, headers?: Record<string, string>) {
    return this.http.post<T>(`${this.base}${path}`, body, {
      headers: new HttpHeaders(headers ?? {}),
    });
  }

  put<T>(path: string, body?: unknown, headers?: Record<string, string>) {
    return this.http.put<T>(`${this.base}${path}`, body ?? {}, {
      headers: new HttpHeaders(headers ?? {}),
    });
  }

  delete<T>(path: string, headers?: Record<string, string>) {
    return this.http.delete<T>(`${this.base}${path}`, {
      headers: new HttpHeaders(headers ?? {}),
    });
  }
}
