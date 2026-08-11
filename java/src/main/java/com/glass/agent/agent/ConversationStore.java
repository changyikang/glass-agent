package com.glass.agent.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

/**
 * 极简的进程内对话记忆。按 {@code conversationId} 保存最近若干轮消息，
 * 让「配镜顾问」智能体能进行多轮问诊：先追问必要信息，用户回答后再给出建议。
 *
 * <p>仅存放在内存中，进程重启即清空；生产环境可替换为 Redis 等持久化实现。
 * 每个会话只保留最近 {@link #MAX_MESSAGES} 条消息，避免上下文无限增长。
 */
@Component
public class ConversationStore {

    /** 单个会话保留的最大消息数（约等于最近 10 轮问答）。 */
    static final int MAX_MESSAGES = 20;

    private final Map<String, List<Message>> store = new ConcurrentHashMap<>();

    /** 返回该会话的历史消息快照（不含本轮新消息）。 */
    public List<Message> history(String conversationId) {
        List<Message> messages = store.get(conversationId);
        return messages == null ? List.of() : List.copyOf(messages);
    }

    /** 记录一轮问答（用户消息 + 助手回复），并裁剪到窗口上限。 */
    public void record(String conversationId, Message userMessage, Message assistantMessage) {
        store.compute(conversationId, (id, existing) -> {
            List<Message> messages = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            messages.add(userMessage);
            messages.add(assistantMessage);
            while (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
            return messages;
        });
    }

    /** 清空某个会话（例如用户重新开始配镜）。 */
    public void clear(String conversationId) {
        store.remove(conversationId);
    }
}
