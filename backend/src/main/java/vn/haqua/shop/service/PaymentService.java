package vn.haqua.shop.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.client.PaymentGatewayClient;
import vn.haqua.shop.config.AppProperties;
import vn.haqua.shop.dto.PaymentInit;
import vn.haqua.shop.entity.Order;
import vn.haqua.shop.entity.Payment;
import vn.haqua.shop.enums.PaymentMethod;
import vn.haqua.shop.enums.PaymentTxnStatus;
import vn.haqua.shop.event.PaymentSucceededEvent;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository payments;
    private final PaymentGatewayClient gateway;
    private final ApplicationEventPublisher events;
    private final AppProperties props;

    /** Tạo giao dịch cho đơn thanh toán online. Gọi trong cùng transaction đặt hàng. */
    @Transactional
    public PaymentInit createPayment(Order order, int installmentMonths) {
        if (!order.getPaymentMethod().isOnline()) {
            return PaymentInit.none();
        }
        String txn = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Payment p = new Payment(order, order.getPaymentMethod(), order.getTotal(), installmentMonths);
        p.setProviderTxnId(txn);
        payments.save(p);
        if (order.getPaymentMethod() == PaymentMethod.QR) {
            return new PaymentInit(gateway.createQr(order, txn), order.getCode().replace("-", ""), null);
        }
        return new PaymentInit(null, null, gateway.createCardSession(order, txn));
    }

    /** Webhook của cổng thanh toán (đã ký số). Idempotent. */
    @Transactional
    public void handleCallback(WebhookRequest req, String signature) {
        String expected = sign(req.signedPayload());
        if (signature == null || !java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.forbidden("Chữ ký không hợp lệ");
        }
        Payment p = payments.findByProviderTxnId(req.txnId())
                .orElseThrow(() -> ApiException.notFound("Không thấy giao dịch"));
        if (p.getStatus() != PaymentTxnStatus.PENDING) {
            return;
        }
        if (!"SUCCESS".equalsIgnoreCase(req.status())) {
            p.setStatus(PaymentTxnStatus.FAILED);
            return;
        }
        if (req.amount() != p.getAmount()) {
            throw ApiException.badRequest("Số tiền không khớp");
        }
        p.setStatus(PaymentTxnStatus.SUCCESS);
        p.setPaidAt(Instant.now());
        events.publishEvent(new PaymentSucceededEvent(p.getOrder().getId(), p.getProviderTxnId()));
    }

    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.payment().webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public record WebhookRequest(String txnId, long amount, String status) {
        String signedPayload() {
            return txnId + "|" + amount + "|" + status;

        }
    }
}
