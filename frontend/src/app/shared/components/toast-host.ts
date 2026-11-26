import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-toast-host',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <div class="host" role="status" aria-live="polite">
      @for (t of toast.toasts(); track t.id) {
        <div class="toast" animate.enter="toast-in" animate.leave="toast-out">
          <span>{{ t.message }}</span>
          @if (t.link) { <a [routerLink]="t.link.href">{{ t.link.label }}</a> }
        </div>
      }
    </div>
  `,
  styles: `
    .host { position: fixed; left: 50%; bottom: 24px; transform: translateX(-50%); z-index: 100; display: flex; flex-direction: column; gap: 8px; align-items: center; }
    .toast { background: var(--ink); color: #fff; padding: 12px 20px; border-radius: 999px; display: flex; gap: 16px; font-size: 14px; box-shadow: 0 8px 24px rgba(0,0,0,.2); }
    .toast a { color: #cfe3c8; font-weight: 600; }
  `,
})
export class ToastHost {
  protected readonly toast = inject(ToastService);
}
