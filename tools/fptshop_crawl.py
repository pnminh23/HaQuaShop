#!/usr/bin/env python3
"""Lấy dữ liệu sản phẩm công khai từ fptshop.com.vn (thông số + ảnh) theo sitemap sản phẩm.

Chỉ đọc trang sản phẩm công khai, tuân thủ robots.txt, 1 request/lần, nghỉ giữa các lần, dừng khi gặp 403/429.
Đầu ra: tools/out/<danh-muc>.csv và ảnh trong tools/out/images/<danh-muc>/. Có ghi giá niêm yết công khai (price_ref) làm mốc tham chiếu thị trường.
Dùng:  python3 tools/fptshop_crawl.py may-tinh-xach-tay --limit 30 --delay 2.5
"""
import argparse, csv, json, re, sys, time, urllib.request, urllib.error, urllib.robotparser
from pathlib import Path
from urllib.parse import urlparse

BASE = 'https://fptshop.com.vn'
UA = 'Mozilla/5.0 (compatible; HaQuaCatalogTool/1.0)'
OUT = Path(__file__).parent / 'out'


class Stop(Exception):
    pass


def get(url: str, binary=False):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            data = r.read()
            return data if binary else data.decode('utf-8', 'replace')
    except urllib.error.HTTPError as e:
        if e.code in (403, 429):
            raise Stop(f'HTTP {e.code} tại {url} – dừng để không làm phiền máy chủ')
        raise


def product_ld(html: str):
    for m in re.finditer(r'<script[^>]*ld\+json[^>]*>(.*?)</script>', html, re.S):
        try:
            d = json.loads(m.group(1))
        except ValueError:
            continue
        for x in (d if isinstance(d, list) else [d]):
            if isinstance(x, dict) and x.get('@type') == 'Product':
                return x
    return None


MAX_IMAGES = 6


def gallery_urls(html: str, main: str, slug: str = ''):
    """Ảnh gallery của sản phẩm = mọi ảnh cùng tên gốc với ảnh chính (…_den_1, …_den_2, …), sắp theo số thứ tự."""
    fn = main.rsplit('/', 1)[-1]
    rules = [r'_\d+_[0-9a-f]{8,12}\.\w+$', r'-\d+\.\w+$']
    for rule in rules:
        stem = re.sub(rule, '', fn)
        if stem == fn:
            continue
        found = set(m.group(0) for m in re.finditer(r'https://cdn2\.fptshop\.com\.vn/unsafe/' + re.escape(stem) + r'(?:_\d+_[0-9a-f]{8,12}|-\d+)\.\w+', html))
        if len(found) >= 2:
            def num(u):
                m = re.search(r'(?:_|-)(\d+)(?:_[0-9a-f]{8,12})?\.\w+$', u)
                return int(m.group(1)) if m else 0
            return sorted(found, key=num)[:MAX_IMAGES]
    return fallback_group(html, main, slug)


def fallback_group(html: str, main: str, slug: str):
    """Ảnh chính không có số thứ tự: gom ảnh trong trang theo tên gốc, chọn nhóm có tên khớp slug sản phẩm nhất."""
    toks = set(t for t in re.split(r'[^a-z0-9]+', slug.lower()) if t)
    groups = {}
    for m in re.finditer(r'https://cdn2\.fptshop\.com\.vn/unsafe/(?:800x0/)?(([A-Za-z0-9_\-]+?)_(\d+|[a-z])_[0-9a-f]{8,12}\.(?:png|jpg|webp))', html):
        groups.setdefault(m.group(2).lower(), {})[m.group(0)] = m.group(3)
    best, score = None, (1, 2)
    for stem, urls in groups.items():
        overlap = len(toks & set(t for t in re.split(r'[^a-z0-9]+', stem) if t))
        if (overlap, len(urls)) > score:
            best, score = stem, (overlap, len(urls))
    if not best:
        return [main]
    key = lambda kv: (0, int(kv[1])) if kv[1].isdigit() else (1, kv[1])
    ordered = [u for u, _ in sorted(groups[best].items(), key=key)]
    return ([main] + [u for u in ordered if u != main])[:MAX_IMAGES]


