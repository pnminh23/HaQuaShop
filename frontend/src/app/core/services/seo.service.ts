import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';

/** Cập nhật title, meta description, canonical và JSON-LD cho từng trang (hiển thị được cả khi SSR). */
@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly doc = inject(DOCUMENT);

  set(title: string, description?: string, canonicalPath?: string): void {
    this.title.setTitle(title);
    if (description) {
      this.meta.updateTag({ name: 'description', content: description });
      this.meta.updateTag({ property: 'og:description', content: description });
    }
    this.meta.updateTag({ property: 'og:title', content: title });
    if (canonicalPath) this.setCanonical(canonicalPath);
  }

  setJsonLd(data: object | null): void {
    this.doc.getElementById('json-ld')?.remove();
    if (!data) return;
    const script = this.doc.createElement('script');
    script.id = 'json-ld';
    script.type = 'application/ld+json';
    script.text = JSON.stringify(data);
    this.doc.head.appendChild(script);
  }

  private setCanonical(path: string): void {
    let link = this.doc.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.doc.createElement('link');
      link.rel = 'canonical';
      this.doc.head.appendChild(link);
    }
    link.href = path;
  }
}
