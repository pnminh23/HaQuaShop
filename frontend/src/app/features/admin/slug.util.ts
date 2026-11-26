/** Cùng quy tắc với SlugUtil ở backend: bỏ dấu tiếng Việt, nối bằng dấu "-". */
export const SlugUtil = {
  toSlug(input: string): string {
    return input
      .replace(/đ/g, 'd').replace(/Đ/g, 'D')
      .normalize('NFD').replace(/[̀-ͯ]/g, '')
      .toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
  },
};
