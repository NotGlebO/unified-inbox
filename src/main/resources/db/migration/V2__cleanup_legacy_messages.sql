
-- V2: Remove legacy Telegram-only message columns.
-- Keep current message fields and conversation identifiers.

-- Remove obsolete constraints and indexes.
ALTER TABLE public.messages
    DROP CONSTRAINT IF EXISTS uq_telegram_message;

DROP INDEX IF EXISTS public.uq_messages_source_external_message;

-- Remove obsolete columns.
ALTER TABLE public.messages
    DROP COLUMN IF EXISTS telegram_message_id,
    DROP COLUMN IF EXISTS chat_id,
    DROP COLUMN IF EXISTS sender,
    DROP COLUMN IF EXISTS received_at,
    DROP COLUMN IF EXISTS source,
    DROP COLUMN IF EXISTS external_chat_id;
