#!/usr/bin/env python3
"""Sinh đơn hàng giả vào MySQL local để test quản lý đơn và báo cáo doanh thu.

    python3 tools/seed_orders.py                     # từ 2026-09-01 đến hiện tại
    python3 tools/seed_orders.py --from 2026-10-01
    python3 tools/seed_orders.py --clean             # xóa toàn bộ dữ liệu giả

Cách nhận biết dữ liệu giả:
  - đơn: 4 ký tự cuối của mã bắt đầu bằng '0' (app không bao giờ sinh số 0)
  - khách: email @seed.haqua.test (không đăng nhập được)
  - giỏ hàng (để tính tỉ lệ chuyển đổi): session_token bắt đầu bằng 'seed-'

Đọc kết nối từ backend/.env. Cần lệnh `mysql`. Không trừ tồn kho.
"""
import argparse
import os
import random
import re
import subprocess
import sys
import uuid
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VN = timezone(timedelta(hours=7))
ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
SEED_ORDER = "code REGEXP '^HQ-[0-9]{6}-0'"
SEED_EMAIL = "email LIKE '%@seed.haqua.test'"


def load_env():
    env = {}
    for line in (ROOT / "backend/.env").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            k, v = line.split("=", 1)
            env[k.strip()] = v.strip()
    env.update({k: v for k, v in os.environ.items() if k in ("DB_URL", "DB_USER", "DB_PASSWORD")})
    return env


ENV = load_env()
m = re.match(r"jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)", ENV.get("DB_URL", ""))
if not m:
    sys.exit("DB_URL trong backend/.env không phải jdbc:mysql://...")
HOST, PORT, DB = m.group(1), m.group(2) or "3306", m.group(3)
if HOST not in ("localhost", "127.0.0.1"):
    sys.exit(f"Chỉ chạy trên database local, DB_URL đang trỏ tới {HOST}")


def mysql(sql):
    r = subprocess.run(
        ["mysql", "--protocol=TCP", f"-h{HOST}", f"-P{PORT}", f"-u{ENV['DB_USER']}",
         "--default-character-set=utf8mb4", "-N", "-B", DB],
        input=sql, text=True, capture_output=True,
        env={**os.environ, "MYSQL_PWD": ENV.get("DB_PASSWORD", "")})
    if r.returncode:
        sys.exit(r.stderr.strip())
    return [line.split("\t") for line in r.stdout.splitlines()]


def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, datetime):
        v = v.astimezone(timezone.utc).strftime("%Y-%m-%d %H:%M:%S.%f")
    if isinstance(v, (int, float)):
        return str(v)
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def insert(table, **cols):
    return f"INSERT INTO {table} ({', '.join(cols)}) VALUES ({', '.join(q(v) for v in cols.values())});"


def clean():
    # Tính phần cần hoàn lại theo trạng thái HIỆN TẠI, vì khi test có thể đã chuyển/hủy đơn giả trên app.
    mysql(f"""
START TRANSACTION;
CREATE TEMPORARY TABLE seed_ids AS SELECT id FROM orders WHERE {SEED_ORDER};
UPDATE products p JOIN (
    SELECT v.product_id pid, SUM(i.qty) n FROM order_items i
    JOIN orders o ON o.id = i.order_id JOIN product_variants v ON v.id = i.variant_id
    WHERE o.id IN (SELECT id FROM seed_ids) AND o.status = 'COMPLETED' GROUP BY v.product_id) x ON x.pid = p.id
SET p.sold_count = GREATEST(p.sold_count - x.n, 0);
UPDATE coupons c JOIN (
    SELECT coupon_id cid, COUNT(*) n FROM orders
    WHERE id IN (SELECT id FROM seed_ids) AND status <> 'CANCELLED' AND coupon_id IS NOT NULL
    GROUP BY coupon_id) x ON x.cid = c.id
SET c.used_count = GREATEST(c.used_count - x.n, 0);
UPDATE users u JOIN (
    SELECT user_id uid, SUM(points_earned) n FROM orders
    WHERE id IN (SELECT id FROM seed_ids) AND status = 'COMPLETED' AND user_id IS NOT NULL
    GROUP BY user_id) x ON x.uid = u.id
SET u.green_points = GREATEST(u.green_points - x.n, 0);
DELETE FROM payments WHERE order_id IN (SELECT id FROM seed_ids);
DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM seed_ids);
DELETE FROM order_items WHERE order_id IN (SELECT id FROM seed_ids);
DELETE FROM orders WHERE id IN (SELECT id FROM seed_ids);
DELETE FROM carts WHERE session_token LIKE 'seed-%';
COMMIT;
""")
    # Khách giả có thể đã được gắn dữ liệu khác khi test (đánh giá, địa chỉ...) nên xóa riêng.
    mysql(f"DELETE FROM users WHERE {SEED_EMAIL};")
    print("Đã xóa dữ liệu giả.")


