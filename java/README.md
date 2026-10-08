# glass-agent（Java + Spring Boot 版）

配眼镜指南**智能体**，基于 **Spring Boot 3 + Spring AI**。它把原 TypeScript MCP Server 的 20 个配镜工具移植为 Java 实现，并在此之上接入大模型：用户用自然语言提问，大模型通过 **Function Calling** 自动选择并调用工具。智能体会**主动追问必要信息**（度数、用途、预算、脸型等），信息足够后给出配镜建议，并在最后**附上京东 / 淘宝 / 拼多多的购买链接**。

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
- `frame_fit_calculator`：解析镜架规格标注（如 `52□18-140`），按盒式标注法算出镜架几何中心距（镜圈宽 + 鼻梁），与瞳距比较得出每片光心移心量与方向，评估镜架与瞳距的贴合度；给定度数时用 Prentice 公式估算「不移心」会产生的水平棱镜
- `visual_acuity_converter`：在小数记录法、五分记录法（对数，中国 GB 标准）、Snellen 分数（20/20 或 6/6）、logMAR 四种等价视力记法间互换（五分 L = 5 + lg(小数)，logMAR = −lg(小数)），并给出该视力的大致水平
- `accommodation_amplitude`：用 Hofstetter 公式按年龄估算调节幅度（最小 = 15 − 0.25×年龄，平均 = 18.5 − 0.30×年龄，最大 = 25 − 0.40×年龄），推算调节近点与「保留一半调节力」的舒适持续用眼最近距离；给定工作距离时判断该距离的调节需求是否在储备之内，不足时给出建议近附加
- `sunglass_tint_guide`：按用光环境推荐太阳镜镜片过滤分类（ISO 12312-1 的 0–4 类）与可见光透过率（VLT），结合是否畏光 / 驾驶 / 带度数给出镜片颜色（灰 / 茶棕 / 墨绿 / 黄琥珀）、偏光、变色片与带度数选配建议，并提醒镜片深浅 ≠ 防紫外线、任何太阳镜都应达到 UV400
- `lens_material_advisor`：按度数、镜框类型（全框 / 半框 / 无框）、使用场景（日常 / 儿童 / 运动 / 安全防护 / 驾驶）与取舍偏好，在 CR-39、Trivex、PC、1.60 / 1.67 / 1.74 高折射树脂、玻璃之间推荐镜片基材，综合抗冲击、阿贝数（边缘色散）、厚度与重量给出首选 / 备选 / 不建议材料，与 `lens_thickness_estimator`、`lens_coating_advisor` 互补
- `clear_vision_range`：按裸眼屈光度（含柱镜按等效球镜 SE = SPH + CYL/2）推算不戴镜能看清的距离范围（远点 ↔ 近点）：近视远点 = 100 ÷ 度数(cm)，远点以外模糊、近处却清楚（即「近视摘镜看手机反而清楚」），正视清晰到无穷远，远视需动用调节力代偿；可选填年龄按 Hofstetter 平均公式估算调节力算出近点与远视代偿，与 `accommodation_amplitude` 互补
- `near_acuity_converter`：近用视力（近视力表）记法换算——在 M 记法（1M 视标在 1m 处张角 5′）、印刷点数（≈ M × 8）、近视力小数三种写法间换算，按测试距离（默认 40cm）算出该距离的近视力小数（= 测试距离m ÷ M）并推出 Snellen / logMAR / 五分等效与对照表（含 Jaeger 近似），与看远的 `visual_acuity_converter` 互补
- `prism_resolver`：棱镜合成与分解（Prentice 向量运算）——`combine` 把水平（base-in/out）与垂直（base-up/down）两个棱镜分量合成为单一合棱镜（大小 = √(水平² + 垂直²)，方向角：颞侧 0°、上 90°、鼻侧 180°、下 270°，逆时针）；`resolve` 把合棱镜（大小 + 方向角）分解回水平、垂直分量。采用以鼻子为参照、左右眼一致的 base-in/out 表述，提示 0.25Δ 处方取整

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

镜架尺寸适配（`lensWidth` / `bridge` / `pd` 必填，`power` / `templeLength` 可选；下例镜架偏宽需向鼻侧移心）：

```bash
curl -X POST http://localhost:8080/api/tools/frame_fit_calculator \
  -H 'Content-Type: application/json' \
  -d '{"lensWidth":52,"bridge":18,"pd":62,"power":-4.0,"templeLength":140}'
```

视力记录法换算（`notation` = decimal / five_minute / logmar / snellen，`value` 必填；Snellen 时 `value` 填分母，`snellenNumerator` 默认 20）：

```bash
curl -X POST http://localhost:8080/api/tools/visual_acuity_converter \
  -H 'Content-Type: application/json' \
  -d '{"notation":"decimal","value":1.0}'
```

太阳镜镜片色号与颜色选择（`environment` 必填：indoor_night / overcast / sunny / bright / snow_water；`lightSensitivity`、`driving`、`hasPrescription` 可选）：

```bash
curl -X POST http://localhost:8080/api/tools/sunglass_tint_guide \
  -H 'Content-Type: application/json' \
  -d '{"environment":"bright","lightSensitivity":"high","driving":true,"hasPrescription":true}'
```

镜片材料（基材）选择顾问（`sph` 必填；`cyl`、`frameType`（full_rim / half_rim / rimless）、`usage`（general / kids / sports / safety / driving）、`priority`（balanced / thinnest / clarity / lightweight）可选）：

```bash
curl -X POST http://localhost:8080/api/tools/lens_material_advisor \
  -H 'Content-Type: application/json' \
  -d '{"sph":-4,"cyl":-1,"frameType":"rimless","usage":"general","priority":"balanced"}'
```

裸眼清晰视界估算（远点 / 近点）（`sph` 必填；`cyl`、`age` 可选）：

```bash
curl -X POST http://localhost:8080/api/tools/clear_vision_range \
  -H 'Content-Type: application/json' \
  -d '{"sph":-3,"cyl":-0.5,"age":25}'
```

近用视力（近视力表）记法换算（`notation` 取 `m_unit` / `point` / `decimal`，`value` 必填；`testDistanceCm` 可选，默认 40）：

```bash
curl -X POST http://localhost:8080/api/tools/near_acuity_converter \
  -H 'Content-Type: application/json' \
  -d '{"notation":"m_unit","value":1.0,"testDistanceCm":40}'
```

棱镜合成与分解（`mode` 取 `combine` / `resolve`；combine 填 `horizontal`+`horizontalBase`(in/out) 与 `vertical`+`verticalBase`(up/down)，resolve 填 `magnitude`+`angle`）：

```bash
curl -X POST http://localhost:8080/api/tools/prism_resolver \
  -H 'Content-Type: application/json' \
  -d '{"mode":"combine","horizontal":3,"horizontalBase":"out","vertical":4,"verticalBase":"up"}'
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
    │   ├── GlassAdvisorTools.java      # 23 个工具的业务逻辑 + @Tool 注解
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
