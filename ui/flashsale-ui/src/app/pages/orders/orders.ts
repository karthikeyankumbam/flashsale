import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { OrdersService } from '../../core/orders.service';
import { getUserId } from '../../core/user';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './orders.html',
  styleUrl: './orders.scss',
})
export class OrdersComponent implements OnInit {
  userId = getUserId();

  orders: any[] = [];
  lastOrderId: string | null = null;
  lastOrder: any | null = null;

  loading = false;
  private reqId = 0;

  constructor(
    private ordersSvc: OrdersService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Always load once when component mounts
    this.refresh(true);

    // Keep this only for setting lastOrderId (optional)
    this.route.queryParamMap.subscribe((q) => {
      this.lastOrderId = q.get('last');
      // optional: fetch last order details whenever param changes
      if (this.lastOrderId) this.fetchLastOrder();
    });
  }

  refresh(showSpinner: boolean) {
    console.log('Calling /orders with userId=', this.userId); // <— confirm in console
    const id = ++this.reqId;

    if (showSpinner) {
      this.loading = true;
      this.cdr.detectChanges();
    }

    this.ordersSvc
      .listOrders(this.userId)
      .pipe(
        finalize(() => {
          if (showSpinner && id === this.reqId) {
            this.loading = false;
            this.cdr.detectChanges();
          }
        })
      )
      .subscribe({
        next: (res) => {
          if (id !== this.reqId) return;
          this.orders = res ?? [];
          this.cdr.detectChanges();
          if (this.lastOrderId) this.fetchLastOrder();
        },
        error: (e) => {
          if (id !== this.reqId) return;
          console.error('orders load error', e);
        },
      });
  }

  private fetchLastOrder() {
    if (!this.lastOrderId) return;
    this.ordersSvc.getOrder(this.userId, this.lastOrderId).subscribe({
      next: (o) => {
        this.lastOrder = o;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  chipColor(status: string): 'primary' | 'accent' | 'warn' | undefined {
    const s = (status || '').toUpperCase();
    if (s === 'PAID') return 'primary';
    if (s === 'PAYMENT_PENDING' || s === 'PENDING' || s === 'CONFIRMED') return 'accent';
    if (s === 'CANCELLED') return 'warn';
    return undefined;
  }
}