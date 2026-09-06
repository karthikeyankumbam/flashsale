import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { CatalogQuery, Product, ProductPage, ProductWriteRequest } from './models';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  constructor(private api: ApiService) {}

  list(query: CatalogQuery = {}) {
    return this.api.get<ProductPage>('/products', { ...query });
  }

  categories() {
    return this.api.get<string[]>('/products/browse/categories');
  }

  getBySku(sku: string) {
    return this.api.get<Product>(`/products/${encodeURIComponent(sku)}`);
  }

  adminList(token: string, query: CatalogQuery & { visibility?: 'all' | 'published' | 'hidden' }) {
    return this.api.get<ProductPage>('/products/admin/items', { ...query }, this.auth(token));
  }

  adminGet(token: string, sku: string) {
    return this.api.get<Product>(
      `/products/admin/items/${encodeURIComponent(sku)}`,
      undefined,
      this.auth(token),
    );
  }

  create(token: string, request: ProductWriteRequest) {
    return this.api.post<Product>('/products/admin/items', request, this.auth(token));
  }

  update(token: string, sku: string, request: ProductWriteRequest) {
    const { sku: _ignored, ...body } = request;
    return this.api.put<Product>(
      `/products/admin/items/${encodeURIComponent(sku)}`,
      body,
      this.auth(token),
    );
  }

  setVisibility(token: string, sku: string, active: boolean) {
    return this.api.put<Product>(
      `/products/admin/items/${encodeURIComponent(sku)}/visibility`,
      { active },
      this.auth(token),
    );
  }

  private auth(token: string) {
    return { Authorization: `Bearer ${token}` };
  }
}
