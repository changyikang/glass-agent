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

    @Test
    void frameFitCalculatorComputesFramePdAndInwardDecentration() {
        // 框 PD = 52 + 18 = 70；移心 = (70 - 62)/2 = 4，向鼻侧内移
        String result = tools.frameFitCalculator(52, 18, 62, -4.0, null);
        assertTrue(result.contains("镜架几何中心距（框 PD）：**70 mm**"));
        assertTrue(result.contains("每片移心量：**4 mm**（向鼻侧内移）"));
        assertTrue(result.contains("贴合评估：**偏大**"));
        // Prentice：0.4cm × 4D = 1.6Δ
        assertTrue(result.contains("每片约产生 **1.6Δ** 水平棱镜"));
        assertTrue(result.contains("该棱镜量已不可忽略"));
    }

    @Test
    void frameFitCalculatorFlagsNarrowerFrameNeedingOutwardDecentration() {
        // 框 PD = 64；移心 = (64 - 68)/2 = -2，向颞侧外移
        String result = tools.frameFitCalculator(48, 16, 68, null, null);
        assertTrue(result.contains("镜架几何中心距（框 PD）：**64 mm**"));
        assertTrue(result.contains("每片移心量：**2 mm**（向颞侧外移）"));
        assertTrue(result.contains("镜架偏窄"));
    }

    @Test
    void frameFitCalculatorRatesWellMatchedFrameAndSkipsPrismWithoutPower() {
        // 框 PD = 62 == PD → 移心 0 → 很合适
        String result = tools.frameFitCalculator(50, 12, 62, null, null);
        assertTrue(result.contains("每片移心量：**0 mm**（几乎无需移心）"));
        assertTrue(result.contains("贴合评估：**很合适**"));
        assertTrue(!result.contains("水平棱镜"));
    }

    @Test
    void frameFitCalculatorRejectsOutOfRangePd() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.frameFitCalculator(52, 18, 30, null, null));
        assertTrue(ex.getMessage().contains("pd"));
    }

    @Test
    void visualAcuityConverterMapsDecimalToAllNotations() {
        String result = tools.visualAcuityConverter("decimal", 1.0, null);
        assertTrue(result.contains("小数记录法：**1**"));
        assertTrue(result.contains("五分记录法（对数）：**5**"));
        assertTrue(result.contains("Snellen（美制/公制）：**20/20** ≈ **6/6**"));
        assertTrue(result.contains("logMAR：**0**"));
        assertTrue(result.contains("视力水平：**正常或以上**"));
    }

    @Test
    void visualAcuityConverterConvertsFiveMinuteToDecimalAndSnellen() {
        // 五分 4.7 → 小数 10^-0.3 ≈ 0.5 → 20/40
        String result = tools.visualAcuityConverter("five_minute", 4.7, null);
        assertTrue(result.contains("小数记录法：**0.5**"));
        assertTrue(result.contains("Snellen（美制/公制）：**20/40**"));
        assertTrue(result.contains("轻度下降"));
    }

    @Test
    void visualAcuityConverterAcceptsSnellenDenominator() {
        String result = tools.visualAcuityConverter("snellen", 200, 20);
        assertTrue(result.contains("小数记录法：**0.1**"));
        assertTrue(result.contains("五分记录法（对数）：**4**"));
        assertTrue(result.contains("logMAR：**1**"));
    }

    @Test
    void visualAcuityConverterRejectsOutOfRangeDecimal() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.visualAcuityConverter("decimal", 0, null));
        assertTrue(ex.getMessage().contains("0 到 3"));
    }

    @Test
    void accommodationAmplitudeAppliesHofstetterFormulasAndNearPoint() {
        // 最小 = 15 - 0.25*45 = 3.75；平均 = 18.5 - 0.30*45 = 5；最大 = 25 - 0.40*45 = 7
        String result = tools.accommodationAmplitude(45, null);
        assertTrue(result.contains("最小（Hofstetter 下限）：**3.75D**"));
        assertTrue(result.contains("平均（预期值）：**5D**"));
        assertTrue(result.contains("最大（上限）：**7D**"));
        // 调节近点 = 100/5 = 20cm；舒适最近 = 100/(5/2) = 40cm
        assertTrue(result.contains("调节近点（按平均调节力）：**20 cm**"));
        assertTrue(result.contains("舒适持续用眼最近距离（保留一半调节力）：**40 cm**"));
    }

    @Test
    void accommodationAmplitudeFlagsStrainedNearWorkAndSuggestsAdd() {
        // 45 岁平均 5D，储备 2.5D；33cm 需求 100/33≈3.03D > 储备
        String result = tools.accommodationAmplitude(45, 33.0);
        assertTrue(result.contains("在 33 cm 处"));
        assertTrue(result.contains("偏吃力"));
        // 建议下加光 = round((3.03 - 2.5)/0.25)*0.25 = 0.5
        assertTrue(result.contains("建议近附加（补足储备）：约 **+0.5D**"));
    }

    @Test
    void accommodationAmplitudeMarksComfortableDistanceWithoutAdd() {
        // 30 岁平均 = 18.5 - 9 = 9.5D，储备 4.75D；40cm 需求 2.5D < 储备
        String result = tools.accommodationAmplitude(30, 40.0);
        assertTrue(result.contains("状态：**舒适"));
        assertTrue(!result.contains("建议近附加"));
    }

    @Test
    void accommodationAmplitudeReportsExhaustedAccommodationForTheVeryOld() {
        // 70 岁平均 = 18.5 - 21 < 0 → 封底为 0 → 近点无法测得
        String result = tools.accommodationAmplitude(70, null);
        assertTrue(result.contains("平均（预期值）：**0D**"));
        assertTrue(result.contains("调节力已近耗竭"));
    }

    @Test
    void accommodationAmplitudeRejectsOutOfRangeAge() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.accommodationAmplitude(200, null));
        assertTrue(ex.getMessage().contains("age"));
    }

    @Test
    void sunglassTintGuideRecommendsCategoryThreeForBrightSun() {
        String result = tools.sunglassTintGuide("bright", null, null, null);
        assertTrue(result.contains("分类：**3 类（深色）**"));
        assertTrue(result.contains("可见光透过率（VLT）：**8%–18%**"));
        assertTrue(!result.contains("白天驾驶：0–3 类"));
    }

    @Test
    void sunglassTintGuideBumpsOneCategoryDeeperForLightSensitiveWearer() {
        // sunny 基准 = 2 类，畏光 → 3 类
        String result = tools.sunglassTintGuide("sunny", "high", null, null);
        assertTrue(result.contains("分类：**3 类（深色）**"));
        assertTrue(result.contains("因畏光 / 对强光敏感"));
    }

    @Test
    void sunglassTintGuideCapsDayDrivingAtCategoryThree() {
        // snow_water 基准 = 4 类，但白天驾驶不允许 4 类
        String result = tools.sunglassTintGuide("snow_water", null, true, null);
        assertTrue(result.contains("分类：**3 类（深色）**"));
        assertTrue(result.contains("4 类（极深）镜片透光过低、法规不允许开车佩戴"));
        assertTrue(result.contains("白天驾驶：0–3 类"));
        assertTrue(result.contains("灰色（中性灰）：**首选**"));
    }

    @Test
    void sunglassTintGuideTreatsIndoorNightDrivingAsNightDriving() {
        String result = tools.sunglassTintGuide("indoor_night", null, true, null);
        assertTrue(result.contains("分类：**0 类（近无色 / 极浅）**"));
        assertTrue(result.contains("夜间 / 昏暗驾驶"));
        assertTrue(result.contains("黄色「夜视镜」并不能真正提升夜间安全"));
        assertTrue(result.contains("偏光：此环境无需偏光"));
    }

    @Test
    void sunglassTintGuideRecommendsPolarizedAndPrescriptionOptions() {
        String result = tools.sunglassTintGuide("snow_water", null, null, true);
        assertTrue(result.contains("偏光：**建议**"));
        assertTrue(result.contains("带度数（近视 / 散光 / 老花）选配"));
        assertTrue(result.contains("多数变色片在车内不会变深"));
        assertTrue(result.contains("更高折射率"));
    }

    @Test
    void sunglassTintGuideRejectsUnknownEnvironment() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.sunglassTintGuide("space", null, null, null));
        assertTrue(ex.getMessage().contains("environment"));
    }

    @Test
    void lensMaterialAdvisorForcesImpactResistantPcForKidsAndBansGlass() {
        String result = tools.lensMaterialAdvisor(-2, null, null, "kids", null);
        assertTrue(result.contains("首选：**PC 聚碳酸酯"));
        assertTrue(result.contains("玻璃：易碎裂溅入眼睛"));
        assertTrue(result.contains("CR-39 普通树脂：抗冲击性不足"));
    }

    @Test
    void lensMaterialAdvisorPrefersTrivexForKidsWhenClarityPrioritized() {
        String result = tools.lensMaterialAdvisor(-2, null, null, "sports", "clarity");
        assertTrue(result.contains("首选：**Trivex"));
        assertTrue(result.contains("阿贝数(45)明显高于 PC"));
    }

    @Test
    void lensMaterialAdvisorRecommendsTrivexForRimlessAndWarnsAgainstGlassAnd174() {
        String result = tools.lensMaterialAdvisor(-3, null, "rimless", null, null);
        assertTrue(result.contains("首选：**Trivex"));
        assertTrue(result.contains("玻璃：无框需在镜片上钻孔"));
        assertTrue(result.contains("1.74 高折射树脂：1.74 偏脆，无框钻孔"));
    }

    @Test
    void lensMaterialAdvisorScalesIndexUpWithPowerForFullRim() {
        assertTrue(tools.lensMaterialAdvisor(-1, null, null, null, null).contains("首选：**CR-39 普通树脂"));
        assertTrue(tools.lensMaterialAdvisor(-3, null, null, null, null).contains("首选：**1.60 高折射树脂"));
        assertTrue(tools.lensMaterialAdvisor(-5, null, null, null, null).contains("首选：**1.67 高折射树脂"));
        String veryHigh = tools.lensMaterialAdvisor(-7, null, null, null, null);
        assertTrue(veryHigh.contains("首选：**1.74 高折射树脂"));
        assertTrue(veryHigh.contains("玻璃：玻璃重且易碎"));
    }

    @Test
    void lensMaterialAdvisorWarnsAboutLowAbbeHalosForNightDriving() {
        String result = tools.lensMaterialAdvisor(-5, null, null, "driving", null);
        assertTrue(result.contains("首选：**1.67 高折射树脂"));
        assertTrue(result.contains("夜间驾驶：低阿贝数材料"));
    }

    @Test
    void lensMaterialAdvisorUsesWorstMeridianAsReferencePower() {
        // sph -3 alone would be 1.60, but -3 + -3 = -6 pushes to 1.74
        String result = tools.lensMaterialAdvisor(-3, -3.0, null, null, null);
        assertTrue(result.contains("参考功率（最大子午线）：6D"));
        assertTrue(result.contains("首选：**1.74 高折射树脂"));
    }

    @Test
    void lensMaterialAdvisorRejectsUnknownFrameType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.lensMaterialAdvisor(-2, null, "octagon", null, null));
        assertTrue(ex.getMessage().contains("frame_type"));
    }

    @Test
    void clearVisionRangeGivesMyopeFarPointAndExplanation() {
        // SE = -2.00 → far point = 100/2 = 50cm
        String result = tools.clearVisionRange(-2, null, null);
        assertTrue(result.contains("低度近视（SE -2D）"));
        assertTrue(result.contains("远点（能看清的最远处）：**50 cm**"));
        assertTrue(result.contains("摘镜看手机 / 看书反而清楚"));
        assertTrue(result.contains("填入 age 可估算最近清晰距离"));
    }

    @Test
    void clearVisionRangeComputesMyopeNearPointFromAge() {
        // SE = -5.00, age 25 → amp = 11; near = 100/16 ≈ 6.3cm; far = 100/5 = 20cm
        String result = tools.clearVisionRange(-5, null, 25);
        assertTrue(result.contains("远点（能看清的最远处）：**20 cm**"));
        assertTrue(result.contains("近点（能看清的最近处）：**6.3 cm**"));
        assertTrue(result.contains("调节幅度约 11D（Hofstetter 平均"));
    }

    @Test
    void clearVisionRangeUsesSphericalEquivalentForAstigmatism() {
        // SE = -3 + (-2/2) = -4 → far point = 100/4 = 25cm
        String result = tools.clearVisionRange(-3, -2.0, null);
        assertTrue(result.contains("等效球镜（SE = SPH + CYL/2）：**-4D**"));
        assertTrue(result.contains("远点（能看清的最远处）：**25 cm**"));
        assertTrue(result.contains("含散光"));
    }

    @Test
    void clearVisionRangeShowsYoungHyperopeCompensating() {
        // SE = +2, age 20 → amp = 12.5 >= 2 → can see far
        String result = tools.clearVisionRange(2, null, 20);
        assertTrue(result.contains("远视"));
        assertTrue(result.contains("可看清远处**（调节代偿）"));
    }

    @Test
    void clearVisionRangeFlagsHyperopeWhoCannotCompensate() {
        // SE = +5, age 60 → amp = 0.5 < 5 → cannot compensate
        String result = tools.clearVisionRange(5, null, 60);
        assertTrue(result.contains("不足以克服 5D 的远视"));
        assertTrue(result.contains("裸眼看远也难以看清"));
    }

    @Test
    void clearVisionRangeKeepsEmmetropeClearToInfinity() {
        // age 30 → amp 9.5 → near point 100/9.5 ≈ 10.5cm
        String result = tools.clearVisionRange(0, null, 30);
        assertTrue(result.contains("正视 / 接近平光"));
        assertTrue(result.contains("清晰到无穷远"));
        assertTrue(result.contains("近点（能看清的最近处）：**10.5 cm**"));
    }

    @Test
    void clearVisionRangeRejectsOutOfRangeSphere() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.clearVisionRange(99, null, null));
        assertTrue(ex.getMessage().contains("sph"));
    }

    @Test
    void nearAcuityConverterReading1MAt40cmIs04Decimal() {
        // decimal = 0.4 / 1.0 = 0.4 → 20/50, 8pt
        String result = tools.nearAcuityConverter("m_unit", 1.0, null);
        assertTrue(result.contains("印张尺寸（M 记法）：**1 M**"));
        assertTrue(result.contains("印刷点数（约）：**8 pt**"));
        assertTrue(result.contains("该距离近视力（小数）：**0.4**"));
        assertTrue(result.contains("Snellen 等效：**20/50**"));
    }

    @Test
    void nearAcuityConverterPointInputConvertsToMByDivide8() {
        // 16pt → 2M; at 40cm decimal = 0.4/2 = 0.2 → 20/100
        String result = tools.nearAcuityConverter("point", 16, null);
        assertTrue(result.contains("印张尺寸（M 记法）：**2 M**"));
        assertTrue(result.contains("该距离近视力（小数）：**0.2**"));
        assertTrue(result.contains("Snellen 等效：**20/100**"));
    }

    @Test
    void nearAcuityConverterNearerDistanceImprovesAcuity() {
        // 1M at 25cm → decimal = 0.25 → 20/80
        String result = tools.nearAcuityConverter("m_unit", 1.0, 25.0);
        assertTrue(result.contains("测试距离 25 cm"));
        assertTrue(result.contains("该距离近视力（小数）：**0.25**"));
        assertTrue(result.contains("Snellen 等效：**20/80**"));
    }

    @Test
    void nearAcuityConverterDecimalInputGivesEquivalentM() {
        // decimal 0.5 at 40cm → M = 0.4/0.5 = 0.8
        String result = tools.nearAcuityConverter("decimal", 0.5, null);
        assertTrue(result.contains("近视力小数 0.5"));
        assertTrue(result.contains("印张尺寸（M 记法）：**0.8 M**"));
        assertTrue(result.contains("该距离近视力（小数）：**0.5**"));
    }

    @Test
    void nearAcuityConverterRejectsOutOfRangeValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.nearAcuityConverter("m_unit", 50, null));
        assertTrue(ex.getMessage().contains("M 记法"));
    }

    @Test
    void prismResolverCombinesThreeAndFourIntoFiveAt53Degrees() {
        // 3-4-5 直角三角形：合棱镜 5Δ，角度 atan2(4,3)=53.1°，基底颞侧偏上。
        String result = tools.prismResolver("combine", 3.0, "out", 4.0, "up", null, null);
        assertTrue(result.contains("合棱镜大小：**5Δ**"));
        assertTrue(result.contains("方向角：**53.1°**"));
        assertTrue(result.contains("基底方向：**颞侧(base-out) 偏 上方(base-up)**"));
    }

    @Test
    void prismResolverCombineBaseInAndDownLandsInThirdQuadrant() {
        // atan2(-1,-1) = 225°
        String result = tools.prismResolver("combine", 1.0, "in", 1.0, "down", null, null);
        assertTrue(result.contains("方向角：**225°**"));
        assertTrue(result.contains("基底方向：**鼻侧(base-in) 偏 下方(base-down)**"));
    }

    @Test
    void prismResolverResolvesFiveAt53BackIntoThreeAndFour() {
        String result = tools.prismResolver("resolve", null, null, null, null, 5.0, 53.13);
        assertTrue(result.contains("水平分量：**3Δ** 基底朝颞侧(base-out)"));
        assertTrue(result.contains("垂直分量：**4Δ** 基底朝上(base-up)"));
    }

    @Test
    void prismResolverResolveNinetyDegreesIsPureVertical() {
        String result = tools.prismResolver("resolve", null, null, null, null, 2.5, 90.0);
        assertTrue(result.contains("水平分量：**0**（纯垂直棱镜）"));
        assertTrue(result.contains("垂直分量：**2.5Δ** 基底朝上(base-up)"));
    }

    @Test
    void prismResolverCombineRequiresBaseForNonZeroComponent() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.prismResolver("combine", 2.0, null, null, null, null, null));
        assertTrue(ex.getMessage().contains("horizontal_base"));
    }

    @Test
    void prismResolverCombineRejectsTwoZeroComponents() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.prismResolver("combine", 0.0, null, 0.0, null, null, null));
        assertTrue(ex.getMessage().contains("至少提供一个非零分量"));
    }

    @Test
    void prismResolverRejectsUnknownMode() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tools.prismResolver("bogus", null, null, null, null, null, null));
        assertTrue(ex.getMessage().contains("mode"));
    }
}
