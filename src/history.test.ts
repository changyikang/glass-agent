import test from "node:test";
import assert from "node:assert/strict";
import { recordCall, getHistory, clearHistory } from "./history.js";

test("recordCall stores the most recent call first and caps the summary length", () => {
  clearHistory();
  recordCall("vision_check_guide", { age_group: "adult" }, {
    content: [{ type: "text", text: "a".repeat(200) }],
  });
  recordCall("lens_recommendation", { sph: -3 }, {
    content: [{ type: "text", text: "第二次调用" }],
  });

  const history = getHistory();
  assert.equal(history.length, 2);
  assert.equal(history[0].tool, "lens_recommendation");
  assert.equal(history[1].tool, "vision_check_guide");
  assert.equal(history[1].summary.length, 161);
  assert.ok(history[1].summary.endsWith("…"));
  assert.equal(history[0].isError, false);
});

test("recordCall marks error results and getHistory respects the limit", () => {
  clearHistory();
  recordCall("prescription_interpreter", {}, {
    content: [{ type: "text", text: "错误：缺少必填参数" }],
    isError: true,
  });
  recordCall("shopping_links", { keywords: ["镜框"] }, {
    content: [{ type: "text", text: "ok" }],
  });

  assert.equal(getHistory()[1].isError, true);
  assert.equal(getHistory(1).length, 1);
  assert.equal(getHistory(1)[0].tool, "shopping_links");
});

test("history is capped at the most recent 50 calls", () => {
  clearHistory();
  for (let i = 0; i < 55; i++) {
    recordCall("vision_check_guide", { age_group: "adult" }, {
      content: [{ type: "text", text: `call ${i}` }],
    });
  }

  const history = getHistory();
  assert.equal(history.length, 50);
  assert.equal(history[0].summary, "call 54");
  assert.equal(history[49].summary, "call 5");
});

test("clearHistory empties the log", () => {
  clearHistory();
  recordCall("shopping_links", { keywords: ["镜片"] }, {
    content: [{ type: "text", text: "ok" }],
  });
  assert.equal(getHistory().length, 1);

  clearHistory();
  assert.equal(getHistory().length, 0);
});
