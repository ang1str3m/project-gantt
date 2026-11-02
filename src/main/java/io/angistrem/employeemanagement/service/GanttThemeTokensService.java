package io.angistrem.employeemanagement.service;

import javafx.scene.paint.Color;

public class GanttThemeTokensService {

    public Color rowStripeFill() {
        return Color.rgb(15, 23, 42, 0.018);
    }

    public Color todayBandFill() {
        return Color.rgb(239, 68, 68, 0.08);
    }

    public Color todayLineStroke() {
        return Color.web("#ef4444");
    }

    public Color gridLineStroke() {
        return Color.web("#e2e8f0");
    }

    public Color gridMajorStroke() {
        return Color.web("#94a3b8");
    }

    public Color gridStrongStroke() {
        return Color.web("#64748b");
    }

    public Color weekMinorStroke() {
        return Color.web("#dbe4ef");
    }

    public Color quarterMinorStroke() {
        return Color.web("#d8e0ea");
    }

    public Color horizontalLineStroke() {
        return Color.web("#e9eef7");
    }

    public double todayLineWidth() {
        return 1.8;
    }

    public double dayMajorLineWidth() {
        return 1.0;
    }

    public double dayBaseLineWidth() {
        return 0.6;
    }

    public double weekMinorLineWidth() {
        return 0.8;
    }

    public double monthStrongLineWidth() {
        return 1.2;
    }

    public double monthMajorLineWidth() {
        return 0.9;
    }

    public double quarterStrongLineWidth() {
        return 1.4;
    }

    public double quarterMajorLineWidth() {
        return 1.0;
    }

    public double quarterMinorLineWidth() {
        return 0.7;
    }

    public double yearStrongLineWidth() {
        return 1.4;
    }

    public double yearMajorLineWidth() {
        return 1.0;
    }

    public double horizontalLineWidth() {
        return 0.8;
    }

    public double dragGuideWidth() {
        return 1.4;
    }

    public double[] dragGuideDashPattern() {
        return new double[]{5.0, 4.0};
    }

    public double dragGuideOpacity() {
        return 0.95;
    }

    public Color resizeHandleFill() {
        return Color.rgb(255, 255, 255, 0.98);
    }

    public Color resizeHandleStroke() {
        return Color.rgb(59, 130, 246, 0.98);
    }

    public double resizeHandleStrokeWidth() {
        return 1.15;
    }

    public String resizeHandleEffect() {
        return "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.24), 10, 0.25, 0, 1);";
    }

    public Color milestoneResizeHandleFill() {
        return Color.rgb(59, 130, 246, 0.18);
    }

    public Color milestoneResizeHandleStroke() {
        return Color.rgb(59, 130, 246, 0.9);
    }

    public double milestoneResizeHandleStrokeWidth() {
        return 1.0;
    }
}
