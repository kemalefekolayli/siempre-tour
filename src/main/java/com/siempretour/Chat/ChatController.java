package com.siempretour.Chat;

import com.siempretour.Chat.Dto.ChatRequest;
import com.siempretour.Chat.Dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Public chat endpoint backing js/chat-widget.js. */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/api/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request));
    }
}
