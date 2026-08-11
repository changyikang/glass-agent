# Contributing / 贡献指南

[English](#english) · [中文](#中文)

## English

Thanks for your interest in improving **glass-agent**! Contributions of all
kinds are welcome — bug reports, documentation fixes, new optical rules, and
new tools.

### Getting started

1. Fork the repository and create a branch from `dev`.
2. Pick the component you want to work on:
   - **TypeScript MCP Server** — root directory (`src/`).
   - **Java Spring Boot Agent** — `java/` directory.
3. Make your change with tests where it makes sense.

### Development workflow

TypeScript MCP Server:

```bash
npm install
npm run build
npm test
```

Java Spring Boot Agent:

```bash
cd java
mvn verify
```

### Pull requests

- Keep each PR focused on a single logical change.
- Make sure `npm test` and `mvn test` pass locally.
- Update the README / docs when behavior or usage changes.
- Follow the existing commit style (`feat:`, `fix:`, `docs:`, `chore:` …).

### Reporting issues

Use the issue templates. Please include your environment (component, OS,
Node.js / JDK version) and clear reproduction steps.

---

## 中文

感谢你参与改进 **glass-agent**！欢迎各种形式的贡献 —— 提交缺陷、修文档、补充配镜规则、新增工具都可以。

### 开始之前

1. Fork 仓库，并从 `dev` 分支切出你的工作分支。
2. 选择要改动的模块：
   - **TypeScript MCP Server** —— 根目录（`src/`）。
   - **Java Spring Boot 智能体** —— `java/` 目录。
3. 完成改动，合适的地方补充测试。

### 本地开发

TypeScript MCP Server：

```bash
npm install
npm run build
npm test
```

Java Spring Boot 智能体：

```bash
cd java
mvn verify
```

### 提交 PR

- 每个 PR 只做一件事，聚焦单一改动。
- 确保本地 `npm test` 和 `mvn test` 都能通过。
- 行为或用法有变化时，同步更新 README / 文档。
- 遵循现有提交信息风格（`feat:`、`fix:`、`docs:`、`chore:` 等）。

### 反馈问题

请使用 Issue 模板，附上运行环境（模块、操作系统、Node.js / JDK 版本）和清晰的复现步骤。
