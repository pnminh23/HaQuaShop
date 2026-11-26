package vn.haqua.shop.service.chat;

import java.util.ArrayList;
import java.util.List;

import vn.haqua.shop.client.LlmClient.Message;

/**
 * Câu trả lời phải nói đúng các sản phẩm đang hiện thẻ. Chỉ dặn trong system prompt chưa đủ vì model hay chép lại
 * danh sách ở câu trả lời trước, nên ghi tên sản phẩm ngay vào lượt hỏi hiện tại (chỉ model thấy, không lưu vào DB).
 */
public final class ReplyGuide {

    private ReplyGuide() {
    }

    public static List<Message> pin(List<Message> history, List<String> productNames) {
        if (productNames.isEmpty() || history.isEmpty()) {
            return history;
        }
        StringBuilder note = new StringBuilder("\n\n[Ghi chú của hệ thống, khách không thấy: trong câu trả lời này hãy "
                + "giới thiệu đầy đủ đúng các sản phẩm sau theo thứ tự, kể cả khi khác hoặc đã nhắc ở câu trả lời trước:");
        for (int i = 0; i < productNames.size(); i++) {
            note.append('\n').append(i + 1).append(". ").append(productNames.get(i));
        }
        note.append(']');
        List<Message> out = new ArrayList<>(history);
        Message last = out.get(out.size() - 1);
        out.set(out.size() - 1, new Message(last.role(), last.content() + note));
        return out;
    }
}
