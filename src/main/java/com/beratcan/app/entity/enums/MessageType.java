package com.beratcan.app.entity.enums;

/**
 * AI Chat mesaj tipi
 */
public enum MessageType {
    /**
     * Kullanıcı text mesajı
     */
    TEXT,

    /**
     * Kullanıcı voice mesajı (text'e çevrilmiş)
     */
    VOICE_INPUT,

    /**
     * AI text yanıtı
     */
    ASSISTANT_TEXT,

    /**
     * AI voice yanıtı (TTS ile üretilmiş, ama text saklanır)
     */
    ASSISTANT_VOICE
}
