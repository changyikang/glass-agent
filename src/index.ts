#!/usr/bin/env node
import { fileURLToPath } from "node:url";
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { recordCall } from "./history.js";

type ToolArgs = Record<string, unknown>;
export type ToolResult = {
  content: Array<{ type: "text"; text: string }>;
  isError?: boolean;
};

export type ToolDefinition = {
  name: string;
  description: string;
  inputSchema: Record<string, unknown>;
  /** A realistic, valid set of arguments used to pre-fill the debug web page. */
  sample: ToolArgs;
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
    sample: { age_group: "adult", concern: "近视" },
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
    sample: { sph: -3.25, cyl: -0.75, usage: "computer", budget: "mid" },
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
    sample: { face_shape: "round", lifestyle: "professional", prescription_strength: "medium" },
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
    sample: {
      od_sph: -3,
      od_cyl: -0.75,
      od_axis: 90,
      os_sph: -2.75,
      os_cyl: -0.5,
      os_axis: 85,
      pd: 62,
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
    sample: {
      age: 45,
      near_difficulty: "obvious",
      screen_hours: 7,
      drive_frequency: "weekly",
      first_time_user: true,
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
    sample: {
      symptom: "dizziness",
      wear_days: 3,
      lens_type: "progressive",
      prescription_changed: true,
    },
    handler: handleNewGlassesTroubleshooting,
  },
  {
    name: "shopping_links",
    description:
      "购物链接生成：把配镜建议转成可点击的电商搜索购买链接。传入若干中文商品关键词（如「1.67 非球面 防蓝光 镜片」「TR90 超轻 近视镜框」），返回每个关键词在京东、淘宝、拼多多的搜索链接，便于用户直接比价选购。",
    inputSchema: {
      type: "object",
      properties: {
        keywords: {
          type: "array",
          items: { type: "string" },
          description:
            "商品搜索关键词列表，建议每项包含关键规格，如折射率/材质/镀膜（镜片）或框型/材质（镜框）。",
        },
      },
      required: ["keywords"],
    },
    sample: { keywords: ["1.67 非球面 防蓝光 镜片", "TR90 超轻 近视镜框"] },
    handler: handleShoppingLinks,
  },
  {
    name: "lens_thickness_estimator",
    description:
      "镜片厚度估算：基于薄透镜矢高（sagitta）近似，按度数、折射率和镜圈宽度估算镜片最厚处（近视看边缘、远视看中心）的厚度与重量倾向，并判断是否值得提高折射率减薄。",
    inputSchema: {
      type: "object",
      properties: {
        sph: {
          type: "number",
          description: "球镜度数，单位D，如 -6.00。近视填负数，远视填正数。",
        },
        cyl: {
          type: "number",
          description: "柱镜度数，单位D，如 -1.00。无散光可不填。",
        },
        lens_index: {
          type: "string",
          enum: ["1.56", "1.60", "1.67", "1.74"],
          description: "镜片折射率：1.56 / 1.60 / 1.67 / 1.74",
        },
        frame_width: {
          type: "number",
          description: "镜圈水平宽度（镜腿上标注的“眼宽”数字），单位mm，常见 48-58，默认 52。",
        },
      },
      required: ["sph", "lens_index"],
    },
    sample: { sph: -6, cyl: -1, lens_index: "1.60", frame_width: 54 },
    handler: handleLensThicknessEstimator,
  },
  {
    name: "pupillary_distance_guide",
    description:
      "瞳距（PD）助手：网购配镜必填的瞳距参数解读与换算。可传入双眼瞳距，或分别传入左右单眼瞳距，工具会校验数值是否落在常见范围、互相核对、按工作距离折算近用瞳距，并给出左右不对称提醒和自测方法。",
    inputSchema: {
      type: "object",
      properties: {
        binocular_pd: {
          type: "number",
          description: "双眼瞳距（远用），单位mm，成人常见 54-74。与左右单眼瞳距至少提供一种。",
        },
        pd_right: {
          type: "number",
          description: "右眼单眼瞳距（瞳孔中心到鼻梁中线），单位mm。若填写需与 pd_left 一起提供。",
        },
        pd_left: {
          type: "number",
          description: "左眼单眼瞳距（瞳孔中心到鼻梁中线），单位mm。若填写需与 pd_right 一起提供。",
        },
        working_distance_cm: {
          type: "number",
          description: "近用工作距离，单位cm，用于折算近用瞳距，默认 40（常见阅读距离）。",
        },
      },
    },
    sample: { binocular_pd: 63, working_distance_cm: 40 },
    handler: handlePupillaryDistanceGuide,
  },
  {
    name: "lens_coating_advisor",
    description:
      "镜片镀膜与功能顾问：按用眼场景逐项判断「减反射绿膜、UV 防护、防蓝光、变色片、偏振太阳镜」值不值得为它多花钱，避免被过度推销。传入日均屏幕时长、户外/日晒频率、夜间驾驶频率等，返回每项功能的推荐等级、原因，以及一份「建议付费 / 可选 / 不必要」的购物清单。",
    inputSchema: {
      type: "object",
      properties: {
        screen_hours: {
          type: "number",
          description: "日均看屏幕（电脑/手机/平板）的时长，单位小时，0-18。",
        },
        outdoor_frequency: {
          type: "string",
          enum: ["rare", "sometimes", "often"],
          description: "户外/日晒暴露频率：rare(很少), sometimes(有时), often(经常)。",
        },
        night_driving: {
          type: "string",
          enum: ["none", "occasional", "frequent"],
          description: "夜间驾驶频率：none(基本不), occasional(偶尔), frequent(经常)。默认 none。",
        },
        light_sensitive: {
          type: "boolean",
          description: "是否对强光/眩光比较敏感（畏光、易被反光晃到）。默认 false。",
        },
        prefer_one_pair: {
          type: "boolean",
          description: "是否希望一副眼镜室内外通用（用于判断是否推荐变色片）。默认 false。",
        },
      },
      required: ["screen_hours", "outdoor_frequency"],
    },
    sample: {
      screen_hours: 9,
      outdoor_frequency: "sometimes",
      night_driving: "occasional",
      light_sensitive: false,
      prefer_one_pair: true,
    },
    handler: handleLensCoatingAdvisor,
  },
  {
    name: "myopia_control_guide",
    description:
      "青少年近视防控指南：根据孩子年龄、当前近视度数、近一年近视加深速度、父母近视情况和日均户外时长，评估近视进展风险，并按适配度排序给出干预手段（户外活动、科学用眼习惯、近视离焦框架镜、角膜塑形镜 OK 镜、低浓度阿托品），并说明各方案的适用条件与就医边界。仅为科普，用药与 OK 镜验配需到专业机构。",
    inputSchema: {
      type: "object",
      properties: {
        age: {
          type: "number",
          description: "孩子年龄（岁），3-18 的整数。",
        },
        current_sph: {
          type: "number",
          description:
            "当前近视球镜度数，单位D，近视填负数（如 -2.50）。大于 -0.50 视为尚未达到近视标准。",
        },
        annual_progression: {
          type: "number",
          description: "近一年近视加深的度数（正数，单位D/年，如 0.75）。不清楚可不填。",
        },
        parent_myopia: {
          type: "string",
          enum: ["none", "one", "both"],
          description: "父母近视情况：none(都不近视), one(一方近视), both(双方近视)。可不填。",
        },
        outdoor_hours: {
          type: "number",
          description: "孩子日均户外活动时长（小时，0-8）。可不填。",
        },
      },
      required: ["age", "current_sph"],
    },
    sample: {
      age: 9,
      current_sph: -1.5,
      annual_progression: 0.75,
      parent_myopia: "both",
      outdoor_hours: 0.5,
    },
    handler: handleMyopiaControlGuide,
  },
  {
    name: "anisometropia_guide",
    description:
      "屈光参差评估：输入左右眼球镜（及可选柱镜）度数，按等效球镜之差评估两眼屈光参差程度（无 / 轻度 / 中度 / 显著），估算框架镜下两眼视网膜影像大小差异（不等像 aniseikonia）与耐受边界，识别「一眼近视一眼远视」的混合性参差和柱镜差异过大等特殊情况，并给出框架镜 vs 隐形眼镜、逐步适应、儿童弱视预警等建议。仅供科普，具体以专业视光 / 眼科评估为准。",
    inputSchema: {
      type: "object",
      properties: {
        right_sph: {
          type: "number",
          description: "右眼(OD)球镜度数，单位D，近视填负数、远视填正数，如 -1.00。",
        },
        left_sph: {
          type: "number",
          description: "左眼(OS)球镜度数，单位D，近视填负数、远视填正数，如 -3.50。",
        },
        right_cyl: {
          type: "number",
          description: "右眼(OD)柱镜度数，单位D，负散光记法填负数，无散光可不填（默认0）。",
        },
        left_cyl: {
          type: "number",
          description: "左眼(OS)柱镜度数，单位D，负散光记法填负数，无散光可不填（默认0）。",
        },
      },
      required: ["right_sph", "left_sph"],
    },
    sample: { right_sph: -1, left_sph: -3.5, right_cyl: 0, left_cyl: -0.75 },
    handler: handleAnisometropiaGuide,
  },
  {
    name: "contact_lens_power",
    description:
      "框架镜度数换算隐形眼镜度数：按镜眼距（顶点距离，默认 12mm）做顶点补偿，把框架镜球镜（及可选柱镜）换算成贴近角膜的隐形眼镜等效光度，并按隐形常见的 0.25D 步进取整。说明为何高度数（约 ±4.00D 以上）必须补偿、低度数可直接沿用，散光可否折算等效球镜配普通球镜片，并提醒隐形还需基弧 / 直径 / 试戴等专业验配。仅供科普参考，不替代验光师验配。",
    inputSchema: {
      type: "object",
      properties: {
        sph: {
          type: "number",
          description: "框架镜球镜度数，单位D，近视填负数、远视填正数，如 -6.00。",
        },
        cyl: {
          type: "number",
          description: "框架镜柱镜度数，单位D，负散光记法填负数，无散光可不填（默认0）。",
        },
        vertex_distance_mm: {
          type: "number",
          description: "镜眼距（框架镜后顶点到角膜的距离），单位mm，常见 10-14，默认 12。",
        },
      },
      required: ["sph"],
    },
    sample: { sph: -6, cyl: -0.75, vertex_distance_mm: 12 },
    handler: handleContactLensPower,
  },
  {
    name: "reading_add_estimator",
    description:
      "老花（近附加 ADD）度数估算：随年龄增长调节力下降，40 岁后看近逐渐吃力。按年龄给出典型近附加（下加光）度数，并按实际工作距离（默认 40cm）做增减，可选传入看远球镜度数以算出「看近总度数 = 看远度数 + ADD」。说明老视机理、老花镜 / 渐进 / 办公镜片的选择，并按 0.25D 步进取整。仅供科普参考，最终度数以主觉验光和试戴为准。",
    inputSchema: {
      type: "object",
      properties: {
        age: {
          type: "number",
          description: "年龄（岁），整数。老视一般 40 岁后逐渐出现。",
        },
        working_distance_cm: {
          type: "number",
          description: "主要用眼（看近）距离，单位cm，常见看书 33-40、电脑 50-70，默认 40。",
        },
        distance_sph: {
          type: "number",
          description:
            "看远球镜度数，单位D，近视填负数、远视填正数（如 -2.00）；不填则只给近附加 ADD，不算看近总度数。",
        },
      },
      required: ["age"],
    },
    sample: { age: 50, working_distance_cm: 40, distance_sph: -2 },
    handler: handleReadingAddEstimator,
  },
];

const toolMap = new Map(tools.map((tool) => [tool.name, tool]));

export function listTools(): Array<Omit<ToolDefinition, "handler">> {
  return tools.map(({ handler: _handler, ...tool }) => tool);
}

export function executeTool(name: string, args: unknown): ToolResult {
  const tool = toolMap.get(name);
  let result: ToolResult;

  if (!tool) {
    result = errorResult(`未知工具：${name}`);
  } else {
    try {
      result = tool.handler(ensureObject(args ?? {}));
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      result = errorResult(message);
    }
  }

  recordCall(name, args, result);
  return result;
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

const shoppingPlatforms: Array<{ name: string; build: (keyword: string) => string }> = [
  { name: "京东", build: (kw) => `https://search.jd.com/Search?keyword=${encodeURIComponent(kw)}&enc=utf-8` },
  { name: "淘宝", build: (kw) => `https://s.taobao.com/search?q=${encodeURIComponent(kw)}` },
  { name: "拼多多", build: (kw) => `https://mobile.yangkeduo.com/search_result.html?search_key=${encodeURIComponent(kw)}` },
];

function handleShoppingLinks(args: ToolArgs): ToolResult {
  const raw = args["keywords"];
  if (!Array.isArray(raw)) {
    throw new Error("参数 keywords 必须是字符串数组");
  }
  const keywords = raw
    .map((item) => (typeof item === "string" ? item.trim() : ""))
    .filter((item) => item.length > 0);
  if (keywords.length === 0) {
    throw new Error("参数 keywords 至少要包含一个非空关键词");
  }
  if (keywords.length > 8) {
    throw new Error("参数 keywords 一次最多支持 8 个");
  }

  const sections = keywords.map((keyword) => {
    const links = shoppingPlatforms.map((p) => `[${p.name}](${p.build(keyword)})`).join(" · ");
    return `**${keyword}**\n- ${links}`;
  });

  const text = [
    "## 🛒 购买链接",
    "以下链接会跳转到各平台的实时搜索结果，可直接点击比价选购：",
    "",
    sections.join("\n\n"),
    "",
    "> 链接为搜索入口，价格与款式以平台实时为准；请务必对照验光单核对度数后再下单。",
  ].join("\n");

  return textResult(text);
}

const LENS_INDICES = ["1.56", "1.60", "1.67", "1.74"] as const;

/** Sagitta-based estimate of a lens' thickest point (edge for minus, center for plus). */
function estimateThickest(power: number, refractiveIndex: number, effectiveDiameter: number, isPlus: boolean): { sag: number; thickest: number } {
  const r = effectiveDiameter / 2;
  const sag = (power * r * r) / (2000 * (refractiveIndex - 1));
  const base = isPlus ? 1.0 : 1.2;
  return { sag, thickest: base + sag };
}

/** The most cost-effective index for a given power, matching lens_recommendation's thresholds. */
function recommendedIndex(power: number): (typeof LENS_INDICES)[number] {
  if (power <= 2) {
    return "1.56";
  }
  if (power <= 4) {
    return "1.60";
  }
  if (power <= 6) {
    return "1.67";
  }
  return "1.74";
}

function thicknessRating(thickest: number): string {
  if (thickest < 2) {
    return "很薄";
  }
  if (thickest < 3.5) {
    return "较薄";
  }
  if (thickest < 5) {
    return "中等";
  }
  if (thickest < 7) {
    return "偏厚";
  }
  return "很厚";
}

function handleLensThicknessEstimator(args: ToolArgs): ToolResult {
  const sph = expectNumber(args, "sph", { min: -20, max: 12 });
  const cyl = optionalNumber(args, "cyl", { min: -8, max: 8 }) ?? 0;
  const lensIndex = expectEnum(args, "lens_index", LENS_INDICES);
  const frameWidth = optionalNumber(args, "frame_width", { min: 40, max: 70 }) ?? 52;

  const n = Number(lensIndex);
  const power = Math.max(Math.abs(sph), Math.abs(sph + cyl));
  const isPlus = sph > 0;
  const effectiveDiameter = frameWidth + 4; // 4mm 偏心余量：实际有效直径通常大于镜圈标称宽度

  const { thickest } = estimateThickest(power, n, effectiveDiameter, isPlus);
  const rating = thicknessRating(thickest);
  const thickestLabel = isPlus ? "中心最厚" : "边缘最厚";
  const lensTypeLabel = isPlus ? "远视 / 正镜片（中心厚、边缘薄）" : "近视 / 负镜片（中心薄、边缘厚）";

  const weightNote =
    thickest >= 5
      ? "偏厚，长时间佩戴重量感会比较明显，建议配合小镜框和更高折射率一起控制。"
      : thickest >= 3.5
        ? "中等，多数人佩戴可接受。"
        : "较轻薄，重量通常不是主要问题。";

  const rec = recommendedIndex(power);
  const chosenRank = LENS_INDICES.indexOf(lensIndex);
  const recRank = LENS_INDICES.indexOf(rec);

  let indexAdvice: string;
  if (chosenRank < recRank) {
    const upgraded = estimateThickest(power, Number(rec), effectiveDiameter, isPlus);
    const delta = thickest - upgraded.thickest;
    indexAdvice = `建议提高到 ${rec}：${thickestLabel}处约从 ${thickest.toFixed(1)} 降到 ${upgraded.thickest.toFixed(1)} mm（减薄约 ${delta.toFixed(1)} mm）。`;
  } else if (chosenRank > recRank && power < 2) {
    indexAdvice = `度数不高，选到 ${lensIndex} 更多是减重/美观考量，性价比一般，1.56 / 1.60 通常已足够。`;
  } else {
    indexAdvice = `当前折射率 ${lensIndex} 与度数基本匹配，可优先在镜框尺寸上再做优化。`;
  }

  return textResult(`## 镜片厚度估算

> 基于薄透镜矢高（sagitta）近似：最厚处 ≈ 基础厚度 + 功率 × 半径² / (2000 × (n−1))。用于横向比较不同折射率与镜框，实际成品还取决于加工工艺、瞳距偏心与镜片设计。

**输入参数**
- 球镜：${formatSignedDiopter(sph)}
- 柱镜：${cyl === 0 ? "无明显散光" : formatSignedDiopter(cyl)}
- 参考功率（最大子午线）：${formatDiopter(power)}
- 折射率：${lensIndex}（n = ${n}）
- 镜圈宽度：${trimTrailingZeros(frameWidth.toFixed(1))} mm（估算有效直径 ${trimTrailingZeros(effectiveDiameter.toFixed(1))} mm）

**估算结果**
- 镜片类型：${lensTypeLabel}
- ${thickestLabel}：约 ${thickest.toFixed(1)} mm（${rating}）
- 重量倾向：${weightNote}

**折射率建议**
- ${indexAdvice}

**实用提醒**
- 边缘/中心厚度对镜框尺寸非常敏感：镜圈越小、越贴合脸型，成品越薄越轻。
- 高度数尽量选全框，避开无框和超大框，边缘更好收。
- 折射率越高越薄，但材料密度也更高，减重幅度通常小于减薄幅度，别只盯着折射率。`);
}

/** 镜片平面到眼球旋转中心的近似距离（mm），用于把远用瞳距折算为近用瞳距。 */
const PD_ROTATION_CENTER_MM = 27;

function pdRangeNote(pd: number): string {
  if (pd < 54) {
    return "低于成人常见范围（约 54-74mm），如果不是儿童或小脸型，请重新测量确认。";
  }
  if (pd > 74) {
    return "高于成人常见范围（约 54-74mm），请重新测量确认，避免加工时光心定位偏差。";
  }
  return "落在成人常见范围（约 54-74mm）内。";
}

function handlePupillaryDistanceGuide(args: ToolArgs): ToolResult {
  const binocular = optionalNumber(args, "binocular_pd", { min: 40, max: 85 });
  const pdRight = optionalNumber(args, "pd_right", { min: 18, max: 45 });
  const pdLeft = optionalNumber(args, "pd_left", { min: 18, max: 45 });
  const workingDistanceCm = optionalNumber(args, "working_distance_cm", { min: 20, max: 100 }) ?? 40;

  const hasMono = pdRight !== undefined || pdLeft !== undefined;
  if (hasMono && (pdRight === undefined || pdLeft === undefined)) {
    throw new Error("单眼瞳距需要左右眼一起提供（pd_left 与 pd_right）");
  }
  if (binocular === undefined && !hasMono) {
    throw new Error("请至少提供双眼瞳距 binocular_pd，或同时提供左右单眼瞳距 pd_left 和 pd_right");
  }

  const monoSum = pdRight !== undefined && pdLeft !== undefined ? pdRight + pdLeft : undefined;
  const totalPd = binocular ?? monoSum!;

  const warnings: string[] = [];
  if (binocular !== undefined && monoSum !== undefined && Math.abs(binocular - monoSum) > 1.5) {
    warnings.push(
      `双眼瞳距 ${trimTrailingZeros(binocular.toFixed(1))}mm 与左右单眼之和 ${trimTrailingZeros(monoSum.toFixed(1))}mm 相差 ${trimTrailingZeros(Math.abs(binocular - monoSum).toFixed(1))}mm，请核对测量数据。`
    );
  }
  if (pdRight !== undefined && pdLeft !== undefined && Math.abs(pdRight - pdLeft) >= 3) {
    warnings.push(
      `左右单眼瞳距相差约 ${trimTrailingZeros(Math.abs(pdRight - pdLeft).toFixed(1))}mm，属于明显不对称，务必按单眼瞳距分别定位光心，不能简单平分双眼瞳距。`
    );
  }

  const displayRight = pdRight ?? totalPd / 2;
  const displayLeft = pdLeft ?? totalPd / 2;
  const monoSource = pdRight !== undefined ? "实测" : "由双眼瞳距均分估算，仅供参考";

  const workingDistanceMm = workingDistanceCm * 10;
  const nearPd = (totalPd * workingDistanceMm) / (workingDistanceMm + PD_ROTATION_CENTER_MM);
  const nearReduction = totalPd - nearPd;

  return textResult(`## 瞳距（PD）助手

> 瞳距（PD）指两眼瞳孔中心的水平距离，是网购或加工配镜时把镜片光心对准眼睛的关键参数。远用看远、近用（阅读）时双眼会内聚，近用瞳距会比远用略小。

**双眼瞳距（远用）**
- ${trimTrailingZeros(totalPd.toFixed(1))} mm${binocular === undefined ? "（由左右单眼相加得到）" : ""}
- ${pdRangeNote(totalPd)}

**单眼瞳距（左右）**
- 右眼 OD：约 ${trimTrailingZeros(displayRight.toFixed(1))} mm
- 左眼 OS：约 ${trimTrailingZeros(displayLeft.toFixed(1))} mm
- 来源：${monoSource}

**近用瞳距（工作距离 ${trimTrailingZeros(workingDistanceCm.toFixed(1))} cm）**
- 约 ${trimTrailingZeros(nearPd.toFixed(1))} mm（比远用约小 ${trimTrailingZeros(nearReduction.toFixed(1))} mm）
- 配单独的阅读镜或看渐进/办公镜的近用区时才需要用到，普通远用镜按远用瞳距即可。

**提醒**
${renderBulletList(warnings, "数值看起来正常，仍建议以视光师现场用瞳距仪测量为准。")}

**自测方法（应急，精度有限）**
- 对着镜子，把直尺贴在眉骨上，平视前方。
- 闭右眼，用左眼把尺子的 0 刻度对准右眼瞳孔中心。
- 保持尺子不动，闭左眼，用右眼读出左眼瞳孔中心对应的刻度，即为双眼瞳距。
- 建议重复 2-3 次取平均；高度数、渐进片和儿童配镜请以专业测量为准。`);
}

type CoatingTier = "must" | "optional" | "skip";
interface CoatingRec {
  name: string;
  tier: CoatingTier;
  verdict: string;
  reason: string;
}

const OUTDOOR_LABELS: Record<string, string> = {
  rare: "很少",
  sometimes: "有时",
  often: "经常",
};
const NIGHT_DRIVING_LABELS: Record<string, string> = {
  none: "基本不",
  occasional: "偶尔",
  frequent: "经常",
};

function handleLensCoatingAdvisor(args: ToolArgs): ToolResult {
  const screenHours = expectNumber(args, "screen_hours", { min: 0, max: 18 });
  const outdoor = expectEnum(args, "outdoor_frequency", ["rare", "sometimes", "often"]);
  const nightDriving = optionalEnum(args, "night_driving", ["none", "occasional", "frequent"]) ?? "none";
  const lightSensitive = optionalBoolean(args, "light_sensitive") ?? false;
  const preferOnePair = optionalBoolean(args, "prefer_one_pair") ?? false;

  const recs: CoatingRec[] = [];

  // 1. 基础膜层：几乎是现代树脂片的默认配置
  recs.push({
    name: "基础膜层（加硬耐磨 + 减反射绿膜 + 疏水防污）",
    tier: "must",
    verdict: "标配（默认就选）",
    reason:
      nightDriving !== "none"
        ? "减少镜片内外反光、提升通透度和夜间抗眩光，同时更耐刮、更好清洁；你经常夜间开车，减反射膜尤其能压低对向车灯的鬼影和光晕。"
        : "减少镜片内外反光、提升通透度，同时更耐刮、更好清洁，是现代树脂镜片的基础配置，几乎不用犹豫。",
  });

  // 2. UV 防护
  if (outdoor === "often") {
    recs.push({
      name: "UV 防护（UV400）",
      tier: "must",
      verdict: "强烈推荐",
      reason: "长期日晒会增加白内障、翼状胬肉等风险，经常在户外一定要确认镜片达到 UV400；太阳镜/变色片更要如此。",
    });
  } else if (outdoor === "sometimes") {
    recs.push({
      name: "UV 防护（UV400）",
      tier: "must",
      verdict: "推荐",
      reason: "紫外线防护属于低成本高收益，多数中高端树脂片本身就带 UV400，下单前确认参数即可。",
    });
  } else {
    recs.push({
      name: "UV 防护（UV400）",
      tier: "must",
      verdict: "标配（通常无需额外加价）",
      reason: "大部分树脂镜片出厂就阻隔 UV400，确认参数标注即可，不必为「UV 防护」单独加钱。",
    });
  }

  // 3. 防蓝光
  if (screenHours >= 8) {
    recs.push({
      name: "防蓝光膜",
      tier: "optional",
      verdict: "可选（重度屏幕使用者可考虑）",
      reason: "长时间看屏幕的人，防蓝光主要作用是主观上的对比度和舒适度，尤其晚上；但它并不能替代「调低亮度/色温 + 定时休息」，护眼作用别被夸大。",
    });
  } else if (screenHours >= 4) {
    recs.push({
      name: "防蓝光膜",
      tier: "optional",
      verdict: "可选",
      reason: "屏幕时间中等，防蓝光更多是心理和轻微舒适度收益，可按预算决定，不是刚需。",
    });
  } else {
    recs.push({
      name: "防蓝光膜",
      tier: "skip",
      verdict: "一般不需要",
      reason: "屏幕使用不多时，防蓝光膜的实际收益很小，还可能让镜片带轻微底色，通常不值得额外花钱。",
    });
  }

  // 4. 变色片（光致变色）
  if (preferOnePair && outdoor !== "rare") {
    recs.push({
      name: "变色片（光致变色）",
      tier: "optional",
      verdict: "推荐",
      reason: "你希望一副镜片室内外通用，又经常见光，变色片能省去换太阳镜的麻烦；注意它变色/褪色都需要时间，且在车内因挡风玻璃阻隔紫外线通常变色不明显。",
    });
  } else if (outdoor === "often" && lightSensitive) {
    recs.push({
      name: "变色片（光致变色）",
      tier: "optional",
      verdict: "推荐",
      reason: "你经常在户外又比较畏光，变色片能随光线自动调节；但室内会残留淡淡底色，介意的话可改配单独太阳镜。",
    });
  } else if (outdoor === "rare" && !preferOnePair) {
    recs.push({
      name: "变色片（光致变色）",
      tier: "skip",
      verdict: "一般不需要",
      reason: "户外时间少、也不追求一副通用，变色片的溢价通常用不上。",
    });
  } else {
    recs.push({
      name: "变色片（光致变色）",
      tier: "optional",
      verdict: "可选",
      reason: "介于需要与不需要之间：想省一副太阳镜可以上，但要接受变色有延迟、车内变色弱、室内有淡底色这几点。",
    });
  }

  // 5. 偏振太阳镜（单独一副）
  if (outdoor === "often") {
    recs.push({
      name: "偏振太阳镜（建议单独配一副）",
      tier: "must",
      verdict: "推荐",
      reason: "经常户外或白天开车，偏振能有效削掉水面、路面、雪地的反射眩光；注意偏振只适合白天，夜间开车不要戴，且可能影响部分液晶仪表/手机屏显示。",
    });
  } else if (outdoor === "sometimes" && lightSensitive) {
    recs.push({
      name: "偏振太阳镜（建议单独配一副）",
      tier: "optional",
      verdict: "可选",
      reason: "你有时户外且比较畏光，一副偏振太阳镜会很舒服；日常近视镜没必要做成偏振，分开配更灵活。",
    });
  } else {
    recs.push({
      name: "偏振太阳镜（建议单独配一副）",
      tier: "skip",
      verdict: "一般不需要",
      reason: "户外强光暴露不多时，普通防 UV 已足够，偏振太阳镜可等有需要时再单独配。",
    });
  }

  const cautions: string[] = [];
  if (nightDriving === "frequent") {
    cautions.push("夜间驾驶最关键的是干净的减反射膜；市面上的黄色「夜视 / 防远光」镜片会降低进光量，多数情况下并不推荐。");
  }
  if (screenHours >= 8) {
    cautions.push("长时间用屏幕，比防蓝光更有效的是调低屏幕亮度和色温、遵循 20-20-20 用眼法则（每 20 分钟看 20 英尺外 20 秒）并保证休息。");
  }
  if (lightSensitive && outdoor === "rare") {
    cautions.push("你畏光但户外不多，如果不适感明显，建议先做一次眼科检查排查干眼或其它眼表问题，而不是只靠镀膜解决。");
  }
  cautions.push("镀膜和功能再全也替代不了准确验光和合适镜框：先把度数、瞳距、镜框选对，再谈镀膜取舍。");

  const renderRec = (r: CoatingRec) => `- **${r.name}**：${r.verdict}\n  - ${r.reason}`;
  const namesByTier = (tier: CoatingTier) =>
    recs.filter((r) => r.tier === tier).map((r) => r.name.replace(/（.*）$/, "").trim());

  const mustList = namesByTier("must");
  const optionalList = namesByTier("optional");
  const skipList = namesByTier("skip");

  return textResult(`## 镜片镀膜与功能顾问

> 镀膜和功能是配镜里最容易被过度推销的部分。下面按你的用眼场景，逐项给出「值不值得为它多花钱」的判断，而不是一律都上。

**你的用眼画像**
- 日均屏幕时长：${trimTrailingZeros(screenHours.toFixed(1))} 小时
- 户外 / 日晒频率：${OUTDOOR_LABELS[outdoor]}
- 夜间驾驶：${NIGHT_DRIVING_LABELS[nightDriving]}
- 是否对强光 / 眩光敏感：${lightSensitive ? "是" : "否"}
- 是否希望一副镜片室内外通用：${preferOnePair ? "是" : "否"}

**逐项建议**
${recs.map(renderRec).join("\n")}

**建议为它们付费**
${renderBulletList(mustList, "暂无必须额外付费的项目。")}

**可以再考虑（按预算决定）**
${renderBulletList(optionalList, "暂无需要再权衡的可选项目。")}

**通常不必额外花钱**
${renderBulletList(skipList, "以上场景下没有明显不值得的项目。")}

**提醒**
${cautions.map((item) => `- ${item}`).join("\n")}`);
}

type MyopiaTier = "priority" | "consider" | "notyet";
interface MyopiaOption {
  name: string;
  tier: MyopiaTier;
  verdict: string;
  reason: string;
}

const PARENT_MYOPIA_LABELS: Record<string, string> = {
  none: "都不近视",
  one: "一方近视",
  both: "双方近视",
};

/** 按当前球镜度数给出近视程度描述（负数越小度数越高）。 */
function myopiaDegreeLabel(sph: number): string {
  if (sph > -0.5) {
    return "尚未达到近视标准（近视前期 / 远视储备阶段）";
  }
  if (sph > -3) {
    return "低度近视";
  }
  if (sph > -6) {
    return "中度近视";
  }
  return "高度近视（≤ -6.00D，需重视眼底随访）";
}

function handleMyopiaControlGuide(args: ToolArgs): ToolResult {
  const age = expectNumber(args, "age", { min: 3, max: 18, integer: true });
  const currentSph = expectNumber(args, "current_sph", { min: -15, max: 2 });
  const annualProgression = optionalNumber(args, "annual_progression", { min: 0, max: 3 });
  const parentMyopia = optionalEnum(args, "parent_myopia", ["none", "one", "both"]);
  const outdoorHours = optionalNumber(args, "outdoor_hours", { min: 0, max: 8 });

  const isMyopic = currentSph <= -0.5;
  const progText =
    annualProgression !== undefined
      ? `${trimTrailingZeros(annualProgression.toFixed(2))}D/年（近视加深）`
      : "未提供";
  const fastProgress = annualProgression !== undefined && annualProgression >= 0.5;

  // ---- 进展风险评分 ----
  let score = 0;
  const factors: string[] = [];
  if (isMyopic && age < 9) {
    score += 2;
    factors.push("发病年龄小（9 岁前已近视），进展风险显著偏高。");
  } else if (isMyopic && age < 12) {
    score += 1;
    factors.push("发病年龄偏小（12 岁前近视），仍需重点防控。");
  }
  if (annualProgression !== undefined) {
    if (annualProgression >= 1.0) {
      score += 2;
      factors.push(`近一年加深约 ${trimTrailingZeros(annualProgression.toFixed(2))}D，进展很快。`);
    } else if (annualProgression >= 0.5) {
      score += 1;
      factors.push(`近一年加深约 ${trimTrailingZeros(annualProgression.toFixed(2))}D，进展偏快。`);
    } else {
      factors.push(`近一年加深约 ${trimTrailingZeros(annualProgression.toFixed(2))}D，进展相对平稳。`);
    }
  }
  if (currentSph <= -6) {
    score += 2;
    factors.push("已属高度近视，眼底并发症风险更高。");
  } else if (currentSph <= -3) {
    score += 1;
    factors.push("已属中度近视。");
  }
  if (parentMyopia === "both") {
    score += 2;
    factors.push("父母双方均近视，遗传易感性较高。");
  } else if (parentMyopia === "one") {
    score += 1;
    factors.push("父母一方近视，有一定遗传倾向。");
  }
  if (outdoorHours !== undefined && outdoorHours < 1) {
    score += 1;
    factors.push("日均户外不足 1 小时，缺少最重要的保护因素。");
  } else if (outdoorHours !== undefined && outdoorHours >= 2) {
    score -= 1;
    factors.push("日均户外 2 小时以上，是重要的保护因素。");
  }
  if (score < 0) {
    score = 0;
  }
  const riskLevel = score >= 4 ? "高" : score >= 2 ? "中" : "低";

  // ---- 逐项方案 ----
  const options: MyopiaOption[] = [];

  // 1. 户外活动（基础，永远优先）
  options.push({
    name: "每天累计 2 小时以上户外活动",
    tier: "priority",
    verdict: outdoorHours !== undefined && outdoorHours < 2 ? "最该优先补上" : "保持",
    reason:
      outdoorHours !== undefined
        ? outdoorHours < 2
          ? `目前日均户外约 ${trimTrailingZeros(outdoorHours.toFixed(1))} 小时，证据最充分且几乎零成本，建议累计到每天 2 小时以上（自然光是关键，阴天也有效）。`
          : `目前日均户外约 ${trimTrailingZeros(outdoorHours.toFixed(1))} 小时，已达标，请继续保持——这是防控近视最基础也最有效的一环。`
        : "证据最充分且几乎零成本：保证每天累计 2 小时以上的户外活动，自然光是关键，阴天也有效。",
  });

  // 2. 科学用眼习惯（基础，永远优先）
  options.push({
    name: "科学用眼习惯",
    tier: "priority",
    verdict: "长期坚持",
    reason:
      "保持读写距离 33cm 以上、坐姿端正；遵循 20-20-20（每近距离用眼 20 分钟，看 20 英尺≈6 米外 20 秒）；保证读写照明充足、少用手机等小屏幕、保证充足睡眠。",
  });

  // 3. 近视离焦框架镜片
  if (isMyopic) {
    options.push({
      name: "近视离焦框架镜片（多区正向离焦等设计）",
      tier: "priority",
      verdict: "日常配镜首选",
      reason:
        "非侵入、佩戴门槛低，适合大多数需要戴镜的近视儿童；相比普通单光镜片有助于延缓进展，是日常眼镜的优先选择。",
    });
  } else {
    options.push({
      name: "近视离焦框架镜片（多区正向离焦等设计）",
      tier: "notyet",
      verdict: "暂不需要",
      reason:
        "目前尚未达到近视标准，普通监测即可；一旦确诊近视需要配镜，可优先选近视离焦设计而非普通单光片。",
    });
  }

  // 4. 角膜塑形镜（OK 镜，夜戴）
  if (age < 8) {
    options.push({
      name: "角膜塑形镜（OK 镜 / 夜戴）",
      tier: "notyet",
      verdict: "年龄偏小，暂不考虑",
      reason:
        "OK 镜通常建议 8 岁以上、能自己配合摘戴与护理的孩子，目前年龄偏小，先从户外和离焦框架镜入手。",
    });
  } else if (!isMyopic) {
    options.push({
      name: "角膜塑形镜（OK 镜 / 夜戴）",
      tier: "notyet",
      verdict: "尚不需要",
      reason: "尚未确诊近视或度数很低，暂不需要 OK 镜，先做好户外与用眼习惯、定期复查。",
    });
  } else if (currentSph < -6) {
    options.push({
      name: "角膜塑形镜（OK 镜 / 夜戴）",
      tier: "consider",
      verdict: "需专业评估（度数偏高）",
      reason:
        "度数偏高、超出 OK 镜常规适配范围（约 -1.00 ~ -6.00D），能否验配需由专业机构评估角膜曲率、厚度等条件后决定。",
    });
  } else {
    options.push({
      name: "角膜塑形镜（OK 镜 / 夜戴）",
      tier: "consider",
      verdict: fastProgress ? "很值得评估" : "可以考虑评估",
      reason:
        "夜间佩戴、白天可获得清晰裸眼视力，对延缓眼轴增长有较好证据；需到正规医疗机构验配，严格护理卫生并定期复查，谨防角膜感染。",
    });
  }

  // 5. 低浓度阿托品
  if (!isMyopic) {
    options.push({
      name: "低浓度阿托品滴眼液（如 0.01%）",
      tier: "notyet",
      verdict: "一般暂不用药",
      reason: "尚未近视时通常不用药，先把户外与用眼习惯做到位；是否需要请由眼科医生评估。",
    });
  } else {
    options.push({
      name: "低浓度阿托品滴眼液（如 0.01%）",
      tier: "consider",
      verdict: riskLevel === "高" || fastProgress ? "建议就诊咨询" : "可咨询医生",
      reason:
        riskLevel === "高" || fastProgress
          ? "进展较快，可就诊时咨询低浓度阿托品，需眼科医生评估并处方、定期随访；不建议自行购买使用。"
          : "低浓度阿托品是控制进展的选项之一，是否使用请由眼科医生评估，切勿自行购买或网购使用。",
    });
  }

  // ---- 提醒 ----
  const cautions: string[] = [];
  cautions.push(
    "角膜塑形镜（OK 镜）和低浓度阿托品都属于医疗行为，必须在正规眼科 / 视光机构验配、开具并定期随访，切勿自行购买或网购使用。"
  );
  cautions.push(
    "判断真性还是假性近视、评估是否用药，需要散瞳验光；建议每 3-6 个月复查一次，有条件时监测眼轴长度。"
  );
  if (!isMyopic) {
    cautions.push(
      "目前重点是「保住远视储备、别过早近视」——多户外、控制近距离用眼比急着配镜更重要。"
    );
  }
  if (currentSph <= -6 || riskLevel === "高") {
    cautions.push("高度近视要把眼底检查列为常规项目，警惕视网膜变性、裂孔等并发症。");
  }
  cautions.push("本工具只做科普参考，不替代医生诊断，具体方案请遵专业机构意见。");

  const renderOption = (o: MyopiaOption) => `- **${o.name}**：${o.verdict}\n  - ${o.reason}`;
  const namesByTier = (tier: MyopiaTier) => options.filter((o) => o.tier === tier).map((o) => o.name);

  return textResult(`## 青少年近视防控指南

> 近视防控的核心是「延缓进展、控制眼轴增长」，越早干预越好，且防控措施可以叠加使用。以下结合孩子的年龄、度数、进展速度、遗传与户外情况给出评估与方案，仅供科普参考。

**孩子情况**
- 年龄：${age} 岁
- 当前度数（球镜）：${formatSignedDiopter(currentSph)} —— ${myopiaDegreeLabel(currentSph)}
- 近一年加深：${progText}
- 父母近视：${parentMyopia !== undefined ? PARENT_MYOPIA_LABELS[parentMyopia] : "未提供"}
- 日均户外：${outdoorHours !== undefined ? `${trimTrailingZeros(outdoorHours.toFixed(1))} 小时` : "未提供"}

**进展风险评估**
- 综合判断：${riskLevel}风险
${renderBulletList(factors, "暂未发现突出的高危因素，继续保持良好用眼习惯和定期复查即可。")}

**逐项方案**
${options.map(renderOption).join("\n")}

**现在最该做的（优先）**
${renderBulletList(namesByTier("priority"), "暂无优先项。")}

**可结合专业机构评估**
${renderBulletList(namesByTier("consider"), "暂无需要评估的医疗方案。")}

**暂不需要 / 条件未到**
${renderBulletList(namesByTier("notyet"), "暂无。")}

**提醒**
${cautions.map((item) => `- ${item}`).join("\n")}`);
}

/** 框架镜下，每 1.00D 屈光参差引起的两眼视网膜影像大小差异（不等像）近似百分比。 */
const ANISEIKONIA_PERCENT_PER_DIOPTER = 1.5;
/** 一般人群对两眼影像大小差异（不等像）的大致耐受上限（百分比）。 */
const ANISEIKONIA_TOLERANCE_PERCENT = 5;

function anisometropiaLevel(seDiff: number): string {
  if (seDiff < 1) {
    return "无明显参差";
  }
  if (seDiff < 2) {
    return "轻度屈光参差";
  }
  if (seDiff < 3) {
    return "中度屈光参差";
  }
  return "显著屈光参差";
}

/** 隐形眼镜默认镜眼距（顶点距离，mm）。 */
const CONTACT_LENS_DEFAULT_VERTEX_MM = 12;
/** 超过该光度（|D|），框架镜与隐形眼镜的度数差异达到临床需要补偿的量级（约 0.25D 起）。 */
const CONTACT_LENS_SIGNIFICANT_D = 4;
/** 隐形眼镜常见的光度步进（D）。 */
const CONTACT_LENS_STEP_D = 0.25;

/** 顶点距离补偿：把框架镜某子午线光度换算为贴近角膜（隐形眼镜）的等效光度。d 为米。 */
function vertexCompensate(power: number, distanceMeters: number): number {
  return power / (1 - distanceMeters * power);
}

/** 按隐形眼镜常见步进（0.25D）取整。 */
function roundToContactStep(value: number): number {
  return Math.round(value / CONTACT_LENS_STEP_D) * CONTACT_LENS_STEP_D;
}

function handleContactLensPower(args: ToolArgs): ToolResult {
  const sph = expectNumber(args, "sph", { min: -30, max: 30 });
  const cyl = optionalNumber(args, "cyl", { min: -10, max: 10 }) ?? 0;
  const vertexMm =
    optionalNumber(args, "vertex_distance_mm", { min: 5, max: 20 }) ??
    CONTACT_LENS_DEFAULT_VERTEX_MM;
  const d = vertexMm / 1000;

  // 分别换算球镜子午线与「球镜+柱镜」子午线，再按步进取整重组球柱镜。
  const sphExact = vertexCompensate(sph, d);
  const cylMeridianExact = vertexCompensate(sph + cyl, d);
  const contactSph = roundToContactStep(sphExact);
  const contactCylMeridian = roundToContactStep(cylMeridianExact);
  const contactCyl = cyl === 0 ? 0 : contactCylMeridian - contactSph;

  // 最强子午线光度，用于判断是否需要补偿。
  const strongestPower = Math.max(Math.abs(sph), Math.abs(sph + cyl));
  const significant = strongestPower >= CONTACT_LENS_SIGNIFICANT_D;
  // 补偿量（球镜子午线上框架镜与隐形眼镜精确度数之差的绝对值）。
  const sphShift = Math.abs(sphExact - sph);

  // 等效球镜折算（低散光把隐形按球镜片配的常见做法）。
  const contactSE = roundToContactStep(vertexCompensate(sph + cyl / 2, d));

  const contactLine = renderPrescriptionLine(contactSph, contactCyl);

  const notes: string[] = [];
  if (significant) {
    notes.push(
      `框架镜最强子午线约 ${formatDiopter(strongestPower)}，已达到需要顶点补偿的量级：${
        sph < 0 ? "近视换算成隐形后度数会变浅" : "远视换算成隐形后度数会变深"
      }，补偿量约 ${formatDiopter(sphShift)}（球镜子午线）。直接照搬框架镜度数会${
        sph < 0 ? "过矫" : "欠矫"
      }。`
    );
  } else {
    notes.push(
      `框架镜最强子午线约 ${formatDiopter(strongestPower)}，未超过约 ${formatDiopter(
        CONTACT_LENS_SIGNIFICANT_D
      )}，顶点补偿量很小（不足半档），隐形眼镜通常可直接按框架镜度数选配。`
    );
  }
  if (cyl !== 0) {
    notes.push(
      `含散光：换算后柱镜约 ${formatSignedDiopter(
        contactCyl
      )}。散光隐形（Toric / 散光片）度数、轴位步进有限，验配更复杂；低散光（约 ≤0.75D）常折算成等效球镜配普通球镜片——等效球镜隐形约为 ${formatSignedDiopter(
        contactSE
      )}。`
    );
  }

  return textResult(`## 隐形眼镜度数换算（顶点距离补偿）

> 框架镜离眼约 ${trimTrailingZeros(
    vertexMm.toFixed(0)
  )}mm，隐形眼镜贴在角膜上，同一屈光需求所需的镜片光度并不相同。度数越高，差异越大。换算公式：F_隐形 = F_框架 ÷ (1 − d × F_框架)，d 为镜眼距（米）。以下为科普估算，实际以专业验配为准。

**输入（框架镜）**
- 处方：${renderPrescriptionLine(sph, cyl)}（${describeEye(sph, cyl)}）
- 镜眼距：${trimTrailingZeros(vertexMm.toFixed(0))} mm

**换算结果（隐形眼镜，按 ${trimTrailingZeros(
    CONTACT_LENS_STEP_D.toFixed(2)
  )}D 步进取整）**
- 隐形眼镜光度：**${contactLine}**${
    cyl !== 0 ? `\n- 折算等效球镜（低散光配球镜片时）：约 ${formatSignedDiopter(contactSE)}` : ""
  }

**说明**
${renderBulletList(notes, "换算完成。")}

**提醒**
- 隐形眼镜验配除光度外还需确定基弧（BC）、直径（DIA）、品牌和现场试戴，本工具只做光度换算。
- 首次配戴或更换品牌请到专业机构验配，并规范护理、控制配戴时长，出现红痛畏光要及时停戴就医。
- 本工具只做科普参考，不替代验光师 / 医生。`);
}

function handleAnisometropiaGuide(args: ToolArgs): ToolResult {
  const rightSph = expectNumber(args, "right_sph", { min: -30, max: 30 });
  const leftSph = expectNumber(args, "left_sph", { min: -30, max: 30 });
  const rightCyl = optionalNumber(args, "right_cyl", { min: -10, max: 10 }) ?? 0;
  const leftCyl = optionalNumber(args, "left_cyl", { min: -10, max: 10 }) ?? 0;

  const rightSE = rightSph + rightCyl / 2;
  const leftSE = leftSph + leftCyl / 2;
  const seDiff = Math.abs(rightSE - leftSE);
  const sphDiff = Math.abs(rightSph - leftSph);
  const cylDiff = Math.abs(rightCyl - leftCyl);

  const level = anisometropiaLevel(seDiff);
  const imageDiffPercent = seDiff * ANISEIKONIA_PERCENT_PER_DIOPTER;

  // 一眼偏近视、一眼偏远视（混合性屈光参差 / antimetropia）
  const antimetropia =
    (rightSE <= -0.5 && leftSE >= 0.5) || (rightSE >= 0.5 && leftSE <= -0.5);

  let advice: string;
  switch (level) {
    case "无明显参差":
      advice = "两眼度数接近，屈光参差不明显，按验光度数配框架镜通常没有额外适应问题。";
      break;
    case "轻度屈光参差":
      advice =
        "轻度参差，绝大多数人配框架镜可以正常适应，初期偶有轻微不适；建议左右镜片选同一系列 / 折射率，一次配齐、连续佩戴适应。";
      break;
    case "中度屈光参差":
      advice =
        "中度参差，多数人可逐步适应框架镜，但初期可能出现头晕、走路踩空感、立体感变化；建议连续佩戴适应，对影像不等敏感者可考虑隐形眼镜（镜眼距贴近角膜，两眼放大差异更小）。";
      break;
    default:
      advice =
        "显著参差，框架镜下两眼影像不等明显，容易头晕、融像困难，建议优先考虑隐形眼镜，或到专业机构做双眼视功能与屈光手术评估。";
  }

  const notes: string[] = [];
  if (antimetropia) {
    notes.push(
      "属于「混合性屈光参差」（一眼偏近视、一眼偏远视），两眼调节需求方向相反，框架镜适应通常更难，更建议专业验配与现场试戴。"
    );
  }
  if (cylDiff >= 1.5) {
    notes.push(
      `两眼柱镜相差约 ${formatDiopter(cylDiff)}，除放大差异外还可能带来子午线方向上的影像倾斜 / 畸变，配镜后务必现场试戴确认清晰度与舒适度。`
    );
  }
  if (imageDiffPercent > ANISEIKONIA_TOLERANCE_PERCENT) {
    notes.push(
      `估算两眼影像大小差异约 ${trimTrailingZeros(imageDiffPercent.toFixed(1))}%，已超过一般耐受上限（约 ${ANISEIKONIA_TOLERANCE_PERCENT}%），框架镜眩晕 / 融像困难的风险较高。`
    );
  }
  if (seDiff >= 2) {
    notes.push(
      "儿童若存在中度以上屈光参差，是弱视和双眼视异常的高危因素，须尽早到眼科 / 视光机构检查，不要仅凭本工具判断。"
    );
  }

  return textResult(`## 屈光参差评估

> 屈光参差指两眼屈光度数不一致。以「等效球镜（SE = 球镜 + 柱镜÷2）」之差衡量：差异越大，框架镜下两眼视网膜影像大小差异（不等像 aniseikonia）越明显，越容易头晕、融像困难。以下为科普估算，实际以专业验光和试戴为准。

**双眼度数**
- 右眼 OD：${renderPrescriptionLine(rightSph, rightCyl)}（等效球镜 ${formatSignedDiopter(rightSE)}）
- 左眼 OS：${renderPrescriptionLine(leftSph, leftCyl)}（等效球镜 ${formatSignedDiopter(leftSE)}）

**参差分析**
- 等效球镜差：${formatDiopter(seDiff)} → **${level}**
- 球镜差：${formatDiopter(sphDiff)}；柱镜差：${formatDiopter(cylDiff)}
- 框架镜下估算影像大小差异：约 ${trimTrailingZeros(imageDiffPercent.toFixed(1))}%（一般耐受上限约 ${ANISEIKONIA_TOLERANCE_PERCENT}%）

**建议**
- ${advice}

**特别提示**
${renderBulletList(notes, "未发现额外的特殊风险；仍建议以专业验光和现场试戴结果为准。")}

**提醒**
- 影像差异百分比为经验估算（约每 1.00D 参差对应 ${trimTrailingZeros(ANISEIKONIA_PERCENT_PER_DIOPTER.toFixed(1))}% 放大差异），实际还取决于镜片基弯、镜眼距和验配方式。
- 隐形眼镜贴近角膜、镜眼距更小，通常能显著减小两眼放大差异，是较大屈光参差的常见方案，但需专业验配。
- 本工具只做科普参考，不替代医生诊断。`);
}

/** 近附加（ADD）常见的度数步进（D）。 */
const READING_ADD_STEP_D = 0.25;
/** 近附加的参考工作距离（cm）：年龄经验表按此距离标定。 */
const READING_ADD_REFERENCE_CM = 40;
/** 近附加的常见上限（D），超过后单副老花镜的清晰景深过窄，一般不再加大。 */
const READING_ADD_MAX_D = 3.5;
/** 老视一般开始出现的年龄（岁）。 */
const PRESBYOPIA_ONSET_AGE = 40;

/** 按 0.25D 步进取整。 */
function roundToReadingStep(value: number): number {
  return Math.round(value / READING_ADD_STEP_D) * READING_ADD_STEP_D;
}

/** 按年龄给出 40cm 参考距离下的典型近附加（下加光）度数（D）。 */
function ageBaseAdd(age: number): number {
  if (age < PRESBYOPIA_ONSET_AGE) return 0;
  if (age <= 41) return 1.0;
  if (age <= 44) return 1.25;
  if (age <= 47) return 1.5;
  if (age <= 49) return 1.75;
  if (age <= 52) return 2.0;
  if (age <= 55) return 2.25;
  return 2.5;
}

function handleReadingAddEstimator(args: ToolArgs): ToolResult {
  const age = expectNumber(args, "age", { min: 1, max: 120, integer: true });
  const workingCm =
    optionalNumber(args, "working_distance_cm", { min: 20, max: 200 }) ??
    READING_ADD_REFERENCE_CM;
  const distanceSph = optionalNumber(args, "distance_sph", { min: -30, max: 30 });

  const baseAdd = ageBaseAdd(age);
  // 工作距离修正：相对 40cm 参考距离的调节需求差（1/距离，单位 D）。
  // 越近需求越大 → 需要更多下加光；越远则更少。
  const distanceAdjust =
    baseAdd > 0 ? 100 / workingCm - 100 / READING_ADD_REFERENCE_CM : 0;
  const rawAdd = Math.min(Math.max(baseAdd + distanceAdjust, 0), READING_ADD_MAX_D);
  const add = roundToReadingStep(rawAdd);

  const notes: string[] = [];

  if (age < PRESBYOPIA_ONSET_AGE) {
    notes.push(
      `${age} 岁一般还有充足的调节力，通常不需要近附加。若此年龄已明显看近吃力，多与远视、调节功能异常或用眼疲劳有关，建议先做主觉验光和调节功能检查，而非直接配老花镜。`
    );
  } else {
    notes.push(
      `近附加度数由验光师用「先给暂定下加光、再用交叉圆柱镜 / 红绿视标微调」确定，本工具只按年龄和距离给经验估算，真实值可能相差约 ±0.25~0.50D。`
    );
    notes.push(
      "配镜原则是「够用即可、留有余量」——在能看清目标距离的前提下选偏低的下加光，可保留更宽的清晰景深，不要盲目追高。左右眼近附加通常相同。"
    );
  }

  if (workingCm <= 33 && baseAdd > 0) {
    notes.push(
      `工作距离约 ${workingCm.toFixed(0)}cm，比参考的 40cm 更近，已相应加大下加光；长时间超近距离用眼更累，注意间歇休息。`
    );
  } else if (workingCm >= 60 && baseAdd > 0) {
    notes.push(
      `工作距离约 ${workingCm.toFixed(0)}cm 偏远（如台式电脑 / 乐谱），下加光相应减小；若既要看电脑又要看更近的纸面，单一下加光难以兼顾，可考虑渐进或办公（中近）镜片。`
    );
  }

  if (add >= READING_ADD_MAX_D) {
    notes.push(
      `估算下加光已达上限约 ${formatDiopter(
        READING_ADD_MAX_D
      )}；单副老花镜再加大清晰范围会更窄，若一副难以兼顾各距离，建议渐进多焦点或分距离配镜。`
    );
  }

  // 看近总度数（每眼）：看远球镜 + 近附加。
  let nearTotalLine = "";
  if (distanceSph !== undefined) {
    const nearTotal = distanceSph + add;
    nearTotalLine = `\n- 看近总度数（每眼球镜）：看远 ${formatSignedDiopter(
      distanceSph
    )} + 下加光 ${formatSignedDiopter(add)} = **${formatSignedDiopter(nearTotal)}**`;
    if (distanceSph < 0 && distanceSph + add < 0) {
      notes.push(
        "你看远是近视，加上下加光后看近仍是负度数：不少中低度近视者看近时摘掉眼镜或戴度数更浅的眼镜即可，是否需要单独的老花镜要结合裸眼近视力判断。"
      );
    }
    notes.push(
      "已有看远度数（近视 / 远视 / 散光）者，多数会选择渐进多焦点或双光镜片，把看远与看近合到一副，避免频繁换镜。"
    );
  } else {
    notes.push(
      "如需算「看近总度数」，请一并提供看远球镜度数（distance_sph）；只有老花、看远正常者，看近总度数就等于下加光本身。"
    );
  }

  const addDisplay = add > 0 ? `**约 +${trimTrailingZeros(add.toFixed(2))}D**` : "**+0.00D（暂不需要）**";

  return textResult(`## 老花（近附加 ADD）度数估算

> 随年龄增长，晶状体调节力逐渐下降，约 40 岁起看近费力、易疲劳，这就是老视（老花）。矫正靠在看远度数上叠加一份「近附加 / 下加光（ADD）」。下加光随年龄增大、约 60 岁后趋于稳定（一般不超过约 ${formatDiopter(
    READING_ADD_MAX_D
  )}），并与实际用眼距离有关。以下为科普估算，实际以主觉验光和试戴为准。

**输入**
- 年龄：${age} 岁
- 主要用眼距离：${workingCm.toFixed(0)} cm（参考距离 ${READING_ADD_REFERENCE_CM} cm）${
    distanceSph !== undefined ? `\n- 看远球镜：${formatSignedDiopter(distanceSph)}` : ""
  }

**估算结果**
- 建议近附加（下加光 ADD）：${addDisplay}${nearTotalLine}

**说明**
${renderBulletList(notes, "估算完成。")}

**提醒**
- 下加光是双眼看近的叠加度数，需与看远度数、瞳距（近用瞳距会略小）、镜片类型一起确定。
- 突然、单眼或快速加重的看近困难，或伴随头痛、视物变形，应先就医排查，而非仅配老花镜。
- 本工具只做科普参考，不替代验光师 / 医生。`);
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

function optionalBoolean(args: ToolArgs, key: string): boolean | undefined {
  if (!(key in args) || args[key] === undefined) {
    return undefined;
  }
  return expectBoolean(args, key);
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
