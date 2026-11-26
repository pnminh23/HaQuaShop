import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { DialogService } from '../../core/services/dialog.service';

@Component({
  selector: 'app-dialog-host',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (dialog.active(); as d) {
      <div class="backdrop" animate.enter="drawer-in" animate.leave="drawer-out" (click)="dialog.close(null)" (keydown.escape)="dialog.close(null)">
        <div class="box" role="alertdialog" aria-modal="true" aria-labelledby="dlg-title" aria-describedby="dlg-msg" (click)="$event.stopPropagation()">
          <h2 id="dlg-title">{{ d.title ?? (d.danger ? 'Xác nhận thao tác' : 'Xác nhận') }}</h2>
          <p id="dlg-msg">{{ d.message }}</p>
          @if (d.input; as inp) {
            <label class="field">{{ inp.label }}
              <input #field class="input" [placeholder]="inp.placeholder ?? ''" [value]="text()" (input)="text.set($any($event.target).value)" (keydown.enter)="submit(d)" />
            </label>
          }
          <div class="actions">
            <button type="button" class="btn" (click)="dialog.close(null)">{{ d.cancelLabel ?? 'Hủy' }}</button>
            <button #ok type="button" class="btn primary" [class.danger-fill]="d.danger" [disabled]="!!d.input && d.input.required !== false && !text().trim()" (click)="submit(d)">{{ d.confirmLabel ?? 'Đồng ý' }}</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .backdrop { position: fixed; inset: 0; z-index: 200; background: rgba(24, 33, 27, .5); display: flex; align-items: center; justify-content: center; padding: 24px; }
    .box { width: min(440px, 100%); background: var(--surface); border-radius: 12px; padding: 24px; display: flex; flex-direction: column; gap: 14px; box-shadow: 0 20px 50px rgba(0, 0, 0, .25); }
    h2 { font-size: 18px; }
    p { color: var(--ink-2); }
    .actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 6px; }
    .danger-fill { background: var(--danger); border-color: var(--danger); &:hover:not(:disabled) { background: #8f1e18; } }
  `,
})
export class DialogHost {
  protected readonly dialog = inject(DialogService);
  protected readonly text = signal('');
  private readonly field = viewChild<ElementRef<HTMLInputElement>>('field');
  private readonly ok = viewChild<ElementRef<HTMLButtonElement>>('ok');

  constructor() {
    effect(() => {
      const d = this.dialog.active();
      this.text.set('');
      // đặt focus vào ô nhập (nếu có) hoặc nút xác nhận để Enter/Esc dùng được ngay
      queueMicrotask(() => (this.field() ?? this.ok())?.nativeElement.focus());
      void d;
    });
  }

  protected submit(d: { input?: unknown }): void {
    if (d.input) { if (this.text().trim()) this.dialog.close(this.text().trim()); } else this.dialog.close(true);
  }
}
