export interface Product {
  sku: string;
  name: string;
  category: string;
  price: number;
  currency: string;
  active: boolean;
  description?: string;
  images?: string[];
  attributes?: Record<string, unknown>;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductPage {
  content: Product[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  numberOfElements: number;
}

export interface CatalogQuery {
  query?: string;
  category?: string;
  currency?: string;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
  sort?: 'name-asc' | 'name-desc' | 'price-asc' | 'price-desc' | 'newest' | 'updated';
}

export interface ProductWriteRequest {
  sku?: string;
  name: string;
  category: string;
  price: number;
  currency: string;
  active: boolean;
  description: string;
  images: string[];
  attributes: Record<string, unknown>;
}

export interface ApiProblem {
  status?: number;
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
}

export interface AuthResponse {
  accessToken: string;
  expiresInSeconds: number;
}

export interface AuthenticatedUser {
  userId: string;
  email: string;
  roles: string[];
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
