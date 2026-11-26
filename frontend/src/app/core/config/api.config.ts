import { environment } from '../../../environments/environment';

export const API_URL = environment.apiUrl;

/** Biến đường dẫn ảnh tương đối (/uploads/...) thành URL đầy đủ. */
export function assetUrl(path: string | null | undefined): string | null {
  if (!path) return null;
  return path.startsWith('http') ? path : environment.assetsUrl + path;
}
