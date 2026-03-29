import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { interval, Subscription } from 'rxjs';

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
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatChipsModule, MatProgressSpinnerModule
  ],
  templateUrl: './orders.html',
  styleUrl: './orders.scss',
})
export class OrdersComponent implements OnDestroy {
  userId = getUserId();
  orders: any[] = [];
  lastOrderId: string | null = null;
  lastOrder: any | null = null;

  loading = false;
  private sub?: Subscription;

  constructor(private ordersSvc: OrdersService, private route: ActivatedRoute) {
    this.route.queryParamMap.subscribe(q => {
      this.lastOrderId = q.get('last');
      this.refresh();
    });

    this.sub = interval(2000).subscribe(() => this.refresh(false));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  refresh(showSpinner = true) {
    if (showSpinner) this.loading = true;

    this.ordersSvc.listOrders(this.userId).subscribe({
      next: (res) => {
        this.orders = res ?? [];
        if (showSpinner) this.loading = false;

        if (this.lastOrderId) {
          this.ordersSvc.getOrder(this.userId, this.lastOrderId).subscribe({
            next: (o) => this.lastOrder = o,
            error: () => {}
          });
        }
      },
      error: () => { if (showSpinner) this.loading = false; }
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