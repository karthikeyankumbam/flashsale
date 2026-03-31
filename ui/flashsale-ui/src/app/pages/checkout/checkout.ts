import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { CartService } from '../../core/cart.service';
import { OrdersService } from '../../core/orders.service';
import { CatalogService } from '../../core/catalog.service';
import { CartResponse, CreateOrderItem, CreateOrderRequest } from '../../core/models';
import { getUserId } from '../../core/user';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatSnackBarModule, MatProgressSpinnerModule
  ],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss',
})
export class CheckoutComponent implements OnInit {
  userId = getUserId();
  cart?: CartResponse;

  loading = false;
  placing = false;

  prices: Record<string, { unitPrice: number; currency: string; name: string }> = {};

  constructor(
    private cartSvc: CartService,
    private ordersSvc: OrdersService,
    private catalogSvc: CatalogService,
    private snack: MatSnackBar,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.cdr.detectChanges();

    this.cartSvc.getCart(this.userId)
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (c) => {
          this.cart = c;
          this.cdr.detectChanges();
          this.loadPrices();
        },
        error: (e) => {
          console.error(e);
          this.snack.open('Failed to load cart', 'Close', { duration: 2500 });
        }
      });
  }

  loadPrices() {
    if (!this.cart || this.cart.items.length === 0) return;

    const calls = this.cart.items.map(i => this.catalogSvc.getBySku(i.sku));
    forkJoin(calls).subscribe({
      next: (products) => {
        products.forEach(p => {
          this.prices[p.sku] = { unitPrice: p.price, currency: p.currency, name: p.name };
        });
        this.cdr.detectChanges();
      },
      error: () => this.snack.open('Failed to load catalog prices', 'Close', { duration: 2500 })
    });
  }

  total(): number {
    if (!this.cart) return 0;
    return this.cart.items.reduce((sum, i) => {
      const p = this.prices[i.sku];
      return sum + (p ? p.unitPrice * i.qty : 0);
    }, 0);
  }

  currency(): string {
    const skus = Object.keys(this.prices);
    return skus.length ? this.prices[skus[0]].currency : 'INR';
  }

  hasItems(): boolean {
    return !!this.cart && this.cart.items.length > 0;
  }

  placeOrder() {
    if (!this.cart || this.cart.items.length === 0) {
      this.snack.open('Cart is empty', 'Close', { duration: 2000 });
      return;
    }

    const items: CreateOrderItem[] = [];
    for (const i of this.cart.items) {
      const p = this.prices[i.sku];
      if (!p) {
        this.snack.open(`Price missing for ${i.sku}. Refresh.`, 'Close', { duration: 2500 });
        return;
      }
      items.push({ sku: i.sku, qty: i.qty, unitPrice: p.unitPrice });
    }

    const req: CreateOrderRequest = { currency: this.currency(), items };
    const idempotencyKey = `UI-${this.userId}-${Date.now()}`;

    this.placing = true;
    this.cdr.detectChanges();

    this.ordersSvc.createOrder(this.userId, idempotencyKey, req)
      .pipe(finalize(() => {
        this.placing = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (res) => {
          this.snack.open(`Order created: ${res.orderId}`, 'OK', { duration: 2000 });
          this.cartSvc.clearCart(this.userId).subscribe({ next: () => {} });
          this.router.navigate(['/orders'], { queryParams: { last: res.orderId } });
        },
        error: (e) => {
          console.error(e);
          this.snack.open('Failed to place order', 'Close', { duration: 2500 });
        }
      });
  }
}