package com.siempretour.Chat.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response consumed by the widget: it reads `data.message`. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
}
