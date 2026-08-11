package com.glass.agent.tool;

import java.util.List;

/**
 * 度数格式化与渲染相关的纯函数工具，等价于 TypeScript 版本中的一组 format/render 帮助函数。
 */
final class Diopters {

    private Diopters() {
    }

    static String severityLabel(double value) {
        if (value < 3) {
            return "低度";
        }
        if (value < 6) {
            return "中度";
        }
        return "高度";
    }

    static String formatDiopter(double value) {
        return trimTrailingZeros(String.format("%.2f", value)) + "D";
    }

    static String formatSignedDiopter(double value) {
        String normalized = trimTrailingZeros(String.format("%.2f", Math.abs(value)));
        String sign = value > 0 ? "+" : value < 0 ? "-" : "";
        return sign + normalized + "D";
    }

    static String trimTrailingZeros(String value) {
        return value.replaceAll("\\.?0+$", "");
    }

    static String renderPrescriptionLine(double sph, double cyl, Integer axis) {
        StringBuilder line = new StringBuilder("SPH ").append(formatSignedDiopter(sph));
        if (cyl != 0) {
            line.append(" / CYL ").append(formatSignedDiopter(cyl));
        }
        if (axis != null) {
            line.append(" / AXIS ").append(axis).append("°");
        }
        return line.toString();
    }

    static String describeEye(double sph, double cyl) {
        StringBuilder parts = new StringBuilder();
        if (sph < 0) {
            parts.append(severityLabel(Math.abs(sph))).append("近视");
        } else if (sph > 0) {
            parts.append(severityLabel(Math.abs(sph))).append("远视");
        } else {
            parts.append("球镜接近平光");
        }
        if (cyl != 0) {
            parts.append("，伴").append(severityLabel(Math.abs(cyl))).append("散光");
        }
        return parts.toString();
    }

    static String renderLensType(String lensType) {
        return switch (lensType) {
            case "single_vision" -> "单光镜片";
            case "progressive" -> "渐进多焦点镜片";
            case "office" -> "办公镜片";
            case "bifocal" -> "双光镜片";
            default -> lensType;
        };
    }

    static String renderBulletList(List<String> items, String fallback) {
        if (items.isEmpty()) {
            return "- " + fallback;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append("- ").append(items.get(i));
        }
        return sb.toString();
    }
}
