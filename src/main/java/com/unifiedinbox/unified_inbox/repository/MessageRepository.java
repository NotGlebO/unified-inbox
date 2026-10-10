package com.unifiedinbox.unified_inbox.repository;

import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository
	extends JpaRepository<MessageEntity, Long> {

	boolean existsByChatIdAndTelegramMessageId(
			Long chatId,
			Long telegramMessageId
	);

	@Query("""
	SELECT DISTINCT m.chatId
	FROM MessageEntity m
	WHERE m.source = 'TELEGRAM'
	""")
	List<Long> findTelegramChatIds();

	MessageEntity findFirstByChatIdAndSourceOrderByReceivedAtDescIdDesc(
			Long chatId,
			String source
	);
	List<MessageEntity> findByChatIdAndSourceOrderByReceivedAtAscIdAsc(
			Long chatId,
			String source
	);
	MessageEntity findFirstByChatIdAndSourceAndDirectionOrderByReceivedAtDescIdDesc(
			Long chatId,
			String source,
			String direction
	);
	boolean existsBySourceAndExternalMessageId(
			String source,
			String externalMessageId
	);

	@Query("""
		SELECT DISTINCT m.externalChatId
		FROM MessageEntity m
		WHERE m.source = 'WHATSAPP'
		AND m.externalChatId IS NOT NULL
		""")
	List<String> findWhatsAppChatIds();

	MessageEntity findFirstByExternalChatIdAndSourceOrderByReceivedAtDescIdDesc(
			String externalChatId,
			String source
	);

	List<MessageEntity> findByExternalChatIdAndSourceOrderByReceivedAtAscIdAsc(
        String externalChatId,
        String source
	);

	
	List<MessageEntity> findByConversationIdOrderByReceivedAtAsc(
			Long conversationId
	);

	boolean existsByConversationIdAndExternalMessageId(
			Long conversationId,
			String externalMessageId
	);

	boolean existsByConversationIdAndTelegramMessageId(
        Long conversationId,
        Long telegramMessageId
	);

}