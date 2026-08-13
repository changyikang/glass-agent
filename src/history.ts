/**
 * In-memory tool-call history shared by the MCP stdio server and the web debug page.
 * Keeps the most recent calls only; resets whenever the process restarts.
 */

export type HistoryEntry = {
  id: number;
  tool: string;
  arguments: unknown;
  isError: boolean;
  summary: string;
  timestamp: string;
};

type RecordableResult = {
  content: Array<{ type: "text"; text: string }>;
  isError?: boolean;
};

const MAX_HISTORY = 50;
const SUMMARY_LIMIT = 160;

const entries: HistoryEntry[] = [];
let nextId = 1;

export function recordCall(tool: string, args: unknown, result: RecordableResult): HistoryEntry {
  const text = result.content[0]?.text ?? "";
  const entry: HistoryEntry = {
    id: nextId++,
    tool,
    arguments: args ?? {},
    isError: result.isError === true,
    summary: text.length > SUMMARY_LIMIT ? `${text.slice(0, SUMMARY_LIMIT)}…` : text,
    timestamp: new Date().toISOString(),
  };

  entries.push(entry);
  while (entries.length > MAX_HISTORY) {
    entries.shift();
  }

  return entry;
}

/** Most recent call first; pass `limit` to cap how many entries come back. */
export function getHistory(limit?: number): HistoryEntry[] {
  const ordered = [...entries].reverse();
  return typeof limit === "number" ? ordered.slice(0, Math.max(0, limit)) : ordered;
}

export function clearHistory(): void {
  entries.length = 0;
}
