#!/usr/bin/env node
import { createServer, IncomingMessage, ServerResponse } from "node:http";
import { listTools, executeTool, ToolResult } from "./index.js";

const PORT = Number(process.env.PORT ?? 3000);
const HOST = process.env.HOST ?? "127.0.0.1";

const toolCatalog = listTools();

function sendJson(response: ServerResponse, statusCode: number, payload: unknown): void {
  response.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
  });
  response.end(JSON.stringify(payload));
}

function sendHtml(response: ServerResponse, html: string): void {
  response.writeHead(200, {
    "Content-Type": "text/html; charset=utf-8",
    "Cache-Control": "no-store",
  });
  response.end(html);
}

async function readJsonBody(request: IncomingMessage): Promise<unknown> {
  const chunks: Buffer[] = [];

  for await (const chunk of request) {
    chunks.push(typeof chunk === "string" ? Buffer.from(chunk) : chunk);
  }

  const raw = Buffer.concat(chunks).toString("utf8").trim();
  if (!raw) {
    return {};
  }

  try {
    return JSON.parse(raw);
  } catch {
    throw new Error("请求体必须是合法 JSON");
  }
}

function renderPage(): string {
  const serializedTools = JSON.stringify(toolCatalog).replace(/</g, "\\u003c");

  return `<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>配镜 MCP 本地调试页</title>
    <style>
      :root {
        --bg: #f3efe7;
        --panel: rgba(255, 251, 245, 0.86);
        --panel-strong: #fffaf2;
        --text: #1f1b16;
        --muted: #665e55;
        --line: rgba(70, 54, 38, 0.14);
        --accent: #cc5a2e;
        --accent-strong: #9f3d18;
        --accent-soft: rgba(204, 90, 46, 0.14);
        --success: #2d6b45;
        --error: #a8362a;
        --shadow: 0 24px 60px rgba(62, 41, 24, 0.12);
      }

      * {
        box-sizing: border-box;
      }

      body {
        margin: 0;
        min-height: 100vh;
        font-family: "Iowan Old Style", "Palatino Linotype", "Book Antiqua", serif;
        color: var(--text);
        background:
          radial-gradient(circle at top left, rgba(204, 90, 46, 0.2), transparent 28%),
          radial-gradient(circle at bottom right, rgba(71, 116, 95, 0.14), transparent 24%),
          linear-gradient(180deg, #f6f0e8 0%, #efe7db 100%);
      }

      .shell {
        width: min(1220px, calc(100vw - 32px));
        margin: 24px auto 40px;
      }

      .hero {
        display: grid;
        gap: 18px;
        padding: 28px;
        border: 1px solid var(--line);
        border-radius: 28px;
        background: linear-gradient(135deg, rgba(255,255,255,0.78), rgba(255,250,242,0.96));
        box-shadow: var(--shadow);
      }

      .eyebrow {
        font-family: "Avenir Next", "Helvetica Neue", sans-serif;
        font-size: 12px;
        letter-spacing: 0.18em;
        text-transform: uppercase;
        color: var(--accent-strong);
      }

      h1 {
        margin: 0;
        font-size: clamp(36px, 5vw, 64px);
        line-height: 0.95;
        max-width: 10ch;
      }

      .hero p {
        margin: 0;
        max-width: 62ch;
        color: var(--muted);
        font-size: 17px;
        line-height: 1.6;
      }

      .hero-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
      }

      .chip {
        padding: 10px 14px;
        border-radius: 999px;
        border: 1px solid var(--line);
        background: rgba(255,255,255,0.64);
        font-family: "Avenir Next", "Helvetica Neue", sans-serif;
        font-size: 13px;
        color: var(--muted);
      }

      .layout {
        display: grid;
        gap: 20px;
        grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.9fr);
        margin-top: 20px;
      }

      .tools {
        display: grid;
        gap: 18px;
      }

      .panel {
        border: 1px solid var(--line);
        border-radius: 24px;
        background: var(--panel);
        backdrop-filter: blur(16px);
        box-shadow: var(--shadow);
      }

      .card {
        padding: 20px;
      }

      .tool-card {
        display: grid;
        gap: 16px;
      }

      .tool-header {
        display: flex;
        gap: 14px;
        align-items: start;
        justify-content: space-between;
      }

      .tool-header h2 {
        margin: 0 0 8px;
        font-size: 24px;
      }

      .tool-header p {
        margin: 0;
        color: var(--muted);
        line-height: 1.55;
      }

      .tool-id {
        display: inline-block;
        margin-top: 8px;
        font-family: "SFMono-Regular", "Menlo", monospace;
        font-size: 12px;
        color: var(--muted);
      }

      .badge {
        flex: 0 0 auto;
        padding: 8px 10px;
        border-radius: 999px;
        background: var(--accent-soft);
        color: var(--accent-strong);
        font-family: "Avenir Next", "Helvetica Neue", sans-serif;
        font-size: 12px;
      }

      .fields {
        display: grid;
        gap: 14px;
        grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
      }

      label {
        display: grid;
        gap: 6px;
        font-size: 14px;
        font-family: "Avenir Next", "Helvetica Neue", sans-serif;
        color: var(--muted);
      }

      .field-title {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .required {
        color: var(--accent-strong);
      }

      input,
      select,
      textarea,
      button {
        font: inherit;
      }

      input,
      select,
      textarea {
        width: 100%;
        padding: 12px 14px;
        border-radius: 14px;
        border: 1px solid rgba(70, 54, 38, 0.16);
        background: rgba(255, 255, 255, 0.86);
        color: var(--text);
      }

      textarea {
        min-height: 132px;
        resize: vertical;
        font-family: "SFMono-Regular", "Menlo", monospace;
        font-size: 13px;
        line-height: 1.5;
      }

      .actions {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
      }

      button {
        appearance: none;
        border: 0;
        border-radius: 999px;
        padding: 12px 18px;
        cursor: pointer;
        transition: transform 160ms ease, opacity 160ms ease, background 160ms ease;
      }

      button:hover {
        transform: translateY(-1px);
      }

      button:disabled {
        opacity: 0.6;
        cursor: wait;
        transform: none;
      }

      .primary {
        background: var(--accent);
        color: #fffaf4;
      }

      .secondary {
        background: rgba(255,255,255,0.78);
        color: var(--muted);
        border: 1px solid var(--line);
      }

      .aside {
        position: sticky;
        top: 20px;
        align-self: start;
      }

      .aside h3 {
        margin: 0 0 10px;
        font-size: 22px;
      }

      .aside p {
        margin: 0 0 16px;
        color: var(--muted);
        line-height: 1.6;
      }

      .result-meta {
        display: grid;
        gap: 10px;
        margin-bottom: 14px;
      }

      .status {
        display: inline-flex;
        width: fit-content;
        padding: 8px 12px;
        border-radius: 999px;
        font-family: "Avenir Next", "Helvetica Neue", sans-serif;
        font-size: 12px;
        letter-spacing: 0.04em;
      }

      .status.ok {
        color: var(--success);
        background: rgba(45, 107, 69, 0.12);
      }

      .status.error {
        color: var(--error);
        background: rgba(168, 54, 42, 0.12);
      }

      pre {
        margin: 0;
        white-space: pre-wrap;
        word-break: break-word;
        border-radius: 18px;
        background: rgba(26, 20, 15, 0.95);
        color: #f9f3eb;
        padding: 18px;
        min-height: 320px;
        box-shadow: inset 0 0 0 1px rgba(255,255,255,0.06);
      }

      .tips {
        margin-top: 16px;
        padding-top: 16px;
        border-top: 1px solid var(--line);
        color: var(--muted);
        line-height: 1.7;
      }

      .tips strong {
        color: var(--text);
      }

      @media (max-width: 920px) {
        .layout {
          grid-template-columns: 1fr;
        }

        .aside {
          position: static;
        }
      }
    </style>
  </head>
  <body>
    <div class="shell">
      <section class="hero">
        <div class="eyebrow">本地调试页</div>
        <h1>配镜顾问工具台</h1>
        <p>这是给本地调试和演示用的页面，不替代 MCP 本体。页面里每个表单都会直接调用同一套工具逻辑，方便你快速验证输入、输出和规则是否符合预期。</p>
        <div class="hero-meta">
          <div class="chip">浏览器访问</div>
          <div class="chip">无需 MCP 客户端</div>
          <div class="chip">共 ${toolCatalog.length} 个可调试工具</div>
        </div>
      </section>

      <section class="layout">
        <div class="tools" id="tools-root"></div>
        <aside class="panel card aside">
          <h3>运行结果</h3>
          <p>选择任意工具，填写参数后点击执行。右侧会保留最近一次调用结果，便于你检查规则、报错和文本输出。</p>
          <div class="result-meta">
            <span id="result-status" class="status ok">等待运行</span>
            <div id="result-tool" class="chip">尚未选择工具</div>
          </div>
          <pre id="result-output">点击左侧任意工具的“运行工具”开始。</pre>
          <div class="tips">
            <strong>提示</strong><br />
            如果想验证 MCP 客户端集成，仍然用 <code>node dist/index.js</code>。这个页面只是本地调试面板。
          </div>
        </aside>
      </section>
    </div>

    <script id="tool-data" type="application/json">${serializedTools}</script>
    <script>
      const tools = JSON.parse(document.getElementById("tool-data").textContent);
      const root = document.getElementById("tools-root");
      const resultStatus = document.getElementById("result-status");
      const resultTool = document.getElementById("result-tool");
      const resultOutput = document.getElementById("result-output");
      const toolLabelMap = {
        vision_check_guide: "视力检查指南",
        lens_recommendation: "镜片推荐",
        frame_selection_guide: "镜框选择指南",
        prescription_interpreter: "验光单解读",
        progressive_lens_assessment: "渐进镜片适配评估",
        new_glasses_troubleshooting: "新眼镜不适排查"
      };
      const fieldLabelMap = {
        age_group: "年龄段",
        concern: "关注点",
        sph: "球镜度数",
        cyl: "柱镜度数",
        usage: "主要用途",
        budget: "预算档次",
        face_shape: "脸型",
        lifestyle: "生活方式",
        prescription_strength: "度数深浅",
        od_sph: "右眼球镜",
        od_cyl: "右眼柱镜",
        od_axis: "右眼轴位",
        os_sph: "左眼球镜",
        os_cyl: "左眼柱镜",
        os_axis: "左眼轴位",
        pd: "瞳距",
        add: "老花附加度数",
        age: "年龄",
        near_difficulty: "近距离阅读困难",
        screen_hours: "日均屏幕时长",
        drive_frequency: "驾驶频率",
        first_time_user: "是否首次尝试",
        symptom: "主要症状",
        wear_days: "佩戴天数",
        lens_type: "镜片类型",
        prescription_changed: "度数是否明显变化"
      };
      const optionLabelMap = {
        age_group: {
          children: "儿童（6-18岁）",
          adult: "成人（18-59岁）",
          senior: "老年（60岁以上）"
        },
        usage: {
          daily: "日常使用",
          computer: "电脑办公",
          driving: "驾驶",
          sports: "运动",
          reading: "阅读"
        },
        budget: {
          economy: "经济型",
          mid: "中档",
          premium: "高端"
        },
        face_shape: {
          oval: "椭圆脸",
          round: "圆脸",
          square: "方脸",
          heart: "心形脸",
          oblong: "长脸"
        },
        lifestyle: {
          professional: "商务",
          casual: "休闲",
          active: "运动",
          fashion: "时尚"
        },
        prescription_strength: {
          low: "低度数",
          medium: "中度数",
          high: "高度数"
        },
        near_difficulty: {
          none: "没有明显困难",
          mild: "轻度困难",
          obvious: "明显困难"
        },
        drive_frequency: {
          rare: "很少",
          weekly: "每周",
          daily: "几乎每天"
        },
        symptom: {
          dizziness: "头晕眩晕",
          blur_distance: "看远模糊",
          blur_near: "看近模糊",
          headache: "头痛",
          double_vision: "重影",
          nose_pain: "鼻梁压痛",
          ear_pain: "耳后压痛",
          slipping: "镜架下滑"
        },
        lens_type: {
          single_vision: "单光镜片",
          progressive: "渐进多焦点镜片",
          office: "办公镜片",
          bifocal: "双光镜片"
        },
        __boolean__: {
          true: "是",
          false: "否"
        }
      };

      function displayToolName(tool) {
        return toolLabelMap[tool.name] || tool.name;
      }

      function displayFieldName(fieldName, fieldSchema) {
        if (fieldLabelMap[fieldName]) {
          return fieldLabelMap[fieldName];
        }
        if (fieldSchema.description) {
          return fieldSchema.description.split("，")[0].split("：")[0];
        }
        return fieldName;
      }

      function displayOption(fieldName, value) {
        const fieldMap = optionLabelMap[fieldName] || optionLabelMap.__boolean__;
        return (fieldMap && fieldMap[String(value)]) || String(value);
      }

      function fieldDefault(field) {
        if (Array.isArray(field.enum) && field.enum.length > 0) {
          return field.enum[0];
        }
        if (field.type === "boolean") {
          return false;
        }
        if (field.type === "number") {
          return "";
        }
        return "";
      }

      function makeInput(toolName, fieldName, fieldSchema, requiredSet) {
        const wrapper = document.createElement("label");
        const title = document.createElement("div");
        title.className = "field-title";
        title.innerHTML = "<span>" + displayFieldName(fieldName, fieldSchema) + "</span>" + (requiredSet.has(fieldName) ? '<span class="required">必填</span>' : "");
        wrapper.appendChild(title);

        let input;
        if (Array.isArray(fieldSchema.enum)) {
          input = document.createElement("select");
          for (const item of fieldSchema.enum) {
            const option = document.createElement("option");
            option.value = item;
            option.textContent = displayOption(fieldName, item);
            input.appendChild(option);
          }
        } else if (fieldSchema.type === "boolean") {
          input = document.createElement("select");
          ["false", "true"].forEach((value) => {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = displayOption("__boolean__", value);
            input.appendChild(option);
          });
        } else {
          input = document.createElement("input");
          input.type = fieldSchema.type === "number" ? "number" : "text";
          if (fieldSchema.type === "number") {
            input.step = "any";
          }
        }

        input.name = fieldName;
        input.dataset.type = fieldSchema.type || "string";
        input.dataset.tool = toolName;
        const value = fieldDefault(fieldSchema);
        input.value = String(value);
        if (fieldSchema.description) {
          input.title = fieldSchema.description;
          input.placeholder = fieldSchema.description;
        }
        wrapper.appendChild(input);
        return wrapper;
      }

      function parseValue(input) {
        const raw = input.value.trim();
        if (raw === "") {
          return undefined;
        }
        if (input.dataset.type === "number") {
          return Number(raw);
        }
        if (input.dataset.type === "boolean") {
          return raw === "true";
        }
        return raw;
      }

      function createToolCard(tool) {
        const card = document.createElement("section");
        card.className = "panel card tool-card";
        const requiredSet = new Set(tool.inputSchema.required || []);

        const header = document.createElement("div");
        header.className = "tool-header";
        header.innerHTML = \`
          <div>
            <h2>\${displayToolName(tool)}</h2>
            <p>\${tool.description}</p>
            <span class="tool-id">\${tool.name}</span>
          </div>
          <div class="badge">\${Object.keys(tool.inputSchema.properties || {}).length} 项参数</div>
        \`;
        card.appendChild(header);

        const fields = document.createElement("div");
        fields.className = "fields";
        for (const [fieldName, fieldSchema] of Object.entries(tool.inputSchema.properties || {})) {
          fields.appendChild(makeInput(tool.name, fieldName, fieldSchema, requiredSet));
        }
        card.appendChild(fields);

        const quickJson = document.createElement("textarea");
        quickJson.value = JSON.stringify(Object.fromEntries(
          Object.entries(tool.inputSchema.properties || {}).map(([fieldName, fieldSchema]) => [fieldName, fieldDefault(fieldSchema)])
        ), null, 2);
        quickJson.setAttribute("aria-label", displayToolName(tool) + " 参数 JSON");
        card.appendChild(quickJson);

        const actions = document.createElement("div");
        actions.className = "actions";

        const runButton = document.createElement("button");
        runButton.className = "primary";
        runButton.textContent = "运行工具";

        const syncButton = document.createElement("button");
        syncButton.className = "secondary";
        syncButton.textContent = "表单写入 JSON";

        const loadButton = document.createElement("button");
        loadButton.className = "secondary";
        loadButton.textContent = "JSON 回填表单";

        syncButton.addEventListener("click", () => {
          const args = {};
          const inputs = fields.querySelectorAll("input, select");
          for (const input of inputs) {
            const value = parseValue(input);
            if (value !== undefined) {
              args[input.name] = value;
            }
          }
          quickJson.value = JSON.stringify(args, null, 2);
        });

        loadButton.addEventListener("click", () => {
          let args;
          try {
            args = JSON.parse(quickJson.value || "{}");
          } catch (error) {
            resultStatus.textContent = "JSON 无效";
            resultStatus.className = "status error";
            resultTool.textContent = displayToolName(tool);
            resultOutput.textContent = String(error);
            return;
          }

          const inputs = fields.querySelectorAll("input, select");
          for (const input of inputs) {
            if (!(input.name in args)) {
              input.value = "";
              continue;
            }
            const value = args[input.name];
            input.value = String(value);
          }
        });

        runButton.addEventListener("click", async () => {
          runButton.disabled = true;
          resultStatus.textContent = "运行中";
          resultStatus.className = "status ok";
          resultTool.textContent = displayToolName(tool);
          resultOutput.textContent = "请求处理中...";

          let args;
          try {
            args = JSON.parse(quickJson.value || "{}");
          } catch (error) {
            resultStatus.textContent = "JSON 无效";
            resultStatus.className = "status error";
            resultOutput.textContent = String(error);
            runButton.disabled = false;
            return;
          }

          try {
            const response = await fetch("/api/run", {
              method: "POST",
              headers: {
                "Content-Type": "application/json"
              },
              body: JSON.stringify({
                name: tool.name,
                arguments: args
              })
            });

            const payload = await response.json();
            const text = Array.isArray(payload.content)
              ? payload.content.map((item) => item.text || JSON.stringify(item, null, 2)).join("\\n\\n")
              : JSON.stringify(payload, null, 2);

            resultStatus.textContent = payload.isError ? "工具返回错误" : "调用成功";
            resultStatus.className = payload.isError ? "status error" : "status ok";
            resultOutput.textContent = text;
          } catch (error) {
            resultStatus.textContent = "请求失败";
            resultStatus.className = "status error";
            resultOutput.textContent = String(error);
          } finally {
            runButton.disabled = false;
          }
        });

        actions.appendChild(runButton);
        actions.appendChild(syncButton);
        actions.appendChild(loadButton);
        card.appendChild(actions);

        root.appendChild(card);
      }

      for (const tool of tools) {
        createToolCard(tool);
      }
    </script>
  </body>
</html>`;
}

