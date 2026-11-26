package vn.haqua.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.haqua.shop.enums.ChatEnums.KnowledgeType;

/** Nguồn kiến thức của chatbot; FAQ dùng chung với trang chủ. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "knowledge_sources")
public class KnowledgeSource extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private KnowledgeType type;

    private String name;

    @Column(length = 4000)
    private String content;

    private boolean enabled = true;

    public KnowledgeSource(KnowledgeType type, String name, String content) {
        this.type = type;
        this.name = name;
        this.content = content;

    }
}
