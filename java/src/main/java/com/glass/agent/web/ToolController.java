package com.glass.agent.web;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.glass.agent.tool.GlassAdvisorTools;
import com.glass.agent.tool.ToolCallHistory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 直接调用工具的 REST 接口，不经过大模型。适合前端表单调试或程序化集成，
 * 对应原 TypeScript 版本的本地 Web 调试页能力。
 */
@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final GlassAdvisorTools tools;
    private final ToolCallHistory history;

    public ToolController(GlassAdvisorTools tools, ToolCallHistory history) {
        this.tools = tools;
        this.history = history;
    }

    /** 执行工具并记录调用历史；工具抛出的校验异常照常向上抛给全局处理器（同时记为失败）。 */
    private ToolResponse run(String name, Object arguments, Supplier<String> call) {
        try {
            String text = call.get();
            history.record(name, arguments, text, false);
            return ToolResponse.of(text);
        } catch (RuntimeException ex) {
            history.record(name, arguments, "错误：" + ex.getMessage(), true);
            throw ex;
        }
    }

    /** 列出全部工具及其说明，便于客户端发现。 */
    @GetMapping
    public List<Map<String, String>> list() {
        return List.of(
                tool("vision_check_guide", "视力检查指南：按年龄段说明检查频率、重点项目、检查前准备和常见关注点。"),
                tool("lens_recommendation", "镜片推荐：根据度数、散光、用途和预算给出折射率、材质、镀膜和选购建议。"),
                tool("frame_selection_guide", "镜框选择指南：结合脸型、生活方式和度数深浅推荐框型、材质和尺寸思路。"),
                tool("prescription_interpreter", "验光单解读：解释 SPH/CYL/AXIS/PD/ADD 的意义，并提示配镜风险点。"),
                tool("progressive_lens_assessment", "渐进镜片适配评估：判断更适合单焦、办公镜还是渐进多焦点镜片。"),
                tool("new_glasses_troubleshooting", "新眼镜不适排查：根据症状、佩戴时长和镜片类型判断是适应期还是需要复查。"),
                tool("shopping_links", "购物链接生成：把配镜建议转成京东/淘宝/拼多多的商品搜索购买链接。"),
                tool("lens_thickness_estimator", "镜片厚度估算：按度数、折射率和镜圈宽度估算镜片最厚处的厚度与重量倾向，并判断是否值得提高折射率减薄。"),
                tool("pupillary_distance_guide", "瞳距（PD）助手：校验瞳距、由单眼/双眼互算、按工作距离折算近用瞳距，并提示左右不对称与自测方法。"),
                tool("lens_coating_advisor", "镜片镀膜与功能顾问：按用眼场景逐项判断减反射、UV、防蓝光、变色片、偏振太阳镜值不值得多花钱，并给出购物清单。"),
                tool("myopia_control_guide", "青少年近视防控指南：按年龄、度数、进展速度、遗传与户外情况评估近视进展风险，并排序给出户外、用眼习惯、离焦框架镜、OK 镜、低浓度阿托品等方案。"),
                tool("anisometropia_guide", "屈光参差评估：按左右眼等效球镜之差评估屈光参差程度，估算框架镜下不等像与耐受边界，识别混合性参差和柱镜差异过大，给出框架镜 vs 隐形眼镜等建议。"),
                tool("contact_lens_power", "隐形眼镜度数换算：按镜眼距（顶点距离）把框架镜球镜/柱镜换算成贴近角膜的隐形眼镜光度并按 0.25D 取整，说明高度数为何要补偿、散光可否折算等效球镜。"),
                tool("reading_add_estimator", "老花（近附加 ADD）度数估算：按年龄给出典型近附加度数，按工作距离增减，并可结合看远球镜算出看近总度数，说明老花镜/渐进/办公镜片的选择。"),
                tool("prescription_transpose", "散光记法转换（柱镜转换）：在负柱镜与正柱镜两种等价写法之间互换（新球镜=原球镜+原柱镜，新柱镜=−原柱镜，新轴位=原轴位±90°），并用等效球镜不变量自检。"),
                tool("frame_fit_calculator", "镜架尺寸适配（光心偏移评估）：按盒式标注法用镜圈宽+鼻梁算出镜架几何中心距，与瞳距比较得出每片移心量与方向，评估贴合度，并用 Prentice 公式估算不移心时的水平棱镜。"),
                tool("visual_acuity_converter", "视力记录法换算：在小数记录法、五分记录法（对数）、Snellen 分数、logMAR 四种等价写法之间互换，并给出该视力的大致水平（五分 L=5+lg(小数)，logMAR=−lg(小数)）。"),
                tool("accommodation_amplitude", "调节幅度评估：用 Hofstetter 公式按年龄估算最小/平均/最大调节幅度（平均=18.5−0.30×年龄）与调节近点，判断某个用眼距离是否落在「保留一半调节力」的舒适储备之内，并在不足时给出建议近附加。"),
                tool("sunglass_tint_guide", "太阳镜镜片色号（透光率）与颜色选择：按用光环境推荐 ISO 12312-1 的 0–4 类过滤分类与 VLT，结合是否畏光/驾驶/带度数给出镜片颜色（灰/茶棕/墨绿/黄琥珀）、偏光、变色片建议，并提醒镜片深浅≠防紫外线，任何太阳镜都应达到 UV400。"),
                tool("lens_material_advisor", "镜片材料（基材）选择顾问：按度数、镜框类型（全框/半框/无框）、使用场景（日常/儿童/运动/安全防护/驾驶）与取舍偏好，在 CR-39、Trivex、PC、1.60/1.67/1.74 高折射树脂、玻璃之间推荐基材，综合抗冲击、阿贝数、厚度与重量给出首选/备选/不建议材料，与 lens_thickness_estimator、lens_coating_advisor 互补。"),
                tool("clear_vision_range", "裸眼清晰视界估算（远点/近点）：按裸眼屈光度（含柱镜按等效球镜）推算不戴镜能看清的距离范围——近视远点=100÷度数(cm)，远点外看远模糊却能看清近处；可选填年龄按 Hofstetter 平均公式估算调节力算出近点与远视代偿，解释「为什么近视的人摘镜看手机反而清楚」。"),
                tool("near_acuity_converter", "近用视力（近视力表）记法换算：在 M 记法、印刷点数(pt)、近视力小数三种写法间换算，并给出 Snellen/logMAR/五分等效与含 Jaeger 的近视力对照表（小数=测试距离m÷M，点数≈M×8）。"),
                tool("prism_resolver", "棱镜合成与分解（Prentice 向量运算）：combine 把水平(base-in/out)+垂直(base-up/down)分量合成为单一合棱镜（大小√(h²+v²)与方向角），resolve 把合棱镜按方向角分解回水平、垂直分量。"));
    }

    @PostMapping("/vision_check_guide")
    public ToolResponse visionCheckGuide(@RequestBody VisionCheckRequest req) {
        return run("vision_check_guide", req, () -> tools.visionCheckGuide(req.ageGroup(), req.concern()));
    }

    @PostMapping("/lens_recommendation")
    public ToolResponse lensRecommendation(@RequestBody LensRequest req) {
        return run("lens_recommendation", req,
                () -> tools.lensRecommendation(req.sph(), req.cyl(), req.usage(), req.budget()));
    }

    @PostMapping("/frame_selection_guide")
    public ToolResponse frameSelectionGuide(@RequestBody FrameRequest req) {
        return run("frame_selection_guide", req,
                () -> tools.frameSelectionGuide(req.faceShape(), req.lifestyle(), req.prescriptionStrength()));
    }

    @PostMapping("/prescription_interpreter")
    public ToolResponse prescriptionInterpreter(@RequestBody PrescriptionRequest req) {
        return run("prescription_interpreter", req, () -> tools.prescriptionInterpreter(
                req.odSph(), req.odCyl(), req.odAxis(),
                req.osSph(), req.osCyl(), req.osAxis(),
                req.pd(), req.add()));
    }

    @PostMapping("/progressive_lens_assessment")
    public ToolResponse progressiveLensAssessment(@RequestBody ProgressiveRequest req) {
        return run("progressive_lens_assessment", req, () -> tools.progressiveLensAssessment(
                req.age(), req.nearDifficulty(), req.screenHours(), req.driveFrequency(), req.firstTimeUser()));
    }

    @PostMapping("/new_glasses_troubleshooting")
    public ToolResponse newGlassesTroubleshooting(@RequestBody TroubleshootingRequest req) {
        return run("new_glasses_troubleshooting", req, () -> tools.newGlassesTroubleshooting(
                req.symptom(), req.wearDays(), req.lensType(), req.prescriptionChanged()));
    }

    @PostMapping("/shopping_links")
    public ToolResponse shoppingLinks(@RequestBody ShoppingRequest req) {
        return run("shopping_links", req, () -> tools.shoppingLinks(req.keywords()));
    }

    @PostMapping("/lens_thickness_estimator")
    public ToolResponse lensThicknessEstimator(@RequestBody ThicknessRequest req) {
        return run("lens_thickness_estimator", req,
                () -> tools.lensThicknessEstimator(req.sph(), req.cyl(), req.lensIndex(), req.frameWidth()));
    }

    @PostMapping("/pupillary_distance_guide")
    public ToolResponse pupillaryDistanceGuide(@RequestBody PupillaryDistanceRequest req) {
        return run("pupillary_distance_guide", req, () -> tools.pupillaryDistanceGuide(
                req.binocularPd(), req.pdRight(), req.pdLeft(), req.workingDistanceCm()));
    }

    @PostMapping("/lens_coating_advisor")
    public ToolResponse lensCoatingAdvisor(@RequestBody CoatingRequest req) {
        return run("lens_coating_advisor", req, () -> tools.lensCoatingAdvisor(
                req.screenHours(), req.outdoorFrequency(), req.nightDriving(),
                req.lightSensitive(), req.preferOnePair()));
    }

    @PostMapping("/myopia_control_guide")
    public ToolResponse myopiaControlGuide(@RequestBody MyopiaControlRequest req) {
        return run("myopia_control_guide", req, () -> tools.myopiaControlGuide(
                req.age(), req.currentSph(), req.annualProgression(),
                req.parentMyopia(), req.outdoorHours()));
    }

    @PostMapping("/anisometropia_guide")
    public ToolResponse anisometropiaGuide(@RequestBody AnisometropiaRequest req) {
        return run("anisometropia_guide", req, () -> tools.anisometropiaGuide(
                req.rightSph(), req.leftSph(), req.rightCyl(), req.leftCyl()));
    }

    @PostMapping("/contact_lens_power")
    public ToolResponse contactLensPower(@RequestBody ContactLensRequest req) {
        return run("contact_lens_power", req,
                () -> tools.contactLensPower(req.sph(), req.cyl(), req.vertexDistanceMm()));
    }

    @PostMapping("/reading_add_estimator")
    public ToolResponse readingAddEstimator(@RequestBody ReadingAddRequest req) {
        return run("reading_add_estimator", req, () -> tools.readingAddEstimator(
                req.age(), req.workingDistanceCm(), req.distanceSph()));
    }

    @PostMapping("/prescription_transpose")
    public ToolResponse prescriptionTranspose(@RequestBody TransposeRequest req) {
        return run("prescription_transpose", req,
                () -> tools.prescriptionTranspose(req.sph(), req.cyl(), req.axis()));
    }

    @PostMapping("/frame_fit_calculator")
    public ToolResponse frameFitCalculator(@RequestBody FrameFitRequest req) {
        return run("frame_fit_calculator", req, () -> tools.frameFitCalculator(
                req.lensWidth(), req.bridge(), req.pd(), req.power(), req.templeLength()));
    }

    @PostMapping("/visual_acuity_converter")
    public ToolResponse visualAcuityConverter(@RequestBody AcuityRequest req) {
        return run("visual_acuity_converter", req, () -> tools.visualAcuityConverter(
                req.notation(), req.value(), req.snellenNumerator()));
    }

    @PostMapping("/accommodation_amplitude")
    public ToolResponse accommodationAmplitude(@RequestBody AccommodationRequest req) {
        return run("accommodation_amplitude", req, () -> tools.accommodationAmplitude(
                req.age(), req.workingDistanceCm()));
    }

    @PostMapping("/sunglass_tint_guide")
    public ToolResponse sunglassTintGuide(@RequestBody SunglassTintRequest req) {
        return run("sunglass_tint_guide", req, () -> tools.sunglassTintGuide(
                req.environment(), req.lightSensitivity(), req.driving(), req.hasPrescription()));
    }

    @PostMapping("/lens_material_advisor")
    public ToolResponse lensMaterialAdvisor(@RequestBody LensMaterialRequest req) {
        return run("lens_material_advisor", req, () -> tools.lensMaterialAdvisor(
                req.sph(), req.cyl(), req.frameType(), req.usage(), req.priority()));
    }

    @PostMapping("/clear_vision_range")
    public ToolResponse clearVisionRange(@RequestBody ClearVisionRangeRequest req) {
        return run("clear_vision_range", req,
                () -> tools.clearVisionRange(req.sph(), req.cyl(), req.age()));
    }

    @PostMapping("/near_acuity_converter")
    public ToolResponse nearAcuityConverter(@RequestBody NearAcuityRequest req) {
        return run("near_acuity_converter", req,
                () -> tools.nearAcuityConverter(req.notation(), req.value(), req.testDistanceCm()));
    }

    @PostMapping("/prism_resolver")
    public ToolResponse prismResolver(@RequestBody PrismResolverRequest req) {
        return run("prism_resolver", req,
                () -> tools.prismResolver(req.mode(), req.horizontal(), req.horizontalBase(),
                        req.vertical(), req.verticalBase(), req.magnitude(), req.angle()));
    }

    private static Map<String, String> tool(String name, String description) {
        return Map.of("name", name, "description", description);
    }

    // ------------------------- 请求/响应体 -------------------------

    /** 统一响应结构，text 为 Markdown 文本。 */
    public record ToolResponse(String text) {
        static ToolResponse of(String text) {
            return new ToolResponse(text);
        }
    }

    public record VisionCheckRequest(String ageGroup, String concern) {
    }

    public record LensRequest(double sph, Double cyl, String usage, String budget) {
    }

    public record FrameRequest(String faceShape, String lifestyle, String prescriptionStrength) {
    }

    public record PrescriptionRequest(double odSph, Double odCyl, Integer odAxis,
                                      double osSph, Double osCyl, Integer osAxis,
                                      Double pd, Double add) {
    }

    public record ProgressiveRequest(int age, String nearDifficulty, double screenHours,
                                     String driveFrequency, boolean firstTimeUser) {
    }

    public record TroubleshootingRequest(String symptom, int wearDays, String lensType, boolean prescriptionChanged) {
    }

    public record ShoppingRequest(List<String> keywords) {
    }

    public record ThicknessRequest(double sph, Double cyl, String lensIndex, Double frameWidth) {
    }

    public record PupillaryDistanceRequest(Double binocularPd, Double pdRight, Double pdLeft,
                                           Double workingDistanceCm) {
    }

    public record CoatingRequest(double screenHours, String outdoorFrequency, String nightDriving,
                                 Boolean lightSensitive, Boolean preferOnePair) {
    }

    public record MyopiaControlRequest(int age, double currentSph, Double annualProgression,
                                       String parentMyopia, Double outdoorHours) {
    }

    public record AnisometropiaRequest(double rightSph, double leftSph, Double rightCyl, Double leftCyl) {
    }

    public record ContactLensRequest(double sph, Double cyl, Double vertexDistanceMm) {
    }

    public record ReadingAddRequest(int age, Double workingDistanceCm, Double distanceSph) {
    }

    public record TransposeRequest(double sph, double cyl, Integer axis) {
    }

    public record FrameFitRequest(double lensWidth, double bridge, double pd, Double power, Double templeLength) {
    }

    public record AcuityRequest(String notation, double value, Integer snellenNumerator) {
    }

    public record AccommodationRequest(int age, Double workingDistanceCm) {
    }

    public record SunglassTintRequest(String environment, String lightSensitivity,
                                      Boolean driving, Boolean hasPrescription) {
    }

    public record LensMaterialRequest(double sph, Double cyl, String frameType,
                                      String usage, String priority) {
    }

    public record ClearVisionRangeRequest(double sph, Double cyl, Integer age) {
    }

    public record NearAcuityRequest(String notation, double value, Double testDistanceCm) {
    }

    public record PrismResolverRequest(String mode, Double horizontal, String horizontalBase,
                                       Double vertical, String verticalBase,
                                       Double magnitude, Double angle) {
    }
}