def refill(a, rp):
    path = OUT / f'{a.category}.csv'
    rows = list(csv.DictReader(path.open(encoding='utf-8-sig')))
    img_dir = OUT / 'images' / a.category
    changed = 0
    try:
        for r in rows:
            if len(json.loads(r.get('images_json') or '[]')) > 1 or not rp.can_fetch(UA, r['source_url']):
                continue
            time.sleep(a.delay)
            html = get(r['source_url'])
            slug = urlparse(r['source_url']).path.rstrip('/').split('/')[-1]
            locals_ = []
            for n, gu in enumerate(gallery_urls(html, r['image_source'], slug), 1):
                ext = Path(urlparse(gu).path).suffix or '.png'
                f = img_dir / f'{slug}-{n}{ext}'
                if not f.exists():
                    time.sleep(a.delay / 2)
                    f.write_bytes(get(gu, binary=True))
                locals_.append(str(f.relative_to(OUT)))
            if len(locals_) > 1:
                r['images_json'] = json.dumps(locals_); r['image_file'] = locals_[0]; changed += 1
                print(f'  {r["name"]}: {len(locals_)} ảnh')
    except Stop as e:
        print(e, file=sys.stderr)
    with path.open('w', newline='', encoding='utf-8-sig') as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys())); w.writeheader(); w.writerows(rows)
    print('Bổ sung gallery cho', changed, 'sản phẩm ->', path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('category', help='vd: dien-thoai, may-tinh-xach-tay, smartwatch, may-tinh-bang')
    ap.add_argument('--sitemap', help='tên sitemap nguồn nếu khác danh mục, vd: phu-kien')
    ap.add_argument('--match', default='', help='chỉ lấy URL chứa chuỗi này, vd: /phu-kien/tai-nghe')
    ap.add_argument('--refill', action='store_true', help='chỉ lấy lại gallery cho dòng CSV hiện có ≤1 ảnh (không duyệt sitemap)')
    ap.add_argument('--append', action='store_true', help='giữ các dòng CSV đã có, lấy thêm --limit sản phẩm mới')
    ap.add_argument('--limit', type=int, default=30)
    ap.add_argument('--delay', type=float, default=2.5)
    a = ap.parse_args()

    rp = urllib.robotparser.RobotFileParser()
    rp.parse(get(BASE + '/robots.txt').splitlines())

    if a.refill:
        return refill(a, rp)
    sm = get(f'{BASE}/products/sitemap-{a.sitemap or a.category}.xml')
    urls = [u for u in re.findall(r'<loc>([^<]+)</loc>', sm) if urlparse(u).path.count('/') >= 2 and a.match in u]
    img_dir = OUT / 'images' / a.category
    img_dir.mkdir(parents=True, exist_ok=True)
    out = OUT / f'{a.category}.csv'
    old = list(csv.DictReader(out.open(encoding='utf-8-sig'))) if a.append and out.exists() else []
    known = {r['source_url'] for r in old}
    skus = {r['sku_source'] for r in old}
    rows, seen = [], {r['name'] for r in old}
    try:
        for u in urls:
            if len(rows) >= a.limit:
                break
            if u in known or not rp.can_fetch(UA, u):
                continue
            time.sleep(a.delay)
            try:
                html = get(u)
                ld = product_ld(html)
            except urllib.error.URLError as e:
                print('bỏ qua', u, e); continue
            if not ld or ld['name'] in seen or (a.append and ld.get('sku', '') in skus):
                continue
            seen.add(ld['name']); skus.add(ld.get('sku', ''))
            img = ld.get('image'); img = img[0] if isinstance(img, list) else img
            slug = urlparse(u).path.rstrip('/').split('/')[-1]
            locals_ = []
            if img and urlparse(img).hostname and urlparse(img).hostname.endswith('fptshop.com.vn'):
                for n, gu in enumerate(gallery_urls(html, img, slug), 1):
                    ext = Path(urlparse(gu).path).suffix or '.png'
                    f = img_dir / f'{slug}-{n}{ext}'
                    if not f.exists():
                        time.sleep(a.delay / 2)
                        f.write_bytes(get(gu, binary=True))
                    locals_.append(str(f.relative_to(OUT)))
            local = locals_[0] if locals_ else ''
            specs = {p['name']: p.get('value') for p in ld.get('additionalProperty', []) if isinstance(p, dict) and 'name' in p}
            offer = ld.get('offers') or {}
            offer = offer[0] if isinstance(offer, list) and offer else offer
            try:
                price_ref = int(float(offer.get('price') or 0)) if isinstance(offer, dict) else 0
            except (TypeError, ValueError):
                price_ref = 0
            rows.append({'price_ref': price_ref, 'name': ld['name'], 'brand': (ld.get('brand') or {}).get('name', ''), 'sku_source': ld.get('sku', ''),
                         'source_url': u, 'image_file': local, 'image_source': img or '', 'images_json': json.dumps(locals_), 'specs_json': json.dumps(specs, ensure_ascii=False)})
            print(f'[{len(rows)}/{a.limit}] {ld["name"]}')
    except Stop as e:
        print(e, file=sys.stderr)
    OUT.mkdir(exist_ok=True)
    with out.open('w', newline='', encoding='utf-8-sig') as f:
        w = csv.DictWriter(f, fieldnames=['price_ref', 'name', 'brand', 'sku_source', 'source_url', 'image_file', 'image_source', 'images_json', 'specs_json'])
        w.writeheader(); w.writerows(old + rows)
    print('Mới', len(rows), '- tổng', len(old) + len(rows), 'sản phẩm ->', out)


main()
