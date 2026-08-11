#!/usr/bin/env node
import { fileURLToPath } from "node:url";
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

type ToolArgs = Record<string, unknown>;
export type ToolResult = {
  content: Array<{ type: "text"; text: string }>;
  isError?: boolean;
};

export type ToolDefinition = {
  name: string;
  description: string;
  inputSchema: Record<string, unknown>;
  handler: (args: ToolArgs) => ToolResult;
};

const server = new Server(
  { name: "glass-agent", version: "1.2.0" },
  { capabilities: { tools: {} } }
);

export const tools: ToolDefinition[] = [
  {
    name: "vision_check_guide",
    description: "视力检查指南：按年龄段说明检查频率、重点项目、检查前准备和常见关注点。",
    inputSchema: {
      type: "object",
      properties: {
        age_group: {
          type: "string",
          enum: ["children", "adult", "senior"],
          description: "年龄段：children(6-18岁), adult(18-59岁), senior(60岁以上)",
        },
        concern: {
          type: "string",
          description: "关注点，可填写近视、散光、老花、弱视、干眼、隐形眼镜等",
        },
      },
      required: ["age_group"],
    },
    handler: handleVisionCheckGuide,
  },
  {
    name: "lens_recommendation",
    description: "镜片推荐：根据度数、散光、用途和预算给出折射率、材质、镀膜和选购建议。",
    inputSchema: {
      type: "object",
      properties: {
        sph: {
          type: "number",
          description: "球镜度数，单位D，如 -3.25。近视填负数，远视填正数。",
        },
        cyl: {
          type: "number",
          description: "柱镜度数，单位D，如 -0.75。无散光可不填。",
        },
        usage: {
          type: "string",
          enum: ["daily", "computer", "driving", "sports", "reading"],
          description: "主要场景：daily(日常), computer(电脑), driving(驾驶), sports(运动), reading(阅读)",
        },
        budget: {
          type: "string",
          enum: ["economy", "mid", "premium"],
          description: "预算：economy(经济), mid(中档), premium(高端)",
        },
      },
      required: ["sph", "usage", "budget"],
    },
    handler: handleLensRecommendation,
  },
  {
    name: "frame_selection_guide",
    description: "镜框选择指南：结合脸型、生活方式和度数深浅推荐框型、材质和尺寸思路。",
    inputSchema: {
      type: "object",
      properties: {
        face_shape: {
          type: "string",
          enum: ["oval", "round", "square", "heart", "oblong"],
          description: "脸型：oval, round, square, heart, oblong",
        },
        lifestyle: {
          type: "string",
          enum: ["professional", "casual", "active", "fashion"],
          description: "生活方式：professional(商务), casual(休闲), active(运动), fashion(时尚)",
        },
        prescription_strength: {
          type: "string",
          enum: ["low", "medium", "high"],
          description: "度数深浅：low(<300度), medium(300-600度), high(>600度)",
        },
      },
      required: ["face_shape", "lifestyle"],
    },
    handler: handleFrameSelectionGuide,
  },
  {
    name: "prescription_interpreter",
    description: "验光单解读：解释 SPH/CYL/AXIS/PD/ADD 的意义，并提示配镜风险点。",
    inputSchema: {
      type: "object",
      properties: {
        od_sph: { type: "number", description: "右眼球镜 OD SPH，单位D" },
        od_cyl: { type: "number", description: "右眼柱镜 OD CYL，单位D，可选" },
        od_axis: { type: "number", description: "右眼轴位 OD AXIS，0-180度，可选" },
        os_sph: { type: "number", description: "左眼球镜 OS SPH，单位D" },
        os_cyl: { type: "number", description: "左眼柱镜 OS CYL，单位D，可选" },
        os_axis: { type: "number", description: "左眼轴位 OS AXIS，0-180度，可选" },
        pd: { type: "number", description: "双眼瞳距 PD，单位mm，可选" },
        add: { type: "number", description: "老花附加度数 ADD，单位D，可选" },
      },
      required: ["od_sph", "os_sph"],
    },
    handler: handlePrescriptionInterpreter,
  },
  {
    name: "progressive_lens_assessment",
    description: "渐进镜片适配评估：判断更适合单焦、办公镜还是渐进多焦点镜片。",
    inputSchema: {
      type: "object",
      properties: {
        age: {
          type: "number",
          description: "年龄，用于判断老花需求与适应预期",
        },
        near_difficulty: {
          type: "string",
          enum: ["none", "mild", "obvious"],
          description: "近距离阅读困难程度",
        },
        screen_hours: {
          type: "number",
          description: "日均电脑或平板使用时长，单位小时",
        },
        drive_frequency: {
          type: "string",
          enum: ["rare", "weekly", "daily"],
          description: "驾驶频率",
        },
        first_time_user: {
          type: "boolean",
          description: "是否第一次尝试多焦点镜片",
        },
      },
      required: ["age", "near_difficulty", "screen_hours", "drive_frequency", "first_time_user"],
    },
    handler: handleProgressiveLensAssessment,
  },
  {
    name: "new_glasses_troubleshooting",
    description: "新眼镜不适排查：根据症状、佩戴时长和镜片类型判断是适应期还是需要复查。",
    inputSchema: {
      type: "object",
      properties: {
        symptom: {
          type: "string",
          enum: [
            "dizziness",
            "blur_distance",
            "blur_near",
            "headache",
            "double_vision",
            "nose_pain",
            "ear_pain",
            "slipping",
          ],
          description: "主要不适症状",
        },
        wear_days: {
          type: "number",
          description: "已经佩戴新眼镜多少天",
        },
        lens_type: {
          type: "string",
          enum: ["single_vision", "progressive", "office", "bifocal"],
          description: "镜片类型",
        },
        prescription_changed: {
          type: "boolean",
          description: "这次配镜度数是否有明显变化",
        },
      },
      required: ["symptom", "wear_days", "lens_type", "prescription_changed"],
    },
    handler: handleNewGlassesTroubleshooting,
  },
];

