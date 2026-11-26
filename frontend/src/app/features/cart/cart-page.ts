import { DialogService } from '../../core/services/dialog.service';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CartItem } from '../../core/models/cart.model';
import { CartService } from '../../core/services/cart.service';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';
import { ProductThumb } from '../../shared/components/product-thumb';
import { VndPipe } from '../../shared/pipes/vnd.pipe';

/** UC06 – quản lý giỏ hàng, UC07 – mã giảm giá. */
@Component({
  selector: 'app-cart-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, VndPipe, ProductThumb],
  templateUrl: './cart-page.html',
  styleUrl: './cart-page.scss',
})
export class CartPage {
  private readonly dialog = inject(DialogService);
  private readonly cartService = inject(CartService);
  private readonly toast = inject(ToastService);

  protected readonly cart = this.cartService.cart;
  protected readonly code = signal('');
  protected readonly couponMsg = signal('');

  protected readonly active = computed(() => this.cart().items.filter((i) => !i.savedForLater));
  protected readonly saved = computed(() => this.cart().items.filter((i) => i.savedForLater));
  protected readonly allChecked = computed(() => this.active().length > 0 && this.active().every((i) => i.selected));
  protected readonly freeShipThreshold = 500_000;
  protected readonly shipProgress = computed(() => Math.min(100, Math.round((this.cart().summary.subtotal / this.freeShipThreshold) * 100)));

  constructor() {
    inject(SeoService).set('Giỏ hàng | HaQua');
    this.cartService.load().subscribe();
  }

  protected async setQty(item: CartItem, qty: number): Promise<void> {
    if (qty <= 0 && !(await this.dialog.confirm({ message: 'Xóa sản phẩm này khỏi giỏ hàng?', confirmLabel: 'Xóa', danger: true }))) return;
    this.cartService.update(item.id, { qty }).subscribe({ error: (e) => this.toast.show(e.error?.detail ?? 'Không cập nhật được') });
  }

  protected toggle(item: CartItem): void {
    this.cartService.update(item.id, { selected: !item.selected }).subscribe();
  }

  protected toggleAll(): void {
    const target = !this.allChecked();
    this.active().filter((i) => i.selected !== target).forEach((i) => this.cartService.update(i.id, { selected: target }).subscribe());
  }

  protected saveForLater(item: CartItem, value: boolean): void {
    this.cartService.update(item.id, { savedForLater: value, selected: !value }).subscribe();
  }

  protected remove(item: CartItem): void {
    this.cartService.remove(item.id).subscribe();
  }

  protected async removeSelected(): Promise<void> {
    if (this.cart().summary.selectedCount && await this.dialog.confirm({ message: 'Xóa các sản phẩm đã chọn?', confirmLabel: 'Xóa', danger: true })) this.cartService.removeSelected().subscribe();
  }

  protected applyCoupon(): void {
    const code = this.code().trim();
    if (!code) return;
    this.cartService.applyCoupon(code).subscribe({
      next: () => this.couponMsg.set(`Đã áp dụng mã ${code.toUpperCase()}`),
      error: (e) => this.couponMsg.set(e.error?.detail ?? 'Mã không hợp lệ'),
    });
  }

  protected removeCoupon(): void {
    this.cartService.removeCoupon().subscribe(() => this.couponMsg.set(''));
  }
}
