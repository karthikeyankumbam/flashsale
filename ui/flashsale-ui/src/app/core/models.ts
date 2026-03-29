export interface Product {
  sku: string;
  name: string;
  category: string;
  price: number;
  currency: string;
  active: boolean;
  attributes?: Record<string, any>;
}

export interface CartItem {
  sku: string;
  qty: number;
}

export interface CartResponse {
  userId: string;
  items: CartItem[];
}

export interface CreateOrderItem {
  sku: string;
  qty: number;
  unitPrice: number;
}

export interface CreateOrderRequest {
  currency: string;
  items: CreateOrderItem[];
}

export interface CreateOrderResponse {
  orderId: string;
  status: string;
}