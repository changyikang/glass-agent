package com.glass.agent.tool;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import static com.glass.agent.tool.Diopters.describeEye;
import static com.glass.agent.tool.Diopters.formatDiopter;
import static com.glass.agent.tool.Diopters.formatSignedDiopter;
import static com.glass.agent.tool.Diopters.renderBulletList;
import static com.glass.agent.tool.Diopters.renderLensType;
import static com.glass.agent.tool.Diopters.renderPrescriptionLine;

/**
 * 配眼镜指南工具集。
 *
 * <p>每个方法都标注了 Spring AI 的 {@link Tool} 注解，会被自动暴露为大模型可调用的 Function；
 * 同时也是普通 Spring Bean 方法，供 REST 控制器直接调用（不经过大模型）。
 *
 * <p>逻辑与原 TypeScript 版本 {@code src/index.ts} 中的 11 个 handler 一一对应。
 */
@Component
public class GlassAdvisorTools {

    // ---------------------------------------------------------------------
    // 1. 视力检查指南
    // ---------------------------------------------------------------------
    @Tool(description = "视力检查指南：按年龄段说明检查频率、重点项目、检查前准备和常见关注点。")
    public String visionCheckGuide(
            @ToolParam(description = "年龄段：children(6-18岁), adult(18-59岁), senior(60岁以上)")
            String ageGroup,
            @ToolParam(required = false, description = "关注点，可填写近视、散光、老花、弱视、干眼、隐形眼镜等")
            String concern) {

        String group = expectEnum("age_group", ageGroup, "children", "adult", "senior");
        String trimmedConcern = concern == null || concern.isBlank() ? null : concern.trim();

        Map<String, String> guides = Map.of(
                "children", """
                        ## 儿童视力检查指南（6-18岁）

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
                        - 如需散瞳，预留返程和休息时间""",
                "adult", """
                        ## 成人视力检查指南（18-59岁）

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
                        - 带上现有眼镜，便于对比旧处方""",
                "senior", """
                        ## 老年视力检查指南（60岁以上）

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
                        - 如需散瞳，当天避免自行驾车""");

        List<String[]> concernTips = List.of(
                new String[]{"近视", """
                        **近视相关**
                        - 儿童青少年重点看近视进展速度，不只看一次度数
                        - 高度近视建议把眼底检查列为常规项目"""},
                new String[]{"散光", """
                        **散光相关**
                        - 散光配镜除了度数，还要关注轴位是否稳定
                        - 散光较大时建议现场试戴，确认清晰度和眩晕感"""},
                new String[]{"老花", """
                        **老花相关**
                        - 需要同时评估远用、近用和中距离（电脑）需求
                        - 不是只测 ADD，还要结合工作距离选镜片方案"""},
                new String[]{"弱视", """
                        **弱视相关**
                        - 儿童要同时评估屈光参差、斜视和双眼视功能
                        - 弱视训练和复查频率通常比普通配镜更密集"""},
                new String[]{"干眼", """
                        **干眼相关**
                        - 先处理眼表状态，再决定最终处方更稳妥
                        - 检查当天避免长时间戴隐形眼镜或熬夜"""},
                new String[]{"隐形", """
                        **隐形眼镜相关**
                        - 需评估角膜状态、泪膜和佩戴习惯
                        - 隐形眼镜验配与框架眼镜处方不能简单等同"""});

        List<String> matchedTips = new ArrayList<>();
        if (trimmedConcern != null) {
            for (String[] pair : concernTips) {
                if (trimmedConcern.contains(pair[0])) {
                    matchedTips.add(pair[1]);
                }
            }
        }

        String suffix = !matchedTips.isEmpty()
                ? "\n\n" + String.join("\n\n", matchedTips)
                        + "\n\n**提醒**\n最终处方应以现场主觉验光和试戴结果为准。"
                : "\n\n**提醒**\n如果近期出现视力突然下降、眼痛、闪光感或飞蚊骤增，应优先就医，不建议只做配镜咨询。";

        return guides.get(group) + suffix;
    }

    // ---------------------------------------------------------------------
    // 2. 镜片推荐
    // ---------------------------------------------------------------------
    @Tool(description = "镜片推荐：根据度数、散光、用途和预算给出折射率、材质、镀膜和选购建议。")
    public String lensRecommendation(
            @ToolParam(description = "球镜度数，单位D，如 -3.25。近视填负数，远视填正数。")
            double sph,
            @ToolParam(required = false, description = "柱镜度数，单位D，如 -0.75。无散光可不填。")
            Double cyl,
            @ToolParam(description = "主要场景：daily(日常), computer(电脑), driving(驾驶), sports(运动), reading(阅读)")
            String usage,
            @ToolParam(description = "预算：economy(经济), mid(中档), premium(高端)")
            String budget) {

        checkRange("sph", sph, -20, 12);
        double cylValue = cyl == null ? 0 : cyl;
        if (cyl != null) {
            checkRange("cyl", cylValue, -8, 8);
        }
        String usageValue = expectEnum("usage", usage, "daily", "computer", "driving", "sports", "reading");
        String budgetValue = expectEnum("budget", budget, "economy", "mid", "premium");

        double meridianPower = Math.max(Math.abs(sph), Math.abs(sph + cylValue));
        double sphericalEquivalent = sph + cylValue / 2;

        String indexRecommendation = meridianPower <= 2
                ? "1.56：轻度度数够用，成本低"
                : meridianPower <= 4
                        ? "1.60：大多数日常配镜的均衡选择"
                        : meridianPower <= 6
                                ? "1.67：中高度数更合适，厚度与重量明显更友好"
                                : "1.74：超高度数可选，但价格更高、色散控制也更需要注意";

        Map<String, Map<String, String>> typeRecommendation = Map.of(
                "daily", Map.of(
                        "economy", "非球面单光镜片，优先保证加工精度和基础防反射",
                        "mid", "品牌非球面单光镜片，兼顾清晰度和耐用性",
                        "premium", "自由曲面或个性化单光镜片，适合对边缘成像和佩戴体验要求高的人群"),
                "computer", Map.of(
                        "economy", "单光镜片 + 优质防反射镀膜，先控制眩光",
                        "mid", "抗疲劳或轻办公型镜片，更适合长时间屏幕使用",
                        "premium", "办公专区镜片，覆盖屏幕和桌面阅读距离"),
                "driving", Map.of(
                        "economy", "高透光率单光镜片 + 防反射镀膜，夜间驾驶更实用",
                        "mid", "高透光率镜片 + 更好的防眩镀膜，白天可另配偏光太阳镜",
                        "premium", "夜间清晰度更好的高端驾驶镜片方案，建议与日间太阳镜分开配置"),
                "sports", Map.of(
                        "economy", "PC 或 Trivex 这类抗冲击材质，优先安全性",
                        "mid", "抗冲击材质 + 疏水防污镀膜，便于频繁清洁",
                        "premium", "运动专用曲面镜片，但需确认处方与镜框包裹角匹配"),
                "reading", Map.of(
                        "economy", "近用单光镜片，适合固定阅读距离",
                        "mid", "近用单光或入门办公镜片，适合阅读兼顾电脑",
                        "premium", "办公镜或渐进镜片前的精细验配方案，适合近中距离切换多的人群"));

        Map<String, String> coatingRecommendation = Map.of(
                "economy", "优先选择防反射 + UV 防护。不要把“防蓝光”当成默认刚需。",
                "mid", "防反射 + UV + 疏水防污，日常体验提升最明显。",
                "premium", "在中档镀膜基础上，选择更耐磨、更稳定的高透方案。");

        Map<String, String> budgetRange = Map.of(
                "economy", "约 300-800 元/副镜片",
                "mid", "约 800-2000 元/副镜片",
                "premium", "约 2000 元以上/副镜片");

        List<String> usageWarnings = new ArrayList<>();
        if (meridianPower >= 5) {
            usageWarnings.add("度数偏高，镜框尽量不要过大，否则边缘厚度和重量会明显增加。");
        }
        if (Math.abs(cylValue) >= 2) {
            usageWarnings.add("散光较大，建议选择成像更稳的非球面或个性化设计，并现场试戴确认轴位适应。");
        }
        if (usageValue.equals("driving")) {
            usageWarnings.add("偏光镜适合白天强光环境，不适合替代夜间驾驶的主力镜片。");
        }
        if (usageValue.equals("sports")) {
            usageWarnings.add("如果是对抗性运动，普通日常镜框不够安全，优先选运动框和抗冲击材质。");
        }
        if (usageValue.equals("reading") && Math.abs(sphericalEquivalent) < 0.5) {
            usageWarnings.add("如果主要是中老年近距离吃力，建议补做 ADD 检查，而不是只按单纯近视/远视选镜片。");
        }

        return """
                ## 镜片推荐

                **处方概览**
                - 球镜：%s
                - 柱镜：%s
                - 等效球镜：%s
                - 参考最大子午线度数：%s

                **折射率建议**
                - %s

                **用途和预算匹配**
                - %s

                **镀膜建议**
                - %s

                **预算区间**
                - %s

                **额外提醒**
                %s

                **结论**
                - 镜片选择先看处方和用途，再看预算；不要只盯着折射率。
                - 最终成品效果还受镜框尺寸、瞳距、加工中心定位影响。""".formatted(
                formatSignedDiopter(sph),
                cylValue == 0 ? "无明显散光" : formatSignedDiopter(cylValue),
                formatSignedDiopter(sphericalEquivalent),
                formatDiopter(meridianPower),
                indexRecommendation,
                typeRecommendation.get(usageValue).get(budgetValue),
                coatingRecommendation.get(budgetValue),
                budgetRange.get(budgetValue),
                renderBulletList(usageWarnings, "整体处方压力不大，优先保证验光准确和镜框尺寸合适。"));
    }

    // ---------------------------------------------------------------------
    // 3. 镜框选择指南
    // ---------------------------------------------------------------------
    @Tool(description = "镜框选择指南：结合脸型、生活方式和度数深浅推荐框型、材质和尺寸思路。")
    public String frameSelectionGuide(
            @ToolParam(description = "脸型：oval, round, square, heart, oblong")
            String faceShape,
            @ToolParam(description = "生活方式：professional(商务), casual(休闲), active(运动), fashion(时尚)")
            String lifestyle,
            @ToolParam(required = false, description = "度数深浅：low(<300度), medium(300-600度), high(>600度)")
            String prescriptionStrength) {

        String face = expectEnum("face_shape", faceShape, "oval", "round", "square", "heart", "oblong");
        String life = expectEnum("lifestyle", lifestyle, "professional", "casual", "active", "fashion");
        String strength = prescriptionStrength == null
                ? "medium"
                : expectEnum("prescription_strength", prescriptionStrength, "low", "medium", "high");

        Map<String, String[]> faceGuides = Map.of(
                "oval", new String[]{"脸部比例均衡，可用多数框型做风格表达。", "方框、矩形框、猫眼框都容易出效果。", "过宽或过高的超大框容易压脸。"},
                "round", new String[]{"面部线条偏柔和，适合用更利落的框型拉开轮廓。", "矩形框、带棱角的方框、眉线框。", "小圆框和过于圆润的框型。"},
                "square", new String[]{"下颌线和额角更分明，适合用曲线柔化。", "圆框、椭圆框、软边猫眼框。", "过硬朗的厚重方框。"},
                "heart", new String[]{"上庭视觉重量较强，适合把重心往下和中部拉。", "轻薄金属框、椭圆框、下缘更稳定的框型。", "上宽下窄、眉线过重的设计。"},
                "oblong", new String[]{"脸型偏长，适合增加横向存在感。", "镜圈高度适中的宽框、大一点的圆角方框。", "过窄、过小的细长框。"});

        Map<String, String> lifestyleGuides = Map.of(
                "professional", "优先稳定、耐看和易搭配，黑、枪灰、深棕、钛色最稳妥。",
                "casual", "可以接受板材、透明灰、琥珀色等更轻松的风格。",
                "active", "优先 TR90、钛或带防滑结构的全框，别把外观放在安全性前面。",
                "fashion", "可以把框型作为造型重点，但仍要确认瞳距和镜圈尺寸是否适合处方。");

        Map<String, List<String>> prescriptionTips = Map.of(
                "low", List.of(
                        "低度数可选范围最大，无框、半框、全框都能尝试。",
                        "如果想要更轻，可以优先考虑钛架或轻板材。"),
                "medium", List.of(
                        "中度数建议优先全框或结构稳定的半框。",
                        "镜圈不要过大，否则边缘厚度和重量会上来。"),
                "high", List.of(
                        "高度数优先小一些的全框，镜圈不宜过宽。",
                        "尽量避开无框和超大框，成品外观和稳定性都更难控制。",
                        "建议选有鼻托、方便微调前倾角和顶点距的镜架。"));

        List<String> materialGuide = List.of(
                "板材：造型感强，适合休闲和时尚风格。",
                "金属/钛：轻、精致、好调校，适合商务和长时间佩戴。",
                "TR90：弹性和耐冲击更好，适合运动或通勤。");

        String[] fg = faceGuides.get(face);
        return """
                ## 镜框选择指南

                **脸型判断**
                - %s

                **推荐方向**
                - %s

                **尽量避开**
                - %s

                **生活方式匹配**
                - %s

                **度数相关建议**
                %s

                **材质参考**
                %s

                **试戴时重点**
                - 鼻托或鼻梁接触是否稳、是否压痛
                - 镜腿是否夹头或易滑落
                - 眼睛是否处在镜圈相对居中的位置
                - 高度数时，优先看成品厚度和重量，再看单纯外观""".formatted(
                fg[0],
                fg[1],
                fg[2],
                lifestyleGuides.get(life),
                bulletJoin(prescriptionTips.get(strength)),
                bulletJoin(materialGuide));
    }

