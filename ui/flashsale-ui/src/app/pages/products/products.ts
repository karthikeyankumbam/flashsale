import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CatalogService } from '../../core/catalog.service';
import { CartService } from '../../core/cart.service';
import { Product } from '../../core/models';
import { getUserId } from '../../core/user';
import { FormsModule } from '@angular/forms';


@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule,FormsModule],
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class ProductsComponent {
  userId = getUserId();
  query = '';
  products: Product[] = [];
  loading = false;
  msg = '';

  constructor(private catalog: CatalogService, private cart: CartService) {
    this.load();
  }

  load() {
    this.loading = true;
    this.catalog.list(this.query).subscribe({
      next: (res) => {
        this.products = res?.content ?? res ?? [];
        this.loading = false;
      },
      error: (e) => {
        this.msg = 'Failed to load products';
        this.loading = false;
      }
    });
  }

  addToCart(p: Product) {
    this.cart.addItem(this.userId, p.sku, 1).subscribe({
      next: () => this.msg = `Added ${p.sku} to cart`,
      error: () => this.msg = `Failed to add ${p.sku}`
    });
  }
}