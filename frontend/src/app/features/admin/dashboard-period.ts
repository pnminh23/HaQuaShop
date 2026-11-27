/** Ngày theo lịch máy người dùng, dạng yyyy-MM-dd (toISOString sẽ lệch ngày vì đổi sang UTC). */
export function isoDate(d: Date): string {
  const two = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${two(d.getMonth() + 1)}-${two(d.getDate())}`;
}

export interface MonthOption { value: string; label: string; from: string; to: string }

/** `count` tháng gần nhất, mới nhất trước. Tháng hiện tại chỉ tính đến hôm nay. */
export function monthOptions(today: Date, count: number): MonthOption[] {
  return Array.from({ length: count }, (_, i) => {
    const first = new Date(today.getFullYear(), today.getMonth() - i, 1);
    const last = i === 0 ? today : new Date(first.getFullYear(), first.getMonth() + 1, 0);
    const from = isoDate(first);
    return { value: from.slice(0, 7), label: `Tháng ${first.getMonth() + 1}/${first.getFullYear()}`, from, to: isoDate(last) };
  });
}

/** Thông báo lỗi cho khoảng ngày tự chọn, '' nếu hợp lệ. Backend kiểm tra lại cùng quy tắc. */
export function rangeError(from: string, to: string, today: string): string {
  if (!from || !to) return 'Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc';
  if (from > to) return 'Ngày bắt đầu phải trước hoặc trùng ngày kết thúc';
  if (from > today) return 'Ngày bắt đầu không được ở tương lai';
  return '';
}