    // ---------------------------------------------------------------------
    // 4. 验光单解读
    // ---------------------------------------------------------------------
    @Tool(description = "验光单解读：解释 SPH/CYL/AXIS/PD/ADD 的意义，并提示配镜风险点。")
    public String prescriptionInterpreter(
            @ToolParam(description = "右眼球镜 OD SPH，单位D") double odSph,
            @ToolParam(required = false, description = "右眼柱镜 OD CYL，单位D，可选") Double odCyl,
            @ToolParam(required = false, description = "右眼轴位 OD AXIS，0-180度，可选") Integer odAxis,
            @ToolParam(description = "左眼球镜 OS SPH，单位D") double osSph,
            @ToolParam(required = false, description = "左眼柱镜 OS CYL，单位D，可选") Double osCyl,
            @ToolParam(required = false, description = "左眼轴位 OS AXIS，0-180度，可选") Integer osAxis,
            @ToolParam(required = false, description = "双眼瞳距 PD，单位mm，可选") Double pd,
            @ToolParam(required = false, description = "老花附加度数 ADD，单位D，可选") Double add) {

        checkRange("od_sph", odSph, -20, 12);
        double odCylValue = odCyl == null ? 0 : odCyl;
        if (odCyl != null) {
            checkRange("od_cyl", odCylValue, -8, 8);
        }
        if (odAxis != null) {
            checkRange("od_axis", odAxis, 0, 180);
        }
        checkRange("os_sph", osSph, -20, 12);
        double osCylValue = osCyl == null ? 0 : osCyl;
        if (osCyl != null) {
            checkRange("os_cyl", osCylValue, -8, 8);
        }
        if (osAxis != null) {
            checkRange("os_axis", osAxis, 0, 180);
        }
        if (pd != null) {
            checkRange("pd", pd, 45, 80);
        }
        if (add != null) {
            checkRange("add", add, 0.5, 3.5);
        }

        validateAxis("右眼", odCylValue, odAxis);
        validateAxis("左眼", osCylValue, osAxis);

        double odEquivalent = odSph + odCylValue / 2;
        double osEquivalent = osSph + osCylValue / 2;
        double anisometropia = Math.abs(odEquivalent - osEquivalent);

        List<String> warnings = new ArrayList<>();
        if (anisometropia >= 2) {
            warnings.add("双眼等效球镜相差较大，可能出现放大率差异、头晕或融合困难。");
        }
        if (Math.max(Math.abs(odEquivalent), Math.abs(osEquivalent)) >= 6) {
            warnings.add("属于中高度屈光不正，配镜建议重视镜框尺寸、折射率和眼底随访。");
        }
        if (Math.max(Math.abs(odCylValue), Math.abs(osCylValue)) >= 2) {
            warnings.add("散光较大时，轴位误差会更影响清晰度和舒适度。");
        }
        if (add != null && add >= 1.5) {
            warnings.add("ADD 已不低，是否需要渐进或办公镜要结合工作距离来定。");
        }
        if (pd == null) {
            warnings.add("当前没有 PD，正式加工前仍需准确测量瞳距，尤其是高度数或渐进镜片。");
        }

        String pdBlock = pd != null ? "**瞳距**\n- " + pd + " mm\n" : "";
        String addBlock = add != null
                ? "**ADD**\n- " + formatSignedDiopter(add) + "，提示需要评估近用或多焦点方案\n"
                : "";

        return """
                ## 验光单解读

                **字段含义**
                - OD / OS：右眼 / 左眼
                - SPH：球镜，近视通常记负数，远视记正数
                - CYL：柱镜，也就是散光
                - AXIS：散光轴位，0-180°
                - PD：瞳距
                - ADD：老花附加度数

                **右眼**
                - 处方：%s
                - 解读：%s
                - 等效球镜：%s

                **左眼**
                - 处方：%s
                - 解读：%s
                - 等效球镜：%s

                **双眼对比**
                - 等效球镜差值：%s
                - 判断：%s

                %s%s
                **配镜提醒**
                %s

                **结论**
                - 验光单不是成品眼镜，最终还要结合试戴、瞳高、镜框参数和加工质量。""".formatted(
                renderPrescriptionLine(odSph, odCylValue, odAxis),
                describeEye(odSph, odCylValue),
                formatSignedDiopter(odEquivalent),
                renderPrescriptionLine(osSph, osCylValue, osAxis),
                describeEye(osSph, osCylValue),
                formatSignedDiopter(osEquivalent),
                formatDiopter(anisometropia),
                anisometropia < 1 ? "双眼比较接近" : anisometropia < 2 ? "有一定差异，配镜要注意平衡" : "差异较大，适应风险更高",
                pdBlock,
                addBlock,
                renderBulletList(warnings, "当前没有特别突出的风险点，仍建议结合试戴与加工参数确认。"));
    }

    // ---------------------------------------------------------------------
    // 5. 渐进镜片适配评估
    // ---------------------------------------------------------------------
    @Tool(description = "渐进镜片适配评估：判断更适合单焦、办公镜还是渐进多焦点镜片。")
    public String progressiveLensAssessment(
            @ToolParam(description = "年龄，用于判断老花需求与适应预期") int age,
            @ToolParam(description = "近距离阅读困难程度：none, mild, obvious") String nearDifficulty,
            @ToolParam(description = "日均电脑或平板使用时长，单位小时") double screenHours,
            @ToolParam(description = "驾驶频率：rare, weekly, daily") String driveFrequency,
            @ToolParam(description = "是否第一次尝试多焦点镜片") boolean firstTimeUser) {

        checkRange("age", age, 18, 90);
        String near = expectEnum("near_difficulty", nearDifficulty, "none", "mild", "obvious");
        checkRange("screen_hours", screenHours, 0, 16);
        String drive = expectEnum("drive_frequency", driveFrequency, "rare", "weekly", "daily");

        String recommendation;
        String fitReason;
        if (near.equals("none") && age < 40) {
            recommendation = "更适合单光镜片，不建议为了“预防”而直接上渐进。";
            fitReason = "目前近距离困难不明显，渐进的收益有限。";
        } else if (screenHours >= 6 && !drive.equals("daily")) {
            recommendation = "优先考虑办公镜片，其次再看是否需要一副单独远用镜。";
            fitReason = "长时间看屏幕的人，中近距离视野通常比通用型渐进更重要。";
        } else if (near.equals("obvious") && age >= 42) {
            recommendation = "可以重点评估渐进多焦点镜片。";
            fitReason = "远近切换需求明确，渐进能减少频繁摘戴两副眼镜。";
        } else {
            recommendation = "处于单光、办公镜和渐进都可能合适的区间，需要按实际工作距离细化。";
            fitReason = "是否开车多、是否长时间看电脑，会显著影响方案选择。";
        }

        List<String> cautions = new ArrayList<>();
        if (firstTimeUser) {
            cautions.add("第一次用渐进或办公镜，建议预留 1-2 周适应期。");
        }
        if (drive.equals("daily")) {
            cautions.add("如果每天开车，通道设计和远用区域要足够稳定，不能只追求近用舒适。");
        }
        if (screenHours >= 8) {
            cautions.add("重度屏幕使用者要特别关注中距离视野宽度，普通渐进不一定最舒服。");
        }
        if (age >= 55) {
            cautions.add("ADD 往往较高，对通道长度和佩戴参数更敏感，验配要细。");
        }

        return """
                ## 多焦点适配评估

                **初步结论**
                - %s

                **原因**
                - %s

                **你的使用画像**
                - 年龄：%d 岁
                - 近距离吃力程度：%s
                - 日均屏幕时长：%s 小时
                - 驾驶频率：%s
                - 是否首次尝试：%s

                **建议关注**
                %s

                **下一步**
                - 让验光师明确测量远用、阅读和电脑距离需求，再决定单光 / 办公镜 / 渐进。""".formatted(
                recommendation,
                fitReason,
                age,
                near.equals("none") ? "不明显" : near.equals("mild") ? "轻度" : "明显",
                trimNumber(screenHours),
                drive.equals("rare") ? "很少" : drive.equals("weekly") ? "每周" : "几乎每天",
                firstTimeUser ? "是" : "否",
                renderBulletList(cautions, "目前没有明显的额外适配风险，但仍建议先试戴再定方案。"));
    }

