package vn.haqua.shop.dto;

import java.time.LocalDate;
import java.util.List;

import vn.haqua.shop.dto.OrderDtos.OrderRowDto;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** changePercent: so với kỳ liền trước cùng độ dài. */
    public record Kpi(String label, double value, double changePercent) {
    }

    public record DayRevenue(LocalDate date, long revenue) {
    }

    public record CategoryRevenue(String category, long revenue, int percent) {
    }

    public record TopProduct(Long productId, String name, long sold, long revenue) {
    }

    public record LowStock(Long variantId, String productName, String variant, int stock) {
    }

    public record Dashboard(int rangeDays, List<Kpi> kpis, List<DayRevenue> revenueByDay,
                            List<CategoryRevenue> revenueByCategory, List<OrderRowDto> recentOrders,
                            List<TopProduct> topProducts, List<LowStock> lowStock) {
    }
}
