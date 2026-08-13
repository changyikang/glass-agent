# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

本项目的重要变更都记录在此。格式参考 [Keep a Changelog](https://keepachangelog.com/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### Added
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
