import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { CatalogService } from '../../core/catalog.service';
import { CartService } from '../../core/cart.service';
import { Product } from '../../core/models';
import { getUserId } from '../../core/user';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatFormFieldModule, MatInputModule,
    MatSnackBarModule, MatProgressSpinnerModule
  ],
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class ProductsComponent {
  userId = getUserId();
  query = '';
  products: Product[] = [];
  loading = false;

  constructor(
    private catalog: CatalogService,
    private cart: CartService,
    private snack: MatSnackBar
  ) {
    this.load();
  }

  load() {
    this.loading = true;
    this.catalog.list(this.query).subscribe({
      next: (res) => {
        this.products = res?.content ?? res ?? [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snack.open('Failed to load products', 'Close', { duration: 2500 });
      }
    });
  }

  addToCart(p: Product) {
    this.cart.addItem(this.userId, p.sku, 1).subscribe({
      next: () => this.snack.open(`Added ${p.name}`, 'OK', { duration: 2000 }),
      error: () => this.snack.open(`Failed to add ${p.sku}`, 'Close', { duration: 2500 })
    });
  }
}