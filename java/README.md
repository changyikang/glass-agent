# glass-agent（Java + Spring Boot 版）

配眼镜指南**智能体**，基于 **Spring Boot 3 + Spring AI**。它把原 TypeScript MCP Server 的 15 个配镜工具移植为 Java 实现，并在此之上接入大模型：用户用自然语言提问，大模型通过 **Function Calling** 自动选择并调用工具。智能体会**主动追问必要信息**（度数、用途、预算、脸型等），信息足够后给出配镜建议，并在最后**附上京东 / 淘宝 / 拼多多的购买链接**。

原 TypeScript / MCP 版本仍保留在仓库根目录，两者并存。

## 两种入口

| 入口 | 路径 | 是否需要大模型 | 说明 |
| --- | --- | --- | --- |
| 智能体对话 | `POST /api/agent/chat` | 是 | 自然语言多轮对话，大模型自动追问并编排工具 |
| 直接调用工具 | `POST /api/tools/{name}` | 否 | 传结构化参数直接拿工具结果，便于调试或程序化集成 |
| 工具列表 | `GET /api/tools` | 否 | 列出全部工具及说明 |
| 调用历史 | `GET /api/history` / `DELETE /api/history` | 否 | 查询最近的工具调用（最新在前，可加 `?limit=N`）或清空 |

## 多轮问诊

`POST /api/agent/chat` 的请求体支持 `conversationId`：

```json
{ "conversationId": "u-123", "message": "我想配副新眼镜，主要用来看电脑" }
```

同一个 `conversationId` 的多次请求会共享对话记忆（进程内保存，最近约 10 轮），
智能体因此能先追问缺失信息、再给建议并附购买链接。不传 `conversationId` 时统一归到 `default` 会话。

## 内置工具

- `vision_check_guide`：按年龄段提供视力检查建议
- `lens_recommendation`：根据度数、用途、预算推荐镜片
- `frame_selection_guide`：根据脸型、生活方式和度数推荐镜框
- `prescription_interpreter`：解读验光单参数
- `progressive_lens_assessment`：评估是否适合渐进镜片或办公镜
- `new_glasses_troubleshooting`：排查新眼镜佩戴不适
- `shopping_links`：把配镜建议转成京东 / 淘宝 / 拼多多的商品搜索购买链接
- `lens_thickness_estimator`：按度数、折射率和镜圈宽度估算镜片最厚处的厚度与重量倾向，并判断是否值得提高折射率减薄
- `pupillary_distance_guide`：校验并互算瞳距（双眼 / 左右单眼），按工作距离折算近用瞳距，提示左右不对称并给出自测方法
- `lens_coating_advisor`：按用眼场景逐项判断减反射、UV、防蓝光、变色片、偏振太阳镜值不值得多花钱，并给出「建议付费 / 可选 / 不必要」购物清单
- `myopia_control_guide`：按孩子年龄、当前度数、近一年加深速度、父母近视与日均户外时长评估近视进展风险，并排序给出户外活动、科学用眼、离焦框架镜、OK 镜、低浓度阿托品等干预方案
- `anisometropia_guide`：按左右眼等效球镜之差评估屈光参差程度，估算框架镜下两眼影像大小差异（不等像）并与耐受上限比较，识别混合性参差与柱镜差异过大等特殊情况
- `contact_lens_power`：按镜眼距（顶点距离）把框架镜球镜/柱镜换算成贴近角膜的隐形眼镜等效光度（按 0.25D 步进取整），说明高度数为何要补偿、低度数可直接沿用、散光如何折算等效球镜
- `reading_add_estimator`：按年龄估算老花（近附加 ADD）度数，按实际工作距离（默认 40cm）增减，并可结合看远球镜算出看近总度数，给出老花镜 / 渐进 / 办公镜片的选择建议（按 0.25D 步进取整）
- `prescription_transpose`：在负柱镜与正柱镜两种等价记法间互换（新球镜 = 原球镜 + 原柱镜，新柱镜 = −原柱镜，新轴位 = 原轴位 ± 90°），展示换算过程并用等效球镜不变量自检

## 环境要求

- JDK 21+
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

购买链接直调：

```bash
curl -X POST http://localhost:8080/api/tools/shopping_links \
  -H 'Content-Type: application/json' \
  -d '{"keywords":["1.67 非球面 防蓝光 镜片","TR90 超轻 近视镜框"]}'
```

镜片厚度估算（`lensIndex` 必填，`cyl` / `frameWidth` 可选）：

