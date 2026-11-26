package vn.haqua.shop.client;

import java.util.List;


/** Dịch vụ LLM bên ngoài. Đổi nhà cung cấp AI chỉ cần cài đặt interface này, không sửa ChatbotService. */
public interface LlmClient {

    record Message(String role, String content) {
    }

    String complete(String systemPrompt, List<Message> history);

}
