import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { CartResponse } from './models';

@Injectable({ providedIn: 'root' })
export class CartService {
  constructor(private api: ApiService) {}

  getCart(userId: string) {
    return this.api.get<CartResponse>(`/cart/${userId}`);
  }

  addItem(userId: string, sku: string, qty: number) {
    return this.api.post<CartResponse>(`/cart/${userId}/items`, { sku, qty });
  }

  setQty(userId: string, sku: string, qty: number) {
    return this.api.put<CartResponse>(`/cart/${userId}/items/${sku}?qty=${qty}`);
  }

  removeItem(userId: string, sku: string) {
    return this.api.delete<CartResponse>(`/cart/${userId}/items/${sku}`);
  }

  clearCart(userId: string) {
    return this.api.delete<void>(`/cart/${userId}`);
  }
}