package com.glass.agent.tool;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工具调用历史的核心行为：最新在前、摘要截断、标记失败、按 limit 取数、超窗口裁剪、可清空。
 * 对照原 TypeScript 版本 {@code src/history.test.ts}。
 */
class ToolCallHistoryTest {

    private final ToolCallHistory history = new ToolCallHistory();

    @Test
    void recordsNewestFirstAndTruncatesSummary() {
        history.record("vision_check_guide", Map.of("age_group", "adult"), "a".repeat(200), false);
        history.record("lens_recommendation", Map.of("sph", -3), "第二次调用", false);

        List<ToolCallHistory.Entry> list = history.list(0);
        assertEquals(2, list.size());
        assertEquals("lens_recommendation", list.get(0).tool());
        assertEquals("vision_check_guide", list.get(1).tool());
        assertEquals(ToolCallHistory.SUMMARY_LIMIT + 1, list.get(1).summary().length());
        assertTrue(list.get(1).summary().endsWith("…"));
        assertFalse(list.get(0).isError());
    }

    @Test
    void marksErrorsAndRespectsLimit() {
        history.record("prescription_interpreter", Map.of(), "错误：缺少必填参数", true);
        history.record("shopping_links", Map.of("keywords", List.of("镜框")), "ok", false);

        assertTrue(history.list(0).get(1).isError());
        assertEquals(1, history.list(1).size());
        assertEquals("shopping_links", history.list(1).get(0).tool());
    }

    @Test
    void trimsToMaxHistory() {
        for (int i = 0; i < ToolCallHistory.MAX_HISTORY + 5; i++) {
            history.record("vision_check_guide", Map.of("age_group", "adult"), "call " + i, false);
        }

        List<ToolCallHistory.Entry> list = history.list(0);
        assertEquals(ToolCallHistory.MAX_HISTORY, list.size());
        assertEquals("call " + (ToolCallHistory.MAX_HISTORY + 4), list.get(0).summary());
    }

    @Test
    void clearEmptiesHistory() {
        history.record("shopping_links", Map.of("keywords", List.of("镜片")), "ok", false);
        assertEquals(1, history.list(0).size());

        history.clear();
        assertTrue(history.list(0).isEmpty());
    }
}
