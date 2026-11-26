import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductCard } from '../../core/models/catalog.model';
import { VndPipe } from '../pipes/vnd.pipe';
import { kindOf, toneOf } from '../utils/product-kind';
import { ProductThumb } from './product-thumb';
import { StarRating } from './star-rating';

@Component({
  selector: 'app-product-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ProductThumb, StarRating, VndPipe],
  template: `
    <article class="card-product">
      <a [routerLink]="['/san-pham', product().slug]" class="media" [attr.aria-label]="product().name">
        <app-product-thumb [kind]="kind()" [imageUrl]="product().imageUrl" [alt]="product().name" [tone]="tone()" ratio="4 / 3" radius="0" />
        @if (badge(); as b) { <span class="badge" [class.new]="!product().discountPercent">{{ b }}</span> }
      </a>
      <div class="body">
        <span class="cat">{{ product().categoryName }}</span>
        <h3><a [routerLink]="['/san-pham', product().slug]">{{ product().name }}</a></h3>
        <app-star-rating [value]="product().rating" [count]="product().reviewCount" />
        <div class="price-row">
          <span class="price">{{ product().price | vnd }}</span>
          @if (product().originalPrice) { <s class="old-price">{{ product().originalPrice | vnd }}</s> }
        </div>
        <button type="button" class="btn sm block" [disabled]="soldOut()" (click)="add.emit(product())">
          {{ soldOut() ? 'Hết hàng' : 'Thêm vào giỏ' }}
        </button>
      </div>
    </article>
  `,
  styleUrl: './product-card.scss',
})
export class ProductCardComponent {
  readonly product = input.required<ProductCard>();
  readonly add = output<ProductCard>();

  protected readonly kind = computed(() => kindOf(this.product().categorySlug));
  protected readonly tone = computed(() => toneOf(this.product().id));
  protected readonly soldOut = computed(() => this.product().status === 'OUT_OF_STOCK' || !this.product().defaultVariantId);
  protected readonly badge = computed(() =>
    this.product().discountPercent ? `-${this.product().discountPercent}%` : this.product().soldCount < 100 && this.product().id > 8 ? 'Mới' : '',
  );
}
