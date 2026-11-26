package vn.haqua.shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.haqua.shop.event.OrderPlacedEvent;
import vn.haqua.shop.event.OrderStatusChangedEvent;

/**
 * Gửi SMS + email sau khi giao dịch đã commit. Bản dev chỉ ghi log;
 * prod thay bằng nhà cung cấp SMS/SMTP thật (không để lỗi gửi làm hỏng đơn hàng).
 */
@Slf4j
@Service
public class NotificationService {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderPlaced(OrderPlacedEvent e) {
        log.info("[SMS→{}] [Email→{}] HaQua: đơn {} đã được ghi nhận, tổng {}₫", e.phone(), e.email(), e.orderCode(), e.total());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onStatusChanged(OrderStatusChangedEvent e) {
        log.info("[SMS→{}] HaQua: đơn {} chuyển sang trạng thái {}", e.phone(), e.orderCode(), e.newStatus());

    }
}
