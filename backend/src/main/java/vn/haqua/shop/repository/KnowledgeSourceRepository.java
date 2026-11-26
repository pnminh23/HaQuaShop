package vn.haqua.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.haqua.shop.entity.KnowledgeSource;
import vn.haqua.shop.enums.ChatEnums.KnowledgeType;

public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, Long> {

    List<KnowledgeSource> findByEnabledTrue();

    List<KnowledgeSource> findByTypeAndEnabledTrue(KnowledgeType type);

}
