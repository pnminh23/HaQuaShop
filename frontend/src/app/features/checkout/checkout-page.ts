import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CheckoutRequest, CheckoutResponse, PaymentMethod, ShippingMethod } from '../../core/models/cart.model';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { SeoService } from '../../core/services/seo.service';
import { VndPipe } from '../../shared/pipes/vnd.pipe';

const SHIPPING: { id: ShippingMethod; title: string; desc: string }[] = [
  { id: 'STANDARD', title: 'Giao tiêu chuẩn', desc: '1–3 ngày · miễn phí từ 500.000₫' },
  { id: 'EXPRESS_2H', title: 'Giao nhanh 2 giờ', desc: 'Chỉ Hà Nội và TP.HCM' },
  { id: 'STORE_PICKUP', title: 'Nhận tại cửa hàng', desc: 'Miễn phí, nhận sau khi được xác nhận' },
];

const PAYMENT: { id: PaymentMethod; title: string; desc: string }[] = [
  { id: 'COD', title: 'Thanh toán khi nhận hàng (COD)', desc: 'Trả tiền mặt cho shipper' },
  { id: 'QR', title: 'Chuyển khoản QR', desc: 'Đơn tự xác nhận sau khi nhận tiền' },
  { id: 'EWALLET', title: 'Ví điện tử', desc: 'Chuyển sang cổng thanh toán' },
  { id: 'CARD', title: 'Thẻ Visa / Mastercard', desc: 'Nhập thẻ trên cổng thanh toán, HaQua không lưu số thẻ' },
  { id: 'INSTALLMENT', title: 'Trả góp 0%', desc: 'Qua thẻ tín dụng, kỳ hạn 3–12 tháng' },
];

/** UC08 – đặt hàng và thanh toán. */
@Component({
  selector: 'app-checkout-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, VndPipe],
  templateUrl: './checkout-page.html',
  styleUrl: './checkout-page.scss',
})
export class CheckoutPage {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly cartService = inject(CartService);
  private readonly orders = inject(OrderService);
  private readonly auth = inject(AuthService);

  protected readonly cart = this.cartService.cart;
  protected readonly lines = computed(() => this.cart().items.filter((i) => i.selected && !i.savedForLater));
  protected readonly shippingOptions = SHIPPING;
  protected readonly paymentOptions = PAYMENT;
  protected readonly terms = [3, 6, 12];

  protected readonly provinces = ['Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng', 'Tỉnh/thành khác'];
  protected readonly invoice = signal(false);
  protected readonly submitting = signal(false);
  protected readonly error = signal('');
  protected readonly done = signal<CheckoutResponse | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    receiverName: ['', Validators.required],
    phone: ['', [Validators.required, Validators.pattern(/^0\d{9}$/)]],
    email: ['', Validators.email],
    province: ['Hà Nội', Validators.required],
    ward: ['', Validators.required],
    detail: ['', Validators.required],
    note: [''],
    shippingMethod: ['STANDARD' as ShippingMethod],
    paymentMethod: ['COD' as PaymentMethod],
    installmentMonths: [12],
    taxCode: [''],
    company: [''],
    invoiceEmail: [''],
  });

  private readonly shipping = signal<ShippingMethod>('STANDARD');
  /** Phí giao thay đổi theo phương thức; server tính lại khi đặt hàng. */
  protected readonly shippingFee = computed(() => {
    const sub = this.cart().summary.subtotal;
    return { STANDARD: sub >= 500_000 ? 0 : 30_000, EXPRESS_2H: 30_000, STORE_PICKUP: 0 }[this.shipping()];
  });
  protected readonly total = computed(() => this.cart().summary.subtotal - this.cart().summary.couponDiscount + this.shippingFee());

  constructor() {
    inject(SeoService).set('Thanh toán | HaQua');
    this.cartService.load().subscribe();
    this.form.controls.shippingMethod.valueChanges.subscribe((m) => this.shipping.set(m));
    effect(() => {
      // thành viên: điền sẵn tên + địa chỉ mặc định
      if (this.auth.isLoggedIn() && !this.form.controls.receiverName.value) {
        this.auth.profile().subscribe((p) => {
          const a = p.addresses.find((x) => x.defaultAddress) ?? p.addresses[0];
          this.form.patchValue(a ? { receiverName: a.receiverName, phone: a.phone, province: a.province, ward: a.ward, detail: a.detail }
            : { receiverName: p.fullName, phone: p.phone, email: p.email });
        });
      }
    });
  }

  protected invalid(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  protected place(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.lines().length) return;
    const v = this.form.getRawValue();
    const req: CheckoutRequest = {
      receiverName: v.receiverName,
      phone: v.phone,
      email: v.email || undefined,
      province: v.province,
      ward: v.ward,
      detail: v.detail,
      note: v.note || undefined,
      shippingMethod: v.shippingMethod,
      paymentMethod: v.paymentMethod,
      installmentMonths: v.paymentMethod === 'INSTALLMENT' ? v.installmentMonths : undefined,
      invoice: this.invoice() ? { taxCode: v.taxCode, company: v.company, email: v.invoiceEmail } : undefined,
    };
    this.submitting.set(true);
    this.error.set('');
    this.orders.checkout(req).subscribe({
      next: (res) => {
        this.submitting.set(false);
        if (res.redirectUrl) {
          // Thẻ / ví / trả góp: sang trang của cổng thanh toán
          window.location.href = res.redirectUrl;
          return;
        }
        this.done.set(res);
      },
      error: (e) => {
        this.submitting.set(false);
        this.error.set(e.error?.detail ?? 'Không đặt được hàng, vui lòng thử lại');
      },
    });
  }

  protected backToCart(): void {
    void this.router.navigateByUrl('/gio-hang');
  }
}
