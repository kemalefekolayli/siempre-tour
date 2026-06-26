package com.siempretour.Chat.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A single prior turn in the conversation, as sent by the chat widget. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {
    private String role;     // "user" | "assistant"
    private String content;
    private Boolean failed;  // set by the widget on error bubbles; skipped server-side
}