# ---------------------------------------------------------------- sinh dữ liệu

LAST = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ"]
MID = ["Văn", "Thị", "Minh", "Ngọc", "Đức", "Thanh", "Quốc", "Thu", "Gia", "Hoài"]
FIRST = ["An", "Bình", "Châu", "Dũng", "Giang", "Hà", "Hải", "Hương", "Khoa", "Lan", "Linh", "Long",
         "Mai", "Nam", "Nhi", "Phúc", "Quân", "Sơn", "Thảo", "Trang", "Tuấn", "Vy"]
PLACES = [("Phường Bến Nghé", "TP. Hồ Chí Minh", 30), ("Phường Tân Định", "TP. Hồ Chí Minh", 8),
          ("Phường Cầu Giấy", "Hà Nội", 22), ("Phường Ba Đình", "Hà Nội", 8),
          ("Phường Hải Châu", "Đà Nẵng", 10), ("Phường Ninh Kiều", "Cần Thơ", 7),
          ("Phường Hồng Bàng", "Hải Phòng", 7), ("Phường Thuận Hóa", "Huế", 4)]
STREETS = ["Lê Lợi", "Trần Hưng Đạo", "Nguyễn Trãi", "Hai Bà Trưng", "Lý Thường Kiệt", "Phan Chu Trinh"]
SPIKES = {(9, 2): 2.0, (9, 9): 2.4, (10, 10): 2.4, (10, 20): 1.8, (11, 11): 3.0, (11, 20): 1.5}
CANCEL_REASONS = ["Khách hủy đơn", "Khách đổi ý", "Không liên lạc được khách", "Hết hàng tại kho"]
STEPS = ["PENDING", "CONFIRMED", "PACKING", "SHIPPING", "COMPLETED"]


def pick(table):
    """table: [(giá_trị, trọng_số), ...]"""
    return random.choices([t[0] for t in table], [t[1] for t in table])[0]


def full_name():
    return f"{random.choice(LAST)} {random.choice(MID)} {random.choice(FIRST)}"


def address():
    ward, province, _ = random.choices(PLACES, [p[2] for p in PLACES])[0]
    return f"{random.randint(1, 300)} {random.choice(STREETS)}, {ward}, {province}", province


def moment(day, now):
    """Một thời điểm trong ngày theo giờ VN (8h–23h, cao điểm trưa và tối), không vượt quá hiện tại."""
    hour = pick(list(zip(range(8, 24), [3, 5, 7, 8, 9, 7, 6, 6, 6, 5, 6, 9, 11, 10, 7, 3])))
    t = datetime(day.year, day.month, day.day, hour, random.randrange(60), random.randrange(60),
                 random.randrange(1_000_000), tzinfo=VN)
    if t > now:
        start = datetime(day.year, day.month, day.day, tzinfo=VN)
        t = start + (now - start) * random.random()
    return t