    // ---------------------------------------------------------------------
    // 6. 新眼镜不适排查
    // ---------------------------------------------------------------------
    @Tool(description = "新眼镜不适排查：根据症状、佩戴时长和镜片类型判断是适应期还是需要复查。")
    public String newGlassesTroubleshooting(
            @ToolParam(description = "主要不适症状：dizziness, blur_distance, blur_near, headache, double_vision, nose_pain, ear_pain, slipping")
            String symptom,
            @ToolParam(description = "已经佩戴新眼镜多少天") int wearDays,
            @ToolParam(description = "镜片类型：single_vision, progressive, office, bifocal") String lensType,
            @ToolParam(description = "这次配镜度数是否有明显变化") boolean prescriptionChanged) {

        String sym = expectEnum("symptom", symptom,
                "dizziness", "blur_distance", "blur_near", "headache",
                "double_vision", "nose_pain", "ear_pain", "slipping");
        checkRange("wear_days", wearDays, 0, 60);
        String lens = expectEnum("lens_type", lensType, "single_vision", "progressive", "office", "bifocal");

        Map<String, String> symptomAdvice = Map.of(
                "dizziness", "常见于度数变化、散光轴位变化、镜片中心点偏移或多焦点初期适应。",
                "blur_distance", "先排查远用度数不足/过矫、瞳距瞳高偏差、镜框前倾角变化。",
                "blur_near", "如果是中老年人，常见于 ADD 不足、工作距离不匹配或把远用镜当近用镜。",
                "headache", "常见于过矫、双眼平衡问题、散光轴位不适或长时间勉强适应。",
                "double_vision", "优先排查棱镜效应、瞳距偏差和双眼视问题，这类情况不要硬扛。",
                "nose_pain", "多半是鼻托/鼻梁受力不均或镜框太重，不一定是处方问题。",
                "ear_pain", "镜腿弯点和夹持力不合适，通常靠调架能改善。",
                "slipping", "镜框重心、鼻托摩擦和镜腿包覆不足，需要做机械调校。");

        boolean isLikelyAdaptation =
                (lens.equals("progressive") || lens.equals("office") || prescriptionChanged) && wearDays <= 14;
        boolean urgentFlags = sym.equals("double_vision") || (sym.equals("headache") && wearDays > 7);

        List<String> actionItems = new ArrayList<>();
        if (sym.equals("nose_pain") || sym.equals("ear_pain") || sym.equals("slipping")) {
            actionItems.add("先回店里做镜架调校，很多佩戴问题不需要重做镜片。");
        } else {
            actionItems.add("带着旧眼镜和验光单回店复查，对比新旧处方和加工参数。");
        }
        if (wearDays <= 3 && isLikelyAdaptation && !urgentFlags) {
            actionItems.add("在安全前提下可继续短时间逐步佩戴，不要一整天硬撑。");
        }
        if (lens.equals("progressive")) {
            actionItems.add("确认看远时从镜片上方区域看、看近时轻微下转眼位，而不是只转头。");
        }
        if (urgentFlags || wearDays > 14) {
            actionItems.add("如果复查后仍明显不适，需考虑重新验光或重做镜片。");
        }

        return """
                ## 新眼镜不适排查

                **症状判断**
                - %s

                **当前情况**
                - 已佩戴：%d 天
                - 镜片类型：%s
                - 本次度数是否有明显变化：%s

                **倾向判断**
                - %s

                **建议动作**
                %s

                **复查时重点问什么**
                - 瞳距、瞳高、散光轴位是否准确
                - 镜框前倾角、顶点距、面弯是否与验配时一致
                - 新旧处方差异是否超过正常适应范围""".formatted(
                symptomAdvice.get(sym),
                wearDays,
                renderLensType(lens),
                prescriptionChanged ? "是" : "否",
                urgentFlags
                        ? "不建议继续硬适应，应尽快复查。"
                        : isLikelyAdaptation
                                ? "有一定概率属于适应期，但仍需留意是否逐日改善。"
                                : "更像是处方、加工或镜架参数问题，而不只是适应期。",
                bulletJoin(actionItems));
    }

