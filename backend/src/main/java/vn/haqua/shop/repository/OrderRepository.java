package vn.haqua.shop.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import vn.haqua.shop.entity.Order;
import vn.haqua.shop.entity.OrderItem;
import vn.haqua.shop.enums.OrderStatus;
import vn.haqua.shop.enums.PaymentMethod;
import vn.haqua.shop.enums.PaymentStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByCode(String code);

    Optional<Order> findByCodeAndPhone(String code, String phone);

    List<Order> findByUserIdOrderByIdDesc(Long userId);

    Page<Order> findByStatusIn(Collection<OrderStatus> statuses, Pageable pageable);

    @Query("""
            select o from Order o where o.status in :statuses and
            (lower(o.code) like lower(concat('%', :q, '%')) or lower(o.receiverName) like lower(concat('%', :q, '%'))
             or o.phone like concat('%', :q, '%'))""")
    Page<Order> search(Collection<OrderStatus> statuses, String q, Pageable pageable);

    List<Order> findByStatusAndPaymentMethodAndPaymentStatusAndCreatedAtBefore(
            OrderStatus status, PaymentMethod method, PaymentStatus paymentStatus, Instant before);

    @Query("""
            select i from OrderItem i
            where i.order.user.id = :userId and i.order.status = 'COMPLETED' and i.variant.product.id = :productId
            order by i.id desc""")
    List<OrderItem> findCompletedItems(Long userId, Long productId);

    @Query("select coalesce(sum(o.total),0) from Order o where o.status <> 'CANCELLED' and o.createdAt >= :from and o.createdAt < :to")
    long revenueBetween(Instant from, Instant to);

    @Query("select count(o) from Order o where o.status <> 'CANCELLED' and o.createdAt >= :from and o.createdAt < :to")
    long countBetween(Instant from, Instant to);

    @Query("select count(o) from Order o where o.channel = 'CHATBOT' and o.createdAt >= :from")
    long countChatbotSince(Instant from);

    @Query("select count(o), coalesce(sum(o.total),0) from Order o where o.user.id = :userId and o.status <> 'CANCELLED'")
    List<Object[]> statsForUser(Long userId);

    List<Order> findTop6ByOrderByIdDesc();

}
