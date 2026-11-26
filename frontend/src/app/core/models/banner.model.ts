export interface Banner {
  id: number;
  title: string;
  subtitle: string | null;
  imageUrl: string;
  altText: string | null;
  linkUrl: string | null;
  ctaLabel: string | null;
  sortOrder: number;
  active: boolean;
}

export type BannerRequest = Pick<Banner, 'title' | 'subtitle' | 'imageUrl' | 'altText' | 'linkUrl' | 'ctaLabel' | 'active'>;
