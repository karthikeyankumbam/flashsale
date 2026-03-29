import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CartService } from '../../core/cart.service';
import { OrdersService } from '../../core/orders.service';
import { CatalogService } from '../../core/catalog.service';
import { CartResponse, CreateOrderItem, CreateOrderRequest } from '../../core/models';
import { getUserId } from '../../core/user';
import { Router, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss',
})
export class CheckoutComponent {
  userId = getUserId();
  cart?: CartResponse;
  msg = '';
  placing = false;

  // price cache loaded from catalog
  prices: Record<string, { unitPrice: number; currency: string }> = {};

  constructor(
    private cartSvc: CartService,
    private ordersSvc: OrdersService,
    private catalogSvc: CatalogService,
    private router: Router
  ) {
    this.loadCart();
  }

  loadCart() {
    this.cartSvc.getCart(this.userId).subscribe({
      next: (c) => {
        this.cart = c;
        this.loadPrices();
      },
      error: () => this.msg = 'Failed to load cart'
    });
  }

  loadPrices() {
    if (!this.cart || this.cart.items.length === 0) return;
    const calls = this.cart.items.map(i => this.catalogSvc.getBySku(i.sku));
    forkJoin(calls).subscribe({
      next: (products) => {
        products.forEach(p => {
          this.prices[p.sku] = { unitPrice: p.price, currency: p.currency };
        });
      },
      error: () => this.msg = 'Failed to load product prices (catalog)'
    });
  }

  total(): number {
    if (!this.cart) return 0;
    return this.cart.items.reduce((sum, i) => {
      const p = this.prices[i.sku];
      return sum + (p ? p.unitPrice * i.qty : 0);
    }, 0);
  }

  placeOrder() {
    if (!this.cart || this.cart.items.length === 0) {
      this.msg = 'Cart is empty';
      return;
    }

    // Build order items using catalog prices
    const items: CreateOrderItem[] = [];
    let currency = 'INR';

    for (const i of this.cart.items) {
      const p = this.prices[i.sku];
      if (!p) {
        this.msg = `Missing price for ${i.sku}. Click Refresh.`;
        return;
      }
      currency = p.currency;
      items.push({ sku: i.sku, qty: i.qty, unitPrice: p.unitPrice });
    }

    const req: CreateOrderRequest = { currency, items };
    const idempotencyKey = `UI-${this.userId}-${Date.now()}`;

    this.placing = true;
    this.msg = 'Placing order...';

    this.ordersSvc.createOrder(this.userId, idempotencyKey, req).subscribe({
      next: (res) => {
        this.msg = `Order created: ${res.orderId}`;
        // optional: clear cart after order creation
        this.cartSvc.clearCart(this.userId).subscribe({ next: () => {} });
        this.placing = false;
        this.router.navigate(['/orders'], { queryParams: { last: res.orderId } });
      },
      error: () => {
        this.msg = 'Failed to place order';
        this.placing = false;
      }
    });
  }
}