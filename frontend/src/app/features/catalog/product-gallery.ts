import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, afterRenderEffect, computed, inject, input, model, signal, viewChild,
} from '@angular/core';
import { ProductDetail } from '../../core/models/catalog.model';
import { ProductKind } from '../../shared/utils/product-kind';
import { ProductThumb } from '../../shared/components/product-thumb';

/**
 * Bộ ảnh trang chi tiết: ảnh lớn trượt ngang (mũi tên, vuốt, phím ← →) và đúng một hàng ảnh nhỏ,
 * tràn thì cuộn bằng mũi tên chứ không xuống dòng.
 */
@Component({
  selector: 'app-product-gallery',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ProductThumb],
  templateUrl: './product-gallery.html',
  styleUrl: './product-gallery.scss',
})
export class ProductGallery {
  readonly images = input.required<ProductDetail['images']>();
  readonly name = input('');
  readonly kind = input<ProductKind>('laptop');
  readonly tone = input('#E7ECE2');
  readonly index = model(0);

  /** Chưa có ảnh vẫn giữ một khung để hiện hình minh họa. */
  protected readonly slides = computed(() => (this.images().length ? this.images() : [null]));
  protected readonly many = computed(() => this.images().length > 1);

  private readonly strip = viewChild<ElementRef<HTMLElement>>('strip');
  protected readonly canBack = signal(false);
  protected readonly canForward = signal(false);
  private swipeX: number | null = null;

  constructor() {
    // theo dõi kích thước dải ảnh nhỏ để biết khi nào cần mũi tên
    const observer = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(() => this.measure());
    inject(DestroyRef).onDestroy(() => observer?.disconnect());
    afterRenderEffect((onCleanup) => {
      const el = this.strip()?.nativeElement;
      this.images();
      if (!el) return;
      observer?.observe(el);
      this.measure();
      onCleanup(() => observer?.unobserve(el));
    });
    // ảnh đang chọn luôn nằm trong vùng nhìn thấy (chỉ cuộn dải ảnh, không cuộn trang)
    afterRenderEffect(() => {
      const el = this.strip()?.nativeElement;
      const item = el?.children[this.index()] as HTMLElement | undefined;
      if (!el || !item) return;
      const left = item.offsetLeft;
      const right = left + item.offsetWidth;
      if (left < el.scrollLeft) el.scrollTo({ left, behavior: 'smooth' });
      else if (right > el.scrollLeft + el.clientWidth) el.scrollTo({ left: right - el.clientWidth, behavior: 'smooth' });
    });
  }

  /** Đi tới ảnh trước/sau, hết thì quay vòng. */
  protected step(delta: number): void {
    const n = this.images().length;
    if (n > 1) this.index.set((this.index() + delta + n) % n);
  }

  protected page(dir: 1 | -1): void {
    const el = this.strip()?.nativeElement;
    el?.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: 'smooth' });
  }

  protected measure(): void {
    const el = this.strip()?.nativeElement;
    if (!el) return;
    this.canBack.set(el.scrollLeft > 1);
    this.canForward.set(el.scrollLeft + el.clientWidth < el.scrollWidth - 1);
  }

  protected swipeStart(e: PointerEvent): void {
    this.swipeX = (e.target as HTMLElement).closest('button') ? null : e.clientX;
  }

  protected swipeEnd(e: PointerEvent): void {
    if (this.swipeX === null) return;
    const dx = e.clientX - this.swipeX;
    this.swipeX = null;
    if (Math.abs(dx) > 40) this.step(dx < 0 ? 1 : -1);
  }
}
