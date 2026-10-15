package com.unifiedinbox.unified_inbox.integration.telegram;

public class TelegramFileTooLargeException extends RuntimeException {

    public TelegramFileTooLargeException() {
        super("Telegram file exceeds download size limit");
    }
}