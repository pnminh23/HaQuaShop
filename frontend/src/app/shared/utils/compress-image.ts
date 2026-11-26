/**
 * Thu nhỏ (cạnh dài ≤ maxSize px) và nén sang WebP, tự giảm chất lượng rồi giảm kích thước
 * cho tới khi ≤ maxBytes (mặc định 450 KB, dưới giới hạn upload của server).
 */
export async function compressImage(file: File, maxSize = 1200, quality = 0.85, maxBytes = 450 * 1024): Promise<Blob> {
  if (!file.type.startsWith('image/')) throw { error: { detail: 'Chỉ nhận file ảnh' } };
  let bmp: ImageBitmap;
  try {
    bmp = await createImageBitmap(file);
  } catch {
    throw { error: { detail: 'Trình duyệt không đọc được định dạng ảnh này (hãy dùng JPG, PNG hoặc WebP)' } };
  }
  try {
    let size = maxSize;
    let q = quality;
    for (let attempt = 0; attempt < 8; attempt++) {
      const scale = Math.min(1, size / Math.max(bmp.width, bmp.height));
      const canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(bmp.width * scale));
      canvas.height = Math.max(1, Math.round(bmp.height * scale));
      canvas.getContext('2d')!.drawImage(bmp, 0, 0, canvas.width, canvas.height);
      const blob = await new Promise<Blob | null>((res) => canvas.toBlob(res, 'image/webp', q));
      if (!blob) throw { error: { detail: 'Không xử lý được ảnh' } };
      if (blob.size <= maxBytes) return blob;
      if (q > 0.6) q -= 0.1; else size = Math.round(size * 0.8);
    }
    throw { error: { detail: 'Ảnh quá lớn, không nén xuống được dưới 450 KB' } };
  } finally {
    bmp.close();
  }
}
