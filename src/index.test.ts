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
    "pupillary_distance_guide",
    "lens_coating_advisor",
    "myopia_control_guide",
    "anisometropia_guide",
  ]);
});

test("anisometropia guide grades a large spherical-equivalent difference as significant and flags aniseikonia over tolerance", () => {
  const tool = getTool("anisometropia_guide");
  const result = tool.handler({ right_sph: -1, left_sph: -5 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // SE diff = 4.00D → significant
  assert.match(text, /等效球镜差：4D → \*\*显著屈光参差\*\*/);
  // 4.00D * 1.5%/D = 6% > 5% tolerance
  assert.match(text, /约 6%/);
  assert.match(text, /超过一般耐受上限/);
  // significant → contacts / professional evaluation
  assert.match(text, /建议优先考虑隐形眼镜/);
});

test("anisometropia guide treats matched eyes as no meaningful difference and folds cylinder into the equivalent", () => {
  const tool = getTool("anisometropia_guide");
  // right SE = -2.375, left SE = -2.75 → diff 0.375 < 1 despite a 0.75D sphere gap
  const result = tool.handler({ right_sph: -2, left_sph: -2, right_cyl: -0.75, left_cyl: -1.5 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /→ \*\*无明显参差\*\*/);
  assert.match(text, /按验光度数配框架镜通常没有额外适应问题/);
});

test("anisometropia guide detects antimetropia (one myopic, one hyperopic)", () => {
  const tool = getTool("anisometropia_guide");
  const result = tool.handler({ right_sph: -1.5, left_sph: 1.5 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /混合性屈光参差/);
});

test("anisometropia guide warns about a large cylinder difference", () => {
  const tool = getTool("anisometropia_guide");
  const result = tool.handler({ right_sph: -2, left_sph: -2, right_cyl: 0, left_cyl: -2 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /两眼柱镜相差约/);
  assert.match(text, /子午线方向上的影像倾斜/);
});

test("anisometropia guide rejects an out-of-range sphere", () => {
  const tool = getTool("anisometropia_guide");
  assert.throws(() => tool.handler({ right_sph: -99, left_sph: -1 }), /right_sph/);
});

test("myopia control guide flags high risk and prioritizes outdoor time for a young, fast-progressing child", () => {
  const tool = getTool("myopia_control_guide");
  const result = tool.handler({
    age: 8,
    current_sph: -2,
    annual_progression: 1,
    parent_myopia: "both",
    outdoor_hours: 0.5,
  });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /综合判断：高风险/);
  // outdoor under 2h → prioritized
  assert.match(text, /户外活动.*：最该优先补上/);
  // myopic → defocus lens is a day-to-day first choice
  assert.match(text, /近视离焦框架镜片.*：日常配镜首选/);
  // fast progression → atropine consult recommended
  assert.match(text, /低浓度阿托品.*：建议就诊咨询/);
});

test("myopia control guide holds ortho-K for children under 8 and treats pre-myopia specially", () => {
  const tool = getTool("myopia_control_guide");
  const result = tool.handler({ age: 6, current_sph: 0.25 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /尚未达到近视标准/);
  assert.match(text, /角膜塑形镜.*：年龄偏小，暂不考虑/);
  // pre-myopia caution about preserving hyperopia reserve
  assert.match(text, /保住远视储备/);
});

test("myopia control guide considers ortho-K needs special evaluation for high myopia", () => {
  const tool = getTool("myopia_control_guide");
  const result = tool.handler({ age: 13, current_sph: -6.5 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /高度近视/);
  assert.match(text, /角膜塑形镜.*：需专业评估（度数偏高）/);
  assert.match(text, /眼底检查列为常规项目/);
});

test("myopia control guide rejects an out-of-range age", () => {
  const tool = getTool("myopia_control_guide");
  assert.throws(() => tool.handler({ age: 25, current_sph: -1 }), /age/);
});

test("lens coating advisor treats base coating as standard and downgrades blue-light for light screen users", () => {
  const tool = getTool("lens_coating_advisor");
  const result = tool.handler({ screen_hours: 2, outdoor_frequency: "rare" });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /基础膜层.*标配/);
  // Light screen use → blue-light lands in the "通常不必额外花钱" bucket
  assert.match(text, /防蓝光膜/);
  assert.match(text, /通常不必额外花钱[\s\S]*防蓝光/);
});

test("lens coating advisor strongly recommends UV and polarized for frequent outdoor use", () => {
  const tool = getTool("lens_coating_advisor");
  const result = tool.handler({
    screen_hours: 9,
    outdoor_frequency: "often",
    light_sensitive: true,
  });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /UV 防护（UV400）\*\*：强烈推荐/);
  assert.match(text, /偏振太阳镜.*\*\*：推荐/);
});

test("lens coating advisor recommends photochromic when one pair is preferred and warns about night-vision lenses", () => {
  const tool = getTool("lens_coating_advisor");
  const result = tool.handler({
    screen_hours: 5,
    outdoor_frequency: "sometimes",
    night_driving: "frequent",
    prefer_one_pair: true,
  });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /变色片（光致变色）\*\*：推荐/);
  assert.match(text, /夜视 \/ 防远光/);
});

test("lens coating advisor rejects out-of-range screen hours", () => {
  const tool = getTool("lens_coating_advisor");
  assert.throws(() => tool.handler({ screen_hours: 30, outdoor_frequency: "rare" }), /screen_hours/);
});

test("pupillary distance guide derives binocular PD from monocular readings and flags asymmetry", () => {
  const tool = getTool("pupillary_distance_guide");
  const result = tool.handler({ pd_right: 30, pd_left: 34 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // 30 + 34 = 64mm binocular
  assert.match(text, /64 mm（由左右单眼相加得到）/);
  assert.match(text, /明显不对称/);
});

test("pupillary distance guide computes a smaller near PD than distance PD", () => {
  const tool = getTool("pupillary_distance_guide");
  const result = tool.handler({ binocular_pd: 63, working_distance_cm: 40 });

  assert.equal(result.isError, undefined);
  // near PD = 63 * 400 / 427 ≈ 59.0mm, reduction ≈ 4.0mm
  assert.match(result.content[0].text, /约 59 mm（比远用约小 4 mm）/);
});

test("pupillary distance guide requires both monocular values together", () => {
  const tool = getTool("pupillary_distance_guide");
  assert.throws(() => tool.handler({ pd_right: 31 }), /需要左右眼一起提供/);
});

test("pupillary distance guide requires at least one PD input", () => {
  const tool = getTool("pupillary_distance_guide");
  assert.throws(() => tool.handler({}), /请至少提供双眼瞳距/);
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
