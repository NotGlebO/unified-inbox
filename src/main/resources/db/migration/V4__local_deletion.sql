
ALTER TABLE messages
    ADD COLUMN locally_deleted_at TIMESTAMPTZ;

ALTER TABLE conversations
    ADD COLUMN hidden_through_message_id BIGINT;

CREATE INDEX idx_messages_visible_conversation
    ON messages (conversation_id, sent_at, id)
    WHERE locally_deleted_at IS NULL;
