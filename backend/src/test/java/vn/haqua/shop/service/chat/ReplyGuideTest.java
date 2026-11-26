package vn.haqua.shop.service.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import vn.haqua.shop.client.LlmClient.Message;

class ReplyGuideTest {

    private static final List<Message> HISTORY = List.of(
            new Message("user", "laptop đồ họa"), new Message("assistant", "danh sách cũ"),
            new Message("user", "dưới 40 triệu thôi"));

    @Test
    void namesTheCardProductsOnTheCurrentQuestionOnly() {
        List<Message> out = ReplyGuide.pin(HISTORY, List.of("Máy A", "Máy B"));

        assertThat(out).hasSize(3);
        assertThat(out.subList(0, 2)).isEqualTo(HISTORY.subList(0, 2));
        assertThat(out.get(2).content()).startsWith("dưới 40 triệu thôi").contains("1. Máy A", "2. Máy B");
    }

    @Test
    void leavesHistoryAloneWhenThereAreNoCards() {
        assertThat(ReplyGuide.pin(HISTORY, List.of())).isEqualTo(HISTORY);
    }
}
