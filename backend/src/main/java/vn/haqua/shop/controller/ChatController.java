package vn.haqua.shop.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.ChatDtos.*;
import vn.haqua.shop.security.CurrentUser;
import vn.haqua.shop.service.ChatbotService;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatbotService service;
    private final vn.haqua.shop.service.CartService carts;

    @GetMapping
    ChatState open(@RequestParam(required = false) String sessionToken) {
        return service.open(sessionToken, CurrentUser.id().orElse(null));
    }

    @PostMapping("/messages")
    ChatState send(@Valid @RequestBody SendRequest req) {
        return service.send(req, CurrentUser.id().orElse(null));
    }

    /** Khách (hoặc widget) hỏi tin mới – dùng cho tin nhắn của nhân viên. */
    @GetMapping("/{token}/messages")
    ChatState poll(@PathVariable String token, @RequestParam(defaultValue = "0") Long after) {
        return service.poll(token, after);
    }

    @PostMapping("/{token}/staff")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void requestStaff(@PathVariable String token) {
        service.requestStaff(token);
    }

    @PostMapping("/{token}/csat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void rate(@PathVariable String token, @Valid @RequestBody CsatRequest req) {
        service.rate(token, req.score());
    }

    /**
     * Thêm sản phẩm vào giỏ từ thẻ gợi ý trong khung chat: đơn sau đó được ghi kênh
     * Chatbot AI.
     */
    @PostMapping("/cart")
    @ResponseStatus(HttpStatus.CREATED)
    vn.haqua.shop.dto.CartDtos.CartDto addToCart(
            @RequestHeader(value = "X-Cart-Token", required = false) String token,
            @Valid @RequestBody vn.haqua.shop.dto.CartDtos.AddItemRequest req) {
        return carts.addItemFromChatbot(new vn.haqua.shop.dto.CartKey(CurrentUser.id().orElse(null), token), req);

    }
}
