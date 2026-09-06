import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { CatalogService } from '../../core/catalog.service';
import { CartService } from '../../core/cart.service';
import { CatalogQuery, Product } from '../../core/models';
import { getUserId } from '../../core/user';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-products',
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
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class ProductsComponent implements OnInit, OnDestroy {
  readonly userId = getUserId();
  readonly pageSize = 9;
  readonly sortOptions = [
    { value: 'updated', label: 'Recently updated' },
    { value: 'name-asc', label: 'Name: A to Z' },
    { value: 'name-desc', label: 'Name: Z to A' },
    { value: 'price-asc', label: 'Price: low to high' },
    { value: 'price-desc', label: 'Price: high to low' },
  ] as const;

  query = '';
  category = '';
  sort: CatalogQuery['sort'] = 'updated';
  page = 0;
  products: Product[] = [];
  categories: string[] = [];
  totalElements = 0;
  totalPages = 0;
  loading = false;
  errorMessage = '';
  quantities: Record<string, number> = {};
  brokenImages = new Set<string>();

  private requestId = 0;
  private readonly destroyed = new Subject<void>();

  constructor(
    private readonly catalog: CatalogService,
    private readonly cart: CartService,
    private readonly snack: MatSnackBar,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.loadCategories();
    this.route.queryParamMap.pipe(takeUntil(this.destroyed)).subscribe((params) => {
      this.query = params.get('query') ?? '';
      this.category = params.get('category') ?? '';
      this.sort = this.readSort(params.get('sort'));
      this.page = this.readPage(params.get('page'));
      this.load();
    });
  }

  ngOnDestroy() {
    this.destroyed.next();
    this.destroyed.complete();
  }

  search() {
    this.updateRoute(0);
  }

  filterChanged() {
    this.updateRoute(0);
  }

  resetFilters() {
    this.query = '';
    this.category = '';
    this.sort = 'updated';
    this.updateRoute(0);
  }

  previousPage() {
    if (this.page > 0) this.updateRoute(this.page - 1);
  }

  nextPage() {
    if (this.page + 1 < this.totalPages) this.updateRoute(this.page + 1);
  }

  load() {
    const id = ++this.requestId;
    this.loading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.catalog
      .list({
        query: this.query.trim(),
        category: this.category,
        sort: this.sort,
        page: this.page,
        size: this.pageSize,
      })
      .pipe(
        finalize(() => {
          if (id === this.requestId) {
            this.loading = false;
            this.cdr.detectChanges();
          }
        }),
      )
      .subscribe({
        next: (response) => {
          if (id !== this.requestId) return;
          this.products = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.page = response.number;
          response.content.forEach((product) => (this.quantities[product.sku] ??= 1));
          this.cdr.detectChanges();
        },
        error: () => {
          if (id !== this.requestId) return;
          this.products = [];
          this.totalElements = 0;
          this.totalPages = 0;
          this.errorMessage =
            'The catalog is temporarily unavailable. Your filters are still here.';
          this.cdr.detectChanges();
        },
      });
  }

  addToCart(product: Product) {
    const quantity = this.normalizedQuantity(product.sku);
    this.quantities[product.sku] = quantity;
    this.cart.addItem(this.userId, product.sku, quantity).subscribe({
      next: () => {
        const message = this.snack.open(
          `${quantity} × ${product.name} added to your cart`,
          'View cart',
          { duration: 3500 },
        );
        message.onAction().subscribe(() => this.router.navigate(['/cart']));
      },
      error: () =>
        this.snack.open(`Could not add ${product.name}. Please try again.`, 'Close', {
          duration: 3000,
        }),
    });
  }

  normalizedQuantity(sku: string) {
    const quantity = Number(this.quantities[sku]);
    return Number.isFinite(quantity) ? Math.min(99, Math.max(1, Math.floor(quantity))) : 1;
  }

  hasImage(product: Product) {
    return !!product.images?.[0] && !this.brokenImages.has(product.sku);
  }

  markImageBroken(sku: string) {
    this.brokenImages.add(sku);
  }

  listingParams() {
    return {
      query: this.query.trim() || null,
      category: this.category || null,
      sort: this.sort === 'updated' ? null : this.sort,
      page: this.page || null,
    };
  }

  trackProduct(_index: number, product: Product) {
    return product.sku;
  }

  private loadCategories() {
    this.catalog.categories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.cdr.detectChanges();
      },
      error: () => {
        this.categories = [];
      },
    });
  }

  private updateRoute(page: number) {
    this.router
      .navigate([], {
        relativeTo: this.route,
        queryParams: {
          query: this.query.trim() || null,
          category: this.category || null,
          sort: this.sort === 'updated' ? null : this.sort,
          page: page || null,
        },
      })
      .then((navigated) => {
        if (!navigated) this.load();
      });
  }

  private readPage(value: string | null) {
    const page = Number(value);
    return Number.isInteger(page) && page >= 0 ? page : 0;
  }

  private readSort(value: string | null): CatalogQuery['sort'] {
    return this.sortOptions.some((option) => option.value === value)
      ? (value as CatalogQuery['sort'])
      : 'updated';
  }
}
