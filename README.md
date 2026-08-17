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
lens recommendation, lens-thickness estimation, frame selection, vision-check
guidance, progressive-lens assessment, and troubleshooting discomfort with new
glasses.

The project ships **two interchangeable implementations that share the same eight
tools**, so you can adopt whichever fits your stack:

| Implementation | Path | Best for |
| --- | --- | --- |
| **TypeScript MCP Server** | root (`src/`) | Plugging into Claude Desktop or any MCP-compatible client over `stdio` |
| **Java Spring Boot Agent** | [`java/`](java/) | A conversational agent that proactively asks the questions it needs, then recommends and attaches purchase links — via Function Calling, plus direct REST access |

### Features

The eight built-in tools:

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

The Java agent adds **multi-turn intake**: pass a `conversationId` and it remembers
the dialogue, so it asks the questions it needs, gives a fitting recommendation, and
attaches purchase links at the end.

### Architecture

```
                         ┌──────────────────────────────┐
        MCP client       │     Eight shared optical      │      REST client
   (Claude Desktop, …)   │        fitting tools          │   (curl / your app)
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

**glass-agent** 把配眼镜的领域知识封装成可被大模型（或任意 MCP 客户端）调用的工具，覆盖验光单解读、镜片推荐、镜片厚度估算、镜框选择、视力检查建议、渐进镜片评估，以及新眼镜佩戴不适排查。

项目提供 **两套可互换、共享同一组八个工具的实现**，你可以按技术栈选用：

| 实现方式 | 路径 | 适用场景 |
| --- | --- | --- |
| **TypeScript MCP Server** | 根目录（`src/`） | 通过 `stdio` 接入 Claude Desktop 或任何兼容 MCP 的客户端 |
| **Java Spring Boot 智能体** | [`java/`](java/) | 会主动追问必要信息的对话式智能体：先问诊、再给建议、并附上购买链接；大模型通过 Function Calling 自动编排工具，同时提供工具直调 REST 接口 |

### 功能

内置八个工具：

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

Java 智能体还支持 **多轮问诊**：请求带上 `conversationId` 即可记住对话上下文，
于是它会主动追问所需信息，给出配镜建议，并在最后附上购买链接。

### 架构

```
                         ┌──────────────────────────────┐
       MCP 客户端         │        八个共享的配镜工具       │       REST 客户端
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
