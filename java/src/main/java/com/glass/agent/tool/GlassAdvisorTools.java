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
 * <p>逻辑与原 TypeScript 版本 {@code src/index.ts} 中的 9 个 handler 一一对应。
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
