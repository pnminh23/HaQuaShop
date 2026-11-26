export type ProductKind = 'laptop' | 'phone' | 'audio';

const BY_CATEGORY: Record<string, ProductKind> = {
  'may-tinh': 'laptop',
  'dien-thoai': 'phone',
  'tai-nghe': 'audio',
};

export function kindOf(categorySlug: string): ProductKind {
  return BY_CATEGORY[categorySlug] ?? 'laptop';
}

const TONES = ['#E7ECE2', '#E5EBE4', '#EDEEE6', '#E3E8DE'];

/** Màu nền ổn định theo id để lưới sản phẩm không đơn điệu. */
export function toneOf(id: number): string {
  return TONES[id % TONES.length];
}
