package vn.haqua.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.haqua.shop.dto.DashboardDtos.Dashboard;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.service.DashboardService;

/** Báo cáo doanh thu theo khoảng ngày tùy chọn (ngày tính theo giờ Việt Nam). Chạy trên H2 riêng. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard-period;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "haqua.seed.enabled=false", "haqua.import.dir=",
        "haqua.upload-dir=target/test-uploads"})
class DashboardPeriodTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate MAR_1 = LocalDate.of(2025, 3, 1);
    private static final LocalDate MAR_31 = LocalDate.of(2025, 3, 31);

    @Autowired DashboardService dashboard;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void orders() {
        jdbc.update("delete from orders");
        order("A", "2025-02-28T23:59:59+07:00", 500, "COMPLETED"); // kỳ liền trước
        order("B", "2025-03-01T00:00:00+07:00", 100, "COMPLETED");
        order("C", "2025-03-15T12:00:00+07:00", 900, "CANCELLED");
        order("D", "2025-03-31T23:59:59+07:00", 900, "PENDING");
        order("E", "2025-04-01T00:00:00+07:00", 7000, "COMPLETED"); // ngoài khoảng
    }

    private void order(String code, String at, long total, String status) {
        jdbc.update("insert into orders (code, created_at, total, status, subtotal, discount, shipping_fee, points_earned, version)"
                + " values (?, ?, ?, ?, 0, 0, 0, 0, 0)", code, OffsetDateTime.parse(at), total, status);
    }

    private static double kpi(Dashboard d, String label) {
        return d.kpis().stream().filter(k -> k.label().equals(label)).findFirst().orElseThrow().value();
    }

    @Test
    void customRangeCountsWholeVietnamDaysAndSkipsCancelled() {
        Dashboard d = dashboard.build(14, MAR_1, MAR_31);

        assertThat(d.from()).isEqualTo(MAR_1);
        assertThat(d.to()).isEqualTo(MAR_31);
        assertThat(d.rangeDays()).isEqualTo(31);
        assertThat(kpi(d, "Doanh thu")).isEqualTo(1000);
        assertThat(kpi(d, "Đơn hàng")).isEqualTo(2);
        assertThat(d.revenueByDay()).hasSize(31);
        assertThat(d.revenueByDay().getFirst().revenue()).isEqualTo(100);
        assertThat(d.revenueByDay().getLast().revenue()).isEqualTo(900);
    }

    @Test
    void comparesWithThePreviousPeriodOfTheSameLength() {
        Dashboard d = dashboard.build(14, MAR_1, MAR_31);

        assertThat(d.kpis().getFirst().changePercent()).isEqualTo(100.0); // 1000 so với 500
    }

    @Test
    void withoutDatesKeepsTheLastDaysPreset() {
        LocalDate today = LocalDate.now(VN);

        Dashboard d = dashboard.build(30, null, null);

        assertThat(d.rangeDays()).isEqualTo(30);
        assertThat(d.from()).isEqualTo(today.minusDays(29));
        assertThat(d.to()).isEqualTo(today);
        assertThat(dashboard.build(999, null, null).rangeDays()).isEqualTo(14);
    }

    @Test
    void endDateInTheFutureStopsAtToday() {
        LocalDate today = LocalDate.now(VN);

        Dashboard d = dashboard.build(14, today.minusDays(2), today.plusDays(20));

        assertThat(d.to()).isEqualTo(today);
        assertThat(d.rangeDays()).isEqualTo(3);
    }

    @Test
    void rejectsBadRanges() {
        LocalDate tomorrow = LocalDate.now(VN).plusDays(1);

        assertThatThrownBy(() -> dashboard.build(14, MAR_1, null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> dashboard.build(14, null, MAR_31)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> dashboard.build(14, MAR_31, MAR_1)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> dashboard.build(14, tomorrow, tomorrow)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> dashboard.build(14, MAR_1, MAR_1.plusDays(366))).isInstanceOf(ApiException.class);
        assertThat(dashboard.build(14, MAR_1, MAR_1.plusDays(365)).rangeDays()).isEqualTo(366);
    }

    @Test
    void csvCoversExactlyTheChosenRange() {
        String csv = dashboard.exportCsv(14, MAR_1, MAR_31);

        assertThat(csv.lines()).hasSize(32).contains("2025-03-01,100", "2025-03-31,900");
    }
}
