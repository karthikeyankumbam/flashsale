import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { CreateOrderRequest, CreateOrderResponse } from './models';

@Injectable({ providedIn: 'root' })
export class OrdersService {
  constructor(private api: ApiService) {}

  createOrder(userId: string, idempotencyKey: string, req: CreateOrderRequest) {
    return this.api.post<CreateOrderResponse>(
      `/orders`,
      req,
      { 'X-User-Id': userId, 'Idempotency-Key': idempotencyKey, 'Content-Type': 'application/json' }
    );
  }

  listOrders(userId: string) {
    return this.api.get<any[]>(`/orders`, {}, { 'X-User-Id': userId });
  }

  getOrder(userId: string, orderId: string) {
    return this.api.get<any>(`/orders/${orderId}`, {}, { 'X-User-Id': userId });
  }
}