const toolMap = new Map(tools.map((tool) => [tool.name, tool]));

export function listTools(): Array<Omit<ToolDefinition, "handler">> {
  return tools.map(({ handler: _handler, ...tool }) => tool);
}

export function executeTool(name: string, args: unknown): ToolResult {
  const tool = toolMap.get(name);
  if (!tool) {
    return errorResult(`未知工具：${name}`);
  }

  try {
    return tool.handler(ensureObject(args ?? {}));
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return errorResult(message);
  }
}

server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: listTools(),
}));

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  return executeTool(request.params.name, request.params.arguments);
});

function handleVisionCheckGuide(args: ToolArgs): ToolResult {
  const ageGroup = expectEnum(args, "age_group", ["children", "adult", "senior"]);
  const concern = optionalString(args, "concern");

  const guides: Record<string, string> = {
    children: `## 儿童视力检查指南（6-18岁）

**建议频率**
- 正常随访：每6-12个月一次
- 已有近视、散光或弱视治疗中：每3-6个月一次

**重点项目**
- 裸眼与矫正视力
- 电脑验光与主觉验光
- 眼位、双眼视功能
- 12岁以下或怀疑假性近视时做散瞳验光

**检查前准备**
- 检查当天减少长时间近距离用眼
- 带上旧眼镜和既往验光记录
- 如需散瞳，预留返程和休息时间`,
    adult: `## 成人视力检查指南（18-59岁）

**建议频率**
- 无明显不适：每1-2年一次
- 近视/散光/长期屏幕工作：每年一次
- 度数近期变化快、夜间眩光明显或头痛：尽快复查

**重点项目**
- 裸眼及矫正视力
- 电脑验光、主觉验光、双眼平衡
- 裂隙灯检查
- 高度近视或家族史人群增加眼压与眼底检查

**检查前准备**
- 软性隐形眼镜建议停戴至少24-72小时
- 检查前休息双眼，避免刚长时间开车或看屏幕
- 带上现有眼镜，便于对比旧处方`,
    senior: `## 老年视力检查指南（60岁以上）

**建议频率**
- 常规筛查：每6-12个月一次
- 有糖尿病、高度近视、青光眼或白内障风险：遵医嘱更密集随访

**重点项目**
- 远近视力和老花需求评估
- 眼压、裂隙灯、眼底检查
- 必要时增加 OCT、视野或白内障评估

**检查前准备**
- 带上所有在用眼镜
- 记录近期症状：眩光、视物变形、飞蚊或闪光
- 如需散瞳，当天避免自行驾车`,
  };

  const concernTips: Array<[string, string]> = [
    [
      "近视",
      `**近视相关**
- 儿童青少年重点看近视进展速度，不只看一次度数
- 高度近视建议把眼底检查列为常规项目`,
    ],
    [
      "散光",
      `**散光相关**
- 散光配镜除了度数，还要关注轴位是否稳定
- 散光较大时建议现场试戴，确认清晰度和眩晕感`,
    ],
    [
      "老花",
      `**老花相关**
- 需要同时评估远用、近用和中距离（电脑）需求
- 不是只测 ADD，还要结合工作距离选镜片方案`,
    ],
    [
      "弱视",
      `**弱视相关**
- 儿童要同时评估屈光参差、斜视和双眼视功能
- 弱视训练和复查频率通常比普通配镜更密集`,
    ],
    [
      "干眼",
      `**干眼相关**
- 先处理眼表状态，再决定最终处方更稳妥
- 检查当天避免长时间戴隐形眼镜或熬夜`,
    ],
    [
      "隐形",
      `**隐形眼镜相关**
- 需评估角膜状态、泪膜和佩戴习惯
- 隐形眼镜验配与框架眼镜处方不能简单等同`,
    ],
  ];

  const matchedTips = concern
    ? concernTips
        .filter(([keyword]) => concern.includes(keyword))
        .map(([, tip]) => tip)
    : [];

  const suffix =
    matchedTips.length > 0
      ? `\n\n${matchedTips.join("\n\n")}\n\n**提醒**\n最终处方应以现场主觉验光和试戴结果为准。`
      : `\n\n**提醒**\n如果近期出现视力突然下降、眼痛、闪光感或飞蚊骤增，应优先就医，不建议只做配镜咨询。`;

  return textResult(`${guides[ageGroup]}${suffix}`);
}

