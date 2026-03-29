import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CartService } from '../../core/cart.service';
import { CartResponse } from '../../core/models';
import { getUserId } from '../../core/user';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
})
export class CartComponent {
  userId = getUserId();
  cart?: CartResponse;
  msg = '';

  constructor(private cartSvc: CartService) {
    this.refresh();
  }

  refresh() {
    this.cartSvc.getCart(this.userId).subscribe({
      next: (c) => this.cart = c,
      error: () => this.msg = 'Failed to load cart'
    });
  }

  setQty(sku: string, qty: number) {
    if (!qty || qty < 1) return;
    this.cartSvc.setQty(this.userId, sku, qty).subscribe({
      next: (c) => this.cart = c,
      error: () => this.msg = 'Failed to update qty'
    });
  }

  remove(sku: string) {
    this.cartSvc.removeItem(this.userId, sku).subscribe({
      next: (c) => this.cart = c,
      error: () => this.msg = 'Failed to remove item'
    });
  }

  clear() {
    this.cartSvc.clearCart(this.userId).subscribe({
      next: () => { this.msg = 'Cart cleared'; this.refresh(); },
      error: () => this.msg = 'Failed to clear cart'
    });
  }
}