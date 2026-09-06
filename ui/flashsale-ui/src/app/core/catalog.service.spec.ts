import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CatalogService } from './catalog.service';
import { environment } from '../../environments/environment';

describe('CatalogService', () => {
  let service: CatalogService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CatalogService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends storefront search, sort, and pagination parameters', () => {
    service
      .list({ query: 'phone', category: 'Electronics', sort: 'price-asc', page: 2, size: 9 })
      .subscribe();

    const request = http.expectOne(
      (candidate) => candidate.url === `${environment.apiBaseUrl}/products`,
    );
    expect(request.request.params.get('query')).toBe('phone');
    expect(request.request.params.get('category')).toBe('Electronics');
    expect(request.request.params.get('sort')).toBe('price-asc');
    expect(request.request.params.get('page')).toBe('2');
    request.flush({
      content: [],
      number: 2,
      size: 9,
      totalElements: 0,
      totalPages: 0,
      first: false,
      last: true,
      empty: true,
      numberOfElements: 0,
    });
  });

  it('protects catalog administration calls with the bearer token', () => {
    service.setVisibility('admin-token', 'PHONE/ONE', true).subscribe();

    const request = http.expectOne(
      `${environment.apiBaseUrl}/products/admin/items/PHONE%2FONE/visibility`,
    );
    expect(request.request.method).toBe('PUT');
    expect(request.request.headers.get('Authorization')).toBe('Bearer admin-token');
    expect(request.request.body).toEqual({ active: true });
    request.flush({
      sku: 'PHONE/ONE',
      name: 'Phone',
      category: 'Electronics',
      price: 100,
      currency: 'INR',
      active: true,
    });
  });
});
