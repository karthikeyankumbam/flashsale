import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { CartService } from '../../core/cart.service';
import { CartResponse } from '../../core/models';
import { getUserId } from '../../core/user';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatTableModule, MatIconModule,
    MatSnackBarModule, MatProgressSpinnerModule
  ],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
})
export class CartComponent implements OnInit {
  userId = getUserId();
  cart?: CartResponse;
  loading = false;

  displayedColumns = ['sku', 'qty', 'actions'];

  constructor(
    private cartSvc: CartService,
    private snack: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh() {
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
        },
        error: (e) => {
          console.error(e);
          this.snack.open('Failed to load cart', 'Close', { duration: 2500 });
          this.cdr.detectChanges();
        }
      });
  }

  inc(sku: string, qty: number) {
    this.cartSvc.setQty(this.userId, sku, qty + 1).subscribe({
      next: (c) => { this.cart = c; this.cdr.detectChanges(); },
      error: () => this.snack.open('Failed to update qty', 'Close', { duration: 2500 })
    });
  }

  dec(sku: string, qty: number) {
    if (qty <= 1) return;
    this.cartSvc.setQty(this.userId, sku, qty - 1).subscribe({
      next: (c) => { this.cart = c; this.cdr.detectChanges(); },
      error: () => this.snack.open('Failed to update qty', 'Close', { duration: 2500 })
    });
  }

  remove(sku: string) {
    this.cartSvc.removeItem(this.userId, sku).subscribe({
      next: (c) => {
        this.cart = c;
        this.snack.open('Removed item', 'OK', { duration: 1500 });
        this.cdr.detectChanges();
      },
      error: () => this.snack.open('Failed to remove item', 'Close', { duration: 2500 })
    });
  }

  clear() {
    this.cartSvc.clearCart(this.userId).subscribe({
      next: () => {
        this.snack.open('Cart cleared', 'OK', { duration: 1500 });
        this.refresh();
      },
      error: () => this.snack.open('Failed to clear cart', 'Close', { duration: 2500 })
    });
  }

  hasItems(): boolean {
    return !!this.cart && this.cart.items.length > 0;
  }
}