function handleLensRecommendation(args: ToolArgs): ToolResult {
  const sph = expectNumber(args, "sph", { min: -20, max: 12 });
  const cyl = optionalNumber(args, "cyl", { min: -8, max: 8 }) ?? 0;
  const usage = expectEnum(args, "usage", ["daily", "computer", "driving", "sports", "reading"]);
  const budget = expectEnum(args, "budget", ["economy", "mid", "premium"]);

  const meridianPower = Math.max(Math.abs(sph), Math.abs(sph + cyl));
  const sphericalEquivalent = sph + cyl / 2;

  const indexRecommendation =
    meridianPower <= 2
      ? "1.56：轻度度数够用，成本低"
      : meridianPower <= 4
        ? "1.60：大多数日常配镜的均衡选择"
        : meridianPower <= 6
          ? "1.67：中高度数更合适，厚度与重量明显更友好"
          : "1.74：超高度数可选，但价格更高、色散控制也更需要注意";

  const typeRecommendation: Record<string, Record<string, string>> = {
    daily: {
      economy: "非球面单光镜片，优先保证加工精度和基础防反射",
      mid: "品牌非球面单光镜片，兼顾清晰度和耐用性",
      premium: "自由曲面或个性化单光镜片，适合对边缘成像和佩戴体验要求高的人群",
    },
    computer: {
      economy: "单光镜片 + 优质防反射镀膜，先控制眩光",
      mid: "抗疲劳或轻办公型镜片，更适合长时间屏幕使用",
      premium: "办公专区镜片，覆盖屏幕和桌面阅读距离",
    },
    driving: {
      economy: "高透光率单光镜片 + 防反射镀膜，夜间驾驶更实用",
      mid: "高透光率镜片 + 更好的防眩镀膜，白天可另配偏光太阳镜",
      premium: "夜间清晰度更好的高端驾驶镜片方案，建议与日间太阳镜分开配置",
    },
    sports: {
      economy: "PC 或 Trivex 这类抗冲击材质，优先安全性",
      mid: "抗冲击材质 + 疏水防污镀膜，便于频繁清洁",
      premium: "运动专用曲面镜片，但需确认处方与镜框包裹角匹配",
    },
    reading: {
      economy: "近用单光镜片，适合固定阅读距离",
      mid: "近用单光或入门办公镜片，适合阅读兼顾电脑",
      premium: "办公镜或渐进镜片前的精细验配方案，适合近中距离切换多的人群",
    },
  };

  const coatingRecommendation: Record<string, string> = {
    economy: "优先选择防反射 + UV 防护。不要把“防蓝光”当成默认刚需。",
    mid: "防反射 + UV + 疏水防污，日常体验提升最明显。",
    premium: "在中档镀膜基础上，选择更耐磨、更稳定的高透方案。",
  };

  const budgetRange: Record<string, string> = {
    economy: "约 300-800 元/副镜片",
    mid: "约 800-2000 元/副镜片",
    premium: "约 2000 元以上/副镜片",
  };

  const usageWarnings: string[] = [];
  if (meridianPower >= 5) {
    usageWarnings.push("度数偏高，镜框尽量不要过大，否则边缘厚度和重量会明显增加。");
  }
  if (Math.abs(cyl) >= 2) {
    usageWarnings.push("散光较大，建议选择成像更稳的非球面或个性化设计，并现场试戴确认轴位适应。");
  }
  if (usage === "driving") {
    usageWarnings.push("偏光镜适合白天强光环境，不适合替代夜间驾驶的主力镜片。");
  }
  if (usage === "sports") {
    usageWarnings.push("如果是对抗性运动，普通日常镜框不够安全，优先选运动框和抗冲击材质。");
  }
  if (usage === "reading" && Math.abs(sphericalEquivalent) < 0.5) {
    usageWarnings.push("如果主要是中老年近距离吃力，建议补做 ADD 检查，而不是只按单纯近视/远视选镜片。");
  }

  return textResult(`## 镜片推荐

**处方概览**
- 球镜：${formatSignedDiopter(sph)}
- 柱镜：${cyl === 0 ? "无明显散光" : formatSignedDiopter(cyl)}
- 等效球镜：${formatSignedDiopter(sphericalEquivalent)}
- 参考最大子午线度数：${formatDiopter(meridianPower)}

**折射率建议**
- ${indexRecommendation}

**用途和预算匹配**
- ${typeRecommendation[usage][budget]}

**镀膜建议**
- ${coatingRecommendation[budget]}

**预算区间**
- ${budgetRange[budget]}

**额外提醒**
${renderBulletList(usageWarnings, "整体处方压力不大，优先保证验光准确和镜框尺寸合适。")}

**结论**
- 镜片选择先看处方和用途，再看预算；不要只盯着折射率。
- 最终成品效果还受镜框尺寸、瞳距、加工中心定位影响。`);
}

