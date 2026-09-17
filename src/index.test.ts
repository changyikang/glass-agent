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
    "contact_lens_power",
    "reading_add_estimator",
    "prescription_transpose",
    "frame_fit_calculator",
    "visual_acuity_converter",
  ]);
});

test("visual acuity converter maps decimal 1.0 to all four equivalent notations", () => {
  const tool = getTool("visual_acuity_converter");
  const result = tool.handler({ notation: "decimal", value: 1.0 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /小数记录法：\*\*1\*\*/);
  assert.match(text, /五分记录法（对数）：\*\*5\*\*/);
  assert.match(text, /Snellen（美制\/公制）：\*\*20\/20\*\*/);
  assert.match(text, /logMAR：\*\*0\*\*/);
  assert.match(text, /视力水平：\*\*正常或以上\*\*/);
});

test("visual acuity converter converts a five-minute log reading back to decimal and Snellen", () => {
  const tool = getTool("visual_acuity_converter");
  // 五分 4.7 → 小数 10^(-0.3) ≈ 0.5 → 20/40
  const result = tool.handler({ notation: "five_minute", value: 4.7 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /小数记录法：\*\*0\.5\*\*/);
  assert.match(text, /Snellen（美制\/公制）：\*\*20\/40\*\*/);
  assert.match(text, /轻度下降/);
});

test("visual acuity converter accepts a Snellen fraction via denominator + numerator", () => {
  const tool = getTool("visual_acuity_converter");
  const result = tool.handler({ notation: "snellen", value: 200, snellen_numerator: 20 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /小数记录法：\*\*0\.1\*\*/);
  assert.match(text, /五分记录法（对数）：\*\*4\*\*/);
  assert.match(text, /logMAR：\*\*1\*\*/);
});

test("visual acuity converter rejects an out-of-range decimal value", () => {
  const tool = getTool("visual_acuity_converter");
  assert.throws(() => tool.handler({ notation: "decimal", value: 0 }), /0 到 3/);
});

test("frame fit calculator computes frame PD and inward decentration for a wider frame", () => {
  const tool = getTool("frame_fit_calculator");
  const result = tool.handler({ lens_width: 52, bridge: 18, pd: 62, power: -4 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // frame PD = 52 + 18 = 70; decentration = (70 - 62)/2 = 4 per eye, inward
  assert.match(text, /镜架几何中心距（框 PD）：\*\*70 mm\*\*/);
  assert.match(text, /每片移心量：\*\*4 mm\*\*（向鼻侧内移）/);
  assert.match(text, /贴合评估：\*\*偏大\*\*/);
  // Prentice: 4mm = 0.4cm × 4D = 1.6Δ
  assert.match(text, /每片约产生 \*\*1\.6Δ\*\* 水平棱镜/);
  assert.match(text, /该棱镜量已不可忽略/);
});

test("frame fit calculator flags a narrower frame needing outward decentration", () => {
  const tool = getTool("frame_fit_calculator");
  const result = tool.handler({ lens_width: 48, bridge: 16, pd: 68 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // frame PD = 64; decentration = (64 - 68)/2 = -2 per eye, outward
  assert.match(text, /镜架几何中心距（框 PD）：\*\*64 mm\*\*/);
  assert.match(text, /每片移心量：\*\*2 mm\*\*（向颞侧外移）/);
  assert.match(text, /镜架偏窄/);
});

test("frame fit calculator rates a well-matched frame and skips prism without power", () => {
  const tool = getTool("frame_fit_calculator");
  const result = tool.handler({ lens_width: 50, bridge: 12, pd: 62 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // frame PD = 62 == PD → decentration 0 → 很合适
  assert.match(text, /每片移心量：\*\*0 mm\*\*（几乎无需移心）/);
  assert.match(text, /贴合评估：\*\*很合适\*\*/);
  assert.doesNotMatch(text, /水平棱镜/);
});

test("frame fit calculator requires the frame and PD measurements", () => {
  const tool = getTool("frame_fit_calculator");
  assert.throws(() => tool.handler({ lens_width: 52, bridge: 18 }), /pd/);
});

test("prescription transpose converts minus-cyl to plus-cyl and rotates the axis 90 degrees", () => {
  const tool = getTool("prescription_transpose");
  const result = tool.handler({ sph: -2, cyl: -0.75, axis: 180 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // new sph = -2 + -0.75 = -2.75, new cyl = +0.75, new axis = 180 -> 90
  assert.match(text, /\*\*SPH -2\.75D \/ CYL \+0\.75D \/ AXIS 90°\*\*/);
  assert.match(text, /转换结果（正柱镜（正散光））/);
  // spherical equivalent invariant: -2 + -0.75/2 = -2.375
  assert.match(text, /等效球镜（SPH \+ CYL\/2）转换前后不变，均为 -2\.38D/);
});

test("prescription transpose converts plus-cyl to minus-cyl and wraps the axis under 90", () => {
  const tool = getTool("prescription_transpose");
  const result = tool.handler({ sph: 1, cyl: 1.5, axis: 60 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // new sph = 1 + 1.5 = 2.50, new cyl = -1.50, new axis = 60 + 90 = 150
  assert.match(text, /\*\*SPH \+2\.5D \/ CYL -1\.5D \/ AXIS 150°\*\*/);
  assert.match(text, /转换结果（负柱镜（负散光））/);
});

test("prescription transpose reports that a pure sphere has no cylinder form", () => {
  const tool = getTool("prescription_transpose");
  const result = tool.handler({ sph: -3, cyl: 0 });

  assert.equal(result.isError, undefined);
  assert.match(result.content[0].text, /无散光.*无需转换/s);
});

test("prescription transpose requires an axis when there is cylinder", () => {
  const tool = getTool("prescription_transpose");
  assert.throws(() => tool.handler({ sph: -2, cyl: -0.75 }), /轴位/);
});

test("reading add estimator gives a typical add for a 50-year-old at 40cm and computes near total", () => {
  const tool = getTool("reading_add_estimator");
  const result = tool.handler({ age: 50, working_distance_cm: 40, distance_sph: -2 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // age 50 → +2.00 base, 40cm reference → no distance adjust
  assert.match(text, /建议近附加（下加光 ADD）：\*\*约 \+2D\*\*/);
  // near total = -2.00 + 2.00 = 0.00
  assert.match(text, /= \*\*0D\*\*/);
  assert.match(text, /主要用眼距离：40 cm/);
});

test("reading add estimator increases the add for a closer working distance", () => {
  const tool = getTool("reading_add_estimator");
  const result = tool.handler({ age: 50, working_distance_cm: 33 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // base +2.00, adjust = 100/33 - 100/40 = 3.03 - 2.50 = +0.53 → 2.50 rounded to 0.25
  assert.match(text, /建议近附加（下加光 ADD）：\*\*约 \+2\.5D\*\*/);
  assert.match(text, /比参考的 40cm 更近/);
});

test("reading add estimator recommends no add below the presbyopia onset age", () => {
  const tool = getTool("reading_add_estimator");
  const result = tool.handler({ age: 32 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /\+0\.00D（暂不需要）/);
  assert.match(text, /通常不需要近附加/);
});

test("reading add estimator caps the add at the practical maximum", () => {
  const tool = getTool("reading_add_estimator");
  const result = tool.handler({ age: 65, working_distance_cm: 20 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // base 2.50 + (100/20 - 2.50 = 2.50) = 5.00, capped at 3.50
  assert.match(text, /建议近附加（下加光 ADD）：\*\*约 \+3\.5D\*\*/);
  assert.match(text, /已达上限/);
});

test("reading add estimator rejects a non-integer age", () => {
  const tool = getTool("reading_add_estimator");
  assert.throws(() => tool.handler({ age: 50.5 }), /age/);
});

test("contact lens power compensates a strong myope down and rounds to 0.25D steps", () => {
  const tool = getTool("contact_lens_power");
  const result = tool.handler({ sph: -6, vertex_distance_mm: 12 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // -6 / (1 - 0.012 * -6) = -6 / 1.072 = -5.597 → nearest 0.25 = -5.50
  assert.match(text, /隐形眼镜光度：\*\*SPH -5\.5D\*\*/);
  assert.match(text, /需要顶点补偿的量级/);
  assert.match(text, /近视换算成隐形后度数会变浅/);
});

test("contact lens power compensates a strong hyperope up", () => {
  const tool = getTool("contact_lens_power");
  const result = tool.handler({ sph: 5, vertex_distance_mm: 12 });

  assert.equal(result.isError, undefined);
  // 5 / (1 - 0.012 * 5) = 5 / 0.94 = 5.319 → nearest 0.25 = 5.25
  assert.match(result.content[0].text, /隐形眼镜光度：\*\*SPH \+5\.25D\*\*/);
  assert.match(result.content[0].text, /远视换算成隐形后度数会变深/);
});

test("contact lens power treats low powers as needing no meaningful compensation", () => {
  const tool = getTool("contact_lens_power");
  const result = tool.handler({ sph: -2 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // -2 / 1.024 = -1.953 → -2.00, same as input
  assert.match(text, /隐形眼镜光度：\*\*SPH -2D\*\*/);
  assert.match(text, /顶点补偿量很小/);
});

test("contact lens power folds astigmatism and offers a spherical-equivalent option", () => {
  const tool = getTool("contact_lens_power");
  const result = tool.handler({ sph: -6, cyl: -0.75, vertex_distance_mm: 12 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /散光隐形/);
  assert.match(text, /折算等效球镜/);
});

test("contact lens power rejects an out-of-range vertex distance", () => {
  const tool = getTool("contact_lens_power");
  assert.throws(
    () => tool.handler({ sph: -3, vertex_distance_mm: 40 }),
    /vertex_distance_mm/
  );
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
