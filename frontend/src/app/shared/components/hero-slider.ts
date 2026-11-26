import { ChangeDetectionStrategy, Component, DestroyRef, afterNextRender, computed, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { assetUrl } from '../../core/config/api.config';
import { Banner } from '../../core/models/banner.model';

const INTERVAL = 6000;

/** Banner trang chủ dạng slideshow: tự chạy, tạm dừng khi rê chuột/focus, hỗ trợ vuốt, phím mũi tên và dấu chấm điều hướng. */
@Component({
  selector: 'app-hero-slider',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './hero-slider.html',
  styleUrl: './hero-slider.scss',
  host: { '(keydown.arrowleft)': 'go(-1)', '(keydown.arrowright)': 'go(1)' },
})
export class HeroSlider {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly banners = input.required<Banner[]>();

  protected readonly index = signal(0);
  protected readonly prev = signal(-1);
  protected readonly dir = signal(1);
  protected readonly paused = signal(false);
  protected readonly started = signal(false);
  protected readonly count = computed(() => this.banners().length);
  protected readonly src = (b: Banner) => assetUrl(b.imageUrl);

  private timer: ReturnType<typeof setTimeout> | undefined;
  private touchX: number | null = null;

  constructor() {
    afterNextRender(() => {
      this.started.set(true);
      this.schedule();
      this.destroyRef.onDestroy(() => clearTimeout(this.timer));
    });
  }

  protected go(step: number): void {
    const n = this.count();
    if (n < 2) return;
    this.move((this.index() + step + n) % n, step);
  }

  protected goto(i: number): void {
    if (i === this.index()) return;
    this.move(i, i > this.index() ? 1 : -1);
  }

  private move(to: number, dir: number): void {
    this.prev.set(this.index());
    this.dir.set(dir);
    this.index.set(to);
    this.schedule();
  }

  protected pause(v: boolean): void {
    this.paused.set(v);
    this.schedule();
  }

  protected open(e: Event, b: Banner): void {
    const link = b.linkUrl;
    if (link?.startsWith('/')) {
      e.preventDefault();
      void this.router.navigateByUrl(link);
    }
  }

  protected down(e: PointerEvent): void { this.touchX = e.clientX; }
  protected up(e: PointerEvent): void {
    if (this.touchX === null) return;
    const dx = e.clientX - this.touchX;
    this.touchX = null;
    if (Math.abs(dx) > 48) this.go(dx < 0 ? 1 : -1);
  }

  private schedule(): void {
    clearTimeout(this.timer);
    if (!this.started() || this.paused() || this.count() < 2) return;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    this.timer = setTimeout(() => this.go(1), INTERVAL);
  }
}