function handleFrameSelectionGuide(args: ToolArgs): ToolResult {
  const faceShape = expectEnum(args, "face_shape", ["oval", "round", "square", "heart", "oblong"]);
  const lifestyle = expectEnum(args, "lifestyle", ["professional", "casual", "active", "fashion"]);
  const prescriptionStrength =
    optionalEnum(args, "prescription_strength", ["low", "medium", "high"]) ?? "medium";

  const faceGuides: Record<
    string,
    {
      summary: string;
      recommend: string;
      avoid: string;
    }
  > = {
    oval: {
      summary: "脸部比例均衡，可用多数框型做风格表达。",
      recommend: "方框、矩形框、猫眼框都容易出效果。",
      avoid: "过宽或过高的超大框容易压脸。",
    },
    round: {
      summary: "面部线条偏柔和，适合用更利落的框型拉开轮廓。",
      recommend: "矩形框、带棱角的方框、眉线框。",
      avoid: "小圆框和过于圆润的框型。",
    },
    square: {
      summary: "下颌线和额角更分明，适合用曲线柔化。",
      recommend: "圆框、椭圆框、软边猫眼框。",
      avoid: "过硬朗的厚重方框。",
    },
    heart: {
      summary: "上庭视觉重量较强，适合把重心往下和中部拉。",
      recommend: "轻薄金属框、椭圆框、下缘更稳定的框型。",
      avoid: "上宽下窄、眉线过重的设计。",
    },
    oblong: {
      summary: "脸型偏长，适合增加横向存在感。",
      recommend: "镜圈高度适中的宽框、大一点的圆角方框。",
      avoid: "过窄、过小的细长框。",
    },
  };

  const lifestyleGuides: Record<string, string> = {
    professional: "优先稳定、耐看和易搭配，黑、枪灰、深棕、钛色最稳妥。",
    casual: "可以接受板材、透明灰、琥珀色等更轻松的风格。",
    active: "优先 TR90、钛或带防滑结构的全框，别把外观放在安全性前面。",
    fashion: "可以把框型作为造型重点，但仍要确认瞳距和镜圈尺寸是否适合处方。",
  };

  const prescriptionTips: Record<string, string[]> = {
    low: [
      "低度数可选范围最大，无框、半框、全框都能尝试。",
      "如果想要更轻，可以优先考虑钛架或轻板材。",
    ],
    medium: [
      "中度数建议优先全框或结构稳定的半框。",
      "镜圈不要过大，否则边缘厚度和重量会上来。",
    ],
    high: [
      "高度数优先小一些的全框，镜圈不宜过宽。",
      "尽量避开无框和超大框，成品外观和稳定性都更难控制。",
      "建议选有鼻托、方便微调前倾角和顶点距的镜架。",
    ],
  };

  const materialGuide = [
    "板材：造型感强，适合休闲和时尚风格。",
    "金属/钛：轻、精致、好调校，适合商务和长时间佩戴。",
    "TR90：弹性和耐冲击更好，适合运动或通勤。",
  ];

  return textResult(`## 镜框选择指南

**脸型判断**
- ${faceGuides[faceShape].summary}

**推荐方向**
- ${faceGuides[faceShape].recommend}

**尽量避开**
- ${faceGuides[faceShape].avoid}

**生活方式匹配**
- ${lifestyleGuides[lifestyle]}

**度数相关建议**
${prescriptionTips[prescriptionStrength].map((tip) => `- ${tip}`).join("\n")}

**材质参考**
${materialGuide.map((tip) => `- ${tip}`).join("\n")}

**试戴时重点**
- 鼻托或鼻梁接触是否稳、是否压痛
- 镜腿是否夹头或易滑落
- 眼睛是否处在镜圈相对居中的位置
- 高度数时，优先看成品厚度和重量，再看单纯外观`);
}

