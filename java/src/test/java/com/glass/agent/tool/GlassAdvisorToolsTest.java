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

    @Test
    void shoppingLinksBuildsEncodedPlatformUrls() {
        String result = tools.shoppingLinks(java.util.List.of("1.67 非球面 防蓝光 镜片", "  "));
        String encoded = java.net.URLEncoder.encode("1.67 非球面 防蓝光 镜片", java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(result.contains("https://search.jd.com/Search?keyword=" + encoded));
        assertTrue(result.contains("淘宝") && result.contains("拼多多"));
    }

    @Test
    void shoppingLinksRejectsEmptyKeywords() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.shoppingLinks(java.util.List.of()));
        assertTrue(ex.getMessage().contains("keywords"));
    }

    @Test
    void thicknessEstimatorRecommendsHigherIndexForStrongMyopia() {
        // 功率 8，有效直径 56（半径 28），n=1.56：矢高 = 8*784/(2000*0.56) = 5.6，边缘 = 1.2 + 5.6 = 6.8mm
        String result = tools.lensThicknessEstimator(-8.0, null, "1.56", 52.0);
        assertTrue(result.contains("边缘最厚：约 6.8 mm"));
        assertTrue(result.contains("建议提高到 1.74"));
    }

    @Test
    void thicknessEstimatorReportsCenterThicknessForPlusLenses() {
        String result = tools.lensThicknessEstimator(5.0, null, "1.60", 52.0);
        assertTrue(result.contains("中心最厚"));
        assertTrue(result.contains("正镜片"));
    }

    @Test
    void thicknessEstimatorRejectsUnsupportedIndex() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.lensThicknessEstimator(-3.0, null, "1.50", null));
        assertTrue(ex.getMessage().contains("lens_index"));
    }

    @Test
    void pupillaryDistanceDerivesBinocularFromMonocularAndFlagsAsymmetry() {
        // 30 + 34 = 64mm 双眼瞳距，左右相差 4mm 属明显不对称
        String result = tools.pupillaryDistanceGuide(null, 30.0, 34.0, null);
        assertTrue(result.contains("64 mm（由左右单眼相加得到）"));
        assertTrue(result.contains("明显不对称"));
    }

    @Test
    void pupillaryDistanceComputesSmallerNearPd() {
        // 近用瞳距 = 63 * 400 / 427 ≈ 59.0mm，比远用约小 4.0mm
        String result = tools.pupillaryDistanceGuide(63.0, null, null, 40.0);
        assertTrue(result.contains("约 59 mm（比远用约小 4 mm）"));
    }

    @Test
    void pupillaryDistanceRequiresBothMonocularValues() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.pupillaryDistanceGuide(null, 31.0, null, null));
        assertTrue(ex.getMessage().contains("左右眼一起提供"));
    }

    @Test
    void pupillaryDistanceRequiresAtLeastOneInput() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.pupillaryDistanceGuide(null, null, null, null));
        assertTrue(ex.getMessage().contains("请至少提供双眼瞳距"));
    }

    @Test
    void coatingAdvisorKeepsBaseCoatingStandardAndSkipsBlueLightForLightScreenUse() {
        String result = tools.lensCoatingAdvisor(2.0, "rare", null, null, null);
        assertTrue(result.contains("基础膜层"));
        assertTrue(result.contains("标配（默认就选）"));
        assertTrue(result.contains("防蓝光膜"));
        // 轻度屏幕使用 → 防蓝光落入「通常不必额外花钱」清单
        int skipHeader = result.indexOf("**通常不必额外花钱**");
        assertTrue(skipHeader > 0 && result.indexOf("防蓝光", skipHeader) > 0);
    }

    @Test
    void coatingAdvisorStronglyRecommendsUvAndPolarizedForFrequentOutdoor() {
        String result = tools.lensCoatingAdvisor(9.0, "often", null, true, null);
        assertTrue(result.contains("UV 防护（UV400）**：强烈推荐"));
        assertTrue(result.contains("偏振太阳镜"));
        assertTrue(result.contains("**：推荐"));
    }

    @Test
    void coatingAdvisorRecommendsPhotochromicAndWarnsAboutNightVisionLenses() {
        String result = tools.lensCoatingAdvisor(5.0, "sometimes", "frequent", null, true);
        assertTrue(result.contains("变色片（光致变色）**：推荐"));
        assertTrue(result.contains("夜视 / 防远光"));
    }

    @Test
    void coatingAdvisorRejectsOutOfRangeScreenHours() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.lensCoatingAdvisor(30.0, "rare", null, null, null));
        assertTrue(ex.getMessage().contains("screen_hours"));
    }

    @Test
    void myopiaControlFlagsHighRiskAndPrioritizesOutdoorForYoungFastProgressor() {
        String result = tools.myopiaControlGuide(8, -2.0, 1.0, "both", 0.5);
        assertTrue(result.contains("综合判断：高风险"));
        assertTrue(result.contains("最该优先补上"));
        assertTrue(result.contains("日常配镜首选"));
        assertTrue(result.contains("建议就诊咨询"));
    }

    @Test
    void myopiaControlHoldsOrthoKForYoungChildAndHandlesPreMyopia() {
        String result = tools.myopiaControlGuide(6, 0.25, null, null, null);
        assertTrue(result.contains("尚未达到近视标准"));
        assertTrue(result.contains("年龄偏小，暂不考虑"));
        assertTrue(result.contains("保住远视储备"));
    }

    @Test
    void myopiaControlMarksOrthoKForSpecialEvaluationOnHighMyopia() {
        String result = tools.myopiaControlGuide(13, -6.5, null, null, null);
        assertTrue(result.contains("高度近视"));
        assertTrue(result.contains("需专业评估（度数偏高）"));
        assertTrue(result.contains("眼底检查列为常规项目"));
    }

    @Test
    void myopiaControlRejectsOutOfRangeAge() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.myopiaControlGuide(25, -1.0, null, null, null));
        assertTrue(ex.getMessage().contains("age"));
    }

    @Test
    void anisometropiaGradesLargeDifferenceAsSignificantAndFlagsAniseikonia() {
        // 等效球镜差 4.00D → 显著；4 * 1.5%/D = 6% > 5% 耐受上限
        String result = tools.anisometropiaGuide(-1.0, -5.0, null, null);
        assertTrue(result.contains("等效球镜差：4D → **显著屈光参差**"));
        assertTrue(result.contains("约 6%"));
        assertTrue(result.contains("超过一般耐受上限"));
        assertTrue(result.contains("建议优先考虑隐形眼镜"));
    }

    @Test
    void anisometropiaFoldsCylinderIntoEquivalentForMatchedEyes() {
        // 右 SE=-2.375，左 SE=-2.75，差 0.375 < 1，尽管球镜差 0.75D
        String result = tools.anisometropiaGuide(-2.0, -2.0, -0.75, -1.5);
        assertTrue(result.contains("→ **无明显参差**"));
        assertTrue(result.contains("按验光度数配框架镜通常没有额外适应问题"));
    }

    @Test
    void anisometropiaDetectsAntimetropia() {
        String result = tools.anisometropiaGuide(-1.5, 1.5, null, null);
        assertTrue(result.contains("混合性屈光参差"));
    }

    @Test
    void anisometropiaWarnsAboutLargeCylinderDifference() {
        String result = tools.anisometropiaGuide(-2.0, -2.0, 0.0, -2.0);
        assertTrue(result.contains("两眼柱镜相差约"));
        assertTrue(result.contains("子午线方向上的影像倾斜"));
    }

    @Test
    void anisometropiaRejectsOutOfRangeSphere() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.anisometropiaGuide(-99.0, -1.0, null, null));
        assertTrue(ex.getMessage().contains("right_sph"));
    }

    @Test
    void contactLensPowerCompensatesStrongMyopeDownAndRoundsToStep() {
        // -6 / (1 - 0.012 * -6) = -5.597 → 就近 0.25 = -5.50
        String result = tools.contactLensPower(-6.0, null, 12.0);
        assertTrue(result.contains("隐形眼镜光度：**SPH -5.5D**"));
        assertTrue(result.contains("需要顶点补偿的量级"));
        assertTrue(result.contains("近视换算成隐形后度数会变浅"));
    }

    @Test
    void contactLensPowerCompensatesStrongHyperopeUp() {
        // 5 / (1 - 0.012 * 5) = 5.319 → 就近 0.25 = 5.25
        String result = tools.contactLensPower(5.0, null, 12.0);
        assertTrue(result.contains("隐形眼镜光度：**SPH +5.25D**"));
        assertTrue(result.contains("远视换算成隐形后度数会变深"));
    }

    @Test
    void contactLensPowerTreatsLowPowerAsNoMeaningfulCompensation() {
        String result = tools.contactLensPower(-2.0, null, null);
        assertTrue(result.contains("隐形眼镜光度：**SPH -2D**"));
        assertTrue(result.contains("顶点补偿量很小"));
    }

    @Test
    void contactLensPowerFoldsAstigmatismAndOffersSphericalEquivalent() {
        String result = tools.contactLensPower(-6.0, -0.75, 12.0);
        assertTrue(result.contains("散光隐形"));
        assertTrue(result.contains("折算等效球镜"));
    }

    @Test
    void contactLensPowerRejectsOutOfRangeVertexDistance() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.contactLensPower(-3.0, null, 40.0));
        assertTrue(ex.getMessage().contains("vertex_distance_mm"));
    }

    @Test
    void readingAddGivesTypicalAddForFiftyYearOldAndComputesNearTotal() {
        // age 50 → +2.00 base，40cm 参考距离无修正；看近总度数 = -2.00 + 2.00 = 0
        String result = tools.readingAddEstimator(50, 40.0, -2.0);
        assertTrue(result.contains("建议近附加（下加光 ADD）：**约 +2D**"));
        assertTrue(result.contains("= **0D**"));
        assertTrue(result.contains("主要用眼距离：40 cm"));
    }

    @Test
    void readingAddIncreasesForCloserWorkingDistance() {
        // base +2.00, 修正 = 100/33 - 100/40 = +0.53 → 就近 0.25 = +2.50
        String result = tools.readingAddEstimator(50, 33.0, null);
        assertTrue(result.contains("建议近附加（下加光 ADD）：**约 +2.5D**"));
        assertTrue(result.contains("比参考的 40cm 更近"));
    }

    @Test
    void readingAddRecommendsNoAddBelowOnsetAge() {
        String result = tools.readingAddEstimator(32, null, null);
        assertTrue(result.contains("+0.00D（暂不需要）"));
        assertTrue(result.contains("通常不需要近附加"));
    }

    @Test
    void readingAddCapsAtPracticalMaximum() {
        // base 2.50 + (100/20 - 2.50 = 2.50) = 5.00，封顶 3.50
        String result = tools.readingAddEstimator(65, 20.0, null);
        assertTrue(result.contains("建议近附加（下加光 ADD）：**约 +3.5D**"));
        assertTrue(result.contains("已达上限"));
    }

    @Test
    void readingAddRejectsOutOfRangeAge() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.readingAddEstimator(150, null, null));
        assertTrue(ex.getMessage().contains("age"));
    }

    @Test
    void prescriptionTransposeConvertsMinusCylToPlusCylAndRotatesAxis() {
        // 新球镜 = -2 + -0.75 = -2.75，新柱镜 = +0.75，新轴位 = 180 → 90
        String result = tools.prescriptionTranspose(-2, -0.75, 180);
        assertTrue(result.contains("**SPH -2.75D / CYL +0.75D / AXIS 90°**"));
        assertTrue(result.contains("转换结果（正柱镜（正散光））"));
        // 等效球镜不变量：-2 + -0.75/2 = -2.375 → -2.38
        assertTrue(result.contains("等效球镜（SPH + CYL/2）转换前后不变，均为 -2.38D"));
    }

    @Test
    void prescriptionTransposeConvertsPlusCylToMinusCylAndWrapsAxis() {
        // 新球镜 = 1 + 1.5 = 2.50，新柱镜 = -1.50，新轴位 = 60 + 90 = 150
        String result = tools.prescriptionTranspose(1, 1.5, 60);
        assertTrue(result.contains("**SPH +2.5D / CYL -1.5D / AXIS 150°**"));
        assertTrue(result.contains("转换结果（负柱镜（负散光））"));
    }

    @Test
    void prescriptionTransposeReportsPureSphereHasNoCylinderForm() {
        String result = tools.prescriptionTranspose(-3, 0, null);
        assertTrue(result.contains("无散光"));
        assertTrue(result.contains("无需转换"));
    }

    @Test
    void prescriptionTransposeRequiresAxisWhenCylinderPresent() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.prescriptionTranspose(-2, -0.75, null));
        assertTrue(ex.getMessage().contains("轴位"));
    }
}
