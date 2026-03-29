import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrdersService } from '../../core/orders.service';
import { getUserId } from '../../core/user';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './orders.html',
  styleUrl: './orders.scss',
})
export class OrdersComponent implements OnDestroy {
  userId = getUserId();

  orders: any[] = [];
  lastOrderId: string | null = null;
  lastOrder: any | null = null;

  msg = '';
  private sub?: Subscription;

  constructor(private ordersSvc: OrdersService, private route: ActivatedRoute) {
    this.route.queryParamMap.subscribe(q => {
      this.lastOrderId = q.get('last');
      this.refresh();
    });

    // Auto refresh every 2 seconds
    this.sub = interval(2000).subscribe(() => this.refresh(false));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  refresh(showMsg = true) {
    this.ordersSvc.listOrders(this.userId).subscribe({
      next: (res) => {
        this.orders = res ?? [];
        if (showMsg) this.msg = '';

        // If we have a last order id, load its latest snapshot
        if (this.lastOrderId) {
          this.ordersSvc.getOrder(this.userId, this.lastOrderId).subscribe({
            next: (o) => this.lastOrder = o,
            error: () => {}
          });
        }
      },
      error: () => {
        if (showMsg) this.msg = 'Failed to load orders';
      }
    });
  }

  badgeClass(status: string) {
    const s = (status || '').toUpperCase();
    if (s === 'PAID') return 'badge paid';
    if (s === 'PAYMENT_PENDING') return 'badge pending';
    if (s === 'PENDING') return 'badge pending';
    if (s === 'CANCELLED') return 'badge cancelled';
    if (s === 'CONFIRMED') return 'badge confirmed';
    return 'badge';
  }
}