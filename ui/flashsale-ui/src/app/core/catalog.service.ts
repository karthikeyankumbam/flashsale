import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Product } from './models';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  constructor(private api: ApiService) {}

  list(query?: string) {
    return this.api.get<any>('/products', { query: query ?? '' });
  }

  getBySku(sku: string) {
    return this.api.get<Product>(`/products/${sku}`);
  }
}