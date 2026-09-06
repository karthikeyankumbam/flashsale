import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  {
    path: 'products',
    loadComponent: () => import('./pages/products/products').then((page) => page.ProductsComponent),
  },
  {
    path: 'products/:sku',
    loadComponent: () =>
      import('./pages/product-detail/product-detail').then((page) => page.ProductDetailComponent),
  },
  {
    path: 'catalog-admin',
    loadComponent: () =>
      import('./pages/catalog-admin/catalog-admin').then((page) => page.CatalogAdminComponent),
  },
  {
    path: 'cart',
    loadComponent: () => import('./pages/cart/cart').then((page) => page.CartComponent),
  },
  {
    path: 'checkout',
    loadComponent: () => import('./pages/checkout/checkout').then((page) => page.CheckoutComponent),
  },
  {
    path: 'orders',
    loadComponent: () => import('./pages/orders/orders').then((page) => page.OrdersComponent),
  },
  { path: '**', redirectTo: 'products' },
];
