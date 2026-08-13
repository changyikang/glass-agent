package com.glass.agent.tool;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/**
 * 进程内工具调用历史，与原 TypeScript 版本 {@code src/history.ts} 对应。
 *
 * <p>记录最近若干次工具调用（含成功与失败），供 REST 接口 {@code /api/history} 查询与清空。
 * 仅存放在内存中，进程重启即清空；只保留最近 {@link #MAX_HISTORY} 条，避免无限增长。
 */
@Component
public class ToolCallHistory {

    /** 保留的最大历史条数。 */
    static final int MAX_HISTORY = 50;

    /** 摘要文本的最大长度，超出部分截断并追加省略号。 */
    static final int SUMMARY_LIMIT = 160;

    private final Deque<Entry> entries = new ArrayDeque<>();
    private final AtomicLong nextId = new AtomicLong(1);

    /** 记录一次调用，返回刚写入的条目。 */
    public synchronized Entry record(String tool, Object arguments, String output, boolean isError) {
        String text = output == null ? "" : output;
        String summary = text.length() > SUMMARY_LIMIT ? text.substring(0, SUMMARY_LIMIT) + "…" : text;
        Entry entry = new Entry(nextId.getAndIncrement(), tool, arguments, isError, summary, Instant.now().toString());

        entries.addLast(entry);
        while (entries.size() > MAX_HISTORY) {
            entries.removeFirst();
        }
        return entry;
    }

    /** 最新的调用排在最前面；{@code limit} 为非正数时返回全部。 */
    public synchronized List<Entry> list(int limit) {
        List<Entry> ordered = new ArrayList<>(entries);
        java.util.Collections.reverse(ordered);
        if (limit > 0 && ordered.size() > limit) {
            return List.copyOf(ordered.subList(0, limit));
        }
        return List.copyOf(ordered);
    }

    /** 清空历史。 */
    public synchronized void clear() {
        entries.clear();
    }

    /** 一条工具调用记录。 */
    public record Entry(long id, String tool, Object arguments, boolean isError, String summary, String timestamp) {
    }
}
