import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../core/auth.service';
import { CatalogService } from '../../core/catalog.service';
import { ApiProblem, Product, ProductWriteRequest } from '../../core/models';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

type Visibility = 'all' | 'published' | 'hidden';

interface ProductFormModel {
  sku: string;
  name: string;
  category: string;
  price: number | null;
  currency: string;
  active: boolean;
  description: string;
  imageLines: string;
  attributesText: string;
}

@Component({
  selector: 'app-catalog-admin',
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
  templateUrl: './catalog-admin.html',
  styleUrl: './catalog-admin.scss',
})
export class CatalogAdminComponent implements OnInit {
  loginEmail = '';
  loginPassword = '';
  loginError = '';
  signingIn = false;
  restoring = false;

  query = '';
  visibility: Visibility = 'all';
  products: Product[] = [];
  page = 0;
  readonly pageSize = 12;
  totalElements = 0;
  totalPages = 0;
  loading = false;
  listError = '';

  form: ProductFormModel = this.emptyForm();
  editingSku: string | null = null;
  saving = false;
  fieldErrors: Record<string, string> = {};
  formError = '';
  brokenPreviewImage = '';

  constructor(
    readonly auth: AuthService,
    private readonly catalog: CatalogService,
    private readonly snack: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    if (!this.auth.token()) return;
    this.restoring = true;
    this.auth
      .restore()
      .pipe(
        finalize(() => {
          this.restoring = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe((user) => {
        if (user?.roles.includes('ADMIN')) this.loadProducts();
      });
  }

  signIn() {
    this.loginError = '';
    if (!this.loginEmail.trim() || !this.loginPassword) {
      this.loginError = 'Enter your administrator email and password.';
      return;
    }

    this.signingIn = true;
    this.auth
      .login(this.loginEmail.trim(), this.loginPassword)
      .pipe(
        finalize(() => {
          this.signingIn = false;
          this.loginPassword = '';
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.loadProducts();
          this.cdr.detectChanges();
        },
        error: (error) => {
          this.loginError = this.errorMessage(
            error,
            'Sign-in failed. Check your credentials and try again.',
          );
          this.cdr.detectChanges();
        },
      });
  }

  signOut() {
    this.auth.logout();
    this.products = [];
    this.loginError = '';
    this.newProduct();
  }

  loadProducts() {
    const token = this.auth.token();
    if (!token) return;
    this.loading = true;
    this.listError = '';
    this.catalog
      .adminList(token, {
        query: this.query.trim(),
        visibility: this.visibility,
        page: this.page,
        size: this.pageSize,
        sort: 'updated',
      })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (response) => {
          this.products = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.page = response.number;
          this.cdr.detectChanges();
        },
        error: (error: HttpErrorResponse) =>
          this.handleAdminError(error, 'Could not load the product workspace.'),
      });
  }

  applyListFilters() {
    this.page = 0;
    this.loadProducts();
  }

  previousPage() {
    if (this.page > 0) {
      this.page--;
      this.loadProducts();
    }
  }

  nextPage() {
    if (this.page + 1 < this.totalPages) {
      this.page++;
      this.loadProducts();
    }
  }

  newProduct() {
    this.editingSku = null;
    this.form = this.emptyForm();
    this.fieldErrors = {};
    this.formError = '';
    this.brokenPreviewImage = '';
  }

  editProduct(product: Product) {
    const token = this.auth.token();
    if (!token) return;
    this.formError = '';
    this.catalog.adminGet(token, product.sku).subscribe({
      next: (fullProduct) => {
        this.editingSku = fullProduct.sku;
        this.form = this.toForm(fullProduct);
        this.fieldErrors = {};
        this.brokenPreviewImage = '';
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) =>
        this.handleAdminError(error, 'Could not open that product.'),
    });
  }

  saveProduct() {
    const token = this.auth.token();
    if (!token || !this.validateForm()) return;

    const request = this.toRequest();
    this.saving = true;
    this.formError = '';
    this.fieldErrors = {};
    const operation = this.editingSku
      ? this.catalog.update(token, this.editingSku, request)
      : this.catalog.create(token, request);

    operation
      .pipe(
        finalize(() => {
          this.saving = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (product) => {
          const wasCreating = !this.editingSku;
          this.editingSku = product.sku;
          this.form = this.toForm(product);
          this.snack.open(wasCreating ? 'Product created' : 'Product updated', 'OK', {
            duration: 2200,
          });
          this.loadProducts();
          this.cdr.detectChanges();
        },
        error: (error: HttpErrorResponse) => {
          const problem = error.error as ApiProblem | undefined;
          this.fieldErrors = this.normalizeErrors(problem?.errors);
          this.formError = this.errorMessage(
            error,
            'Could not save the product. Review the fields and try again.',
          );
          if (error.status === 401 || error.status === 403)
            this.handleAdminError(error, this.formError);
          this.cdr.detectChanges();
        },
      });
  }

  changeVisibility(product: Product) {
    const token = this.auth.token();
    if (!token) return;
    this.catalog.setVisibility(token, product.sku, !product.active).subscribe({
      next: (updated) => {
        this.snack.open(updated.active ? 'Product published' : 'Product hidden', 'OK', {
          duration: 2200,
        });
        if (this.editingSku === updated.sku) this.form.active = updated.active;
        this.loadProducts();
      },
      error: (error: HttpErrorResponse) =>
        this.handleAdminError(error, 'Could not change product visibility.'),
    });
  }

  previewImages() {
    return this.form.imageLines
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean);
  }

  previewAttributes() {
    try {
      const value = JSON.parse(this.form.attributesText || '{}');
      return value && typeof value === 'object' && !Array.isArray(value)
        ? Object.entries(value as Record<string, unknown>)
        : [];
    } catch {
      return [];
    }
  }

  previewImageAvailable() {
    const first = this.previewImages()[0];
    return !!first && first !== this.brokenPreviewImage;
  }

  markPreviewImageBroken() {
    this.brokenPreviewImage = this.previewImages()[0] ?? '';
  }

  trackProduct(_index: number, product: Product) {
    return product.sku;
  }

  private validateForm() {
    const errors: Record<string, string> = {};
    const skuPattern = /^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/;
    if (!this.form.sku.trim()) errors['sku'] = 'SKU is required.';
    else if (!skuPattern.test(this.form.sku.trim()))
      errors['sku'] = 'Use letters, numbers, dots, underscores or hyphens.';
    if (!this.form.name.trim()) errors['name'] = 'Product name is required.';
    if (!this.form.category.trim()) errors['category'] = 'Category is required.';
    if (!Number.isInteger(Number(this.form.price)) || Number(this.form.price) <= 0)
      errors['price'] = 'Enter a positive whole-number price.';
    if (!/^[A-Za-z]{3}$/.test(this.form.currency.trim()))
      errors['currency'] = 'Use a three-letter currency code such as INR.';

    const images = this.previewImages();
    if (images.length > 8) errors['images'] = 'Add no more than eight image links.';
    if (images.some((image) => !this.isHttpUrl(image)))
      errors['images'] = 'Every image must be a complete HTTP or HTTPS URL.';

    try {
      const attributes = JSON.parse(this.form.attributesText || '{}');
      if (!attributes || typeof attributes !== 'object' || Array.isArray(attributes)) {
        errors['attributes'] = 'Specifications must be a JSON object.';
      } else if (Object.keys(attributes).length > 30) {
        errors['attributes'] = 'Add no more than 30 specifications.';
      }
    } catch {
      errors['attributes'] = 'Specifications contain invalid JSON.';
    }

    this.fieldErrors = errors;
    this.formError = Object.keys(errors).length ? 'Check the highlighted fields.' : '';
    return Object.keys(errors).length === 0;
  }

  private toRequest(): ProductWriteRequest {
    return {
      sku: this.form.sku.trim(),
      name: this.form.name.trim(),
      category: this.form.category.trim(),
      price: Number(this.form.price),
      currency: this.form.currency.trim().toUpperCase(),
      active: this.form.active,
      description: this.form.description.trim(),
      images: this.previewImages(),
      attributes: JSON.parse(this.form.attributesText || '{}') as Record<string, unknown>,
    };
  }

  private toForm(product: Product): ProductFormModel {
    return {
      sku: product.sku,
      name: product.name,
      category: product.category,
      price: product.price,
      currency: product.currency,
      active: product.active,
      description: product.description ?? '',
      imageLines: (product.images ?? []).join('\n'),
      attributesText: JSON.stringify(product.attributes ?? {}, null, 2),
    };
  }

  private emptyForm(): ProductFormModel {
    return {
      sku: '',
      name: '',
      category: '',
      price: null,
      currency: 'INR',
      active: false,
      description: '',
      imageLines: '',
      attributesText: '{\n  \n}',
    };
  }

  private isHttpUrl(value: string) {
    try {
      const url = new URL(value);
      return url.protocol === 'http:' || url.protocol === 'https:';
    } catch {
      return false;
    }
  }

  private handleAdminError(error: HttpErrorResponse, fallback: string) {
    if (error.status === 401 || error.status === 403) {
      this.auth.logout();
      this.loginError =
        error.status === 403
          ? 'This account does not have permission to manage the catalog.'
          : 'Your session expired. Sign in again.';
      this.products = [];
    } else {
      this.listError = this.errorMessage(error, fallback);
    }
    this.cdr.detectChanges();
  }

  private normalizeErrors(errors?: Record<string, string>) {
    return Object.entries(errors ?? {}).reduce<Record<string, string>>(
      (result, [field, message]) => {
        const topLevelField = field.split(/[.[\]]/)[0];
        result[topLevelField] ??= message;
        return result;
      },
      {},
    );
  }

  private errorMessage(error: unknown, fallback: string) {
    if (error instanceof Error && !(error instanceof HttpErrorResponse)) return error.message;
    const problem = (error as HttpErrorResponse | undefined)?.error as ApiProblem | undefined;
    return problem?.detail || problem?.title || fallback;
  }
}