function handlePrescriptionInterpreter(args: ToolArgs): ToolResult {
  const odSph = expectNumber(args, "od_sph", { min: -20, max: 12 });
  const odCyl = optionalNumber(args, "od_cyl", { min: -8, max: 8 }) ?? 0;
  const odAxis = optionalNumber(args, "od_axis", { min: 0, max: 180, integer: true });
  const osSph = expectNumber(args, "os_sph", { min: -20, max: 12 });
  const osCyl = optionalNumber(args, "os_cyl", { min: -8, max: 8 }) ?? 0;
  const osAxis = optionalNumber(args, "os_axis", { min: 0, max: 180, integer: true });
  const pd = optionalNumber(args, "pd", { min: 45, max: 80 });
  const add = optionalNumber(args, "add", { min: 0.5, max: 3.5 });

  validateAxis("右眼", odCyl, odAxis);
  validateAxis("左眼", osCyl, osAxis);

  const odEquivalent = odSph + odCyl / 2;
  const osEquivalent = osSph + osCyl / 2;
  const anisometropia = Math.abs(odEquivalent - osEquivalent);

  const warnings: string[] = [];
  if (anisometropia >= 2) {
    warnings.push("双眼等效球镜相差较大，可能出现放大率差异、头晕或融合困难。");
  }
  if (Math.max(Math.abs(odEquivalent), Math.abs(osEquivalent)) >= 6) {
    warnings.push("属于中高度屈光不正，配镜建议重视镜框尺寸、折射率和眼底随访。");
  }
  if (Math.max(Math.abs(odCyl), Math.abs(osCyl)) >= 2) {
    warnings.push("散光较大时，轴位误差会更影响清晰度和舒适度。");
  }
  if (add !== undefined && add >= 1.5) {
    warnings.push("ADD 已不低，是否需要渐进或办公镜要结合工作距离来定。");
  }
  if (pd === undefined) {
    warnings.push("当前没有 PD，正式加工前仍需准确测量瞳距，尤其是高度数或渐进镜片。");
  }

  return textResult(`## 验光单解读

**字段含义**
- OD / OS：右眼 / 左眼
- SPH：球镜，近视通常记负数，远视记正数
- CYL：柱镜，也就是散光
- AXIS：散光轴位，0-180°
- PD：瞳距
- ADD：老花附加度数

**右眼**
- 处方：${renderPrescriptionLine(odSph, odCyl, odAxis)}
- 解读：${describeEye(odSph, odCyl)}
- 等效球镜：${formatSignedDiopter(odEquivalent)}

**左眼**
- 处方：${renderPrescriptionLine(osSph, osCyl, osAxis)}
- 解读：${describeEye(osSph, osCyl)}
- 等效球镜：${formatSignedDiopter(osEquivalent)}

**双眼对比**
- 等效球镜差值：${formatDiopter(anisometropia)}
- 判断：${anisometropia < 1 ? "双眼比较接近" : anisometropia < 2 ? "有一定差异，配镜要注意平衡" : "差异较大，适应风险更高"}

${pd !== undefined ? `**瞳距**\n- ${pd} mm\n` : ""}${add !== undefined ? `**ADD**\n- ${formatSignedDiopter(add)}，提示需要评估近用或多焦点方案\n` : ""}
**配镜提醒**
${renderBulletList(warnings, "当前没有特别突出的风险点，仍建议结合试戴与加工参数确认。")}

**结论**
- 验光单不是成品眼镜，最终还要结合试戴、瞳高、镜框参数和加工质量。`);
}

