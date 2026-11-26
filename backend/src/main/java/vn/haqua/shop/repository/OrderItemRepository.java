package vn.haqua.shop.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import vn.haqua.shop.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** [categoryName, tổng doanh thu] của đơn chưa hủy trong khoảng thời gian. */
    @Query("""
            select i.variant.product.category.name, sum(i.unitPrice * i.qty) from OrderItem i
            where i.order.status <> 'CANCELLED' and i.order.createdAt >= :from and i.order.createdAt < :to
            group by i.variant.product.category.name""")
    List<Object[]> revenueByCategory(Instant from, Instant to);

    /** [productId, tên, số lượng bán, doanh thu] sắp theo số lượng giảm dần. */
    @Query("""
            select i.variant.product.id, i.productName, sum(i.qty), sum(i.unitPrice * i.qty) from OrderItem i
            where i.order.status <> 'CANCELLED' and i.order.createdAt >= :from and i.order.createdAt < :to
            group by i.variant.product.id, i.productName order by sum(i.qty) desc""")
    List<Object[]> topProducts(Instant from, Instant to, org.springframework.data.domain.Pageable pageable);

}
