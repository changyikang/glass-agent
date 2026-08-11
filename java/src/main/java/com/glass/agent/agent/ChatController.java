package com.glass.agent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能体对话接口。用户发一句自然语言，大模型自行决定是否调用配镜工具并组织回答。
 *
 * <p>需要配置大模型 API Key（环境变量 {@code AI_API_KEY}）。未配置时调用会失败，
 * 此时可改用 {@code /api/tools/**} 直接调用工具。
 */
@RestController
@RequestMapping("/api/agent")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient glassChatClient) {
        this.chatClient = glassChatClient;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse(null, "message 不能为空"));
        }
        try {
            String reply = chatClient.prompt()
                    .user(request.message())
                    .call()
                    .content();
            return ResponseEntity.ok(new ChatResponse(reply, null));
        } catch (Exception ex) {
            String hint = "调用大模型失败：" + ex.getMessage()
                    + "。请确认已配置 AI_API_KEY 与 spring.ai.openai.base-url / options.model。"
                    + "在未接入大模型时，可改用 /api/tools/** 直接调用工具。";
            return ResponseEntity.status(502).body(new ChatResponse(null, hint));
        }
    }

    public record ChatRequest(String message) {
    }

    public record ChatResponse(String reply, String error) {
    }
}
