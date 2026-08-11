package com.glass.agent.agent;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对话记忆的核心行为：按会话隔离、追加历史、超窗口裁剪、可清空。
 */
class ConversationStoreTest {

    private final ConversationStore store = new ConversationStore();

    @Test
    void recordsAndIsolatesByConversationId() {
        store.record("a", new UserMessage("我近视-3.00"), new AssistantMessage("请问主要用途？"));
        assertEquals(2, store.history("a").size());
        assertTrue(store.history("b").isEmpty());
    }

    @Test
    void trimsToWindowLimit() {
        for (int i = 0; i < ConversationStore.MAX_MESSAGES; i++) {
            store.record("a", new UserMessage("u" + i), new AssistantMessage("a" + i));
        }
        assertEquals(ConversationStore.MAX_MESSAGES, store.history("a").size());
    }

    @Test
    void clearRemovesHistory() {
        store.record("a", new UserMessage("hi"), new AssistantMessage("你好"));
        store.clear("a");
        assertTrue(store.history("a").isEmpty());
    }
}
