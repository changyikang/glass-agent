<div align="center">

# 👓 glass-agent

**An optical-fitting advisor built on the Model Context Protocol (MCP) and LLM Function Calling.**

**基于 MCP 与大模型 Function Calling 的智能配镜顾问**

[![CI](https://github.com/changyikang/glass-agent/actions/workflows/ci.yml/badge.svg)](https://github.com/changyikang/glass-agent/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Node.js](https://img.shields.io/badge/Node.js-%E2%89%A518-339933?logo=node.js&logoColor=white)](package.json)
[![Java](https://img.shields.io/badge/Java-21%2B-007396?logo=openjdk&logoColor=white)](java/pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)](java/pom.xml)
[![MCP](https://img.shields.io/badge/MCP-compatible-000000)](https://modelcontextprotocol.io)

[English](#english) · [中文](#中文)

</div>

---

## English

### Overview

**glass-agent** turns the domain knowledge of eyeglasses fitting into callable
tools that an LLM (or any MCP client) can use. It covers prescription reading,
lens recommendation, lens-thickness estimation, lens-coating advice, frame
selection, vision-check guidance, progressive-lens assessment,
troubleshooting discomfort with new glasses, children's myopia control,
spectacle-to-contact-lens power conversion, presbyopia reading-add
estimation, cylinder (plus/minus) prescription transposition,
frame-size-to-PD fit checking, visual-acuity notation conversion,
accommodation-amplitude estimation, sunglass tint (filter-category)
selection, lens base-material selection, naked-eye clear-range
(far-point / near-point) estimation, near-vision acuity
notation conversion, prism vector math, astigmatism-type
classification, progressive-lens fitting-height checking,
minimum lens-blank-size calculation, near-vision vertical
prismatic imbalance checking, lens-tilt (pantoscopic / wrap)
power-change compensation, and spectacle magnification
(retinal-image magnification / minification) estimation.

The project ships **two interchangeable implementations that share the same twenty-nine
tools**, so you can adopt whichever fits your stack:

| Implementation | Path | Best for |
| --- | --- | --- |
| **TypeScript MCP Server** | root (`src/`) | Plugging into Claude Desktop or any MCP-compatible client over `stdio` |
| **Java Spring Boot Agent** | [`java/`](java/) | A conversational agent that proactively asks the questions it needs, then recommends and attaches purchase links — via Function Calling, plus direct REST access |

### Features

The twenty-nine built-in tools:

| Tool | What it does |
| --- | --- |
| `vision_check_guide` | Vision-check advice by age group (frequency, focus areas, preparation) |
| `lens_recommendation` | Recommends lens index, material and coatings from prescription, use case and budget |
| `frame_selection_guide` | Suggests frame shape, material and size from face shape, lifestyle and power |
| `prescription_interpreter` | Explains SPH / CYL / AXIS / PD / ADD and flags fitting risks |
| `progressive_lens_assessment` | Assesses single-vision vs. office vs. progressive lenses |
| `new_glasses_troubleshooting` | Tells adaptation from a real problem needing a re-check |
| `shopping_links` | Turns a recommendation into ready-to-click JD / Taobao / Pinduoduo search links |
| `lens_thickness_estimator` | Estimates a lens' thickest point (edge for myopia, center for hyperopia) and weight tendency from power, index and frame width, and flags whether a higher index is worth it |
| `pupillary_distance_guide` | Validates and cross-checks pupillary distance (binocular vs. monocular), derives the near PD for a working distance, flags left/right asymmetry and explains how to self-measure |
| `lens_coating_advisor` | Judges — per your usage profile (screen time, sun exposure, night driving) — whether each coating/function (anti-reflective, UV, blue-light, photochromic, polarized) is worth paying for, and returns a pay / consider / skip shopping list |
| `myopia_control_guide` | Assesses a child's myopia-progression risk from age, current power, yearly progression, parental myopia and daily outdoor time, then ranks interventions (outdoor time, eye-use habits, defocus spectacle lenses, ortho-K, low-dose atropine) with their eligibility and medical boundaries |
| `anisometropia_guide` | Grades the anisometropia between two eyes from the spherical-equivalent difference, estimates the spectacle aniseikonia (image-size difference) against tolerance, detects antimetropia and large cylinder gaps, and advises on framed glasses vs. contacts and gradual adaptation |
| `contact_lens_power` | Converts a spectacle prescription into the equivalent contact-lens power via vertex-distance compensation (rounded to 0.25 D steps), explaining why strong powers (≈ ±4.00 D+) must be compensated, when low powers can be used as-is, and how astigmatism can fold into a spherical equivalent |
| `reading_add_estimator` | Estimates the presbyopia reading addition (ADD) from age, adjusts it for the actual working distance (default 40 cm), and — given the distance prescription — computes the near total power, with guidance on reading vs. progressive vs. office lenses |
| `prescription_transpose` | Converts a prescription between minus-cylinder and plus-cylinder notation (new SPH = SPH + CYL, new CYL = −CYL, new AXIS = AXIS ± 90°), showing the working and using the spherical-equivalent invariant as a self-check |
| `frame_fit_calculator` | Decodes a frame's box measurements (`52□18-140` style) into its geometric center distance (lens width + bridge), compares it against the wearer's PD to give the per-eye optical-center decentration and direction, rates the frame-to-PD fit, and — given a lens power — estimates via Prentice's rule the unwanted prism from an un-decentered optical center |
| `visual_acuity_converter` | Converts a visual-acuity reading between the four equivalent notations — decimal, five-minute log (China's GB standard), Snellen fraction (20/20 or 6/6) and logMAR — via `L = 5 + lg(decimal)` and `logMAR = −lg(decimal)`, and labels the rough acuity level |
| `accommodation_amplitude` | Estimates the eye's amplitude of accommodation from age via Hofstetter's formulas (min = 15 − 0.25·age, avg = 18.5 − 0.30·age, max = 25 − 0.40·age), derives the near point of accommodation and the comfortable sustained near distance (keeping half the amplitude in reserve), and — given a working distance — judges whether its demand fits within that reserve and suggests a near add if not |
| `sunglass_tint_guide` | Recommends a sunglass lens filter category (ISO 12312-1 categories 0–4, with their visible-light-transmission ranges) from the light environment, adjusts it for light sensitivity and driving (day driving never allows category 4; `indoor_night` + driving is treated as night driving needing a clear lens), and advises on tint color (grey / brown / green / amber), polarization and photochromic/prescription options — while stressing that darkness ≠ UV protection and every pair should be UV400 |
| `lens_material_advisor` | Recommends a lens base material (CR-39 / Trivex / PC / high-index 1.60·1.67·1.74 / glass) from prescription power, frame type (full-rim / half-rim / rimless), usage (general / kids / sports / safety / driving) and priority (balanced / thinnest / clarity / lightweight), weighing impact resistance, Abbe number (chromatic aberration), thickness and weight into a primary pick, alternatives and materials to avoid — forcing impact-resistant PC/Trivex for kids/sports/safety, favoring tough Trivex/PC for rimless drilling, and flagging low-Abbe halos for night driving; complements `lens_thickness_estimator` (thinness) and `lens_coating_advisor` (coatings) |
| `clear_vision_range` | Estimates the range of distances the naked eye sees clearly (far point ↔ near point) from the uncorrected refractive error (spherical equivalent SE = SPH + CYL/2): a myope's far point = 100 ÷ power (cm) — beyond it blurs, nearer stays clear, which is why a myope sees a phone clearly with glasses off; an emmetrope is clear to infinity; a hyperope must spend accommodation to clear distance. Given age it derives accommodation amplitude via Hofstetter's average (18.5 − 0.30·age) to compute the near point and whether a hyperope can compensate; complements `accommodation_amplitude` |
| `near_acuity_converter` | Converts a near-vision reading between the notations used on reading cards — M-units (standard; a 1M optotype subtends 5′ at 1 m), print point size (≈ M × 8) and a direct near-acuity decimal — at a given test distance (default 40 cm): near decimal = test distance (m) ÷ M, from which it derives the Snellen / logMAR / five-minute equivalents and an acuity level, plus a common near-acuity reference table (including an approximate Jaeger J column). Complements the distance-chart `visual_acuity_converter` |
| `prism_resolver` | Prism vector math (Prentice). `combine` adds a horizontal (base-in / base-out) and a vertical (base-up / base-down) prism into a single resultant — magnitude = √(H² + V²) and a base-direction angle (convention: base-out 0°, up 90°, base-in 180°, down 270°, counterclockwise); `resolve` decomposes a resultant (magnitude + angle) back into its horizontal and vertical components. Uses base-in/out (nose-referenced, so unambiguous per eye) rather than the eye-specific 360° notation, and notes the 0.25 Δ prescription rounding |
| `astigmatism_type_guide` | Classifies an astigmatic prescription by **refractive type** — from the signs of the two principal-meridian powers (P₁ = SPH, P₂ = SPH + CYL): both minus → compound myopic, both plus → compound hyperopic, one plano → simple myopic/hyperopic, opposite signs → mixed — and by **axis rule** from the minus-cylinder axis (±30° window: 0–30°/150–180° with-the-rule, 60–120° against-the-rule, else oblique), transposing a plus-cyl input first so the verdict is notation-independent. Reports both principal meridians, the spherical equivalent and rule-specific adaptation notes; complements `prescription_interpreter` and `prescription_transpose` |
| `progressive_fit_check` | Checks whether a frame is tall enough to fit progressive lenses: a progressive needs enough **fitting height** (pupil center to the lowest edge of the lens) for the far→near corridor. Given a measured `fitting_height` — or a frame `frame_b` (box B value), from which it estimates `fitting_height ≈ B/2 + 2` — it rates each design (standard/long-corridor min ≈ 18 mm, short-corridor ≈ 14 mm, premium/freeform ≈ 13 mm) as ample / tight / insufficient. With a target `design` it reports feasibility and margin; without one it recommends the best fit (preferring standard > short > premium), and when the height is below every threshold (< 13 mm) it advises a taller frame or single-vision/bifocal instead. Complements `progressive_lens_assessment` (whether you need progressive) and `frame_fit_calculator` (horizontal fit) |
| `minimum_blank_size` | Computes the minimum lens-blank diameter needed to cut a frame: **MBS = ED + 2 × decentration + cutting allowance** (default 2 mm), where decentration per lens = (lens width A + bridge DBL)/2 − PD/2. Given A, DBL and PD — plus an optional effective diameter `ed`, or a frame height `frame_b` from which it conservatively estimates `ed ≈ √(A² + B²)` (box diagonal) — it reports the frame center distance, per-lens decentration, the ED used and the minimum blank diameter, and rates it against common blank sizes (65/70/75/80 mm). With a `blank_diameter` on hand it reports whether that blank is big enough and the margin. Complements `frame_fit_calculator` (decentration / induced prism) |
| `vertical_imbalance_check` | Estimates the differential **vertical prism** at the reading position in anisometropia (Prentice's rule). From each eye's SPH (optional CYL / AXIS) it computes the vertical-meridian power `F_v = SPH + CYL·cos²(axis)`, each lens's induced vertical prism and base direction at a given downward reading drop (`reading_drop`, default 8 mm), and the differential imbalance `= |F_v_OD − F_v_OS| × drop(cm)`, graded against clinical thresholds (< 1 Δ usually tolerated, 1–1.5 Δ borderline, ≥ 1.5 Δ significant), with management options (separate readers, contacts, slab-off / Fresnel prism). Complements `anisometropia_guide` and `prism_resolver` |
| `lens_tilt_compensation` | Estimates the as-worn power change when a lens is tilted relative to the line of sight (Martin's oblique-refraction formulae): pantoscopic tilt (about a horizontal axis) and face-form / wrap (about a vertical axis). From the sphere power `power`, tilt angle `tilt_angle`, type `tilt_type` (pantoscopic → induced axis 180°, faceform → 90°) and refractive index `index` (default 1.5) it computes the effective sphere `S' = F·(1 + sin²θ/(2n))` and the induced cylinder `C' = S'·tan²θ` at the type's axis, reports the as-worn power and the deviation from the ordered power, and grades it by the induced-cylinder size (< 0.12 D negligible, 0.12–0.25 D borderline, ≥ 0.25 D noticeable → recommend position-of-wear compensation). High powers and large wrap (sports / wrap sunglasses) amplify it; moderate pantoscopic tilt (~8–12°) is normal design. Complements `frame_fit_calculator` and `contact_lens_power` |
| `spectacle_magnification` | Estimates how much a spectacle lens magnifies / minifies the retinal image — why plus (hyperopic) lenses make the eyes look bigger, minus (myopic) lenses smaller, and the root of aniseikonia in anisometropia. SM = power factor × shape factor, where `PF = 1 / (1 − d·Fv)` (d = vertex distance in m, Fv = back vertex power) and `SF = 1 / (1 − (t/n)·F1)` (t = center thickness in m, n = index, F1 = front-surface / base-curve power). From the sphere `power` (required), `vertex_distance` (default 12 mm), optional `base_curve` + `center_thickness` (both needed to include the shape factor, otherwise only the power factor) and `index` (default 1.5) it reports the magnification ratio, the percent magnification / minification and a magnitude grade (< 5% small, 5–8% noticeable, ≥ 8% significant). Complements `anisometropia_guide` (two-eye aniseikonia) and `contact_lens_power` |

The Java agent adds **multi-turn intake**: pass a `conversationId` and it remembers
the dialogue, so it asks the questions it needs, gives a fitting recommendation, and
attaches purchase links at the end.

### Architecture

```
                         ┌──────────────────────────────┐
        MCP client       │  Twenty-nine shared optical   │      REST client
   (Claude Desktop, …)   │             tools             │   (curl / your app)
            │            └──────────────────────────────┘            │
            │                 ▲                    ▲                  │
            ▼                 │                    │                  ▼
 ┌─────────────────────┐      │                    │      ┌─────────────────────┐
 │ TypeScript MCP      │──────┘                    └──────│ Java Spring Boot     │
 │ Server (stdio)      │                                  │ Agent (Spring AI)    │
 │  src/index.ts       │                                  │  /api/agent/chat     │
 │  src/web.ts (debug) │                                  │  /api/tools/{name}   │
 └─────────────────────┘                                  └─────────────────────┘
```

Both implementations expose the **same tool contract**; the Java agent adds an
LLM layer that selects and calls tools from natural-language questions.

### Requirements

- TypeScript MCP Server: **Node.js 18+** and npm
- Java Agent: **JDK 21+** and Maven 3.8+

### Quick start — TypeScript MCP Server

```bash
npm install
npm run build
npm test          # compile + run tests
npm start         # start the stdio MCP server
npm run web       # start the local web debug page (http://127.0.0.1:3000)
```

Each tool card on the debug page has a **"填充示例数据" (fill sample data)** button that
loads a realistic, valid payload into the form and JSON box so you can try a tool in one click.
A **tool-call history** panel shows the most recent 50 calls (from both MCP clients and the page
itself), newest first, and can be refreshed or cleared. It is also available as a small REST API:
`GET /api/history` (optionally `?limit=N`) and `DELETE /api/history`.

Register it with an MCP client such as Claude Desktop:

```json
{
  "mcpServers": {
    "glass-agent": {
      "command": "node",
      "args": ["/absolute/path/to/glass-agent/dist/index.js"]
    }
  }
}
```

### Quick start — Java Spring Boot Agent

```bash
cd java
mvn spring-boot:run   # starts on http://localhost:8080
```

Configure the model via environment variables (no keys in code). It defaults to
the OpenAI-compatible endpoint of Alibaba Cloud (Qwen); you can switch to
DeepSeek or OpenAI:

```bash
export AI_API_KEY=your-key
export AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export AI_MODEL=qwen-plus
```

Call a tool directly (no LLM key required):

```bash
curl -X POST http://localhost:8080/api/tools/lens_recommendation \
  -H 'Content-Type: application/json' \
  -d '{"sph":-7.5,"cyl":-2.0,"usage":"daily","budget":"premium"}'
```

Chat with the agent (needs `AI_API_KEY`). Send the same `conversationId` across
turns so it remembers the dialogue, asks what it still needs, then recommends and
attaches purchase links:

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"conversationId":"u-123","message":"I want new glasses, mostly for the computer"}'
```

See [`java/README.md`](java/README.md) for full details.

### Project structure

```text
glass-agent/
├── src/                     # TypeScript MCP server
│   ├── index.ts             # MCP entry point + tool implementations
│   ├── web.ts               # local web debug server
│   └── index.test.ts        # tests
├── java/                    # Java + Spring Boot agent (see java/README.md)
│   └── src/main/java/com/glass/agent/
├── .github/                 # CI workflow, issue & PR templates
├── package.json
└── tsconfig.json
```

### Roadmap

- [ ] More optical business rules and a case library
- [x] Tool-call history (debug page panel + REST API, both implementations)
- [x] One-click sample-data fill on the debug page
- [ ] A more polished product-grade frontend
- [ ] Broader automated test coverage

### Contributing

Contributions are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md).

### License

Released under the [MIT License](LICENSE).

---

## 中文

### 项目简介

**glass-agent** 把配眼镜的领域知识封装成可被大模型（或任意 MCP 客户端）调用的工具，覆盖验光单解读、镜片推荐、镜片厚度估算、镜片镀膜取舍、镜框选择、视力检查建议、渐进镜片评估、新眼镜佩戴不适排查、青少年近视防控、框架镜到隐形眼镜的度数换算、老花（近附加）度数估算、散光（正/负柱镜）记法转换、镜架尺寸与瞳距的适配评估、视力记录法换算、调节幅度评估、太阳镜镜片色号（过滤分类）选择、镜片材料（基材）选择、裸眼清晰视界（远点 / 近点）估算、近用视力（近视力表）记法换算、棱镜合成与分解（Prentice 向量运算）、散光类型判定、渐进镜片装配高度评估、最小镜片毛坯直径计算、近用垂直棱镜差（上下不平衡）评估、镜片倾斜（前倾角 / 面弯）引起的度数变化与补偿估算，以及眼镜放大率（视网膜像放大 / 缩小）估算。

项目提供 **两套可互换、共享同一组二十九个工具的实现**，你可以按技术栈选用：

| 实现方式 | 路径 | 适用场景 |
| --- | --- | --- |
| **TypeScript MCP Server** | 根目录（`src/`） | 通过 `stdio` 接入 Claude Desktop 或任何兼容 MCP 的客户端 |
| **Java Spring Boot 智能体** | [`java/`](java/) | 会主动追问必要信息的对话式智能体：先问诊、再给建议、并附上购买链接；大模型通过 Function Calling 自动编排工具，同时提供工具直调 REST 接口 |

### 功能

内置二十九个工具：

| 工具 | 作用 |
| --- | --- |
| `vision_check_guide` | 按年龄段给出视力检查建议（频率、重点项目、检查前准备） |
| `lens_recommendation` | 根据度数、散光、用途和预算推荐折射率、材质与镀膜 |
| `frame_selection_guide` | 结合脸型、生活方式和度数推荐框型、材质与尺寸 |
| `prescription_interpreter` | 解读 SPH / CYL / AXIS / PD / ADD 并提示配镜风险点 |
| `progressive_lens_assessment` | 评估更适合单焦、办公镜还是渐进多焦点镜片 |
| `new_glasses_troubleshooting` | 区分是适应期还是需要复查的真问题 |
| `shopping_links` | 把配镜建议转成可直接点击的京东 / 淘宝 / 拼多多搜索购买链接 |
| `lens_thickness_estimator` | 按度数、折射率和镜圈宽度估算镜片最厚处（近视看边缘、远视看中心）的厚度与重量倾向，并判断是否值得提高折射率减薄 |
| `pupillary_distance_guide` | 校验并互算瞳距（双眼 / 左右单眼），按工作距离折算近用瞳距，提示左右不对称并给出自测方法 |
| `lens_coating_advisor` | 按用眼场景（屏幕时长、日晒、夜间驾驶）逐项判断减反射、UV、防蓝光、变色片、偏振太阳镜值不值得多花钱，并给出「建议付费 / 可选 / 不必要」购物清单 |
| `myopia_control_guide` | 按孩子年龄、当前度数、近一年加深速度、父母近视与日均户外时长评估近视进展风险，并排序给出户外活动、科学用眼、离焦框架镜、OK 镜、低浓度阿托品等干预方案及其适用条件与就医边界 |
| `anisometropia_guide` | 按左右眼等效球镜之差评估屈光参差程度，估算框架镜下两眼影像大小差异（不等像）并与耐受上限比较，识别「一眼近视一眼远视」与柱镜差异过大等特殊情况，给出框架镜 vs 隐形眼镜、逐步适应等建议 |
| `contact_lens_power` | 按镜眼距（顶点距离）把框架镜球镜/柱镜换算成贴近角膜的隐形眼镜等效光度（按 0.25D 步进取整），说明高度数（约 ±4.00D 以上）为何必须补偿、低度数可直接沿用，以及散光如何折算等效球镜 |
| `reading_add_estimator` | 按年龄估算老花（近附加 ADD）度数，按实际工作距离（默认 40cm）增减，并可结合看远球镜算出看近总度数，给出老花镜 / 渐进 / 办公镜片的选择建议（按 0.25D 步进取整） |
| `prescription_transpose` | 在负柱镜与正柱镜两种等价记法间互换（新球镜 = 原球镜 + 原柱镜，新柱镜 = −原柱镜，新轴位 = 原轴位 ± 90°），展示换算过程并用等效球镜不变量自检 |
| `frame_fit_calculator` | 解析镜架规格标注（如 `52□18-140`），按盒式标注法算出镜架几何中心距（镜圈宽 + 鼻梁），与瞳距比较得出每片光心移心量与方向，评估镜架与瞳距的贴合度；给定度数时用 Prentice 公式估算「不移心」会产生的水平棱镜 |
| `visual_acuity_converter` | 在小数记录法、五分记录法（对数，中国 GB 标准）、Snellen 分数（20/20 或 6/6）、logMAR 四种等价视力记法间互换（五分 L = 5 + lg(小数)，logMAR = −lg(小数)），并给出该视力的大致水平 |
| `accommodation_amplitude` | 用 Hofstetter 公式按年龄估算调节幅度（最小 = 15 − 0.25×年龄，平均 = 18.5 − 0.30×年龄，最大 = 25 − 0.40×年龄），推算调节近点与「保留一半调节力」的舒适持续用眼最近距离；给定工作距离时判断该距离的调节需求是否在储备之内，不足时给出建议近附加 |
| `sunglass_tint_guide` | 按用光环境推荐太阳镜镜片过滤分类（ISO 12312-1 的 0–4 类及其可见光透过率区间），结合是否畏光、是否驾驶（白天驾驶禁用 4 类；`indoor_night` + 驾驶按夜间驾驶只用无色片），给出镜片颜色（灰 / 茶棕 / 墨绿 / 黄琥珀）、偏光、变色片与带度数选配建议，并强调镜片深浅 ≠ 防紫外线、任何太阳镜都应达到 UV400 |
| `lens_material_advisor` | 按度数、镜框类型（全框 / 半框 / 无框）、使用场景（日常 / 儿童 / 运动 / 安全防护 / 驾驶）与取舍偏好（均衡 / 最薄 / 最清晰 / 最轻），在 CR-39、Trivex、PC、1.60 / 1.67 / 1.74 高折射树脂、玻璃之间推荐镜片基材，综合抗冲击、阿贝数（边缘色散）、厚度与重量给出首选 / 备选 / 不建议材料——儿童 / 运动 / 安全强制用抗冲击的 PC / Trivex、无框优先韧性好的 Trivex / PC、并对夜间驾驶提示低阿贝数色散/光晕；与 `lens_thickness_estimator`（减薄）、`lens_coating_advisor`（镀膜）互补 |
| `clear_vision_range` | 按裸眼屈光度（含柱镜按等效球镜 SE = SPH + CYL/2）推算不戴镜能看清的距离范围（远点 ↔ 近点）：近视远点 = 100 ÷ 度数(cm)，远点以外模糊、近处却清楚——这正是「近视摘镜看手机反而清楚」的原因；正视清晰到无穷远；远视需动用调节力把焦点拉回。可选填年龄，按 Hofstetter 平均公式（18.5 − 0.30×年龄）估算调节力算出近点、判断远视能否代偿；与 `accommodation_amplitude` 互补 |
| `near_acuity_converter` | 近用视力（近视力表）记法换算：在近视力卡常用的 M 记法（国际标准，1M 视标在 1m 处张角 5′）、印刷点数（≈ M × 8）、直接的近视力小数三种写法间换算，按测试距离（默认 40cm）算出该距离的近视力小数（= 测试距离m ÷ M），并推出 Snellen / logMAR / 五分等效与视力水平，附含 Jaeger（J 记法）近似的近视力对照表；与看远的 `visual_acuity_converter` 互补 |
| `prism_resolver` | 棱镜合成与分解（Prentice 向量运算）：`combine` 把水平（基底朝鼻侧 base-in / 颞侧 base-out）与垂直（base-up / base-down）两个棱镜分量合成为单一合棱镜——大小 = √(水平² + 垂直²)，方向角（约定：颞侧 0°、上 90°、鼻侧 180°、下 270°，逆时针）；`resolve` 把合棱镜（大小 + 方向角）分解回水平、垂直分量。采用以鼻子为参照、左右眼含义一致的 base-in/out 表述（而非左右眼不同的 360° 记法），并提示 0.25Δ 处方取整 |
| `astigmatism_type_guide` | 散光类型判定：按两条主子午线光度（P₁ = SPH、P₂ = SPH + CYL）的正负判定**屈光类型**（都为负→复性近视散光、都为正→复性远视散光、一条平光→单纯近视/远视散光、一正一负→混合散光），按**负柱镜轴位**（±30° 窗口：0–30°/150–180° 顺规、60–120° 逆规、其余斜轴）判定**轴向规则**，正柱镜输入会先转成负柱镜轴位以保证与记法无关。报出两条主子午线、等效球镜与按轴向规则给出的适应提示，与 `prescription_interpreter`、`prescription_transpose` 互补 |
| `progressive_fit_check` | 渐进镜片装配高度评估：判断镜框在垂直方向是否「够高」装配渐进多焦点镜片——渐进片需要足够的**装配高度**（瞳孔中心到镜片最低缘的垂直距离）容纳看远到看近的过渡通道。可给实测 `fitting_height`，或给镜框高度 `frame_b`（B 值）由工具粗估（≈ B/2 + 2）；对常规/长通道（最小约 18mm）、短通道（约 14mm）、个性化/自由曲面（约 13mm）三种设计评估充足 / 临界 / 不足。给定目标 `design` 时报出可行性与余量，不给时按「优先常规 > 短通道 > 个性化」推荐；高度低于所有门槛（< 13mm）则建议换更高镜框或改用单光 / 双光。与 `progressive_lens_assessment`（是否需要渐进）、`frame_fit_calculator`（水平贴合）互补 |
| `minimum_blank_size` | 最小镜片毛坯直径（MBS）计算：**MBS = 有效直径 ED + 2 × 每片移心量 + 加工余量**（默认 2mm），移心量 = (镜圈宽 A + 鼻梁 DBL) ÷ 2 − 瞳距 PD ÷ 2。给镜圈宽 A、鼻梁 DBL、瞳距 PD（可选有效直径 `ed`，或镜框高度 `frame_b` 由盒对角线 `ed ≈ √(A²+B²)` 保守估算），算出框几何中心距、每片移心量、所用 ED 与最小毛坯直径，并对照常见毛坯规格（65/70/75/80mm）判断是否易得；给手头毛坯直径 `blank_diameter` 时判断够不够用并给出余量。与 `frame_fit_calculator`（移心 / 棱镜）互补 |
| `vertical_imbalance_check` | 近用垂直棱镜差（上下不平衡）评估（Prentice 向量）：屈光参差者向下阅读时两片各产生垂直棱镜。按左右眼球镜 SPH（可含柱镜 CYL / 轴位 AXIS）算出各自垂直子午线光度 `F_v = SPH + CYL·cos²(轴位)`、每片在给定向下注视量（`reading_drop`，默认 8mm）下的垂直棱镜与基底方向，并给出差异棱镜 `= |F_v右 − F_v左| × 向下量(cm)`，按经验阈值分级（<1Δ 多可耐受、1–1.5Δ 临界、≥1.5Δ 显著常需处理），给出分开配近用镜 / 隐形眼镜 / 削底(slab-off) / 压贴棱镜等方案。与 `anisometropia_guide`、`prism_resolver` 互补 |
| `lens_tilt_compensation` | 镜片倾斜（前倾角 / 面弯）引起的度数变化与补偿估算（Martin 斜向屈光）：镜片相对视线倾斜时如戴光度会变化——球镜略增强并引入一个轴位与旋转轴平行的散光。前倾角（pantoscopic，绕水平轴）引入轴位 180° 散光；面弯 / 包裹角（faceform / wrap，常见于运动镜、太阳镜，绕垂直轴）引入轴位 90° 散光。按球镜 `power`、倾斜角 `tilt_angle`、类型 `tilt_type`、折射率 `index`（默认 1.5）算等效球镜 `S' = F·(1 + sin²θ/(2n))`、引入散光 `C' = S'·tan²θ`，给出如戴等效度数与偏差，按引入散光大小分级（<0.12D 可忽略、0.12–0.25D 临界、≥0.25D 明显需补偿）。高度数、大面弯时偏差放大，高端 / 自由曲面镜片会做「如戴位置补偿」抵消；适度前倾角（约 8–12°）属正常设计。与 `frame_fit_calculator`、`contact_lens_power` 互补 |
| `spectacle_magnification` | 眼镜放大率（视网膜像放大 / 缩小）估算：框架镜会把视网膜像放大或缩小——正镜（远视）戴镜显眼大、负镜（近视）显眼小，也是屈光参差引起不等像（aniseikonia）的根源。放大率 SM = 镜度因子 × 外形因子，`PF = 1 / (1 − d·Fv)`（d 镜眼距 m、Fv 后顶点度数），`SF = 1 / (1 − (t/n)·F1)`（t 中心厚度 m、n 折射率、F1 前表面 / 基弧面度数）。给球镜 `power`（必填）、镜眼距 `vertex_distance`（默认 12mm），可选前表面度数 `base_curve` 与中心厚度 `center_thickness`（两者齐备才计外形因子，否则只给镜度因子）、折射率 `index`（默认 1.5），输出放大率倍数、放大 / 缩小百分比与量级分级（<5% 较小、5–8% 可察觉、≥8% 明显）。与 `anisometropia_guide`（两眼不等像）、`contact_lens_power` 互补 |

Java 智能体还支持 **多轮问诊**：请求带上 `conversationId` 即可记住对话上下文，
于是它会主动追问所需信息，给出配镜建议，并在最后附上购买链接。

### 架构

```
                         ┌──────────────────────────────┐
       MCP 客户端         │     二十九个共享的配镜工具     │       REST 客户端
   (Claude Desktop 等)   │                              │   (curl / 你的应用)
            │            └──────────────────────────────┘            │
            │                 ▲                    ▲                  │
            ▼                 │                    │                  ▼
 ┌─────────────────────┐      │                    │      ┌─────────────────────┐
 │ TypeScript MCP      │──────┘                    └──────│ Java Spring Boot     │
 │ Server (stdio)      │                                  │ 智能体 (Spring AI)   │
 │  src/index.ts       │                                  │  /api/agent/chat     │
 │  src/web.ts (调试)  │                                  │  /api/tools/{name}   │
 └─────────────────────┘                                  └─────────────────────┘
```

两套实现暴露 **相同的工具契约**；Java 智能体在其上叠加一层大模型，能从自然语言问题里自动选择并调用工具。

### 环境要求

- TypeScript MCP Server：**Node.js 18+** 与 npm
- Java 智能体：**JDK 21+** 与 Maven 3.8+

### 快速上手 —— TypeScript MCP Server

```bash
npm install
npm run build
npm test          # 编译并运行测试
npm start         # 启动 stdio MCP Server
npm run web       # 启动本地网页调试页（http://127.0.0.1:3000）
```

调试页每个工具卡片上都有一个**「填充示例数据」**按钮，点击即可把一份真实有效的参数
一键写入表单和 JSON 编辑框，方便快速试用。页面还带一个**工具调用历史**面板，展示最近 50 次
调用（含 MCP 客户端与本页调用），最新在前，可刷新或清空；同时提供 REST 接口
`GET /api/history`（可加 `?limit=N`）与 `DELETE /api/history`。

接入 Claude Desktop 等 MCP 客户端：

```json
{
  "mcpServers": {
    "glass-agent": {
      "command": "node",
      "args": ["/绝对路径/glass-agent/dist/index.js"]
    }
  }
}
```

### 快速上手 —— Java Spring Boot 智能体

```bash
cd java
mvn spring-boot:run   # 默认启动在 http://localhost:8080
```

大模型通过环境变量配置，代码里不写死密钥。默认指向阿里云百炼（通义千问）的 OpenAI 兼容端点，可切换到 DeepSeek 或 OpenAI：

```bash
export AI_API_KEY=你的密钥
export AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export AI_MODEL=qwen-plus
```

工具直调（无需大模型密钥）：

```bash
curl -X POST http://localhost:8080/api/tools/lens_recommendation \
  -H 'Content-Type: application/json' \
  -d '{"sph":-7.5,"cyl":-2.0,"usage":"daily","budget":"premium"}'
```

智能体对话（需配置 `AI_API_KEY`）。多轮之间传同一个 `conversationId`，
它就能记住上下文：先主动追问缺的信息，再给出建议并附上购买链接：

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"conversationId":"u-123","message":"我想配副新眼镜，主要用来看电脑"}'
```

完整说明见 [`java/README.md`](java/README.md)。

### 项目结构

```text
glass-agent/
├── src/                     # TypeScript MCP Server
│   ├── index.ts             # MCP 入口 + 工具实现
│   ├── web.ts               # 本地网页调试服务
│   └── index.test.ts        # 测试
├── java/                    # Java + Spring Boot 智能体（见 java/README.md）
│   └── src/main/java/com/glass/agent/
├── .github/                 # CI 工作流、Issue 与 PR 模板
├── package.json
└── tsconfig.json
```

### 后续规划

- [ ] 增加更多配镜业务规则与案例库
- [x] 工具调用历史记录（调试页面板 + REST 接口，两套实现）
- [x] 调试页示例数据一键填充
- [ ] 更正式的产品级前端页面
- [ ] 更全面的自动化测试

### 参与贡献

欢迎贡献，请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

### 许可协议

本项目基于 [MIT 许可协议](LICENSE) 开源。
