package vn.haqua.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.haqua.shop.enums.ChatEnums.HandoffRule;
import vn.haqua.shop.enums.ChatEnums.Tone;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chatbot_config")
public class ChatbotConfig extends BaseEntity {

    private String displayName = "Trợ lý HaQua";

    @Enumerated(EnumType.STRING)
    private Tone tone = Tone.FRIENDLY;

    @Enumerated(EnumType.STRING)
    private HandoffRule handoffRule = HandoffRule.ASK_OR_UNSURE;

    @Column(length = 2000)
    private String extraInstructions;

    private boolean enabled = true;

}
