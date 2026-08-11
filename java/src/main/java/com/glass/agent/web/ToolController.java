package com.glass.agent.web;

import java.util.List;
import java.util.Map;

import com.glass.agent.tool.GlassAdvisorTools;

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

    public ToolController(GlassAdvisorTools tools) {
        this.tools = tools;
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
                tool("shopping_links", "购物链接生成：把配镜建议转成京东/淘宝/拼多多的商品搜索购买链接。"));
    }

    @PostMapping("/vision_check_guide")
    public ToolResponse visionCheckGuide(@RequestBody VisionCheckRequest req) {
        return ToolResponse.of(tools.visionCheckGuide(req.ageGroup(), req.concern()));
    }

    @PostMapping("/lens_recommendation")
    public ToolResponse lensRecommendation(@RequestBody LensRequest req) {
        return ToolResponse.of(tools.lensRecommendation(req.sph(), req.cyl(), req.usage(), req.budget()));
    }

    @PostMapping("/frame_selection_guide")
    public ToolResponse frameSelectionGuide(@RequestBody FrameRequest req) {
        return ToolResponse.of(tools.frameSelectionGuide(req.faceShape(), req.lifestyle(), req.prescriptionStrength()));
    }

    @PostMapping("/prescription_interpreter")
    public ToolResponse prescriptionInterpreter(@RequestBody PrescriptionRequest req) {
        return ToolResponse.of(tools.prescriptionInterpreter(
                req.odSph(), req.odCyl(), req.odAxis(),
                req.osSph(), req.osCyl(), req.osAxis(),
                req.pd(), req.add()));
    }

    @PostMapping("/progressive_lens_assessment")
    public ToolResponse progressiveLensAssessment(@RequestBody ProgressiveRequest req) {
        return ToolResponse.of(tools.progressiveLensAssessment(
                req.age(), req.nearDifficulty(), req.screenHours(), req.driveFrequency(), req.firstTimeUser()));
    }

    @PostMapping("/new_glasses_troubleshooting")
    public ToolResponse newGlassesTroubleshooting(@RequestBody TroubleshootingRequest req) {
        return ToolResponse.of(tools.newGlassesTroubleshooting(
                req.symptom(), req.wearDays(), req.lensType(), req.prescriptionChanged()));
    }

    @PostMapping("/shopping_links")
    public ToolResponse shoppingLinks(@RequestBody ShoppingRequest req) {
        return ToolResponse.of(tools.shoppingLinks(req.keywords()));
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
}
