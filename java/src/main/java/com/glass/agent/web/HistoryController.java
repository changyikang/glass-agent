package com.glass.agent.web;

import java.util.List;
import java.util.Map;

import com.glass.agent.tool.ToolCallHistory;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工具调用历史的 REST 接口，与原 TypeScript 版本 {@code /api/history} 对应。
 *
 * <ul>
 *   <li>{@code GET /api/history?limit=N} —— 最新在前，可用 {@code limit} 限制条数；</li>
 *   <li>{@code DELETE /api/history} —— 清空历史。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final ToolCallHistory history;

    public HistoryController(ToolCallHistory history) {
        this.history = history;
    }

    @GetMapping
    public List<ToolCallHistory.Entry> list(@RequestParam(name = "limit", defaultValue = "0") int limit) {
        return history.list(limit);
    }

    @DeleteMapping
    public Map<String, Object> clear() {
        history.clear();
        return Map.of("cleared", true);
    }
}
