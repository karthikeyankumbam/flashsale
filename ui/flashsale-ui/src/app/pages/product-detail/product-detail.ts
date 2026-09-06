import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { CartService } from '../../core/cart.service';
import { CatalogService } from '../../core/catalog.service';
import { Product } from '../../core/models';
import { getUserId } from '../../core/user';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss',
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  product?: Product;
  sku = '';
  quantity = 1;
  selectedImage = 0;
  loading = true;
  unavailable = false;
  failed = false;
  returnParams: Record<string, string> = {};
  brokenImages = new Set<string>();

  private readonly userId = getUserId();
  private readonly destroyed = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly catalog: CatalogService,
    private readonly cart: CartService,
    private readonly snack: MatSnackBar,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.route.queryParamMap.pipe(takeUntil(this.destroyed)).subscribe((params) => {
      this.returnParams = ['query', 'category', 'sort', 'page'].reduce<Record<string, string>>(
        (result, key) => {
          const value = params.get(key);
          if (value) result[key] = value;
          return result;
        },
        {},
      );
    });

    this.route.paramMap.pipe(takeUntil(this.destroyed)).subscribe((params) => {
      this.sku = params.get('sku') ?? '';
      this.load();
    });
  }

  ngOnDestroy() {
    this.destroyed.next();
    this.destroyed.complete();
  }

  load() {
    this.loading = true;
    this.unavailable = false;
    this.failed = false;
    this.product = undefined;
    this.selectedImage = 0;

    this.catalog
      .getBySku(this.sku)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (product) => {
          this.product = product;
          this.cdr.detectChanges();
        },
        error: (error: HttpErrorResponse) => {
          this.unavailable = error.status === 404;
          this.failed = error.status !== 404;
          this.cdr.detectChanges();
        },
      });
  }

  chooseImage(index: number) {
    this.selectedImage = index;
  }

  imageAvailable(url?: string) {
    return !!url && !this.brokenImages.has(url);
  }

  markImageBroken(url: string) {
    this.brokenImages.add(url);
  }

  addToCart() {
    if (!this.product) return;
    this.quantity = this.normalizeQuantity();
    this.cart.addItem(this.userId, this.product.sku, this.quantity).subscribe({
      next: () => {
        const message = this.snack.open(
          `${this.quantity} × ${this.product!.name} added to your cart`,
          'View cart',
          { duration: 3500 },
        );
        message.onAction().subscribe(() => this.router.navigate(['/cart']));
      },
      error: () =>
        this.snack.open('We could not add this product. Please try again.', 'Close', {
          duration: 3000,
        }),
    });
  }

  private normalizeQuantity() {
    const quantity = Number(this.quantity);
    return Number.isFinite(quantity) ? Math.min(99, Math.max(1, Math.floor(quantity))) : 1;
  }
}
