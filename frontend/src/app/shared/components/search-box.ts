import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, output } from '@angular/core';
import { Subject, debounce, distinctUntilChanged, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

/**
 * Ô tìm kiếm dùng chung cho mọi bảng quản trị: gõ xong ~350ms tự phát `search`, không cần bấm Enter.
 * Enter vẫn tìm ngay (không chờ debounce).
 */
@Component({
  selector: 'app-search-box',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<input type="search" class="input" [attr.aria-label]="label()" [placeholder]="placeholder()"
    (input)="typed($any($event.target).value)" (keydown.enter)="now($any($event.target).value)" />`,
  styles: `:host { display: block; width: 100%; max-width: 320px; } input { width: 100%; }`,
})
export class SearchBox {
  readonly placeholder = input('Tìm kiếm…');
  readonly label = input('Tìm kiếm');
  readonly delay = input(350);
  readonly search = output<string>();

  private readonly typing = new Subject<string>();
  private last = '';

  constructor() {
    this.typing
      .pipe(debounce(() => timer(this.delay())), distinctUntilChanged(), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe((v) => this.emit(v));
  }

  protected typed(v: string): void { this.typing.next(v.trim()); }
  protected now(v: string): void { this.emit(v.trim()); }

  private emit(v: string): void {
    if (v === this.last) return; // tránh gọi API lặp khi Enter ngay sau debounce
    this.last = v;
    this.search.emit(v);
  }
}