function handleProgressiveLensAssessment(args: ToolArgs): ToolResult {
  const age = expectNumber(args, "age", { min: 18, max: 90, integer: true });
  const nearDifficulty = expectEnum(args, "near_difficulty", ["none", "mild", "obvious"]);
  const screenHours = expectNumber(args, "screen_hours", { min: 0, max: 16 });
  const driveFrequency = expectEnum(args, "drive_frequency", ["rare", "weekly", "daily"]);
  const firstTimeUser = expectBoolean(args, "first_time_user");

  let recommendation = "";
  let fitReason = "";

  if (nearDifficulty === "none" && age < 40) {
    recommendation = "更适合单光镜片，不建议为了“预防”而直接上渐进。";
    fitReason = "目前近距离困难不明显，渐进的收益有限。";
  } else if (screenHours >= 6 && driveFrequency !== "daily") {
    recommendation = "优先考虑办公镜片，其次再看是否需要一副单独远用镜。";
    fitReason = "长时间看屏幕的人，中近距离视野通常比通用型渐进更重要。";
  } else if (nearDifficulty === "obvious" && age >= 42) {
    recommendation = "可以重点评估渐进多焦点镜片。";
    fitReason = "远近切换需求明确，渐进能减少频繁摘戴两副眼镜。";
  } else {
    recommendation = "处于单光、办公镜和渐进都可能合适的区间，需要按实际工作距离细化。";
    fitReason = "是否开车多、是否长时间看电脑，会显著影响方案选择。";
  }

  const cautions: string[] = [];
  if (firstTimeUser) {
    cautions.push("第一次用渐进或办公镜，建议预留 1-2 周适应期。");
  }
  if (driveFrequency === "daily") {
    cautions.push("如果每天开车，通道设计和远用区域要足够稳定，不能只追求近用舒适。");
  }
  if (screenHours >= 8) {
    cautions.push("重度屏幕使用者要特别关注中距离视野宽度，普通渐进不一定最舒服。");
  }
  if (age >= 55) {
    cautions.push("ADD 往往较高，对通道长度和佩戴参数更敏感，验配要细。");
  }

  return textResult(`## 多焦点适配评估

**初步结论**
- ${recommendation}

**原因**
- ${fitReason}

**你的使用画像**
- 年龄：${age} 岁
- 近距离吃力程度：${nearDifficulty === "none" ? "不明显" : nearDifficulty === "mild" ? "轻度" : "明显"}
- 日均屏幕时长：${screenHours} 小时
- 驾驶频率：${driveFrequency === "rare" ? "很少" : driveFrequency === "weekly" ? "每周" : "几乎每天"}
- 是否首次尝试：${firstTimeUser ? "是" : "否"}

**建议关注**
${renderBulletList(cautions, "目前没有明显的额外适配风险，但仍建议先试戴再定方案。")}

**下一步**
- 让验光师明确测量远用、阅读和电脑距离需求，再决定单光 / 办公镜 / 渐进。`);
}

