import { Pipe, PipeTransform } from '@angular/core';

/** 24990000 → "24.990.000₫" */
@Pipe({ name: 'vnd' })
export class VndPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return new Intl.NumberFormat('vi-VN').format(value ?? 0) + '₫';
  }
}
