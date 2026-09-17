# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

本项目的重要变更都记录在此。格式参考 [Keep a Changelog](https://keepachangelog.com/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### Added
- `visual_acuity_converter` tool (both implementations): converts a visual-acuity reading between the four equivalent notations — decimal (China's eye-chart bottom row), five-minute log / logarithmic (GB 11533, e.g. 5.0), Snellen fraction (20/20 or 6/6) and logMAR (research standard) — by normalizing any input to a decimal acuity (`L = 5 + lg(decimal)`, `logMAR = −lg(decimal)`, `Snellen = numerator ÷ decimal`) and deriving the other three, plus a rough acuity-level label and a common-reference table. It clarifies that acuity is not the same as refractive power. Brings the shared tool set to seventeen, all reachable via `/api/tools/**`.
- 新增 `visual_acuity_converter` 工具（两套实现）：在四种等价视力记法间互换——小数记录法（国内视力表下排）、五分记录法 / 对数记录法（GB 11533，如 5.0）、Snellen 分数（20/20 或 6/6）、logMAR（科研常用）。把任一记法先统一换算为小数视力（`L = 5 + lg(小数)`、`logMAR = −lg(小数)`、`Snellen = 分子 ÷ 小数`）再推出其余三种，并给出视力大致水平与常用对照表，同时说明「视力 ≠ 屈光度数」。共享工具增至十七个，均可经 `/api/tools/**` 访问。
- `frame_fit_calculator` tool (both implementations): decodes a frame's box measurements (`52□18-140` style) into its geometric center distance (frame PD = lens width + bridge), compares that against the wearer's PD to give the per-eye optical-center decentration and its direction (nasal / temporal), rates how well the frame matches the PD (well-suited → clearly too big), estimates the rough front width, and — when a lens power is supplied — uses Prentice's rule to estimate the unwanted horizontal prism that would arise if the optical center were left un-decentered, motivating why a well-matched frame keeps high-power lenses thinner. Brings the shared tool set to sixteen, all reachable via `/api/tools/**`.
- 新增 `frame_fit_calculator` 工具（两套实现）：解析镜架规格标注（如 `52□18-140`），按盒式标注法算出镜架几何中心距（框 PD = 镜圈宽 + 鼻梁），与佩戴者瞳距比较得出每片光心的移心量与方向（向鼻侧内移 / 向颞侧外移），按移心量评估镜架与瞳距的贴合度（很合适 → 明显偏大），粗估镜架正面宽度；当提供镜片度数时，用 Prentice 公式估算「若不移心」会产生的水平棱镜，说明为何贴合瞳距的镜架能让高度数镜片更薄。共享工具增至十六个，均可经 `/api/tools/**` 访问。
- `prescription_transpose` tool (both implementations): converts a prescription between minus-cylinder and plus-cylinder notation — the two equivalent ways to write the same lens, which optometrists, hospitals and software often need to interchange. It applies the standard transposition (new SPH = SPH + CYL, new CYL = −CYL, new AXIS = AXIS ± 90° normalized to 1–180°), shows the step-by-step working, uses the unchanged spherical equivalent (SPH + CYL/2) as a self-check, handles a pure sphere (no cylinder to transpose), and explains that both forms describe the identical lens. Brings the shared tool set to fifteen, all reachable via `/api/tools/**`.
- 新增 `prescription_transpose` 工具（两套实现）：在「负柱镜」与「正柱镜」两种等价写法之间互换——同一副镜片可用两种记法表示，验光单在验光师、医院、不同软件之间流转时常需互换。按标准柱镜转换公式换算（新球镜 = 原球镜 + 原柱镜，新柱镜 = −原柱镜，新轴位 = 原轴位 ± 90°，归一化到 1–180°），展示逐步换算过程，用转换前后不变的等效球镜（SPH + CYL/2）自检，并处理无散光（无柱镜可转换）的情形，说明两种写法描述的是完全相同的镜片。共享工具增至十五个，均可经 `/api/tools/**` 访问。
- `reading_add_estimator` tool (both implementations): estimates the presbyopia reading addition (ADD) from age using a stepped age table (roughly +1.00 D at 40 up to +2.50 D at 56+), adjusts it for the actual working distance relative to a 40 cm reference (closer = more add, farther = less), rounds to 0.25 D steps and caps at +3.50 D, and — when the distance spherical power is supplied — computes the near total power (`distance SPH + ADD`). It explains the mechanism of presbyopia, flags when no add is yet needed (below age 40), and advises on reading vs. progressive vs. office lenses. Brings the shared tool set to fourteen, all reachable via `/api/tools/**`.
- 新增 `reading_add_estimator` 工具（两套实现）：按年龄用分段经验表估算老花（近附加 ADD）度数（约 40 岁 +1.00D 起，56 岁及以上 +2.50D），并按实际工作距离相对 40cm 参考做增减（越近下加光越大、越远越小），按 0.25D 步进取整、封顶 +3.50D；当提供看远球镜度数时，算出「看近总度数 = 看远度数 + ADD」。工具解释老视机理、在 40 岁前提示尚不需要下加光，并给出老花镜 / 渐进 / 办公镜片的选择建议。共享工具增至十四个，均可经 `/api/tools/**` 访问。
- `contact_lens_power` tool (both implementations): converts a spectacle prescription (sphere plus optional cylinder) into the equivalent contact-lens power via vertex-distance compensation (`F_contact = F_spec / (1 − d·F_spec)`, default vertex 12 mm), rounding to 0.25 D steps. It converts each meridian separately to keep sphere/cylinder consistent, flags whether compensation is meaningful (strong powers ≈ ±4.00 D+ shift by ≥ a step; low powers can be used as-is), and for astigmatism offers a spherical-equivalent option for fitting a spherical lens — plus reminders that a real fitting also needs base curve / diameter / trial. Brings the shared tool set to thirteen. The Java `ToolController` also gains the previously missing `anisometropia_guide` direct REST endpoint, so all thirteen tools are reachable via `/api/tools/**`.
- 新增 `contact_lens_power` 工具（两套实现）：按镜眼距（顶点距离，默认 12mm）做顶点补偿，把框架镜球镜（及可选柱镜）换算成贴近角膜的隐形眼镜等效光度（公式 `F_隐形 = F_框架 ÷ (1 − d·F_框架)`），并按隐形常见的 0.25D 步进取整。分别换算两条子午线以保持球柱镜一致，判断补偿是否有意义（约 ±4.00D 以上的高度数会偏移 ≥ 半档，低度数可直接沿用），对散光给出「折算等效球镜配普通球镜片」的选项，并提醒隐形还需基弧 / 直径 / 试戴等专业验配。共享工具增至十三个。同时为 Java `ToolController` 补上此前缺失的 `anisometropia_guide` 直调 REST 接口，使十三个工具均可经 `/api/tools/**` 访问。
- `anisometropia_guide` tool (both implementations): grades the anisometropia between the two eyes from the spherical-equivalent (SPH + CYL/2) difference — none / mild / moderate / significant — estimates the spectacle aniseikonia (image-size difference, ≈1.5% per diopter) against a ~5% tolerance, detects antimetropia (one eye myopic, one hyperopic) and large cylinder gaps, and advises on framed glasses vs. contact lenses and gradual adaptation, with an amblyopia caution for children. Brings the shared tool set to twelve.
- 新增 `anisometropia_guide` 工具（两套实现）：按左右眼等效球镜（SPH + CYL÷2）之差评估屈光参差程度（无 / 轻度 / 中度 / 显著），估算框架镜下两眼视网膜影像大小差异（不等像，约每 1.00D 对应 1.5%）并与约 5% 的耐受上限比较，识别「一眼近视一眼远视」的混合性参差与柱镜差异过大等特殊情况，给出框架镜 vs 隐形眼镜、逐步适应等建议，并对儿童弱视风险作出提示。共享工具增至十二个。
- `myopia_control_guide` tool (both implementations): for a school-age child it scores myopia-progression risk from age, current power, yearly progression, parental myopia and daily outdoor time, then ranks interventions — outdoor time, eye-use habits, defocus spectacle lenses, ortho-K, low-dose atropine — as priority / consider / not-yet with each option's eligibility rules and the medical boundary (ortho-K and atropine must go through a professional). Brings the shared tool set to eleven.
- 新增 `myopia_control_guide` 工具（两套实现）：面向学龄儿童，按年龄、当前度数、近一年加深速度、父母近视与日均户外时长评估近视进展风险（高/中/低），并把户外活动、科学用眼、离焦框架镜、角膜塑形镜（OK 镜）、低浓度阿托品等干预手段按「优先 / 可评估 / 暂不需要」分级排序，逐项给出适用条件与就医边界（OK 镜、阿托品必须到专业机构）。共享工具增至十一个。
- `lens_coating_advisor` tool (both implementations): given a usage profile (daily screen hours, sun/outdoor exposure, night-driving frequency, light sensitivity, and whether one pair should work indoors and out), it rates each coating/function — anti-reflective base coating, UV400, blue-light, photochromic, polarized — as pay / consider / skip with reasons, and returns a consolidated shopping list plus cautions (e.g. avoiding yellow "night-vision" lenses). Brings the shared tool set to ten.
- 新增 `lens_coating_advisor` 工具（两套实现）：按用眼画像（日均屏幕时长、户外日晒、夜间驾驶频率、是否畏光、是否想一副室内外通用）逐项判断减反射基础膜、UV400、防蓝光、变色片、偏振太阳镜「值不值得多花钱」，给出推荐等级与原因，并汇总成「建议付费 / 可选 / 不必要」购物清单及提醒（如不推荐黄色「夜视」镜片）。共享工具增至十个。
- `pupillary_distance_guide` tool (both implementations): validates a pupillary distance, cross-checks binocular against left/right monocular readings (and derives one from the other), converts the distance PD into a near PD for a given working distance, flags left/right asymmetry, and explains how to self-measure. Brings the shared tool set to nine.
- 新增 `pupillary_distance_guide` 工具（两套实现）：校验瞳距、把双眼瞳距与左右单眼互相核对/互算、按工作距离折算近用瞳距、提示左右不对称，并给出自测方法。共享工具增至九个。
- `lens_thickness_estimator` tool (both implementations): a sagitta-based estimate of a lens' thickest point (edge for myopia, center for hyperopia) and weight tendency from power, refractive index and frame width, plus a recommendation on whether a higher index is worth it. Brings the shared tool set to eight.
- 新增 `lens_thickness_estimator` 工具（两套实现）：基于薄透镜矢高近似，按度数、折射率和镜圈宽度估算镜片最厚处（近视看边缘、远视看中心）的厚度与重量倾向，并判断是否值得提高折射率减薄。共享工具增至八个。
- Tool-call history in both implementations: an in-memory log of the most recent 50 tool calls (success and error), newest first. The TypeScript debug page gains a history panel, and both implementations expose `GET /api/history` (optional `?limit=N`) and `DELETE /api/history`.
- 两套实现新增工具调用历史：进程内记录最近 50 次调用（含成功与失败），最新在前。TypeScript 调试页新增历史面板，两套实现均提供 `GET /api/history`（可加 `?limit=N`）与 `DELETE /api/history`。
- One-click sample-data fill on the TypeScript debug page: each tool now ships a realistic `sample` payload (validated by a unit test), and the debug page has a "填充示例数据" button that fills the form and JSON box from it.
- 调试页示例数据一键填充：每个工具新增一份经过单元测试校验的真实 `sample` 参数，调试页新增“填充示例数据”按钮，一键把示例写入表单和 JSON 编辑框。
- `shopping_links` tool (both implementations) that turns a recommendation into ready-to-click JD / Taobao / Pinduoduo search links.
- 新增 `shopping_links` 工具（两套实现）：把配镜建议转成京东 / 淘宝 / 拼多多的商品搜索购买链接。
- Multi-turn intake for the Java agent: a `conversationId` on `/api/agent/chat` plus an in-memory `ConversationStore`, and a system prompt that proactively asks the questions it needs before recommending and attaching purchase links.
- Java 智能体的多轮问诊：`/api/agent/chat` 支持 `conversationId` 并新增进程内 `ConversationStore`，系统提示词改为先主动追问必要信息、再给建议并附购买链接。

### Changed
- Bump the Java toolchain from 17 to the 21 LTS (pom, CI, docs and badges).
- 将 Java 版本从 17 升级到 21 LTS（pom、CI、文档与徽章同步）。

### Added
- Bilingual (English / 中文) README with badges, architecture overview and quickstart for both components.
- 双语（英文 / 中文）README，含徽章、架构说明与两个模块的快速上手。
- `LICENSE` (MIT), `CONTRIBUTING.md`, issue / pull request templates and a GitHub Actions CI workflow.
- `LICENSE`（MIT）、`CONTRIBUTING.md`、Issue / PR 模板，以及 GitHub Actions CI 工作流。

## [1.2.0] - 2026-08-11

### Added
- Java + Spring Boot agent version under `java/`, using Spring AI Function Calling to orchestrate the six optical tools.
- `java/` 目录下新增 Java + Spring Boot 智能体版本，基于 Spring AI Function Calling 编排六个配镜工具。

## [1.1.0]

### Added
- Optical fitting guide MCP server with six tools and a local web debugging page.
- 配镜指南 MCP Server，内置六个工具与本地网页调试页。

[Unreleased]: https://github.com/changyikang/glass-agent/compare/main...HEAD
