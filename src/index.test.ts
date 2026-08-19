import test from "node:test";
import assert from "node:assert/strict";
import { executeTool, tools } from "./index.js";
import { getHistory, clearHistory } from "./history.js";

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
    "lens_thickness_estimator",
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

test("lens thickness estimator recommends a higher index for strong myopia", () => {
  const tool = getTool("lens_thickness_estimator");
  const result = tool.handler({ sph: -8, lens_index: "1.56", frame_width: 52 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // power 8, effective diameter 56 (r=28): sag = 8*784/(2000*0.44)... n=1.56 → 2000*0.56
  // sag = 8*784/1120 = 5.6, edge = 1.2 + 5.6 = 6.8mm
  assert.match(text, /边缘最厚：约 6\.8 mm/);
  assert.match(text, /建议提高到 1\.74/);
});

test("lens thickness estimator reports center thickness for plus lenses", () => {
  const tool = getTool("lens_thickness_estimator");
  const result = tool.handler({ sph: 5, lens_index: "1.60", frame_width: 52 });

  assert.equal(result.isError, undefined);
  assert.match(result.content[0].text, /中心最厚/);
  assert.match(result.content[0].text, /正镜片/);
});

test("lens thickness estimator rejects an unsupported index", () => {
  const tool = getTool("lens_thickness_estimator");
  assert.throws(() => tool.handler({ sph: -3, lens_index: "1.50" }), /lens_index/);
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

test("executeTool records every call in the shared history log", () => {
  clearHistory();
  executeTool("vision_check_guide", { age_group: "adult" });
  executeTool("missing_tool", {});

  const history = getHistory();
  assert.equal(history.length, 2);
  assert.equal(history[0].tool, "missing_tool");
  assert.equal(history[0].isError, true);
  assert.equal(history[1].tool, "vision_check_guide");
  assert.equal(history[1].isError, false);
});
