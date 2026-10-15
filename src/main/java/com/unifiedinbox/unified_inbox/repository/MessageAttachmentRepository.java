package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.MessageAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageAttachmentRepository
        extends JpaRepository<MessageAttachmentEntity, Long> {

    List<MessageAttachmentEntity> findByMessageId(Long messageId);
    List<MessageAttachmentEntity> findByMessageIdIn(
            List<Long> messageIds
    );
}