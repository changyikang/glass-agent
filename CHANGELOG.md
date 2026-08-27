# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

本项目的重要变更都记录在此。格式参考 [Keep a Changelog](https://keepachangelog.com/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### Added
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
