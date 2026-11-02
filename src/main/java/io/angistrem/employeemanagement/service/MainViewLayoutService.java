package io.angistrem.employeemanagement.service;

import java.util.Locale;

public class MainViewLayoutService {

    private static final double DEFAULT_HEADER_HEIGHT = 64.0;
    private static final double MIN_CHART_HEIGHT = 120.0;

    public double resolveTableRowHeight(String textStyle) {
        String normalized = textStyle == null ? "standard" : textStyle.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "compact" -> 25.0;
            case "presentation" -> 31.0;
            default -> 28.0;
        };
    }

    public double resolveHeaderHeight(double rawHeaderHeight) {
        return rawHeaderHeight > 0 ? rawHeaderHeight : DEFAULT_HEADER_HEIGHT;
    }

    public double resolveChartHeight(int visibleRows, double rowHeight) {
        return Math.max(visibleRows * rowHeight, MIN_CHART_HEIGHT);
    }

    public double accumulateHorizontalScrollbarHeight(
            double currentSpacerHeight,
            boolean isHorizontal,
            boolean isVisible,
            double scrollbarHeight
    ) {
        if (!isHorizontal || !isVisible) {
            return currentSpacerHeight;
        }
        return Math.max(currentSpacerHeight, scrollbarHeight);
    }
}
