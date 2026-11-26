package vn.haqua.shop.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.config.AppProperties;
import vn.haqua.shop.dto.DashboardDtos.*;
import vn.haqua.shop.dto.OrderDtos.OrderRowDto;
import vn.haqua.shop.enums.Role;
import vn.haqua.shop.repository.CartRepository;
import vn.haqua.shop.repository.OrderItemRepository;
import vn.haqua.shop.repository.OrderRepository;
import vn.haqua.shop.repository.ProductVariantRepository;
import vn.haqua.shop.repository.UserRepository;

/** UC12 – Bảng điều khiển thống kê. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final UserRepository users;
    private final CartRepository carts;
    private final ProductVariantRepository variants;
    private final AppProperties props;

    @Transactional(readOnly = true)
    public Dashboard build(int rangeDays) {
        int days = List.of(7, 14, 30).contains(rangeDays) ? rangeDays : 14;
        LocalDate today = LocalDate.now(ZONE);
        var from = today.minusDays(days - 1L).atStartOfDay(ZONE).toInstant();
        var to = today.plusDays(1).atStartOfDay(ZONE).toInstant();
        var prevFrom = today.minusDays(2L * days - 1).atStartOfDay(ZONE).toInstant();

        long revenue = orders.revenueBetween(from, to), prevRevenue = orders.revenueBetween(prevFrom, from);
        long count = orders.countBetween(from, to), prevCount = orders.countBetween(prevFrom, from);
        long newUsers = users.countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Role.CUSTOMER, from, to);
        long prevUsers = users.countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Role.CUSTOMER, prevFrom, from);
        double conv = conversion(count, carts.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to));
        double prevConv = conversion(prevCount, carts.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(prevFrom, from));

        List<Kpi> kpis = List.of(
                new Kpi("Doanh thu", revenue, change(revenue, prevRevenue)),
                new Kpi("Đơn hàng", count, change(count, prevCount)),
                new Kpi("Khách hàng mới", newUsers, change(newUsers, prevUsers)),
                new Kpi("Tỉ lệ chuyển đổi (%)", conv, change(conv, prevConv)));

        List<DayRevenue> series = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = today.minusDays(days - 1L - i);
            series.add(new DayRevenue(d, orders.revenueBetween(d.atStartOfDay(ZONE).toInstant(),
                    d.plusDays(1).atStartOfDay(ZONE).toInstant())));
        }

        var rows = items.revenueByCategory(from, to);
        long sum = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        List<CategoryRevenue> byCat = rows.stream().map(r -> new CategoryRevenue((String) r[0], (Long) r[1],
                sum == 0 ? 0 : (int) Math.round((Long) r[1] * 100.0 / sum))).toList();

        List<OrderRowDto> recent = orders.findTop6ByOrderByIdDesc().stream().map(o -> new OrderRowDto(o.getId(), o.getCode(),
                o.getReceiverName(), o.getChannel(), o.getTotal(), o.getStatus(), o.getPaymentStatus(), o.getCreatedAt())).toList();
        List<TopProduct> top = items.topProducts(from, to, PageRequest.of(0, 5)).stream()
                .map(r -> new TopProduct((Long) r[0], (String) r[1], (Long) r[2], (Long) r[3])).toList();
        List<LowStock> low = variants.findLowStock(props.shop().lowStockThreshold()).stream().limit(10)
                .map(v -> new LowStock(v.getId(), v.getProduct().getName(), v.label(), v.getStock())).toList();

        return new Dashboard(days, kpis, series, byCat, recent, top, low);
    }

    /** CSV (mở được bằng Excel) của doanh thu theo ngày trong khoảng đang chọn. */
    @Transactional(readOnly = true)
    public String exportCsv(int rangeDays) {
        StringBuilder sb = new StringBuilder("﻿Ngày,Doanh thu (VND)\n");
        build(rangeDays).revenueByDay().forEach(d -> sb.append(d.date()).append(',').append(d.revenue()).append('\n'));
        return sb.toString();
    }

    private static double change(double now, double prev) {
        return prev == 0 ? (now == 0 ? 0 : 100) : Math.round((now - prev) / prev * 1000) / 10.0;
    }

    private static double conversion(long orderCount, long cartCount) {
        return cartCount == 0 ? 0 : Math.round(orderCount * 1000.0 / cartCount) / 10.0;

    }
}
