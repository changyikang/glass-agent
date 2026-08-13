import test from "node:test";
import assert from "node:assert/strict";
import { executeTool, tools } from "./index.js";

function getTool(name: string) {
  const tool = tools.find((item) => item.name === name);
  assert.ok(tool, `expected tool ${name} to exist`);
  return tool;
}

test("exports the expected MCP tool set", () => {
  const toolNames = tools.map((tool) => tool.name);

  assert.deepEqual(toolNames, [
    "vision_check_guide",
    "lens_recommendation",
    "frame_selection_guide",
    "prescription_interpreter",
    "progressive_lens_assessment",
    "new_glasses_troubleshooting",
    "shopping_links",
  ]);
});

test("shopping links builds per-platform search URLs and encodes keywords", () => {
  const tool = getTool("shopping_links");
  const result = tool.handler({ keywords: ["1.67 非球面 防蓝光 镜片", "  "] });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  const encoded = encodeURIComponent("1.67 非球面 防蓝光 镜片");
  assert.match(text, new RegExp(`https://search\\.jd\\.com/Search\\?keyword=${encoded}`));
  assert.ok(text.includes("淘宝") && text.includes("拼多多"));
});

test("shopping links rejects an empty keyword list", () => {
  const tool = getTool("shopping_links");
  assert.throws(() => tool.handler({ keywords: [] }), /至少要包含一个非空关键词/);
});

test("lens recommendation returns domain-specific driving guidance", () => {
  const tool = getTool("lens_recommendation");
  const result = tool.handler({
    sph: -4.5,
    cyl: -1.25,
    usage: "driving",
    budget: "mid",
  });

  assert.equal(result.isError, undefined);
  assert.match(result.content[0].text, /高透光率镜片 \+ 更好的防眩镀膜/);
  assert.match(result.content[0].text, /偏光镜适合白天强光环境/);
});

test("prescription interpreter rejects astigmatism without axis", () => {
  const tool = getTool("prescription_interpreter");
  assert.throws(
    () =>
      tool.handler({
        od_sph: -3,
        od_cyl: -1,
        os_sph: -2.5,
      }),
    /右眼有散光时必须提供轴位/
  );
});

test("executeTool returns MCP-style error result for unknown tools", () => {
  const result = executeTool("missing_tool", {});

  assert.equal(result.isError, true);
  assert.match(result.content[0].text, /未知工具/);
});

test("every tool ships a sample payload that runs successfully", () => {
  for (const tool of tools) {
    assert.ok(tool.sample, `expected ${tool.name} to have a sample payload`);
    const result = executeTool(tool.name, tool.sample);
    assert.equal(result.isError, undefined, `sample for ${tool.name} should not error: ${result.content[0]?.text}`);
  }
});