function handleNewGlassesTroubleshooting(args: ToolArgs): ToolResult {
  const symptom = expectEnum(args, "symptom", [
    "dizziness",
    "blur_distance",
    "blur_near",
    "headache",
    "double_vision",
    "nose_pain",
    "ear_pain",
    "slipping",
  ]);
  const wearDays = expectNumber(args, "wear_days", { min: 0, max: 60, integer: true });
  const lensType = expectEnum(args, "lens_type", ["single_vision", "progressive", "office", "bifocal"]);
  const prescriptionChanged = expectBoolean(args, "prescription_changed");

  const symptomAdvice: Record<string, string> = {
    dizziness: "常见于度数变化、散光轴位变化、镜片中心点偏移或多焦点初期适应。",
    blur_distance: "先排查远用度数不足/过矫、瞳距瞳高偏差、镜框前倾角变化。",
    blur_near: "如果是中老年人，常见于 ADD 不足、工作距离不匹配或把远用镜当近用镜。",
    headache: "常见于过矫、双眼平衡问题、散光轴位不适或长时间勉强适应。",
    double_vision: "优先排查棱镜效应、瞳距偏差和双眼视问题，这类情况不要硬扛。",
    nose_pain: "多半是鼻托/鼻梁受力不均或镜框太重，不一定是处方问题。",
    ear_pain: "镜腿弯点和夹持力不合适，通常靠调架能改善。",
    slipping: "镜框重心、鼻托摩擦和镜腿包覆不足，需要做机械调校。",
  };

  const isLikelyAdaptation =
    (lensType === "progressive" || lensType === "office" || prescriptionChanged) && wearDays <= 14;

  const urgentFlags = symptom === "double_vision" || (symptom === "headache" && wearDays > 7);

  const actionItems: string[] = [];
  if (symptom === "nose_pain" || symptom === "ear_pain" || symptom === "slipping") {
    actionItems.push("先回店里做镜架调校，很多佩戴问题不需要重做镜片。");
  } else {
    actionItems.push("带着旧眼镜和验光单回店复查，对比新旧处方和加工参数。");
  }
  if (wearDays <= 3 && isLikelyAdaptation && !urgentFlags) {
    actionItems.push("在安全前提下可继续短时间逐步佩戴，不要一整天硬撑。");
  }
  if (lensType === "progressive") {
    actionItems.push("确认看远时从镜片上方区域看、看近时轻微下转眼位，而不是只转头。");
  }
  if (urgentFlags || wearDays > 14) {
    actionItems.push("如果复查后仍明显不适，需考虑重新验光或重做镜片。");
  }

  return textResult(`## 新眼镜不适排查

**症状判断**
- ${symptomAdvice[symptom]}

**当前情况**
- 已佩戴：${wearDays} 天
- 镜片类型：${renderLensType(lensType)}
- 本次度数是否有明显变化：${prescriptionChanged ? "是" : "否"}

**倾向判断**
- ${urgentFlags ? "不建议继续硬适应，应尽快复查。" : isLikelyAdaptation ? "有一定概率属于适应期，但仍需留意是否逐日改善。" : "更像是处方、加工或镜架参数问题，而不只是适应期。"}

**建议动作**
${actionItems.map((item) => `- ${item}`).join("\n")}

**复查时重点问什么**
- 瞳距、瞳高、散光轴位是否准确
- 镜框前倾角、顶点距、面弯是否与验配时一致
- 新旧处方差异是否超过正常适应范围`);
}

function ensureObject(value: unknown): ToolArgs {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    throw new Error("工具参数必须是对象");
  }
  return value as ToolArgs;
}

function expectNumber(
  args: ToolArgs,
  key: string,
  options: { min?: number; max?: number; integer?: boolean } = {}
): number {
  const value = args[key];
  if (typeof value !== "number" || Number.isNaN(value) || !Number.isFinite(value)) {
    throw new Error(`参数 ${key} 必须是数字`);
  }
  if (options.integer && !Number.isInteger(value)) {
    throw new Error(`参数 ${key} 必须是整数`);
  }
  if (options.min !== undefined && value < options.min) {
    throw new Error(`参数 ${key} 不能小于 ${options.min}`);
  }
  if (options.max !== undefined && value > options.max) {
    throw new Error(`参数 ${key} 不能大于 ${options.max}`);
  }
  return value;
}