def target_status(age_days):
    if age_days >= 6:
        table = [("COMPLETED", 90), ("CANCELLED", 10)]
    elif age_days >= 3:
        table = [("COMPLETED", 62), ("SHIPPING", 20), ("PACKING", 8), ("CANCELLED", 10)]
    elif age_days >= 1:
        table = [("COMPLETED", 15), ("SHIPPING", 38), ("PACKING", 22), ("CONFIRMED", 15), ("CANCELLED", 10)]
    else:
        table = [("PENDING", 45), ("CONFIRMED", 30), ("PACKING", 15), ("CANCELLED", 10)]
    return pick(table)


def generate(start, seed):
    random.seed(seed)
    now = datetime.now(VN)
    if start > now.date():
        sys.exit("--from phải trước hôm nay")
    if int(mysql(f"SELECT (SELECT COUNT(*) FROM orders WHERE {SEED_ORDER}) + (SELECT COUNT(*) FROM users WHERE {SEED_EMAIL});")[0][0]):
        sys.exit("Đã có dữ liệu giả. Chạy với --clean trước khi sinh lại.")

    variants = [
        dict(id=int(r[0]), product=int(r[1]), name=r[2], color=r[3], storage=r[4], price=int(r[5]))
        for r in mysql("""SELECT v.id, p.id, p.name, COALESCE(v.color,''), COALESCE(v.storage,''), v.price
            FROM product_variants v JOIN products p ON p.id = v.product_id
            WHERE p.status = 'ACTIVE' AND v.price > 0 ORDER BY v.id;""")]
    if not variants:
        sys.exit("Không có sản phẩm đang bán có giá > 0.")
    # Hàng rẻ bán chạy hơn; thêm độ "hot" ngẫu nhiên để có top sản phẩm rõ ràng.
    weights = [(10_000_000 / v["price"]) ** 0.45 * random.paretovariate(1.6) for v in variants]

    coupon = mysql("""SELECT id, type, discount_value, max_discount, min_order FROM coupons
        WHERE code = 'HAQUA10';""")
    demo = mysql("""SELECT id, full_name, phone, email FROM users WHERE email = 'khach@haqua.vn';""")

    sql = ["START TRANSACTION;"]

    # Khách hàng: một phần đăng ký trước kỳ, còn lại rải đều đến nay.
    first_day = datetime(start.year, start.month, start.day, tzinfo=VN)
    customers = []
    for n in range(1, 121):
        if n <= 25:
            created = first_day - timedelta(days=random.uniform(1, 30))
        else:
            created = first_day + (now - first_day) * random.random()
        c = dict(var=f"@u{n}", created=created, name=full_name(), phone=f"0990{n:06d}",
                 email=f"khach{n:03d}@seed.haqua.test", address=address())
        customers.append(c)
        sql.append(insert("users", created_at=created, email=c["email"], phone=c["phone"], password_hash="!",
                          full_name=c["name"], role="CUSTOMER", status="ACTIVE", green_points=0, failed_logins=0))
        sql.append(f"SET {c['var']} = LAST_INSERT_ID();")

    # Thời điểm đặt đơn: tăng trưởng dần, cuối tuần và ngày sale đông hơn.
    times = []
    total_days = (now.date() - start).days + 1
    for i in range(total_days):
        day = start + timedelta(days=i)
        mean = (5 + 4 * i / total_days) * (1.3 if day.weekday() >= 5 else 1.0) * SPIKES.get((day.month, day.day), 1.0)
        if day == now.date():
            mean *= max((now.hour - 8) / 15, 0.3)
        for _ in range(max(1, round(random.gauss(mean, mean * 0.18)))):
            times.append(moment(day, now))
    times.sort()  # id tăng theo thời gian, vì "đơn gần đây" sắp theo id

    codes, count_status, sold, coupon_used, points = set(), {}, {}, 0, {}
    for created in times:
        while True:
            code = f"HQ-{created:%y%m%d}-0" + "".join(random.choices(ALPHABET, k=3))
            if code not in codes:
                codes.add(code)
                break

        status = target_status((now - created).total_seconds() / 86400)
        channel = pick([("WEBSITE", 70), ("CHATBOT", 18), ("STORE", 12)])

        user, kind = None, pick([("guest", 20), ("member", 77), ("demo", 3)])
        eligible = [c for c in customers if c["created"] < created]
        if kind == "demo" and demo:
            user = demo[0][0]
            name, phone, email = demo[0][1], demo[0][2], demo[0][3]
            addr, province = address()
        elif kind == "member" and eligible:
            c = random.choice(eligible)
            user, name, phone, email = c["var"], c["name"], c["phone"], c["email"]
            addr, province = c["address"]
        else:
            name, phone, email = full_name(), f"0991{random.randrange(10**6):06d}", None
            addr, province = address()

        lines, chosen = [], set()
        for _ in range(pick([(1, 76), (2, 18), (3, 6)])):
            v = random.choices(variants, weights)[0]
            if v["id"] in chosen:
                continue
            chosen.add(v["id"])
            qty = pick([(1, 88), (2, 10), (3, 2)]) if v["price"] < 5_000_000 else 1
            lines.append((v, qty))
        subtotal = sum(v["price"] * n for v, n in lines)

        discount, coupon_id = 0, None
        if coupon and channel != "STORE" and random.random() < 0.15:
            cid, ctype, value, cap, minimum = coupon[0]
            if subtotal >= int(minimum):
                discount = subtotal * int(value) // 100 if ctype == "PERCENT" else int(value)
                if int(cap) > 0:
                    discount = min(discount, int(cap))
                discount, coupon_id = min(discount, subtotal), int(cid)

        if channel == "STORE":
            shipping = "STORE_PICKUP"
            method = pick([("COD", 45), ("CARD", 30), ("QR", 25)])
        else:
            options = [("STANDARD", 80), ("STORE_PICKUP", 8)]
            if province in ("Hà Nội", "TP. Hồ Chí Minh"):
                options.append(("EXPRESS_2H", 20))
            shipping = pick(options)
            method = pick([("COD", 45), ("QR", 22), ("EWALLET", 14), ("CARD", 12), ("INSTALLMENT", 7)])
        if method == "INSTALLMENT" and subtotal < 3_000_000:
            method = "QR"
        # Đơn online chưa trả tiền mà để Chờ xác nhận sẽ bị job tự hủy (QR), nên đơn chờ chỉ dùng COD.
        if status == "PENDING":
            method = "COD"
        fee = {"STANDARD": 0 if subtotal >= int(ENV.get("HAQUA_SHOP_FREE_SHIP_THRESHOLD") or 500000)
               else int(ENV.get("HAQUA_SHOP_STANDARD_SHIP_FEE") or 30000),
               "EXPRESS_2H": int(ENV.get("HAQUA_SHOP_EXPRESS_SHIP_FEE") or 30000),
               "STORE_PICKUP": 0}[shipping]
        total = subtotal - discount + fee

        online = method != "COD"
        if status == "CANCELLED":
            paid_then_refunded = online and random.random() < 0.4
            payment_status = "REFUNDED" if paid_then_refunded else "UNPAID"
            path = STEPS[:2 if paid_then_refunded else random.randint(1, 3)] + ["CANCELLED"]
        else:
            path = STEPS[:STEPS.index(status) + 1]
            payment_status = "PAID" if online or status == "COMPLETED" else "UNPAID"

        # Mốc thời gian chuyển trạng thái, luôn nằm trong khoảng (lúc đặt, hiện tại].
        span = min(now - created, timedelta(days=random.uniform(1.2, 4)))
        marks = sorted(random.random() for _ in path[1:])
        stamps = [created] + [created + span * x for x in marks]

        earned = total // int(ENV.get("HAQUA_SHOP_GREEN_POINT_RATE") or 100000)
        sql.append(
            f"INSERT INTO orders (created_at, code, user_id, coupon_id, receiver_name, phone, email, shipping_address, "
            f"channel, shipping_method, subtotal, discount, shipping_fee, total, payment_method, payment_status, status, "
            f"points_earned, version) VALUES ({q(created)}, {q(code)}, {user or 'NULL'}, {q(coupon_id)}, {q(name)}, "
            f"{q(phone)}, {q(email)}, {q(addr)}, {q(channel)}, {q(shipping)}, {subtotal}, {discount}, {fee}, {total}, "
            f"{q(method)}, {q(payment_status)}, {q(status)}, {earned}, 0);")
        sql.append("SET @o = LAST_INSERT_ID();")
        for v, qty in lines:
            label = " · ".join(s for s in (v["color"], v["storage"]) if s.strip())
            sql.append(
                f"INSERT INTO order_items (created_at, order_id, variant_id, product_name, variant_label, unit_price, qty) "
                f"VALUES ({q(created)}, @o, {v['id']}, {q(v['name'])}, {q(label)}, {v['price']}, {qty});")
            if status == "COMPLETED":
                sold[v["product"]] = sold.get(v["product"], 0) + qty
        previous = None
        for step, at in zip(path, stamps):
            note = "Đặt hàng" if previous is None else random.choice(CANCEL_REASONS) if step == "CANCELLED" else None
            sql.append(
                f"INSERT INTO order_status_history (created_at, changed_at, order_id, from_status, to_status, note) "
                f"VALUES ({q(at)}, {q(at)}, @o, {q(previous)}, {q(step)}, {q(note)});")
            previous = step
        if online:
            succeeded = payment_status in ("PAID", "REFUNDED")
            months = random.choice([3, 6, 9, 12]) if method == "INSTALLMENT" else 0
            sql.append(
                f"INSERT INTO payments (created_at, order_id, method, provider_txn_id, amount, installment_months, status, paid_at) "
                f"VALUES ({q(created)}, @o, {q(method)}, {q('TXN-' + uuid.uuid4().hex[:16].upper())}, {total}, {months}, "
                f"{q('SUCCESS' if succeeded else 'FAILED')}, {q(stamps[1] if succeeded and len(stamps) > 1 else None)});")

        count_status[status] = count_status.get(status, 0) + 1
        if coupon_id and status != "CANCELLED":
            coupon_used += 1
        if status == "COMPLETED" and user:
            points[user] = points.get(user, 0) + earned

        # Giỏ hàng tạo trong ngày đó: 1 đơn thành công + vài giỏ bỏ dở (tỉ lệ chuyển đổi ~25%).
        for _ in range(1 + pick([(1, 10), (2, 25), (3, 35), (4, 20), (5, 10)])):
            at = created - timedelta(minutes=random.uniform(0, 45))
            sql.append(insert("carts", created_at=at, updated_at=at, session_token=f"seed-{uuid.uuid4()}",
                              from_chatbot=int(random.random() < 0.15)))

    for product, n in sold.items():
        sql.append(f"UPDATE products SET sold_count = sold_count + {n} WHERE id = {product};")
    for user, n in points.items():
        sql.append(f"UPDATE users SET green_points = green_points + {n} WHERE id = {user};")
    if coupon and coupon_used:
        sql.append(f"UPDATE coupons SET used_count = used_count + {coupon_used} WHERE id = {int(coupon[0][0])};")
    sql.append("COMMIT;")

    mysql("\n".join(sql))
    print(f"Đã sinh {len(times)} đơn từ {start} đến {now:%Y-%m-%d %H:%M}, {len(customers)} khách giả.")
    print("Theo trạng thái:", ", ".join(f"{k}={v}" for k, v in sorted(count_status.items())))


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--from", dest="start", type=date.fromisoformat, default=date(2026, 9, 1))
    ap.add_argument("--seed", type=int, default=2026, help="đổi số để ra bộ dữ liệu khác")
    ap.add_argument("--clean", action="store_true", help="xóa dữ liệu giả")
    args = ap.parse_args()
    clean() if args.clean else generate(args.start, args.seed)
