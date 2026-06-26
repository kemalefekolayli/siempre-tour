package com.siempretour.Chat.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** Payload sent by js/chat-widget.js to POST /api/chat. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatRequest {
    private String message;
    private String language;            // "tr" | "en"
    private List<ChatMessage> history;  // recent turns (last ~10)

    // The widget also sends a client-side systemPrompt; it is intentionally
    // ignored. The authoritative prompt is fixed server-side (see ChatService)
    // to prevent prompt-injection / turning the bot into a general assistant.
    private String systemPrompt;
}
