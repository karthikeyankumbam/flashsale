import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';

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
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class ProductsComponent implements OnInit {
  userId = getUserId();
  query = '';
  products: Product[] = [];
  loading = false;

  private reqId = 0;

  constructor(
    private catalog: CatalogService,
    private cart: CartService,
    private snack: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    const id = ++this.reqId;
    this.loading = true;
    this.cdr.detectChanges();

    this.catalog
      .list(this.query)
      .pipe(
        finalize(() => {
          if (id === this.reqId) {
            this.loading = false;
            this.cdr.detectChanges();
          }
        })
      )
      .subscribe({
        next: (res) => {
          if (id !== this.reqId) return;
          this.products = res?.content ?? res ?? [];
          this.cdr.detectChanges();
        },
        error: (e) => {
          if (id !== this.reqId) return;
          console.error('products load error', e);
          this.snack.open('Failed to load products', 'Close', { duration: 2500 });
          this.cdr.detectChanges();
        },
      });
  }

  addToCart(p: Product) {
    this.cart.addItem(this.userId, p.sku, 1).subscribe({
      next: () => this.snack.open(`Added ${p.name}`, 'OK', { duration: 2000 }),
      error: () => this.snack.open(`Failed to add ${p.sku}`, 'Close', { duration: 2500 }),
    });
  }
}