async function handleRun(request: IncomingMessage, response: ServerResponse): Promise<void> {
  try {
    const payload = await readJsonBody(request);
    if (payload === null || typeof payload !== "object" || Array.isArray(payload)) {
      sendJson(response, 400, { content: [{ type: "text", text: "错误：请求体必须是对象" }], isError: true });
      return;
    }

    const { name, arguments: args } = payload as { name?: unknown; arguments?: unknown };
    if (typeof name !== "string" || !name.trim()) {
      sendJson(response, 400, { content: [{ type: "text", text: "错误：name 必须是非空字符串" }], isError: true });
      return;
    }

    const result = executeTool(name, args);
    sendJson(response, result.isError ? 400 : 200, result);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    sendJson(response, 400, { content: [{ type: "text", text: `错误：${message}` }], isError: true } satisfies ToolResult);
  }
}

async function main(): Promise<void> {
  const server = createServer(async (request, response) => {
    const url = new URL(request.url ?? "/", `http://${request.headers.host ?? `${HOST}:${PORT}`}`);

    if (request.method === "GET" && url.pathname === "/") {
      sendHtml(response, renderPage());
      return;
    }

    if (request.method === "GET" && url.pathname === "/api/tools") {
      sendJson(response, 200, toolCatalog);
      return;
    }

    if (request.method === "POST" && url.pathname === "/api/run") {
      await handleRun(request, response);
      return;
    }

    sendJson(response, 404, {
      content: [{ type: "text", text: `错误：未找到 ${request.method ?? "GET"} ${url.pathname}` }],
      isError: true,
    });
  });

  await new Promise<void>((resolve) => {
    server.listen(PORT, HOST, () => {
      console.log(`Glass MCP web playground running at http://${HOST}:${PORT}`);
      resolve();
    });
  });
}

await main();