    // ---------------------------------------------------------------------
    // 7. 购物链接生成
    // ---------------------------------------------------------------------
    @Tool(description = "购物链接生成：把配镜建议转成可点击的电商搜索购买链接。传入若干中文商品关键词"
            + "（如「1.67 非球面 防蓝光 镜片」「TR90 超轻 近视镜框」），返回每个关键词在京东、淘宝、拼多多的搜索链接，"
            + "便于用户直接比价选购。")
    public String shoppingLinks(
            @ToolParam(description = "商品搜索关键词列表，建议每项包含关键规格，如折射率/材质/镀膜（镜片）或框型/材质（镜框）")
            List<String> keywords) {

        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("参数 keywords 必须是非空的关键词列表");
        }
        List<String> cleaned = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank()) {
                cleaned.add(keyword.trim());
            }
        }
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("参数 keywords 至少要包含一个非空关键词");
        }
        if (cleaned.size() > 8) {
            throw new IllegalArgumentException("参数 keywords 一次最多支持 8 个");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 🛒 购买链接\n");
        sb.append("以下链接会跳转到各平台的实时搜索结果，可直接点击比价选购：\n\n");
        for (int i = 0; i < cleaned.size(); i++) {
            String keyword = cleaned.get(i);
            if (i > 0) {
                sb.append("\n\n");
            }
            sb.append("**").append(keyword).append("**\n- ");
            sb.append("[京东](").append(jdSearchUrl(keyword)).append(") · ");
            sb.append("[淘宝](").append(taobaoSearchUrl(keyword)).append(") · ");
            sb.append("[拼多多](").append(pddSearchUrl(keyword)).append(")");
        }
        sb.append("\n\n> 链接为搜索入口，价格与款式以平台实时为准；请务必对照验光单核对度数后再下单。");
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // 8. 镜片厚度估算
    // ---------------------------------------------------------------------
    private static final List<String> LENS_INDICES = List.of("1.56", "1.60", "1.67", "1.74");

    @Tool(description = "镜片厚度估算：基于薄透镜矢高（sagitta）近似，按度数、折射率和镜圈宽度估算镜片最厚处"
            + "（近视看边缘、远视看中心）的厚度与重量倾向，并判断是否值得提高折射率减薄。")
    public String lensThicknessEstimator(
            @ToolParam(description = "球镜度数，单位D，如 -6.00。近视填负数，远视填正数。")
            double sph,
            @ToolParam(required = false, description = "柱镜度数，单位D，如 -1.00。无散光可不填。")
            Double cyl,
            @ToolParam(description = "镜片折射率：1.56 / 1.60 / 1.67 / 1.74")
            String lensIndex,
            @ToolParam(required = false, description = "镜圈水平宽度（镜腿上标注的“眼宽”数字），单位mm，常见 48-58，默认 52。")
            Double frameWidth) {

        checkRange("sph", sph, -20, 12);
        double cylValue = cyl == null ? 0 : cyl;
        if (cyl != null) {
            checkRange("cyl", cylValue, -8, 8);
        }
        String index = expectEnum("lens_index", lensIndex, "1.56", "1.60", "1.67", "1.74");
        double frameWidthValue = frameWidth == null ? 52 : frameWidth;
        if (frameWidth != null) {
            checkRange("frame_width", frameWidthValue, 40, 70);
        }

        double n = Double.parseDouble(index);
        double power = Math.max(Math.abs(sph), Math.abs(sph + cylValue));
        boolean isPlus = sph > 0;
        double effectiveDiameter = frameWidthValue + 4; // 4mm 偏心余量：实际有效直径通常大于镜圈标称宽度

        double thickest = estimateThickest(power, n, effectiveDiameter, isPlus);
        String rating = thicknessRating(thickest);
        String thickestLabel = isPlus ? "中心最厚" : "边缘最厚";
        String lensTypeLabel = isPlus
                ? "远视 / 正镜片（中心厚、边缘薄）"
                : "近视 / 负镜片（中心薄、边缘厚）";

        String weightNote = thickest >= 5
                ? "偏厚，长时间佩戴重量感会比较明显，建议配合小镜框和更高折射率一起控制。"
                : thickest >= 3.5
                        ? "中等，多数人佩戴可接受。"
                        : "较轻薄，重量通常不是主要问题。";

        String rec = recommendedIndex(power);
        int chosenRank = LENS_INDICES.indexOf(index);
        int recRank = LENS_INDICES.indexOf(rec);

        String indexAdvice;
        if (chosenRank < recRank) {
            double upgraded = estimateThickest(power, Double.parseDouble(rec), effectiveDiameter, isPlus);
            double delta = thickest - upgraded;
            indexAdvice = "建议提高到 " + rec + "：" + thickestLabel + "处约从 "
                    + format1(thickest) + " 降到 " + format1(upgraded) + " mm（减薄约 " + format1(delta) + " mm）。";
        } else if (chosenRank > recRank && power < 2) {
            indexAdvice = "度数不高，选到 " + index + " 更多是减重/美观考量，性价比一般，1.56 / 1.60 通常已足够。";
        } else {
            indexAdvice = "当前折射率 " + index + " 与度数基本匹配，可优先在镜框尺寸上再做优化。";
        }

        return """
                ## 镜片厚度估算

                > 基于薄透镜矢高（sagitta）近似：最厚处 ≈ 基础厚度 + 功率 × 半径² / (2000 × (n−1))。用于横向比较不同折射率与镜框，实际成品还取决于加工工艺、瞳距偏心与镜片设计。

                **输入参数**
                - 球镜：%s
                - 柱镜：%s
                - 参考功率（最大子午线）：%s
                - 折射率：%s（n = %s）
                - 镜圈宽度：%s mm（估算有效直径 %s mm）

                **估算结果**
                - 镜片类型：%s
                - %s：约 %s mm（%s）
                - 重量倾向：%s

                **折射率建议**
                - %s

                **实用提醒**
                - 边缘/中心厚度对镜框尺寸非常敏感：镜圈越小、越贴合脸型，成品越薄越轻。
                - 高度数尽量选全框，避开无框和超大框，边缘更好收。
                - 折射率越高越薄，但材料密度也更高，减重幅度通常小于减薄幅度，别只盯着折射率。""".formatted(
                formatSignedDiopter(sph),
                cylValue == 0 ? "无明显散光" : formatSignedDiopter(cylValue),
                formatDiopter(power),
                index,
                trimNumber(n),
                trimNumber(frameWidthValue),
                trimNumber(effectiveDiameter),
                lensTypeLabel,
                thickestLabel,
                format1(thickest),
                rating,
                weightNote,
                indexAdvice);
    }

    // ---------------------------------------------------------------------
    // 9. 瞳距（PD）助手
    // ---------------------------------------------------------------------
    /** 镜片平面到眼球旋转中心的近似距离（mm），用于把远用瞳距折算为近用瞳距。 */
    private static final double PD_ROTATION_CENTER_MM = 27;

    @Tool(description = "瞳距（PD）助手：网购配镜必填的瞳距参数解读与换算。可传入双眼瞳距，或分别传入左右单眼瞳距，"
            + "工具会校验数值是否落在常见范围、互相核对、按工作距离折算近用瞳距，并给出左右不对称提醒和自测方法。")
    public String pupillaryDistanceGuide(
            @ToolParam(required = false, description = "双眼瞳距（远用），单位mm，成人常见 54-74。与左右单眼瞳距至少提供一种。")
            Double binocularPd,
            @ToolParam(required = false, description = "右眼单眼瞳距（瞳孔中心到鼻梁中线），单位mm。若填写需与 pd_left 一起提供。")
            Double pdRight,
            @ToolParam(required = false, description = "左眼单眼瞳距（瞳孔中心到鼻梁中线），单位mm。若填写需与 pd_right 一起提供。")
            Double pdLeft,
            @ToolParam(required = false, description = "近用工作距离，单位cm，用于折算近用瞳距，默认 40（常见阅读距离）。")
            Double workingDistanceCm) {

        if (binocularPd != null) {
            checkRange("binocular_pd", binocularPd, 40, 85);
        }
        if (pdRight != null) {
            checkRange("pd_right", pdRight, 18, 45);
        }
        if (pdLeft != null) {
            checkRange("pd_left", pdLeft, 18, 45);
        }
        double workingDistanceCmValue = workingDistanceCm == null ? 40 : workingDistanceCm;
        if (workingDistanceCm != null) {
            checkRange("working_distance_cm", workingDistanceCmValue, 20, 100);
        }

        boolean hasMono = pdRight != null || pdLeft != null;
        if (hasMono && (pdRight == null || pdLeft == null)) {
            throw new IllegalArgumentException("单眼瞳距需要左右眼一起提供（pd_left 与 pd_right）");
        }
        if (binocularPd == null && !hasMono) {
            throw new IllegalArgumentException("请至少提供双眼瞳距 binocular_pd，或同时提供左右单眼瞳距 pd_left 和 pd_right");
        }

        Double monoSum = (pdRight != null && pdLeft != null) ? pdRight + pdLeft : null;
        double totalPd = binocularPd != null ? binocularPd : monoSum;

        List<String> warnings = new ArrayList<>();
        if (binocularPd != null && monoSum != null && Math.abs(binocularPd - monoSum) > 1.5) {
            warnings.add("双眼瞳距 " + format1Trim(binocularPd) + "mm 与左右单眼之和 " + format1Trim(monoSum)
                    + "mm 相差 " + format1Trim(Math.abs(binocularPd - monoSum)) + "mm，请核对测量数据。");
        }
        if (pdRight != null && pdLeft != null && Math.abs(pdRight - pdLeft) >= 3) {
            warnings.add("左右单眼瞳距相差约 " + format1Trim(Math.abs(pdRight - pdLeft))
                    + "mm，属于明显不对称，务必按单眼瞳距分别定位光心，不能简单平分双眼瞳距。");
        }

        double displayRight = pdRight != null ? pdRight : totalPd / 2;
        double displayLeft = pdLeft != null ? pdLeft : totalPd / 2;
        String monoSource = pdRight != null ? "实测" : "由双眼瞳距均分估算，仅供参考";

        double workingDistanceMm = workingDistanceCmValue * 10;
        double nearPd = (totalPd * workingDistanceMm) / (workingDistanceMm + PD_ROTATION_CENTER_MM);
        double nearReduction = totalPd - nearPd;

        return """
                ## 瞳距（PD）助手

                > 瞳距（PD）指两眼瞳孔中心的水平距离，是网购或加工配镜时把镜片光心对准眼睛的关键参数。远用看远、近用（阅读）时双眼会内聚，近用瞳距会比远用略小。

                **双眼瞳距（远用）**
                - %s mm%s
                - %s

                **单眼瞳距（左右）**
                - 右眼 OD：约 %s mm
                - 左眼 OS：约 %s mm
                - 来源：%s

                **近用瞳距（工作距离 %s cm）**
                - 约 %s mm（比远用约小 %s mm）
                - 配单独的阅读镜或看渐进/办公镜的近用区时才需要用到，普通远用镜按远用瞳距即可。

                **提醒**
                %s

                **自测方法（应急，精度有限）**
                - 对着镜子，把直尺贴在眉骨上，平视前方。
                - 闭右眼，用左眼把尺子的 0 刻度对准右眼瞳孔中心。
                - 保持尺子不动，闭左眼，用右眼读出左眼瞳孔中心对应的刻度，即为双眼瞳距。
                - 建议重复 2-3 次取平均；高度数、渐进片和儿童配镜请以专业测量为准。""".formatted(
                format1Trim(totalPd),
                binocularPd == null ? "（由左右单眼相加得到）" : "",
                pdRangeNote(totalPd),
                format1Trim(displayRight),
                format1Trim(displayLeft),
                monoSource,
                trimNumber(workingDistanceCmValue),
                format1Trim(nearPd),
                format1Trim(nearReduction),
                renderBulletList(warnings, "数值看起来正常，仍建议以视光师现场用瞳距仪测量为准。"));
    }

    // ---------------------------------------------------------------------
    // 10. 镜片镀膜与功能顾问
    // ---------------------------------------------------------------------
    private static final Map<String, String> OUTDOOR_LABELS = Map.of(
            "rare", "很少", "sometimes", "有时", "often", "经常");
    private static final Map<String, String> NIGHT_DRIVING_LABELS = Map.of(
            "none", "基本不", "occasional", "偶尔", "frequent", "经常");

    /** 单条镀膜建议：名称、分级（must/optional/skip）、推荐等级文案、原因。 */
    private record CoatingRec(String name, String tier, String verdict, String reason) {
    }

    @Tool(description = "镜片镀膜与功能顾问：按用眼场景逐项判断「减反射绿膜、UV 防护、防蓝光、变色片、偏振太阳镜」"
            + "值不值得为它多花钱，避免被过度推销。传入日均屏幕时长、户外/日晒频率、夜间驾驶频率等，"
            + "返回每项功能的推荐等级、原因，以及一份「建议付费 / 可选 / 不必要」的购物清单。")
    public String lensCoatingAdvisor(
            @ToolParam(description = "日均看屏幕（电脑/手机/平板）的时长，单位小时，0-18。")
            double screenHours,
            @ToolParam(description = "户外/日晒暴露频率：rare(很少), sometimes(有时), often(经常)。")
            String outdoorFrequency,
            @ToolParam(required = false, description = "夜间驾驶频率：none(基本不), occasional(偶尔), frequent(经常)。默认 none。")
            String nightDriving,
            @ToolParam(required = false, description = "是否对强光/眩光比较敏感（畏光、易被反光晃到）。默认 false。")
            Boolean lightSensitive,
            @ToolParam(required = false, description = "是否希望一副眼镜室内外通用（用于判断是否推荐变色片）。默认 false。")
            Boolean preferOnePair) {

        checkRange("screen_hours", screenHours, 0, 18);
        String outdoor = expectEnum("outdoor_frequency", outdoorFrequency, "rare", "sometimes", "often");
        String night = nightDriving == null
                ? "none"
                : expectEnum("night_driving", nightDriving, "none", "occasional", "frequent");
        boolean sensitive = lightSensitive != null && lightSensitive;
        boolean onePair = preferOnePair != null && preferOnePair;

        List<CoatingRec> recs = new ArrayList<>();

        // 1. 基础膜层
        recs.add(new CoatingRec(
                "基础膜层（加硬耐磨 + 减反射绿膜 + 疏水防污）",
                "must",
                "标配（默认就选）",
                !night.equals("none")
                        ? "减少镜片内外反光、提升通透度和夜间抗眩光，同时更耐刮、更好清洁；你经常夜间开车，减反射膜尤其能压低对向车灯的鬼影和光晕。"
                        : "减少镜片内外反光、提升通透度，同时更耐刮、更好清洁，是现代树脂镜片的基础配置，几乎不用犹豫。"));

        // 2. UV 防护
        if (outdoor.equals("often")) {
            recs.add(new CoatingRec("UV 防护（UV400）", "must", "强烈推荐",
                    "长期日晒会增加白内障、翼状胬肉等风险，经常在户外一定要确认镜片达到 UV400；太阳镜/变色片更要如此。"));
        } else if (outdoor.equals("sometimes")) {
            recs.add(new CoatingRec("UV 防护（UV400）", "must", "推荐",
                    "紫外线防护属于低成本高收益，多数中高端树脂片本身就带 UV400，下单前确认参数即可。"));
        } else {
            recs.add(new CoatingRec("UV 防护（UV400）", "must", "标配（通常无需额外加价）",
                    "大部分树脂镜片出厂就阻隔 UV400，确认参数标注即可，不必为「UV 防护」单独加钱。"));
        }

        // 3. 防蓝光
        if (screenHours >= 8) {
            recs.add(new CoatingRec("防蓝光膜", "optional", "可选（重度屏幕使用者可考虑）",
                    "长时间看屏幕的人，防蓝光主要作用是主观上的对比度和舒适度，尤其晚上；但它并不能替代「调低亮度/色温 + 定时休息」，护眼作用别被夸大。"));
        } else if (screenHours >= 4) {
            recs.add(new CoatingRec("防蓝光膜", "optional", "可选",
                    "屏幕时间中等，防蓝光更多是心理和轻微舒适度收益，可按预算决定，不是刚需。"));
        } else {
            recs.add(new CoatingRec("防蓝光膜", "skip", "一般不需要",
                    "屏幕使用不多时，防蓝光膜的实际收益很小，还可能让镜片带轻微底色，通常不值得额外花钱。"));
        }

        // 4. 变色片
        if (onePair && !outdoor.equals("rare")) {
            recs.add(new CoatingRec("变色片（光致变色）", "optional", "推荐",
                    "你希望一副镜片室内外通用，又经常见光，变色片能省去换太阳镜的麻烦；注意它变色/褪色都需要时间，且在车内因挡风玻璃阻隔紫外线通常变色不明显。"));
        } else if (outdoor.equals("often") && sensitive) {
            recs.add(new CoatingRec("变色片（光致变色）", "optional", "推荐",
                    "你经常在户外又比较畏光，变色片能随光线自动调节；但室内会残留淡淡底色，介意的话可改配单独太阳镜。"));
        } else if (outdoor.equals("rare") && !onePair) {
            recs.add(new CoatingRec("变色片（光致变色）", "skip", "一般不需要",
                    "户外时间少、也不追求一副通用，变色片的溢价通常用不上。"));
        } else {
            recs.add(new CoatingRec("变色片（光致变色）", "optional", "可选",
                    "介于需要与不需要之间：想省一副太阳镜可以上，但要接受变色有延迟、车内变色弱、室内有淡底色这几点。"));
        }

        // 5. 偏振太阳镜
        if (outdoor.equals("often")) {
            recs.add(new CoatingRec("偏振太阳镜（建议单独配一副）", "must", "推荐",
                    "经常户外或白天开车，偏振能有效削掉水面、路面、雪地的反射眩光；注意偏振只适合白天，夜间开车不要戴，且可能影响部分液晶仪表/手机屏显示。"));
        } else if (outdoor.equals("sometimes") && sensitive) {
            recs.add(new CoatingRec("偏振太阳镜（建议单独配一副）", "optional", "可选",
                    "你有时户外且比较畏光，一副偏振太阳镜会很舒服；日常近视镜没必要做成偏振，分开配更灵活。"));
        } else {
            recs.add(new CoatingRec("偏振太阳镜（建议单独配一副）", "skip", "一般不需要",
                    "户外强光暴露不多时，普通防 UV 已足够，偏振太阳镜可等有需要时再单独配。"));
        }

        List<String> cautions = new ArrayList<>();
        if (night.equals("frequent")) {
            cautions.add("夜间驾驶最关键的是干净的减反射膜；市面上的黄色「夜视 / 防远光」镜片会降低进光量，多数情况下并不推荐。");
        }
        if (screenHours >= 8) {
            cautions.add("长时间用屏幕，比防蓝光更有效的是调低屏幕亮度和色温、遵循 20-20-20 用眼法则（每 20 分钟看 20 英尺外 20 秒）并保证休息。");
        }
        if (sensitive && outdoor.equals("rare")) {
            cautions.add("你畏光但户外不多，如果不适感明显，建议先做一次眼科检查排查干眼或其它眼表问题，而不是只靠镀膜解决。");
        }
        cautions.add("镀膜和功能再全也替代不了准确验光和合适镜框：先把度数、瞳距、镜框选对，再谈镀膜取舍。");

        StringBuilder recBlock = new StringBuilder();
        for (int i = 0; i < recs.size(); i++) {
            CoatingRec r = recs.get(i);
            if (i > 0) {
                recBlock.append("\n");
            }
            recBlock.append("- **").append(r.name()).append("**：").append(r.verdict())
                    .append("\n  - ").append(r.reason());
        }

        return """
                ## 镜片镀膜与功能顾问

                > 镀膜和功能是配镜里最容易被过度推销的部分。下面按你的用眼场景，逐项给出「值不值得为它多花钱」的判断，而不是一律都上。

                **你的用眼画像**
                - 日均屏幕时长：%s 小时
                - 户外 / 日晒频率：%s
                - 夜间驾驶：%s
                - 是否对强光 / 眩光敏感：%s
                - 是否希望一副镜片室内外通用：%s

                **逐项建议**
                %s

                **建议为它们付费**
                %s

                **可以再考虑（按预算决定）**
                %s

                **通常不必额外花钱**
                %s

                **提醒**
                %s""".formatted(
                trimNumber(screenHours),
                OUTDOOR_LABELS.get(outdoor),
                NIGHT_DRIVING_LABELS.get(night),
                sensitive ? "是" : "否",
                onePair ? "是" : "否",
                recBlock.toString(),
                renderBulletList(coatingNamesByTier(recs, "must"), "暂无必须额外付费的项目。"),
                renderBulletList(coatingNamesByTier(recs, "optional"), "暂无需要再权衡的可选项目。"),
                renderBulletList(coatingNamesByTier(recs, "skip"), "以上场景下没有明显不值得的项目。"),
                bulletJoin(cautions));
    }

    // ---------------------------------------------------------------------
    // 11. 青少年近视防控指南
    // ---------------------------------------------------------------------
    private static final Map<String, String> PARENT_MYOPIA_LABELS = Map.of(
            "none", "都不近视", "one", "一方近视", "both", "双方近视");

    /** 单条防控方案：名称、分级（priority/consider/notyet）、推荐等级文案、原因。 */
    private record MyopiaOption(String name, String tier, String verdict, String reason) {
    }

    @Tool(description = "青少年近视防控指南：根据孩子年龄、当前近视度数、近一年近视加深速度、父母近视情况和日均户外时长，"
            + "评估近视进展风险，并按适配度排序给出干预手段（户外活动、科学用眼习惯、近视离焦框架镜、角膜塑形镜 OK 镜、低浓度阿托品），"
            + "并说明各方案的适用条件与就医边界。仅为科普，用药与 OK 镜验配需到专业机构。")
    public String myopiaControlGuide(
            @ToolParam(description = "孩子年龄（岁），3-18 的整数。") int age,
            @ToolParam(description = "当前近视球镜度数，单位D，近视填负数（如 -2.50）。大于 -0.50 视为尚未达到近视标准。")
            double currentSph,
            @ToolParam(required = false, description = "近一年近视加深的度数（正数，单位D/年，如 0.75）。不清楚可不填。")
            Double annualProgression,
            @ToolParam(required = false, description = "父母近视情况：none(都不近视), one(一方近视), both(双方近视)。可不填。")
            String parentMyopia,
            @ToolParam(required = false, description = "孩子日均户外活动时长（小时，0-8）。可不填。")
            Double outdoorHours) {

        checkRange("age", age, 3, 18);
        checkRange("current_sph", currentSph, -15, 2);
        if (annualProgression != null) {
            checkRange("annual_progression", annualProgression, 0, 3);
        }
        String parent = parentMyopia == null
                ? null
                : expectEnum("parent_myopia", parentMyopia, "none", "one", "both");
        if (outdoorHours != null) {
            checkRange("outdoor_hours", outdoorHours, 0, 8);
        }

        boolean isMyopic = currentSph <= -0.5;
        String progText = annualProgression != null
                ? trimNumber(annualProgression) + "D/年（近视加深）"
                : "未提供";
        boolean fastProgress = annualProgression != null && annualProgression >= 0.5;

        // ---- 进展风险评分 ----
        int score = 0;
        List<String> factors = new ArrayList<>();
        if (isMyopic && age < 9) {
            score += 2;
            factors.add("发病年龄小（9 岁前已近视），进展风险显著偏高。");
        } else if (isMyopic && age < 12) {
            score += 1;
            factors.add("发病年龄偏小（12 岁前近视），仍需重点防控。");
        }
        if (annualProgression != null) {
            if (annualProgression >= 1.0) {
                score += 2;
                factors.add("近一年加深约 " + trimNumber(annualProgression) + "D，进展很快。");
            } else if (annualProgression >= 0.5) {
                score += 1;
                factors.add("近一年加深约 " + trimNumber(annualProgression) + "D，进展偏快。");
            } else {
                factors.add("近一年加深约 " + trimNumber(annualProgression) + "D，进展相对平稳。");
            }
        }
        if (currentSph <= -6) {
            score += 2;
            factors.add("已属高度近视，眼底并发症风险更高。");
        } else if (currentSph <= -3) {
            score += 1;
            factors.add("已属中度近视。");
        }
        if ("both".equals(parent)) {
            score += 2;
            factors.add("父母双方均近视，遗传易感性较高。");
        } else if ("one".equals(parent)) {
            score += 1;
            factors.add("父母一方近视，有一定遗传倾向。");
        }
        if (outdoorHours != null && outdoorHours < 1) {
            score += 1;
            factors.add("日均户外不足 1 小时，缺少最重要的保护因素。");
        } else if (outdoorHours != null && outdoorHours >= 2) {
            score -= 1;
            factors.add("日均户外 2 小时以上，是重要的保护因素。");
        }
        if (score < 0) {
            score = 0;
        }
        String riskLevel = score >= 4 ? "高" : score >= 2 ? "中" : "低";

        // ---- 逐项方案 ----
        List<MyopiaOption> options = new ArrayList<>();

        // 1. 户外活动
        options.add(new MyopiaOption(
                "每天累计 2 小时以上户外活动",
                "priority",
                outdoorHours != null && outdoorHours < 2 ? "最该优先补上" : "保持",
                outdoorHours != null
                        ? (outdoorHours < 2
                                ? "目前日均户外约 " + format1Trim(outdoorHours)
                                        + " 小时，证据最充分且几乎零成本，建议累计到每天 2 小时以上（自然光是关键，阴天也有效）。"
                                : "目前日均户外约 " + format1Trim(outdoorHours)
                                        + " 小时，已达标，请继续保持——这是防控近视最基础也最有效的一环。")
                        : "证据最充分且几乎零成本：保证每天累计 2 小时以上的户外活动，自然光是关键，阴天也有效。"));

        // 2. 科学用眼习惯
        options.add(new MyopiaOption(
                "科学用眼习惯",
                "priority",
                "长期坚持",
                "保持读写距离 33cm 以上、坐姿端正；遵循 20-20-20（每近距离用眼 20 分钟，看 20 英尺≈6 米外 20 秒）；"
                        + "保证读写照明充足、少用手机等小屏幕、保证充足睡眠。"));

        // 3. 近视离焦框架镜片
        if (isMyopic) {
            options.add(new MyopiaOption(
                    "近视离焦框架镜片（多区正向离焦等设计）",
                    "priority",
                    "日常配镜首选",
                    "非侵入、佩戴门槛低，适合大多数需要戴镜的近视儿童；相比普通单光镜片有助于延缓进展，是日常眼镜的优先选择。"));
        } else {
            options.add(new MyopiaOption(
                    "近视离焦框架镜片（多区正向离焦等设计）",
                    "notyet",
                    "暂不需要",
                    "目前尚未达到近视标准，普通监测即可；一旦确诊近视需要配镜，可优先选近视离焦设计而非普通单光片。"));
        }

        // 4. 角膜塑形镜（OK 镜）
        if (age < 8) {
            options.add(new MyopiaOption(
                    "角膜塑形镜（OK 镜 / 夜戴）",
                    "notyet",
                    "年龄偏小，暂不考虑",
                    "OK 镜通常建议 8 岁以上、能自己配合摘戴与护理的孩子，目前年龄偏小，先从户外和离焦框架镜入手。"));
        } else if (!isMyopic) {
            options.add(new MyopiaOption(
                    "角膜塑形镜（OK 镜 / 夜戴）",
                    "notyet",
                    "尚不需要",
                    "尚未确诊近视或度数很低，暂不需要 OK 镜，先做好户外与用眼习惯、定期复查。"));
        } else if (currentSph < -6) {
            options.add(new MyopiaOption(
                    "角膜塑形镜（OK 镜 / 夜戴）",
                    "consider",
                    "需专业评估（度数偏高）",
                    "度数偏高、超出 OK 镜常规适配范围（约 -1.00 ~ -6.00D），能否验配需由专业机构评估角膜曲率、厚度等条件后决定。"));
        } else {
            options.add(new MyopiaOption(
                    "角膜塑形镜（OK 镜 / 夜戴）",
                    "consider",
                    fastProgress ? "很值得评估" : "可以考虑评估",
                    "夜间佩戴、白天可获得清晰裸眼视力，对延缓眼轴增长有较好证据；需到正规医疗机构验配，严格护理卫生并定期复查，谨防角膜感染。"));
        }

        // 5. 低浓度阿托品
        if (!isMyopic) {
            options.add(new MyopiaOption(
                    "低浓度阿托品滴眼液（如 0.01%）",
                    "notyet",
                    "一般暂不用药",
                    "尚未近视时通常不用药，先把户外与用眼习惯做到位；是否需要请由眼科医生评估。"));
        } else {
            boolean urge = riskLevel.equals("高") || fastProgress;
            options.add(new MyopiaOption(
                    "低浓度阿托品滴眼液（如 0.01%）",
                    "consider",
                    urge ? "建议就诊咨询" : "可咨询医生",
                    urge
                            ? "进展较快，可就诊时咨询低浓度阿托品，需眼科医生评估并处方、定期随访；不建议自行购买使用。"
                            : "低浓度阿托品是控制进展的选项之一，是否使用请由眼科医生评估，切勿自行购买或网购使用。"));
        }

        // ---- 提醒 ----
        List<String> cautions = new ArrayList<>();
        cautions.add("角膜塑形镜（OK 镜）和低浓度阿托品都属于医疗行为，必须在正规眼科 / 视光机构验配、开具并定期随访，切勿自行购买或网购使用。");
        cautions.add("判断真性还是假性近视、评估是否用药，需要散瞳验光；建议每 3-6 个月复查一次，有条件时监测眼轴长度。");
        if (!isMyopic) {
            cautions.add("目前重点是「保住远视储备、别过早近视」——多户外、控制近距离用眼比急着配镜更重要。");
        }
        if (currentSph <= -6 || riskLevel.equals("高")) {
            cautions.add("高度近视要把眼底检查列为常规项目，警惕视网膜变性、裂孔等并发症。");
        }
        cautions.add("本工具只做科普参考，不替代医生诊断，具体方案请遵专业机构意见。");

        StringBuilder optionBlock = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            MyopiaOption o = options.get(i);
            if (i > 0) {
                optionBlock.append("\n");
            }
            optionBlock.append("- **").append(o.name()).append("**：").append(o.verdict())
                    .append("\n  - ").append(o.reason());
        }

        return """
                ## 青少年近视防控指南

                > 近视防控的核心是「延缓进展、控制眼轴增长」，越早干预越好，且防控措施可以叠加使用。以下结合孩子的年龄、度数、进展速度、遗传与户外情况给出评估与方案，仅供科普参考。

                **孩子情况**
                - 年龄：%d 岁
                - 当前度数（球镜）：%s —— %s
                - 近一年加深：%s
                - 父母近视：%s
                - 日均户外：%s

                **进展风险评估**
                - 综合判断：%s风险
                %s

                **逐项方案**
                %s

                **现在最该做的（优先）**
                %s

                **可结合专业机构评估**
                %s

                **暂不需要 / 条件未到**
                %s

                **提醒**
                %s""".formatted(
                age,
                formatSignedDiopter(currentSph),
                myopiaDegreeLabel(currentSph),
                progText,
                parent != null ? PARENT_MYOPIA_LABELS.get(parent) : "未提供",
                outdoorHours != null ? format1Trim(outdoorHours) + " 小时" : "未提供",
                riskLevel,
                renderBulletList(factors, "暂未发现突出的高危因素，继续保持良好用眼习惯和定期复查即可。"),
                optionBlock.toString(),
                renderBulletList(myopiaNamesByTier(options, "priority"), "暂无优先项。"),
                renderBulletList(myopiaNamesByTier(options, "consider"), "暂无需要评估的医疗方案。"),
                renderBulletList(myopiaNamesByTier(options, "notyet"), "暂无。"),
                bulletJoin(cautions));
    }

    /** 框架镜下，每 1.00D 屈光参差引起的两眼视网膜影像大小差异（不等像）近似百分比。 */
    private static final double ANISEIKONIA_PERCENT_PER_DIOPTER = 1.5;
    /** 一般人群对两眼影像大小差异（不等像）的大致耐受上限（百分比）。 */
    private static final double ANISEIKONIA_TOLERANCE_PERCENT = 5;

    @Tool(description = "屈光参差评估：输入左右眼球镜（及可选柱镜）度数，按等效球镜之差评估两眼屈光参差程度"
            + "（无 / 轻度 / 中度 / 显著），估算框架镜下两眼视网膜影像大小差异（不等像 aniseikonia）与耐受边界，"
            + "识别「一眼近视一眼远视」的混合性参差和柱镜差异过大等特殊情况，并给出框架镜 vs 隐形眼镜、逐步适应、"
            + "儿童弱视预警等建议。仅供科普，具体以专业视光 / 眼科评估为准。")
    public String anisometropiaGuide(
            @ToolParam(description = "右眼(OD)球镜度数，单位D，近视填负数、远视填正数，如 -1.00。") double rightSph,
            @ToolParam(description = "左眼(OS)球镜度数，单位D，近视填负数、远视填正数，如 -3.50。") double leftSph,
            @ToolParam(required = false, description = "右眼(OD)柱镜度数，单位D，负散光记法填负数，无散光可不填（默认0）。")
            Double rightCyl,
            @ToolParam(required = false, description = "左眼(OS)柱镜度数，单位D，负散光记法填负数，无散光可不填（默认0）。")
            Double leftCyl) {

        checkRange("right_sph", rightSph, -30, 30);
        checkRange("left_sph", leftSph, -30, 30);
        double rCyl = rightCyl == null ? 0 : rightCyl;
        double lCyl = leftCyl == null ? 0 : leftCyl;
        if (rightCyl != null) {
            checkRange("right_cyl", rCyl, -10, 10);
        }
        if (leftCyl != null) {
            checkRange("left_cyl", lCyl, -10, 10);
        }

        double rightSE = rightSph + rCyl / 2;
        double leftSE = leftSph + lCyl / 2;
        double seDiff = Math.abs(rightSE - leftSE);
        double sphDiff = Math.abs(rightSph - leftSph);
        double cylDiff = Math.abs(rCyl - lCyl);

        String level = anisometropiaLevel(seDiff);
        double imageDiffPercent = seDiff * ANISEIKONIA_PERCENT_PER_DIOPTER;

        boolean antimetropia =
                (rightSE <= -0.5 && leftSE >= 0.5) || (rightSE >= 0.5 && leftSE <= -0.5);

        String advice = switch (level) {
            case "无明显参差" -> "两眼度数接近，屈光参差不明显，按验光度数配框架镜通常没有额外适应问题。";
            case "轻度屈光参差" -> "轻度参差，绝大多数人配框架镜可以正常适应，初期偶有轻微不适；"
                    + "建议左右镜片选同一系列 / 折射率，一次配齐、连续佩戴适应。";
            case "中度屈光参差" -> "中度参差，多数人可逐步适应框架镜，但初期可能出现头晕、走路踩空感、立体感变化；"
                    + "建议连续佩戴适应，对影像不等敏感者可考虑隐形眼镜（镜眼距贴近角膜，两眼放大差异更小）。";
            default -> "显著参差，框架镜下两眼影像不等明显，容易头晕、融像困难，"
                    + "建议优先考虑隐形眼镜，或到专业机构做双眼视功能与屈光手术评估。";
        };

        List<String> notes = new ArrayList<>();
        if (antimetropia) {
            notes.add("属于「混合性屈光参差」（一眼偏近视、一眼偏远视），两眼调节需求方向相反，框架镜适应通常更难，更建议专业验配与现场试戴。");
        }
        if (cylDiff >= 1.5) {
            notes.add("两眼柱镜相差约 " + formatDiopter(cylDiff)
                    + "，除放大差异外还可能带来子午线方向上的影像倾斜 / 畸变，配镜后务必现场试戴确认清晰度与舒适度。");
        }
        if (imageDiffPercent > ANISEIKONIA_TOLERANCE_PERCENT) {
            notes.add("估算两眼影像大小差异约 " + format1Trim(imageDiffPercent)
                    + "%，已超过一般耐受上限（约 " + trimNumber(ANISEIKONIA_TOLERANCE_PERCENT)
                    + "%），框架镜眩晕 / 融像困难的风险较高。");
        }
        if (seDiff >= 2) {
            notes.add("儿童若存在中度以上屈光参差，是弱视和双眼视异常的高危因素，须尽早到眼科 / 视光机构检查，不要仅凭本工具判断。");
        }

        return """
                ## 屈光参差评估

                > 屈光参差指两眼屈光度数不一致。以「等效球镜（SE = 球镜 + 柱镜÷2）」之差衡量：差异越大，框架镜下两眼视网膜影像大小差异（不等像 aniseikonia）越明显，越容易头晕、融像困难。以下为科普估算，实际以专业验光和试戴为准。

                **双眼度数**
                - 右眼 OD：%s（等效球镜 %s）
                - 左眼 OS：%s（等效球镜 %s）

                **参差分析**
                - 等效球镜差：%s → **%s**
                - 球镜差：%s；柱镜差：%s
                - 框架镜下估算影像大小差异：约 %s%%（一般耐受上限约 %s%%）

                **建议**
                - %s

                **特别提示**
                %s

                **提醒**
                - 影像差异百分比为经验估算（约每 1.00D 参差对应 %s%% 放大差异），实际还取决于镜片基弯、镜眼距和验配方式。
                - 隐形眼镜贴近角膜、镜眼距更小，通常能显著减小两眼放大差异，是较大屈光参差的常见方案，但需专业验配。
                - 本工具只做科普参考，不替代医生诊断。""".formatted(
                renderPrescriptionLine(rightSph, rCyl, null),
                formatSignedDiopter(rightSE),
                renderPrescriptionLine(leftSph, lCyl, null),
                formatSignedDiopter(leftSE),
                formatDiopter(seDiff),
                level,
                formatDiopter(sphDiff),
                formatDiopter(cylDiff),
                format1Trim(imageDiffPercent),
                trimNumber(ANISEIKONIA_TOLERANCE_PERCENT),
                advice,
                renderBulletList(notes, "未发现额外的特殊风险；仍建议以专业验光和现场试戴结果为准。"),
                format1Trim(ANISEIKONIA_PERCENT_PER_DIOPTER));
    }

    private static String anisometropiaLevel(double seDiff) {
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
    private static final double CONTACT_LENS_DEFAULT_VERTEX_MM = 12;
    /** 超过该光度（|D|），框架镜与隐形眼镜的度数差异达到需要补偿的量级（约 0.25D 起）。 */
    private static final double CONTACT_LENS_SIGNIFICANT_D = 4;
    /** 隐形眼镜常见的光度步进（D）。 */
    private static final double CONTACT_LENS_STEP_D = 0.25;

    /** 近附加（ADD）常见的度数步进（D）。 */
    private static final double READING_ADD_STEP_D = 0.25;
    /** 近附加的参考工作距离（cm）：年龄经验表按此距离标定。 */
    private static final double READING_ADD_REFERENCE_CM = 40;
    /** 近附加的常见上限（D），超过后单副老花镜的清晰景深过窄，一般不再加大。 */
    private static final double READING_ADD_MAX_D = 3.5;
    /** 老视一般开始出现的年龄（岁）。 */
    private static final int PRESBYOPIA_ONSET_AGE = 40;

    @Tool(description = "框架镜度数换算隐形眼镜度数：按镜眼距（顶点距离，默认 12mm）做顶点补偿，把框架镜球镜"
            + "（及可选柱镜）换算成贴近角膜的隐形眼镜等效光度，并按隐形常见的 0.25D 步进取整。说明为何高度数"
            + "（约 ±4.00D 以上）必须补偿、低度数可直接沿用，散光可否折算等效球镜配普通球镜片，并提醒隐形还需"
            + "基弧 / 直径 / 试戴等专业验配。仅供科普参考，不替代验光师验配。")
    public String contactLensPower(
            @ToolParam(description = "框架镜球镜度数，单位D，近视填负数、远视填正数，如 -6.00。") double sph,
            @ToolParam(required = false, description = "框架镜柱镜度数，单位D，负散光记法填负数，无散光可不填（默认0）。")
            Double cyl,
            @ToolParam(required = false, description = "镜眼距（框架镜后顶点到角膜的距离），单位mm，常见 10-14，默认 12。")
            Double vertexDistanceMm) {

        checkRange("sph", sph, -30, 30);
        double c = cyl == null ? 0 : cyl;
        if (cyl != null) {
            checkRange("cyl", c, -10, 10);
        }
        double vertexMm = vertexDistanceMm == null ? CONTACT_LENS_DEFAULT_VERTEX_MM : vertexDistanceMm;
        if (vertexDistanceMm != null) {
            checkRange("vertex_distance_mm", vertexMm, 5, 20);
        }
        double d = vertexMm / 1000;

        // 分别换算球镜子午线与「球镜+柱镜」子午线，再按步进取整重组球柱镜。
        double sphExact = vertexCompensate(sph, d);
        double cylMeridianExact = vertexCompensate(sph + c, d);
        double contactSph = roundToContactStep(sphExact);
        double contactCylMeridian = roundToContactStep(cylMeridianExact);
        double contactCyl = c == 0 ? 0 : contactCylMeridian - contactSph;

        double strongestPower = Math.max(Math.abs(sph), Math.abs(sph + c));
        boolean significant = strongestPower >= CONTACT_LENS_SIGNIFICANT_D;
        double sphShift = Math.abs(sphExact - sph);
        double contactSE = roundToContactStep(vertexCompensate(sph + c / 2, d));

        List<String> notes = new ArrayList<>();
        if (significant) {
            notes.add("框架镜最强子午线约 " + formatDiopter(strongestPower)
                    + "，已达到需要顶点补偿的量级："
                    + (sph < 0 ? "近视换算成隐形后度数会变浅" : "远视换算成隐形后度数会变深")
                    + "，补偿量约 " + formatDiopter(sphShift) + "（球镜子午线）。直接照搬框架镜度数会"
                    + (sph < 0 ? "过矫" : "欠矫") + "。");
        } else {
            notes.add("框架镜最强子午线约 " + formatDiopter(strongestPower)
                    + "，未超过约 " + formatDiopter(CONTACT_LENS_SIGNIFICANT_D)
                    + "，顶点补偿量很小（不足半档），隐形眼镜通常可直接按框架镜度数选配。");
        }
        if (c != 0) {
            notes.add("含散光：换算后柱镜约 " + formatSignedDiopter(contactCyl)
                    + "。散光隐形（Toric / 散光片）度数、轴位步进有限，验配更复杂；"
                    + "低散光（约 ≤0.75D）常折算成等效球镜配普通球镜片——等效球镜隐形约为 "
                    + formatSignedDiopter(contactSE) + "。");
        }

        String seLine = c != 0
                ? "\n- 折算等效球镜（低散光配球镜片时）：约 " + formatSignedDiopter(contactSE)
                : "";

        return """
                ## 隐形眼镜度数换算（顶点距离补偿）

                > 框架镜离眼约 %smm，隐形眼镜贴在角膜上，同一屈光需求所需的镜片光度并不相同。度数越高，差异越大。换算公式：F_隐形 = F_框架 ÷ (1 − d × F_框架)，d 为镜眼距（米）。以下为科普估算，实际以专业验配为准。

                **输入（框架镜）**
                - 处方：%s（%s）
                - 镜眼距：%s mm

                **换算结果（隐形眼镜，按 %sD 步进取整）**
                - 隐形眼镜光度：**%s**%s

                **说明**
                %s

                **提醒**
                - 隐形眼镜验配除光度外还需确定基弧（BC）、直径（DIA）、品牌和现场试戴，本工具只做光度换算。
                - 首次配戴或更换品牌请到专业机构验配，并规范护理、控制配戴时长，出现红痛畏光要及时停戴就医。
                - 本工具只做科普参考，不替代验光师 / 医生。""".formatted(
                trimNumber(vertexMm),
                renderPrescriptionLine(sph, c, null),
                describeEye(sph, c),
                trimNumber(vertexMm),
                trimNumber(CONTACT_LENS_STEP_D),
                renderPrescriptionLine(contactSph, contactCyl, null),
                seLine,
                bulletJoin(notes));
    }

    @Tool(description = "老花（近附加 ADD）度数估算：随年龄增长调节力下降，40 岁后看近逐渐吃力。按年龄给出典型近附加"
            + "（下加光）度数，并按实际工作距离（默认 40cm）做增减，可选传入看远球镜度数以算出「看近总度数 = "
            + "看远度数 + ADD」。说明老视机理、老花镜 / 渐进 / 办公镜片的选择，并按 0.25D 步进取整。"
            + "仅供科普参考，最终度数以主觉验光和试戴为准。")
    public String readingAddEstimator(
            @ToolParam(description = "年龄（岁），整数。老视一般 40 岁后逐渐出现。") int age,
            @ToolParam(required = false, description = "主要用眼（看近）距离，单位cm，常见看书 33-40、电脑 50-70，默认 40。")
            Double workingDistanceCm,
            @ToolParam(required = false, description = "看远球镜度数，单位D，近视填负数、远视填正数（如 -2.00）；"
                    + "不填则只给近附加 ADD，不算看近总度数。")
            Double distanceSph) {

        checkRange("age", age, 1, 120);
        double workingCm = workingDistanceCm == null ? READING_ADD_REFERENCE_CM : workingDistanceCm;
        if (workingDistanceCm != null) {
            checkRange("working_distance_cm", workingCm, 20, 200);
        }
        if (distanceSph != null) {
            checkRange("distance_sph", distanceSph, -30, 30);
        }

        double baseAdd = ageBaseAdd(age);
        // 工作距离修正：相对 40cm 参考距离的调节需求差（1/距离，单位 D）。越近需求越大 → 需要更多下加光。
        double distanceAdjust = baseAdd > 0 ? 100 / workingCm - 100 / READING_ADD_REFERENCE_CM : 0;
        double rawAdd = Math.min(Math.max(baseAdd + distanceAdjust, 0), READING_ADD_MAX_D);
        double add = roundToReadingStep(rawAdd);

        List<String> notes = new ArrayList<>();
        if (age < PRESBYOPIA_ONSET_AGE) {
            notes.add(age + " 岁一般还有充足的调节力，通常不需要近附加。若此年龄已明显看近吃力，"
                    + "多与远视、调节功能异常或用眼疲劳有关，建议先做主觉验光和调节功能检查，而非直接配老花镜。");
        } else {
            notes.add("近附加度数由验光师用「先给暂定下加光、再用交叉圆柱镜 / 红绿视标微调」确定，"
                    + "本工具只按年龄和距离给经验估算，真实值可能相差约 ±0.25~0.50D。");
            notes.add("配镜原则是「够用即可、留有余量」——在能看清目标距离的前提下选偏低的下加光，"
                    + "可保留更宽的清晰景深，不要盲目追高。左右眼近附加通常相同。");
        }

        if (workingCm <= 33 && baseAdd > 0) {
            notes.add("工作距离约 " + trimNumber(workingCm) + "cm，比参考的 40cm 更近，已相应加大下加光；"
                    + "长时间超近距离用眼更累，注意间歇休息。");
        } else if (workingCm >= 60 && baseAdd > 0) {
            notes.add("工作距离约 " + trimNumber(workingCm) + "cm 偏远（如台式电脑 / 乐谱），下加光相应减小；"
                    + "若既要看电脑又要看更近的纸面，单一下加光难以兼顾，可考虑渐进或办公（中近）镜片。");
        }

        if (add >= READING_ADD_MAX_D) {
            notes.add("估算下加光已达上限约 " + formatDiopter(READING_ADD_MAX_D)
                    + "；单副老花镜再加大清晰范围会更窄，若一副难以兼顾各距离，建议渐进多焦点或分距离配镜。");
        }

        // 看近总度数（每眼）：看远球镜 + 近附加。
        String nearTotalLine = "";
        if (distanceSph != null) {
            double nearTotal = distanceSph + add;
            nearTotalLine = "\n- 看近总度数（每眼球镜）：看远 " + formatSignedDiopter(distanceSph)
                    + " + 下加光 " + formatSignedDiopter(add) + " = **" + formatSignedDiopter(nearTotal) + "**";
            if (distanceSph < 0 && distanceSph + add < 0) {
                notes.add("你看远是近视，加上下加光后看近仍是负度数：不少中低度近视者看近时摘掉眼镜或戴度数更浅的眼镜即可，"
                        + "是否需要单独的老花镜要结合裸眼近视力判断。");
            }
            notes.add("已有看远度数（近视 / 远视 / 散光）者，多数会选择渐进多焦点或双光镜片，"
                    + "把看远与看近合到一副，避免频繁换镜。");
        } else {
            notes.add("如需算「看近总度数」，请一并提供看远球镜度数（distance_sph）；"
                    + "只有老花、看远正常者，看近总度数就等于下加光本身。");
        }

        String addDisplay = add > 0
                ? "**约 +" + trimNumber(add) + "D**"
                : "**+0.00D（暂不需要）**";
        String distanceLine = distanceSph != null
                ? "\n- 看远球镜：" + formatSignedDiopter(distanceSph)
                : "";

        return """
                ## 老花（近附加 ADD）度数估算

                > 随年龄增长，晶状体调节力逐渐下降，约 40 岁起看近费力、易疲劳，这就是老视（老花）。矫正靠在看远度数上叠加一份「近附加 / 下加光（ADD）」。下加光随年龄增大、约 60 岁后趋于稳定（一般不超过约 %sD），并与实际用眼距离有关。以下为科普估算，实际以主觉验光和试戴为准。

                **输入**
                - 年龄：%d 岁
                - 主要用眼距离：%s cm（参考距离 %s cm）%s

                **估算结果**
                - 建议近附加（下加光 ADD）：%s%s

                **说明**
                %s

                **提醒**
                - 下加光是双眼看近的叠加度数，需与看远度数、瞳距（近用瞳距会略小）、镜片类型一起确定。
                - 突然、单眼或快速加重的看近困难，或伴随头痛、视物变形，应先就医排查，而非仅配老花镜。
                - 本工具只做科普参考，不替代验光师 / 医生。""".formatted(
                trimNumber(READING_ADD_MAX_D),
                age,
                trimNumber(workingCm),
                trimNumber(READING_ADD_REFERENCE_CM),
                distanceLine,
                addDisplay,
                nearTotalLine,
                bulletJoin(notes));
    }

    @Tool(description = "散光记法转换（柱镜转换）：同一副镜片可以用「负柱镜」或「正柱镜」两种等价写法表示，"
            + "验光单在验光师、医院、不同软件之间流转时常需要互换。按标准公式换算：新球镜 = 原球镜 + 原柱镜，"
            + "新柱镜 = −原柱镜，新轴位 = 原轴位 ± 90°（落在 1–180° 内）。等效球镜（SPH + CYL/2）在转换前后"
            + "保持不变，可用于自检。仅做记法换算与科普，不改变镜片本身，也不替代验光。")
    public String prescriptionTranspose(
            @ToolParam(description = "球镜度数 SPH，单位D，近视填负数、远视填正数，如 -2.00。") double sph,
            @ToolParam(description = "柱镜度数 CYL，单位D。负散光记法填负数（如 -0.75），正散光记法填正数；"
                    + "填 0 表示无散光（无需转换）。") double cyl,
            @ToolParam(required = false, description = "散光轴位 AXIS，0-180 度的整数。有散光（cyl≠0）时必填；无散光时不要填。")
            Integer axis) {

        checkRange("sph", sph, -30, 30);
        checkRange("cyl", cyl, -10, 10);
        if (axis != null) {
            checkRange("axis", axis, 0, 180);
        }
        validateAxis("处方", cyl, axis);

        // 等效球镜是转换前后的不变量，可用于自检。
        double se = sph + cyl / 2;

        if (cyl == 0) {
            return """
                    ## 散光记法转换（柱镜转换）

                    > 「负柱镜」与「正柱镜」是同一副镜片的两种等价写法。转换只是换个记法，镜片本身不变。

                    **输入**
                    - 处方：%s（%s）

                    **结果**
                    - 该处方柱镜为 0（无散光），只有球镜，不存在正 / 负柱镜之分，无需转换。
                    - 等效球镜：%s

                    **提醒**
                    - 本工具只做记法换算与科普，不改变镜片本身，也不替代验光。""".formatted(
                    renderPrescriptionLine(sph, cyl, null),
                    describeEye(sph, cyl),
                    formatSignedDiopter(se));
        }

        String currentForm = cyl < 0 ? "负柱镜（负散光）" : "正柱镜（正散光）";
        String targetForm = cyl < 0 ? "正柱镜（正散光）" : "负柱镜（负散光）";

        double newSph = sph + cyl;
        double newCyl = -cyl;
        int newAxis = transposeAxis(axis);

        return """
                ## 散光记法转换（柱镜转换）

                > 同一副镜片可以用「负柱镜」或「正柱镜」两种等价写法表示，两者矫正效果完全相同。验光单在验光师、医院、镜片加工软件之间流转时，常需要在两种记法间互换。转换公式：新球镜 = 原球镜 + 原柱镜，新柱镜 = −原柱镜，新轴位 = 原轴位 ± 90°（归一化到 1–180°）。

                **输入（%s）**
                - 处方：%s（%s）

                **转换结果（%s）**
                - 处方：**%s**

                **换算过程**
                - 新球镜 = %s + (%s) = %s
                - 新柱镜 = −(%s) = %s
                - 新轴位 = %d° %s 90° = %d°

                **自检**
                - 等效球镜（SPH + CYL/2）转换前后不变，均为 %s，可据此核对换算是否正确。

                **说明**
                - 两种写法描述的是完全相同的镜片，光学效果一致，不存在「哪种度数更好」。
                - 临床习惯：验光 / 眼镜行业多用负柱镜记法，部分眼科医生和早期设备用正柱镜记法。
                - 轴位相差 90° 是转换的固有结果，不是错误；报读处方时务必带上轴位，避免歧义。

                **提醒**
                - 本工具只做记法换算与科普，不改变镜片本身，也不替代验光。""".formatted(
                currentForm,
                renderPrescriptionLine(sph, cyl, axis),
                describeEye(sph, cyl),
                targetForm,
                renderPrescriptionLine(newSph, newCyl, newAxis),
                formatSignedDiopter(sph),
                formatSignedDiopter(cyl),
                formatSignedDiopter(newSph),
                formatSignedDiopter(cyl),
                formatSignedDiopter(newCyl),
                axis,
                axis + 90 > 180 ? "−" : "+",
                newAxis,
                formatSignedDiopter(se));
    }

    @Tool(description = "镜架尺寸适配（光心偏移评估）：镜架规格常写成「镜圈宽 A□鼻梁 DBL-镜腿」（如 52□18-140）。"
            + "按盒式标注法，镜架几何中心距 = 镜圈宽 + 鼻梁，它与瞳距（PD）之差决定加工时每片光心要移多少。"
            + "给定镜圈宽度、鼻梁宽度和瞳距，算出几何中心距、每片移心量与方向、粗略正面宽度，并按移心量评估镜架是否贴合瞳距；"
            + "若提供度数，用 Prentice 公式估算「若不移心」会产生的水平棱镜。仅做科普估算，实际以视光师现场测量为准。")
    public String frameFitCalculator(
            @ToolParam(description = "镜圈宽度（镜片水平宽度 A / eye size），单位mm，常见 48-58，如 52。") double lensWidth,
            @ToolParam(description = "鼻梁宽度（两镜片间距 DBL / bridge），单位mm，常见 14-22，如 18。") double bridge,
            @ToolParam(description = "佩戴者双眼远用瞳距 PD，单位mm，成人常见约 54-74，如 62。") double pd,
            @ToolParam(required = false, description = "单眼球镜（或等效球镜）度数，单位D，近视填负数、远视填正数（如 -4.00）；"
                    + "用于估算「不移心」会引入的水平棱镜，不填则跳过。")
            Double power,
            @ToolParam(required = false, description = "镜腿长度，单位mm，常见 135-150，仅用于展示规格，不参与计算。")
            Double templeLength) {

        checkRange("lens_width", lensWidth, 30, 70);
        checkRange("bridge", bridge, 10, 30);
        checkRange("pd", pd, 40, 85);
        if (power != null) {
            checkRange("power", power, -30, 30);
        }
        if (templeLength != null) {
            checkRange("temple_length", templeLength, 100, 160);
        }

        // 盒式标注法：镜架几何中心距 = 镜圈宽度 + 鼻梁宽度。
        double framePd = lensWidth + bridge;
        // 每片光心相对镜架几何中心的移心量；正数=向鼻侧内移，负数=向颞侧外移。
        double decentration = (framePd - pd) / 2;
        double absDec = Math.abs(decentration);
        // 正面宽度粗估（不含铰链/镜腿外张），仅作参考。
        double frontWidth = 2 * lensWidth + bridge;

        String rating = frameFitRating(absDec);
        String directionWord = decentration > 0.05 ? "向鼻侧内移"
                : decentration < -0.05 ? "向颞侧外移" : "几乎无需移心";

        List<String> notes = new ArrayList<>();
        if (absDec < 2) {
            notes.add("镜架几何中心距与你的瞳距很接近，光心基本落在瞳孔正前方，几乎不用移心，加工最理想。");
        } else if (absDec < 4) {
            notes.add("需要常规量级的移心，正规加工可轻松处理，对中低度数几乎没有影响。");
        } else if (absDec < 6) {
            notes.add("移心量偏大：高度数（约 ±4.00D 以上）会使镜片一侧明显偏厚、可能需要更大的镜片毛坯，"
                    + "建议优先挑选规格更贴合瞳距的镜架。");
        } else {
            notes.add("移心量过大：镜架规格与你的瞳距明显不匹配，除非特别中意，否则建议换一副几何中心距更接近瞳距的镜架，"
                    + "以免镜片偏厚、外观与光学效果打折。");
        }

        if (decentration > 0.05) {
            notes.add("镜架几何中心距 " + mm(framePd) + "mm 大于瞳距 " + mm(pd) + "mm（镜架偏宽），"
                    + "光心需" + directionWord + "每片 " + mm(absDec) + "mm。");
        } else if (decentration < -0.05) {
            notes.add("镜架几何中心距 " + mm(framePd) + "mm 小于瞳距 " + mm(pd) + "mm（镜架偏窄），"
                    + "光心需" + directionWord + "每片 " + mm(absDec) + "mm；外移更依赖大毛坯，度数高时尤其注意。");
        } else {
            notes.add("镜架几何中心距 " + mm(framePd) + "mm 与瞳距 " + mm(pd) + "mm 基本一致，光心几乎正对瞳孔。");
        }

        String prismLine = "";
        if (power != null) {
            // Prentice 公式：棱镜量(Δ) = 移心量(cm) × 光度(D)。
            double perEyePrism = (absDec / 10) * Math.abs(power);
            prismLine = "\n- 度数 " + formatSignedDiopter(power) + " 时，若把光心留在镜架几何中心而不移心，"
                    + "每片约产生 **" + trimNumber(round2(perEyePrism)) + "Δ** 水平棱镜"
                    + "（Prentice：移心 " + mm(absDec) + "mm × " + trimNumber(Math.abs(power)) + "D）。";
            if (perEyePrism < 0.5) {
                notes.add("该棱镜量很小，即使不刻意移心通常也可忽略；但正规加工仍会把光心对准瞳孔。");
            } else {
                notes.add("该棱镜量已不可忽略：正规加工会把光心移到瞳孔位置消除它，"
                        + "但移心越大所需毛坯越大、镜片边缘厚度越不对称。");
            }
        }

        String templeLine = templeLength != null ? "\n- 镜腿长度：" + mm(templeLength) + " mm" : "";

        return """
                ## 镜架尺寸适配（光心偏移评估）

                > 镜架规格常写成「镜圈宽 A□鼻梁 DBL-镜腿」（例如 52□18-140）。按盒式标注法，**镜架几何中心距 = 镜圈宽 + 鼻梁**，它与你的瞳距（PD）之差，决定了加工时每片镜片的光心要向内或向外移多少（移心量 = (几何中心距 − 瞳距) ÷ 2）。几何中心距越接近瞳距，光心越正对瞳孔，加工越理想。

                **输入**
                - 镜圈宽度 A：%s mm
                - 鼻梁宽度 DBL：%s mm
                - 双眼瞳距 PD：%s mm%s

                **测算结果**
                - 镜架几何中心距（框 PD）：**%s mm**（镜圈宽 %s + 鼻梁 %s）
                - 每片移心量：**%s mm**（%s）
                - 正面宽度（粗估，不含镜腿外张）：约 %s mm
                - 贴合评估：**%s**%s

                **说明**
                %s

                **提醒**
                - 移心量只反映镜架规格与瞳距的匹配度，不代表镜架戴在脸上是否舒适；镜圈弧度、鼻托、镜腿宽度与脸宽也很重要。
                - 高度数、渐进 / 多焦点镜片对光心（含瞳高）定位更敏感，务必以视光师现场测量为准。
                - 本工具只做科普估算，不替代专业验配。""".formatted(
                mm(lensWidth),
                mm(bridge),
                mm(pd),
                templeLine,
                mm(framePd),
                mm(lensWidth),
                mm(bridge),
                mm(absDec),
                directionWord,
                mm(frontWidth),
                rating,
                prismLine,
                bulletJoin(notes));
    }

    /** 按每片移心量（mm）评估镜架与瞳距的贴合度。 */
    private static String frameFitRating(double absDec) {
        if (absDec < 2) {
            return "很合适";
        }
        if (absDec < 4) {
            return "合适";
        }
        if (absDec < 6) {
            return "偏大";
        }
        return "明显偏大";
    }

    /** 毫米数值展示：保留一位小数并去掉尾零，与 TS 版本 mm() 对齐。 */
    private static String mm(double value) {
        return format1Trim(value);
    }

    /** 四舍五入到两位小数。 */
    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** 轴位转换：转换柱镜正负号时，轴位旋转 90°，并归一化到 (0, 180]。 */
    private static int transposeAxis(int axis) {
        int rotated = axis + 90;
        return rotated > 180 ? rotated - 180 : rotated;
    }

    /** 按年龄给出 40cm 参考距离下的典型近附加（下加光）度数（D）。 */
    private static double ageBaseAdd(int age) {
        if (age < PRESBYOPIA_ONSET_AGE) {
            return 0;
        }
        if (age <= 41) {
            return 1.0;
        }
        if (age <= 44) {
            return 1.25;
        }
        if (age <= 47) {
            return 1.5;
        }
        if (age <= 49) {
            return 1.75;
        }
        if (age <= 52) {
            return 2.0;
        }
        if (age <= 55) {
            return 2.25;
        }
        return 2.5;
    }

    /** 按 0.25D 步进取整。 */
    private static double roundToReadingStep(double value) {
        return Math.round(value / READING_ADD_STEP_D) * READING_ADD_STEP_D;
    }

    /** 顶点距离补偿：把框架镜某子午线光度换算为贴近角膜（隐形眼镜）的等效光度。d 为米。 */
    private static double vertexCompensate(double power, double distanceMeters) {
        return power / (1 - distanceMeters * power);
    }

    /** 按隐形眼镜常见步进（0.25D）取整。 */
    private static double roundToContactStep(double value) {
        return Math.round(value / CONTACT_LENS_STEP_D) * CONTACT_LENS_STEP_D;
    }

    /** 按当前球镜度数给出近视程度描述（负数越小度数越高）。 */
    private static String myopiaDegreeLabel(double sph) {
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

    /** 取指定分级的方案名称，用于生成分组清单。 */
    private static List<String> myopiaNamesByTier(List<MyopiaOption> options, String tier) {
        List<String> names = new ArrayList<>();
        for (MyopiaOption o : options) {
            if (o.tier().equals(tier)) {
                names.add(o.name());
            }
        }
        return names;
    }

    /** 取指定分级的功能名称，去掉名称里括号内的补充说明，用于生成购物清单。 */
    private static List<String> coatingNamesByTier(List<CoatingRec> recs, String tier) {
        List<String> names = new ArrayList<>();
        for (CoatingRec r : recs) {
            if (r.tier().equals(tier)) {
                names.add(r.name().replaceAll("（.*）$", "").trim());
            }
        }
        return names;
    }

    private static String pdRangeNote(double pd) {
        if (pd < 54) {
            return "低于成人常见范围（约 54-74mm），如果不是儿童或小脸型，请重新测量确认。";
        }
        if (pd > 74) {
            return "高于成人常见范围（约 54-74mm），请重新测量确认，避免加工时光心定位偏差。";
        }
        return "落在成人常见范围（约 54-74mm）内。";
    }

    /** 薄透镜矢高近似：估算镜片最厚处（近视看边缘、远视看中心）的毫米厚度。 */
    private static double estimateThickest(double power, double refractiveIndex, double effectiveDiameter, boolean isPlus) {
        double r = effectiveDiameter / 2;
        double sag = (power * r * r) / (2000 * (refractiveIndex - 1));
        double base = isPlus ? 1.0 : 1.2;
        return base + sag;
    }

    /** 与 lensRecommendation 相同的功率阈值，给出性价比最优的折射率。 */
    private static String recommendedIndex(double power) {
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

    private static String thicknessRating(double thickest) {
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

    private static String format1(double value) {
        return String.format("%.1f", value);
    }

    /** 保留一位小数后去掉多余的尾零，与 TS 版本 trimTrailingZeros(x.toFixed(1)) 对齐。 */
    private static String format1Trim(double value) {
        return Diopters.trimTrailingZeros(String.format("%.1f", value));
    }

    private static String encodeKeyword(String keyword) {
        return URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    }

    private static String jdSearchUrl(String keyword) {
        return "https://search.jd.com/Search?keyword=" + encodeKeyword(keyword) + "&enc=utf-8";
    }

    private static String taobaoSearchUrl(String keyword) {
        return "https://s.taobao.com/search?q=" + encodeKeyword(keyword);
    }

    private static String pddSearchUrl(String keyword) {
        return "https://mobile.yangkeduo.com/search_result.html?search_key=" + encodeKeyword(keyword);
    }

    // ---------------------------------------------------------------------
    // 参数校验帮助函数（等价于 TS 版本的 expect*/validate*）
    // ---------------------------------------------------------------------
    private static String expectEnum(String key, String value, String... allowed) {
        if (value != null) {
            for (String option : allowed) {
                if (option.equals(value)) {
                    return value;
                }
            }
        }
        throw new IllegalArgumentException("参数 " + key + " 必须是以下值之一：" + String.join(", ", allowed));
    }

    private static void checkRange(String key, double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("参数 " + key + " 必须是数字");
        }
        if (value < min) {
            throw new IllegalArgumentException("参数 " + key + " 不能小于 " + trimNumber(min));
        }
        if (value > max) {
            throw new IllegalArgumentException("参数 " + key + " 不能大于 " + trimNumber(max));
        }
    }

    private static void validateAxis(String label, double cyl, Integer axis) {
        if (cyl != 0 && axis == null) {
            throw new IllegalArgumentException(label + "有散光时必须提供轴位");
        }
        if (cyl == 0 && axis != null) {
            throw new IllegalArgumentException(label + "未填写散光时不应单独提供轴位");
        }
    }

    private static String bulletJoin(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append("- ").append(items.get(i));
        }
        return sb.toString();
    }

    private static String trimNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return Diopters.trimTrailingZeros(String.format("%.2f", value));
    }
}
