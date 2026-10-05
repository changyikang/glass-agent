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
    "accommodation_amplitude",
    "sunglass_tint_guide",
    "lens_material_advisor",
    "clear_vision_range",
    "near_acuity_converter",
  ]);
});

test("near acuity converter: reading 1M at 40cm is 0.4 decimal (20/50)", () => {
  const tool = getTool("near_acuity_converter");
  // decimal = testDist(m) / M = 0.4 / 1.0 = 0.4 → 20/50, 8pt
  const result = tool.handler({ notation: "m_unit", value: 1.0 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /印张尺寸（M 记法）：\*\*1 M\*\*/);
  assert.match(text, /印刷点数（约）：\*\*8 pt\*\*/);
  assert.match(text, /该距离近视力（小数）：\*\*0\.4\*\*/);
  assert.match(text, /Snellen 等效：\*\*20\/50\*\*/);
});

test("near acuity converter: point-size input converts to M via ÷8", () => {
  const tool = getTool("near_acuity_converter");
  // 16pt → 2M; at 40cm decimal = 0.4/2 = 0.2 → 20/100
  const result = tool.handler({ notation: "point", value: 16 });

  const text = result.content[0].text;
  assert.match(text, /印张尺寸（M 记法）：\*\*2 M\*\*/);
  assert.match(text, /该距离近视力（小数）：\*\*0\.2\*\*/);
  assert.match(text, /Snellen 等效：\*\*20\/100\*\*/);
});

test("near acuity converter: a nearer test distance improves the acuity for the same M", () => {
  const tool = getTool("near_acuity_converter");
  // 1M at 25cm → decimal = 0.25/1 = 0.25 → 20/80 (vs 0.4 at 40cm)
  const result = tool.handler({ notation: "m_unit", value: 1.0, test_distance_cm: 25 });

  const text = result.content[0].text;
  assert.match(text, /测试距离 25 cm/);
  assert.match(text, /该距离近视力（小数）：\*\*0\.25\*\*/);
  assert.match(text, /Snellen 等效：\*\*20\/80\*\*/);
});

test("near acuity converter: decimal input is echoed back and gives the equivalent M", () => {
  const tool = getTool("near_acuity_converter");
  // decimal 0.5 at 40cm → M = 0.4/0.5 = 0.8 → 6.4 ≈ 6pt
  const result = tool.handler({ notation: "decimal", value: 0.5 });

  const text = result.content[0].text;
  assert.match(text, /近视力小数 0\.5/);
  assert.match(text, /印张尺寸（M 记法）：\*\*0\.8 M\*\*/);
  assert.match(text, /该距离近视力（小数）：\*\*0\.5\*\*/);
});

test("near acuity converter rejects out-of-range values", () => {
  const tool = getTool("near_acuity_converter");
  assert.throws(() => tool.handler({ notation: "m_unit", value: 50 }), /M 记法/);
  assert.throws(() => tool.handler({ notation: "decimal", value: 5 }), /近视力小数/);
});

test("clear vision range gives a myope's far point and the摘镜看近清楚 explanation", () => {
  const tool = getTool("clear_vision_range");
  // SE = -2.00 → far point = 100/2 = 50cm
  const result = tool.handler({ sph: -2 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /低度近视（SE -2D）/);
  assert.match(text, /远点（能看清的最远处）：\*\*50 cm\*\*/);
  assert.match(text, /摘镜看手机 \/ 看书反而清楚/);
  // no age → near point is left to accommodation
  assert.match(text, /填入 age 可估算最近清晰距离/);
});

test("clear vision range computes a myope's near point from age", () => {
  const tool = getTool("clear_vision_range");
  // SE = -5.00, age 25 → amp = 18.5 - 7.5 = 11; near = 100/(5+11) ≈ 6.3cm; far = 100/5 = 20cm
  const result = tool.handler({ sph: -5, age: 25 });

  const text = result.content[0].text;
  assert.match(text, /远点（能看清的最远处）：\*\*20 cm\*\*/);
  assert.match(text, /近点（能看清的最近处）：\*\*6\.3 cm\*\*/);
  assert.match(text, /调节幅度约 11D（Hofstetter 平均/);
});

test("clear vision range uses the spherical equivalent for astigmatism", () => {
  const tool = getTool("clear_vision_range");
  // SE = -3 + (-2/2) = -4 → far point = 100/4 = 25cm
  const result = tool.handler({ sph: -3, cyl: -2 });

  const text = result.content[0].text;
  assert.match(text, /等效球镜（SE = SPH \+ CYL\/2）：\*\*-4D\*\*/);
  assert.match(text, /远点（能看清的最远处）：\*\*25 cm\*\*/);
  assert.match(text, /含散光/);
});

test("clear vision range shows a young hyperope compensating with accommodation", () => {
  const tool = getTool("clear_vision_range");
  // SE = +2, age 20 → amp = 18.5 - 6 = 12.5 >= 2 → can see far; residual 10.5 → near 100/10.5 ≈ 9.5cm
  const result = tool.handler({ sph: 2, age: 20 });

  const text = result.content[0].text;
  assert.match(text, /远视/);
  assert.match(text, /可看清远处\*\*（调节代偿）/);
});

test("clear vision range flags a hyperope whose accommodation can't compensate", () => {
  const tool = getTool("clear_vision_range");
  // SE = +5, age 60 → amp = 18.5 - 18 = 0.5 < 5 → cannot compensate
  const result = tool.handler({ sph: 5, age: 60 });

  const text = result.content[0].text;
  assert.match(text, /不足以克服 5D 的远视/);
  assert.match(text, /裸眼看远也难以看清/);
});

test("clear vision range keeps an emmetrope clear to infinity", () => {
  const tool = getTool("clear_vision_range");
  const result = tool.handler({ sph: 0, age: 30 });

  const text = result.content[0].text;
  assert.match(text, /正视 \/ 接近平光/);
  assert.match(text, /清晰到无穷远/);
  // age 30 → amp 9.5 → near point 100/9.5 ≈ 10.5cm
  assert.match(text, /近点（能看清的最近处）：\*\*10\.5 cm\*\*/);
});

test("clear vision range rejects an out-of-range sphere", () => {
  const tool = getTool("clear_vision_range");
  assert.throws(() => tool.handler({ sph: 99 }), /sph/);
});

test("lens material advisor forces impact-resistant PC/Trivex for kids and bans glass", () => {
  const tool = getTool("lens_material_advisor");
  const result = tool.handler({ sph: -2, usage: "kids" });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // default (balanced) safety pick is PC
  assert.match(text, /首选：\*\*PC 聚碳酸酯/);
  assert.match(text, /玻璃：易碎裂溅入眼睛/);
  assert.match(text, /CR-39 普通树脂：抗冲击性不足/);
});

test("lens material advisor prefers Trivex for kids when clarity is prioritized", () => {
  const tool = getTool("lens_material_advisor");
  const result = tool.handler({ sph: -2, usage: "sports", priority: "clarity" });

  const text = result.content[0].text;
  assert.match(text, /首选：\*\*Trivex/);
  assert.match(text, /阿贝数\(45\)明显高于 PC/);
});

test("lens material advisor recommends Trivex for a rimless frame and warns against glass/1.74", () => {
  const tool = getTool("lens_material_advisor");
  const result = tool.handler({ sph: -3, frame_type: "rimless" });

  const text = result.content[0].text;
  assert.match(text, /首选：\*\*Trivex/);
  assert.match(text, /玻璃：无框需在镜片上钻孔/);
  assert.match(text, /1\.74 高折射树脂：1\.74 偏脆，无框钻孔/);
});

test("lens material advisor scales the index up with power for a full-rim frame", () => {
  const tool = getTool("lens_material_advisor");
  const low = tool.handler({ sph: -1 }).content[0].text;
  const mid = tool.handler({ sph: -3 }).content[0].text;
  const high = tool.handler({ sph: -5 }).content[0].text;
  const veryHigh = tool.handler({ sph: -7 }).content[0].text;

  assert.match(low, /首选：\*\*CR-39 普通树脂/);
  assert.match(mid, /首选：\*\*1\.60 高折射树脂/);
  assert.match(high, /首选：\*\*1\.67 高折射树脂/);
  assert.match(veryHigh, /首选：\*\*1\.74 高折射树脂/);
  // full-rim always warns off glass
  assert.match(mid, /玻璃：玻璃重且易碎/);
});

test("lens material advisor warns about low-Abbe halos for night-focused driving", () => {
  const tool = getTool("lens_material_advisor");
  // high power → 1.67 (low Abbe) + driving usage
  const result = tool.handler({ sph: -5, usage: "driving" });

  const text = result.content[0].text;
  assert.match(text, /首选：\*\*1\.67 高折射树脂/);
  assert.match(text, /夜间驾驶：低阿贝数材料/);
});

test("lens material advisor uses the worst meridian (sph + cyl) as the reference power", () => {
  const tool = getTool("lens_material_advisor");
  // sph -3 alone would be 1.60, but -3 + -3 = -6 pushes to 1.74
  const result = tool.handler({ sph: -3, cyl: -3 });

  const text = result.content[0].text;
  assert.match(text, /参考功率（最大子午线）：6D/);
  assert.match(text, /首选：\*\*1\.74 高折射树脂/);
});

test("lens material advisor validates the frame_type enum", () => {
  const tool = getTool("lens_material_advisor");
  assert.throws(() => tool.handler({ sph: -2, frame_type: "octagon" }), /frame_type/);
});

test("sunglass tint guide recommends a category 3 dark lens for bright sun with VLT range", () => {
  const tool = getTool("sunglass_tint_guide");
  const result = tool.handler({ environment: "bright" });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /分类：\*\*3 类（深色）\*\*/);
  assert.match(text, /可见光透过率（VLT）：\*\*8%–18%\*\*/);
  // no driving flag → no driving-legality line
  assert.doesNotMatch(text, /白天驾驶：0–3 类/);
});

test("sunglass tint guide bumps one category deeper for a light-sensitive wearer", () => {
  const tool = getTool("sunglass_tint_guide");
  // sunny base = category 2, high sensitivity → category 3
  const result = tool.handler({ environment: "sunny", light_sensitivity: "high" });

  const text = result.content[0].text;
  assert.match(text, /分类：\*\*3 类（深色）\*\*/);
  assert.match(text, /畏光 \/ 对强光敏感/);
});

test("sunglass tint guide caps day driving at category 3 and never allows category 4", () => {
  const tool = getTool("sunglass_tint_guide");
  // snow_water base = category 4, but daytime driving is not allowed at 4
  const result = tool.handler({ environment: "snow_water", driving: true });

  const text = result.content[0].text;
  assert.match(text, /分类：\*\*3 类（深色）\*\*/);
  assert.match(text, /4 类（极深）镜片透光过低、法规不允许开车佩戴/);
  assert.match(text, /白天驾驶：0–3 类/);
  // grey is the primary driving tint recommendation
  assert.match(text, /灰色（中性灰）：\*\*首选\*\*/);
});

test("sunglass tint guide treats indoor_night + driving as night driving with a clear lens", () => {
  const tool = getTool("sunglass_tint_guide");
  const result = tool.handler({ environment: "indoor_night", driving: true });

  const text = result.content[0].text;
  assert.match(text, /分类：\*\*0 类（近无色 \/ 极浅）\*\*/);
  assert.match(text, /夜间 \/ 昏暗驾驶/);
  assert.match(text, /黄色「夜视镜」并不能真正提升夜间安全/);
  // no polarized for night
  assert.match(text, /偏光：此环境无需偏光/);
});

test("sunglass tint guide recommends polarized and prescription options when relevant", () => {
  const tool = getTool("sunglass_tint_guide");
  const result = tool.handler({ environment: "snow_water", has_prescription: true });

  const text = result.content[0].text;
  assert.match(text, /偏光：\*\*建议\*\*/);
  assert.match(text, /带度数（近视 \/ 散光 \/ 老花）选配/);
  // photochromic-in-car caveat
  assert.match(text, /多数变色片在车内不会变深/);
  // high category (4) triggers the higher-index thinning note
  assert.match(text, /更高折射率/);
});

test("sunglass tint guide rejects an unknown environment", () => {
  const tool = getTool("sunglass_tint_guide");
  assert.throws(() => tool.handler({ environment: "space" }), /environment/);
});

test("accommodation amplitude applies Hofstetter formulas and near point for a 45-year-old", () => {
  const tool = getTool("accommodation_amplitude");
  const result = tool.handler({ age: 45 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  // min = 15 - 0.25*45 = 3.75; avg = 18.5 - 0.30*45 = 5; max = 25 - 0.40*45 = 7
  assert.match(text, /最小（Hofstetter 下限）：\*\*3\.75D\*\*/);
  assert.match(text, /平均（预期值）：\*\*5D\*\*/);
  assert.match(text, /最大（上限）：\*\*7D\*\*/);
  // near point (avg) = 100/5 = 20cm; comfortable nearest = 100/(5/2) = 40cm
  assert.match(text, /调节近点（按平均调节力）：\*\*20 cm\*\*/);
  assert.match(text, /舒适持续用眼最近距离（保留一半调节力）：\*\*40 cm\*\*/);
});

test("accommodation amplitude flags strained near work and suggests a near add", () => {
  const tool = getTool("accommodation_amplitude");
  // age 45 → avg 5D, reserve 2.5D; at 33cm demand = 100/33 ≈ 3.03D > reserve
  const result = tool.handler({ age: 45, working_distance_cm: 33 });

  assert.equal(result.isError, undefined);
  const text = result.content[0].text;
  assert.match(text, /在 33 cm 处/);
  assert.match(text, /偏吃力/);
  // add = round((3.03 - 2.5)/0.25)*0.25 = 0.5
  assert.match(text, /建议近附加（补足储备）：约 \*\*\+0\.5D\*\*/);
});

test("accommodation amplitude marks a comfortable distance without a near add", () => {
  const tool = getTool("accommodation_amplitude");
  // age 30 → avg = 18.5 - 9 = 9.5D, reserve 4.75D; at 40cm demand = 2.5D < reserve
  const result = tool.handler({ age: 30, working_distance_cm: 40 });

  const text = result.content[0].text;
  assert.match(text, /状态：\*\*舒适/);
  assert.doesNotMatch(text, /建议近附加/);
});

test("accommodation amplitude reports exhausted accommodation for the very old", () => {
  const tool = getTool("accommodation_amplitude");
  // age 70 → avg = 18.5 - 21 < 0 → clamped to 0 → near point not measurable
  const result = tool.handler({ age: 70 });

  const text = result.content[0].text;
  assert.match(text, /平均（预期值）：\*\*0D\*\*/);
  assert.match(text, /调节力已近耗竭/);
});

test("accommodation amplitude rejects an out-of-range age", () => {
  const tool = getTool("accommodation_amplitude");
  assert.throws(() => tool.handler({ age: 200 }), /age/);
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
