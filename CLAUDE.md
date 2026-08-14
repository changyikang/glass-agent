# CLAUDE.md

给在本仓库中工作的 Claude（及其它自动化迭代）的项目须知。

## 项目简介

glass-agent 是「智能配镜顾问」，把配眼镜领域知识封装成可被大模型 / MCP 客户端调用的工具。
**两套共享同一组工具契约的实现**，改动共享工具/规则时两边必须同步：

- **TypeScript MCP Server**（根目录 `src/`）：`index.ts` 工具实现 + MCP 入口，`web.ts` 本地调试页，`history.ts` 工具调用历史。
- **Java + Spring Boot 智能体**（`java/`）：Spring AI Function Calling，含多轮问诊 `/api/agent/chat`、工具直调 `/api/tools/**`、调用历史 `/api/history`。

## 构建与测试（提交前必须全绿）

- TypeScript：`npm ci`（或 `npm install`）+ `npm run build` + `npm test`
- Java：在 `java/` 目录跑 `mvn -B verify`

## 每次迭代的约定

- 一次只做**一个小而完整、真实可用**的功能；想不出值得做的就**不提交**，绝不凑数。
- 改到共享工具/规则时，**TypeScript 与 Java 两边都要同步实现并各自加/更新单元测试**。
- 同步文档：README（中英两侧）、`java/README.md`、`CHANGELOG.md` 的 Unreleased、以及 README 的 Roadmap（含工具数量与示例）。
- 提交信息用规范前缀（feat/fix/docs/test/refactor/chore），结尾附：
  `Co-Authored-By: Claude <noreply@anthropic.com>`

## Git 分支工作流（重要，避免建出多余分支）

**目标分支就是 `dev`，且本地分支名也要叫 `dev`。** 收尾的 Stop hook 是按「本地分支
是否有同名远程分支未推送」来判定的；如果本地分支名和推送目标不一致（例如
`git push HEAD:dev`），hook 会误报未推送、进而诱使你去建一个多余的同名远程分支。

因此每次迭代按下面来，既满足「推 dev」的要求，又不会触发 hook 建多余分支：

```bash
git fetch origin dev
git checkout -B dev origin/dev   # 本地分支就叫 dev，基于最新 dev
# ...改动、构建、测试全绿...
git commit ...
git push -u origin dev           # 同名推送，hook 不会误报
```

- **不要**用 `git push HEAD:<别的名字>` 这种跨名推送。
- **不要**为了安抚 hook 去把同一 commit 再推一个别名分支。
- **不要**创建 Pull Request（作者会自行从 dev 开 PR 到 main），除非明确要求。
- 注意：本环境允许 `push`，但**禁止删除远程分支**（`git push --delete` 会返回 403）。
  所以更要一开始就用对分支名，别推出删不掉的多余分支。
