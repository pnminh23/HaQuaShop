package vn.haqua.shop.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private final PaymentService service;

    PaymentWebhookController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void webhook(@RequestBody PaymentService.WebhookRequest req,
                 @RequestHeader(value = "X-Signature", required = false) String signature) {
        service.handleCallback(req, signature);

    }
}
