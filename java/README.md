# glass-agent（Java + Spring Boot 版）

配眼镜指南**智能体**，基于 **Spring Boot 3 + Spring AI**。它把原 TypeScript MCP Server 的 6 个配镜工具移植为 Java 实现，并在此之上接入大模型：用户用自然语言提问，大模型通过 **Function Calling** 自动选择并调用工具，返回专业配镜建议。

原 TypeScript / MCP 版本仍保留在仓库根目录，两者并存。

## 两种入口

| 入口 | 路径 | 是否需要大模型 | 说明 |
| --- | --- | --- | --- |
| 智能体对话 | `POST /api/agent/chat` | 是 | 自然语言对话，大模型自动编排工具 |
| 直接调用工具 | `POST /api/tools/{name}` | 否 | 传结构化参数直接拿工具结果，便于调试或程序化集成 |
| 工具列表 | `GET /api/tools` | 否 | 列出全部工具及说明 |

## 内置工具

- `vision_check_guide`：按年龄段提供视力检查建议
- `lens_recommendation`：根据度数、用途、预算推荐镜片
- `frame_selection_guide`：根据脸型、生活方式和度数推荐镜框
- `prescription_interpreter`：解读验光单参数
- `progressive_lens_assessment`：评估是否适合渐进镜片或办公镜
- `new_glasses_troubleshooting`：排查新眼镜佩戴不适

## 环境要求

- JDK 17+
- Maven 3.8+

## 配置大模型

通过环境变量配置，代码里不写死密钥。默认指向阿里云百炼（通义千问）的 OpenAI 兼容端点，可切换到 DeepSeek 或 OpenAI：

```bash
export AI_API_KEY=你的密钥
# 通义千问（默认）
export AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export AI_MODEL=qwen-plus
# 或 DeepSeek
# export AI_BASE_URL=https://api.deepseek.com/v1
# export AI_MODEL=deepseek-chat
# 或 OpenAI
# export AI_BASE_URL=https://api.openai.com/v1
# export AI_MODEL=gpt-4o-mini
```

> 未配置 `AI_API_KEY` 时应用仍能启动，`/api/tools/**` 照常可用；只有 `/api/agent/chat` 在密钥无效时会返回带提示的错误。

## 运行

```bash
cd java
mvn spring-boot:run
```

默认端口 8080（示例中用 8089）。

## 调用示例

工具直调：

```bash
curl -X POST http://localhost:8080/api/tools/lens_recommendation \
  -H 'Content-Type: application/json' \
  -d '{"sph":-7.5,"cyl":-2.0,"usage":"daily","budget":"premium"}'
```

智能体对话（需配置密钥）：

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"我近视-3.00度，主要用电脑，中等预算，帮我推荐镜片"}'
```

## 测试

```bash
mvn test
```

## 项目结构

```
java/
├── pom.xml
└── src/main/java/com/glass/agent/
    ├── GlassAgentApplication.java      # 启动类
    ├── tool/
    │   ├── GlassAdvisorTools.java      # 6 个工具的业务逻辑 + @Tool 注解
    │   └── Diopters.java               # 度数格式化帮助函数
    ├── agent/
    │   ├── ChatConfig.java             # ChatClient 装配（系统提示词 + 挂载工具）
    │   └── ChatController.java         # 智能体对话接口
    └── web/
        ├── ToolController.java         # 工具直调 REST 接口
        └── ApiExceptionHandler.java    # 参数校验错误统一处理
```

## 设计说明

- `GlassAdvisorTools` 中的每个方法既是普通 Spring Bean 方法（供 REST 控制器调用），也标注了 Spring AI 的 `@Tool`（供大模型调用），一套逻辑两种入口。
- 参数校验逻辑与原 TypeScript 版本保持一致，错误信息形如「参数 xxx 必须是...」。
- 切换大模型只需改环境变量，无需改代码，因为统一走 OpenAI 兼容接口。
