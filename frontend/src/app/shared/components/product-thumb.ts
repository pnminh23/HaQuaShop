import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { assetUrl } from '../../core/config/api.config';
import { ProductKind } from '../utils/product-kind';

/** Ảnh sản phẩm; nếu chưa có ảnh thật thì vẽ hình minh họa theo loại thiết bị. */
@Component({
  selector: 'app-product-thumb',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="thumb" [style.background]="tone()" [style.aspect-ratio]="ratio()" [style.border-radius]="radius()">
      @if (src(); as s) {
        <img [src]="s" [alt]="alt()" loading="lazy" />
      } @else {
        <svg viewBox="0 0 100 100" [style.width]="size()" role="img" [attr.aria-label]="alt()">
          @switch (kind()) {
            @case ('phone') {
              <rect x="34" y="12" width="32" height="76" rx="7" fill="#2c4231" />
              <rect x="37" y="18" width="26" height="62" rx="3" fill="#cfdcc9" />
              <circle cx="50" cy="84" r="1.8" fill="#cfdcc9" />
            }
            @case ('audio') {
              <path d="M24 56v-8a26 26 0 0152 0v8" fill="none" stroke="#2c4231" stroke-width="6" stroke-linecap="round" />
              <rect x="18" y="52" width="14" height="24" rx="6" fill="#3e5a43" />
              <rect x="68" y="52" width="14" height="24" rx="6" fill="#3e5a43" />
            }
            @default {
              <rect x="20" y="24" width="60" height="38" rx="4" fill="#2c4231" />
              <rect x="24" y="28" width="52" height="30" rx="2" fill="#cfdcc9" />
              <path d="M12 68h76l-6 8H18z" fill="#3e5a43" />
            }
          }
        </svg>
      }
    </div>
  `,
  styles: `
    .thumb { display: flex; align-items: center; justify-content: center; overflow: hidden; width: 100%; }
    img { width: 100%; height: 100%; object-fit: cover; }
    svg { max-height: 100%; }
  `,
})
export class ProductThumb {
  readonly kind = input<ProductKind>('laptop');
  readonly imageUrl = input<string | null | undefined>(null);
  readonly alt = input('');
  readonly tone = input('#E7ECE2');
  readonly ratio = input('1 / 1');
  readonly radius = input('18px');
  readonly size = input('58%');

  protected readonly src = computed(() => assetUrl(this.imageUrl()));
}
