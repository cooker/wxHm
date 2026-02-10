package com.wxhm.repository;

import com.wxhm.entity.WeChatTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeChatTemplateRepository extends JpaRepository<WeChatTemplate, Long> {

    Optional<WeChatTemplate> findFirstByOrderByUpdatedAtDescIdDesc();

    List<WeChatTemplate> findAllByOrderByUpdatedAtDescIdDesc();
}
