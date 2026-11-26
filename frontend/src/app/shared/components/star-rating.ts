import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-star-rating',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="stars" [attr.aria-label]="'Đánh giá ' + value() + ' trên 5 sao'">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="#C98A1B" aria-hidden="true">
        <path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z" />
      </svg>
      <strong>{{ value().toString().replace('.', ',') }}</strong>
      @if (count() !== null) { <span class="muted">({{ count() }})</span> }
    </span>
  `,
  styles: `.stars { display: inline-flex; align-items: center; gap: 4px; font-size: 14px; }`,
})
export class StarRating {
  readonly value = input.required<number>();
  readonly count = input<number | null>(null);
}
