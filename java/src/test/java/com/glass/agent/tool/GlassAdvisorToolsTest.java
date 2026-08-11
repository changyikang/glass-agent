package com.glass.agent.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工具业务逻辑单元测试，对照原 TypeScript 版本 {@code src/index.test.ts} 的核心断言。
 */
class GlassAdvisorToolsTest {

    private final GlassAdvisorTools tools = new GlassAdvisorTools();

    @Test
    void visionCheckGuideReturnsAgeGroupContent() {
        String result = tools.visionCheckGuide("children", "近视");
        assertTrue(result.contains("儿童视力检查指南"));
        assertTrue(result.contains("近视相关"));
    }

    @Test
    void lensRecommendationReflectsHighPrescription() {
        String result = tools.lensRecommendation(-7.5, -2.0, "daily", "premium");
        assertTrue(result.contains("1.74"));
        assertTrue(result.contains("散光较大"));
    }

    @Test
    void prescriptionInterpreterFlagsAnisometropia() {
        String result = tools.prescriptionInterpreter(-1.0, 0.0, null, -4.0, 0.0, null, 62.0, null);
        assertTrue(result.contains("差异较大"));
    }

    @Test
    void prescriptionInterpreterRequiresAxisWhenCylPresent() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.prescriptionInterpreter(-1.0, -0.75, null, -1.0, 0.0, null, null, null));
        assertTrue(ex.getMessage().contains("轴位"));
    }

    @Test
    void progressiveAssessmentSuggestsOfficeForScreenHeavyUser() {
        String result = tools.progressiveLensAssessment(45, "mild", 9, "weekly", true);
        assertTrue(result.contains("办公镜片"));
    }

    @Test
    void troubleshootingFlagsUrgentDoubleVision() {
        String result = tools.newGlassesTroubleshooting("double_vision", 5, "single_vision", true);
        assertTrue(result.contains("尽快复查"));
    }

    @Test
    void invalidEnumThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.visionCheckGuide("teenager", null));
        assertTrue(ex.getMessage().contains("age_group"));
    }
}