```bash
curl -X POST http://localhost:8080/api/tools/lens_thickness_estimator \
  -H 'Content-Type: application/json' \
  -d '{"sph":-6.0,"cyl":-1.0,"lensIndex":"1.60","frameWidth":54}'
```

瞳距助手直调（`binocularPd` 与 `pdLeft`/`pdRight` 至少提供一种，`workingDistanceCm` 可选，默认 40）：

```bash
curl -X POST http://localhost:8080/api/tools/pupillary_distance_guide \
  -H 'Content-Type: application/json' \
  -d '{"binocularPd":63,"workingDistanceCm":40}'
```

镜片镀膜与功能顾问（`screenHours` 与 `outdoorFrequency` 必填，`nightDriving` / `lightSensitive` / `preferOnePair` 可选）：

```bash
curl -X POST http://localhost:8080/api/tools/lens_coating_advisor \
  -H 'Content-Type: application/json' \
  -d '{"screenHours":9,"outdoorFrequency":"sometimes","nightDriving":"occasional","preferOnePair":true}'
```

青少年近视防控指南（`age` 与 `currentSph` 必填，`annualProgression` / `parentMyopia` / `outdoorHours` 可选）：

```bash
curl -X POST http://localhost:8080/api/tools/myopia_control_guide \
  -H 'Content-Type: application/json' \
  -d '{"age":9,"currentSph":-1.5,"annualProgression":0.75,"parentMyopia":"both","outdoorHours":0.5}'
```

屈光参差评估（`rightSph` 与 `leftSph` 必填，`rightCyl` / `leftCyl` 可选）：

```bash
curl -X POST http://localhost:8080/api/tools/anisometropia_guide \
  -H 'Content-Type: application/json' \
  -d '{"rightSph":-1.0,"leftSph":-3.5,"leftCyl":-0.75}'
```

隐形眼镜度数换算（`sph` 必填，`cyl` / `vertexDistanceMm` 可选，镜眼距默认 12mm）：

```bash
curl -X POST http://localhost:8080/api/tools/contact_lens_power \
  -H 'Content-Type: application/json' \
  -d '{"sph":-6.0,"cyl":-0.75,"vertexDistanceMm":12}'
```

老花（近附加 ADD）度数估算（`age` 必填，`workingDistanceCm` / `distanceSph` 可选，工作距离默认 40cm）：

```bash
curl -X POST http://localhost:8080/api/tools/reading_add_estimator \
  -H 'Content-Type: application/json' \
  -d '{"age":50,"workingDistanceCm":40,"distanceSph":-2.0}'
```

散光记法转换（`sph` / `cyl` 必填，有散光时 `axis` 必填；下例把负柱镜换成正柱镜）：

```bash
curl -X POST http://localhost:8080/api/tools/prescription_transpose \
  -H 'Content-Type: application/json' \
  -d '{"sph":-2.0,"cyl":-0.75,"axis":180}'
```

查看 / 清空工具调用历史（进程内保存最近 50 次，最新在前）：

```bash
curl http://localhost:8080/api/history          # 全部，或加 ?limit=10
curl -X DELETE http://localhost:8080/api/history # 清空
```

智能体对话（需配置密钥，多轮之间传同一个 `conversationId`）：

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"conversationId":"u-123","message":"我想配副新眼镜，主要用来看电脑"}'
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
    │   ├── GlassAdvisorTools.java      # 15 个工具的业务逻辑 + @Tool 注解
    │   ├── ToolCallHistory.java        # 进程内工具调用历史（最近 50 次）
    │   └── Diopters.java               # 度数格式化帮助函数
    ├── agent/
    │   ├── ChatConfig.java             # ChatClient 装配（系统提示词 + 挂载工具）
    │   ├── ChatController.java         # 智能体对话接口（多轮问诊）
    │   └── ConversationStore.java      # 进程内对话记忆（按 conversationId）
    └── web/
        ├── ToolController.java         # 工具直调 REST 接口（同时记录调用历史）
        ├── HistoryController.java      # 工具调用历史 REST 接口
        └── ApiExceptionHandler.java    # 参数校验错误统一处理
```

## 设计说明

- `GlassAdvisorTools` 中的每个方法既是普通 Spring Bean 方法（供 REST 控制器调用），也标注了 Spring AI 的 `@Tool`（供大模型调用），一套逻辑两种入口。
- 参数校验逻辑与原 TypeScript 版本保持一致，错误信息形如「参数 xxx 必须是...」。
- 切换大模型只需改环境变量，无需改代码，因为统一走 OpenAI 兼容接口。
