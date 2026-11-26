import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

export interface FilterOption { value: string; label: string }

/** Tiêu đề cột kèm bộ lọc ngay trên header bảng: `<th appFilter label="Trạng thái" [options]="..." [(value)]="x">`. */
@Component({
  selector: 'th[appFilter]',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="th-filter">
    <span>{{ label() }}</span>
    <select [attr.aria-label]="'Lọc theo ' + label()" [class.on]="!!value()" (change)="value.set($any($event.target).value)">
      <option value="" [selected]="!value()">Tất cả</option>
      @for (o of options(); track o.value) { <option [value]="o.value" [selected]="o.value === value()">{{ o.label }}</option> }
    </select>
  </div>`,
  styles: `
    .th-filter { display: flex; flex-direction: column; gap: 4px; align-items: flex-start; }
    select { height: 28px; max-width: 150px; border: 1px solid var(--border); border-radius: 4px; background: var(--surface); padding: 0 6px; font-size: 13px; font-weight: 500; color: var(--ink-2); cursor: pointer;
      &.on { border-color: var(--green-600); background: var(--green-50); color: var(--green-700); font-weight: 600; } }
  `,
})
export class ThFilter {
  readonly label = input.required<string>();
  readonly options = input.required<FilterOption[]>();
  readonly value = model('');
}
