import { DialogService } from '../../core/services/dialog.service';
import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AdminApiService } from '../../core/services/admin-api.service';
import { OrderDetail } from '../../core/models/order.model';
import { ToastService } from '../../core/services/toast.service';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { STATUS_LABEL } from '../orders/order-lookup';

/** Nội dung một đơn hàng + thao tác xử lý, dùng chung cho popup xem nhanh và trang chi tiết. */
@Component({
  selector: 'app-order-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, VndPipe],
  styleUrls: ['./admin.scss', './order-view.scss'],
  template: `
    @let o = order();
    <div class="stack">
      <span class="chip" [class]="'status-' + o.status">{{ label(o.status) }}</span>
      <ol class="tl">@for (h of o.history; track h.changedAt) { <li><strong>{{ label(h.toStatus) }}</strong> <span class="muted">{{ h.changedAt | date: 'dd/MM HH:mm' }} {{ h.note ? '· ' + h.note : '' }}</span></li> }</ol>
      <div><strong>{{ o.receiverName }} · {{ o.phone }}</strong><br />{{ o.shippingAddress }}<br /><span class="muted">Thanh toán: {{ o.paymentMethod }} · {{ o.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán' }}</span></div>
      <ul>@for (i of o.items; track i.productName) { <li class="row between top"><span>{{ i.productName }} <small class="muted">{{ i.variantLabel }} × {{ i.qty }}</small></span><strong>{{ i.lineTotal | vnd }}</strong></li> }</ul>
      <div class="row between"><span>Tạm tính {{ o.subtotal | vnd }} · Giảm {{ o.discount | vnd }} · Ship {{ o.shippingFee | vnd }}</span><strong>{{ o.total | vnd }}</strong></div>
    </div>
    <div class="actions" [class.sticky]="sticky()" role="toolbar" aria-label="Xử lý đơn hàng">
      <ng-content select="[leading]" />
      <span class="grow"></span>
      @if (o.canAdvance && o.nextAction) { <button type="button" class="btn primary" (click)="advance()">{{ o.nextAction }}</button> }
      @if (canCancel()) { <button type="button" class="btn danger" (click)="cancel()">Hủy đơn</button> }
    </div>
  `,
})
export class OrderView {
  private readonly dialog = inject(DialogService);
  private readonly api = inject(AdminApiService);
  private readonly toast = inject(ToastService);

  readonly order = input.required<OrderDetail>();
  /** Ghim thanh thao tác ở đáy khung nhìn (trang chi tiết). */
  readonly sticky = input(false);
  /** Phát ra đơn mới sau mỗi thao tác để nơi chứa cập nhật lại danh sách. */
  readonly changed = output<OrderDetail>();

  protected readonly label = (s: string) => STATUS_LABEL[s] ?? s;

  protected canCancel(): boolean { return ['PENDING', 'CONFIRMED', 'PACKING'].includes(this.order().status); }

  protected advance(): void {
    const o = this.order();
    this.api.advanceOrder(o.id).subscribe({
      next: (r) => this.done(r, 'Đã cập nhật trạng thái'),
      error: (e) => {
        this.toast.show(e.error?.detail ?? 'Không chuyển được trạng thái');
        this.api.order(o.id).subscribe((fresh) => this.changed.emit(fresh)); // đơn có thể đã bị người khác đổi → tải lại
      },
    });
  }

  protected async cancel(): Promise<void> {
    const reason = await this.dialog.prompt({ title: 'Hủy đơn hàng', message: 'Đơn sẽ được hủy và hoàn lại tồn kho.', confirmLabel: 'Hủy đơn', cancelLabel: 'Đóng', danger: true, input: { label: 'Lý do hủy đơn', placeholder: 'VD: Khách đổi ý' } });
    if (!reason) return;
    this.api.cancelOrder(this.order().id, reason).subscribe({
      next: (r) => this.done(r, 'Đã hủy đơn, hoàn tồn kho'),
      error: (e) => this.toast.show(e.error?.detail ?? 'Không hủy được đơn'),
    });
  }

  private done(o: OrderDetail, message: string): void { this.toast.show(message); this.changed.emit(o); }
}
