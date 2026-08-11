package com.glass.agent.agent;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能体对话接口。用户发一句自然语言，大模型自行决定是否调用配镜工具并组织回答。
 *
 * <p>支持多轮问诊：请求可携带 {@code conversationId}，服务端按该 id 记忆最近若干轮对话，
 * 于是智能体能先追问必要信息（度数、用途、预算、脸型等），用户回答后再给出配镜建议与购买链接。
 * 不传 {@code conversationId} 时统一归到 {@code "default"} 会话。
 *
 * <p>需要配置大模型 API Key（环境变量 {@code AI_API_KEY}）。未配置时调用会失败，
 * 此时可改用 {@code /api/tools/**} 直接调用工具。
 */
@RestController
@RequestMapping("/api/agent")
public class ChatController {

    private final ChatClient chatClient;
    private final ConversationStore conversations;

    public ChatController(ChatClient glassChatClient, ConversationStore conversations) {
        this.chatClient = glassChatClient;
        this.conversations = conversations;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse(null, null, "message 不能为空"));
        }
        String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? "default"
                : request.conversationId().trim();
        try {
            UserMessage userMessage = new UserMessage(request.message());
            List<Message> messages = new ArrayList<>(conversations.history(conversationId));
            messages.add(userMessage);

            String reply = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();

            conversations.record(conversationId, userMessage, new AssistantMessage(reply));
            return ResponseEntity.ok(new ChatResponse(reply, conversationId, null));
        } catch (Exception ex) {
            String hint = "调用大模型失败：" + ex.getMessage()
                    + "。请确认已配置 AI_API_KEY 与 spring.ai.openai.base-url / options.model。"
                    + "在未接入大模型时，可改用 /api/tools/** 直接调用工具。";
            return ResponseEntity.status(502).body(new ChatResponse(null, conversationId, hint));
        }
    }

    public record ChatRequest(String message, String conversationId) {
    }

    public record ChatResponse(String reply, String conversationId, String error) {
    }
}
