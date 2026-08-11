# glass-mcp

一个用于配眼镜咨询场景的 MCP Server，提供验光单解读、镜片推荐、镜框选择、视力检查建议，以及新眼镜不适排查等能力。

这个项目目前提供三种运行方式：

- `stdio` MCP Server（TypeScript）：用于 Claude Desktop 或其他支持 MCP 的客户端
- 本地 Web 调试页（TypeScript）：用于浏览器里直接填写参数、查看工具返回结果
- **Java + Spring Boot 智能体**：见 [`java/`](java/) 目录，基于 Spring AI，用大模型通过 Function Calling 自动编排这 6 个工具，并提供工具直调 REST 接口

## 功能

当前内置工具：

- `vision_check_guide`：按年龄段提供视力检查建议
- `lens_recommendation`：根据度数、用途、预算推荐镜片
- `frame_selection_guide`：根据脸型、生活方式和度数推荐镜框
- `prescription_interpreter`：解读验光单参数
- `progressive_lens_assessment`：评估是否适合渐进镜片或办公镜
- `new_glasses_troubleshooting`：排查新眼镜佩戴不适

## 环境要求

- Node.js 18+
- npm

## 安装

```bash
cd /Users/kang/develop/project/glass-mcp
npm install
```

## 常用命令

```bash
npm run build
npm test
npm start
npm run web
```

说明：

- `npm run build`：编译 TypeScript 到 `dist/`
- `npm test`：编译并运行测试
- `npm start`：启动 `stdio` MCP Server
- `npm run web`：启动本地网页调试页

## 作为 MCP Server 使用

先编译：

```bash
npm run build
```

然后直接启动：

```bash
node dist/index.js
```

如果要接到 Claude Desktop 或其他 MCP 客户端，可以使用类似配置：

```json
{
  "mcpServers": {
    "glass-mcp": {
      "command": "node",
      "args": ["/Users/kang/develop/project/glass-mcp/dist/index.js"]
    }
  }
}
```

## 本地网页调试

启动：

```bash
npm run web
```

浏览器打开：

[http://127.0.0.1:3000](http://127.0.0.1:3000)

页面用途：

- 直接查看全部工具
- 通过表单或 JSON 输入参数
- 执行工具并查看文本结果
- 用于本地演示，不依赖 MCP 客户端

注意：这个页面只是调试入口，不替代真正的 MCP `stdio` 接入方式。

## 项目结构

```text
src/
  index.ts       MCP Server 主入口与工具实现
  web.ts         本地网页调试服务
  index.test.ts  基础测试
dist/            编译产物
```

## 验证项目是否正常

推荐按下面顺序验证：

1. 运行测试

```bash
npm test
```

2. 验证 MCP 入口能启动

```bash
node dist/index.js
```

正常情况进程会保持运行并等待客户端连接。

3. 验证本地页面

```bash
npm run web
```

然后打开 [http://127.0.0.1:3000](http://127.0.0.1:3000)。

## 当前实现说明

- 工具返回的是文本型 MCP 结果
- 页面展示层已经做了中文化处理
- 页面中的中文下拉框选项会映射回原始英文枚举值再调用后端
- 目前重点是本地调试和 MCP 工具调用，不是完整线上产品

## 后续可扩展方向

- 增加更多配镜业务规则和案例库
- 增加工具调用历史记录
- 增加示例数据一键填充
- 增加更正式的前端产品页面
- 增加更多自动化测试