function optionalNumber(
  args: ToolArgs,
  key: string,
  options: { min?: number; max?: number; integer?: boolean } = {}
): number | undefined {
  if (!(key in args) || args[key] === undefined) {
    return undefined;
  }
  return expectNumber(args, key, options);
}

function expectBoolean(args: ToolArgs, key: string): boolean {
  const value = args[key];
  if (typeof value !== "boolean") {
    throw new Error(`参数 ${key} 必须是布尔值`);
  }
  return value;
}

function expectEnum<T extends string>(args: ToolArgs, key: string, allowed: readonly T[]): T {
  const value = args[key];
  if (typeof value !== "string" || !allowed.includes(value as T)) {
    throw new Error(`参数 ${key} 必须是以下值之一：${allowed.join(", ")}`);
  }
  return value as T;
}

function optionalEnum<T extends string>(
  args: ToolArgs,
  key: string,
  allowed: readonly T[]
): T | undefined {
  if (!(key in args) || args[key] === undefined) {
    return undefined;
  }
  return expectEnum(args, key, allowed);
}

function optionalString(args: ToolArgs, key: string): string | undefined {
  const value = args[key];
  if (value === undefined) {
    return undefined;
  }
  if (typeof value !== "string") {
    throw new Error(`参数 ${key} 必须是字符串`);
  }
  return value.trim() || undefined;
}

function validateAxis(label: string, cyl: number, axis: number | undefined): void {
  if (cyl !== 0 && axis === undefined) {
    throw new Error(`${label}有散光时必须提供轴位`);
  }
  if (cyl === 0 && axis !== undefined) {
    throw new Error(`${label}未填写散光时不应单独提供轴位`);
  }
}

function renderPrescriptionLine(sph: number, cyl: number, axis?: number): string {
  const parts = [`SPH ${formatSignedDiopter(sph)}`];
  if (cyl !== 0) {
    parts.push(`CYL ${formatSignedDiopter(cyl)}`);
  }
  if (axis !== undefined) {
    parts.push(`AXIS ${axis}°`);
  }
  return parts.join(" / ");
}

function describeEye(sph: number, cyl: number): string {
  const parts: string[] = [];
  if (sph < 0) {
    parts.push(`${severityLabel(Math.abs(sph))}近视`);
  } else if (sph > 0) {
    parts.push(`${severityLabel(Math.abs(sph))}远视`);
  } else {
    parts.push("球镜接近平光");
  }
  if (cyl !== 0) {
    parts.push(`伴${severityLabel(Math.abs(cyl))}散光`);
  }
  return parts.join("，");
}

function severityLabel(value: number): string {
  if (value < 3) {
    return "低度";
  }
  if (value < 6) {
    return "中度";
  }
  return "高度";
}

function formatDiopter(value: number): string {
  return `${trimTrailingZeros(value.toFixed(2))}D`;
}

function formatSignedDiopter(value: number): string {
  const normalized = trimTrailingZeros(Math.abs(value).toFixed(2));
  const sign = value > 0 ? "+" : value < 0 ? "-" : "";
  return `${sign}${normalized}D`;
}

function trimTrailingZeros(value: string): string {
  return value.replace(/\.?0+$/, "");
}

function renderLensType(lensType: string): string {
  const labels: Record<string, string> = {
    single_vision: "单光镜片",
    progressive: "渐进多焦点镜片",
    office: "办公镜片",
    bifocal: "双光镜片",
  };
  return labels[lensType] ?? lensType;
}

function renderBulletList(items: string[], fallback: string): string {
  if (items.length === 0) {
    return `- ${fallback}`;
  }
  return items.map((item) => `- ${item}`).join("\n");
}

function textResult(text: string): ToolResult {
  return {
    content: [{ type: "text", text }],
  };
}

function errorResult(message: string): ToolResult {
  return {
    content: [{ type: "text", text: `错误：${message}` }],
    isError: true,
  };
}

async function main(): Promise<void> {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

if (isMainModule(import.meta.url)) {
  await main();
}

function isMainModule(moduleUrl: string): boolean {
  const entry = process.argv[1];
  if (!entry) {
    return false;
  }
  return fileURLToPath(moduleUrl) === entry;
}
