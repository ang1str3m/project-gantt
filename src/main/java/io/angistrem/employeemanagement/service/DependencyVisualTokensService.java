package io.angistrem.employeemanagement.service;

import javafx.scene.paint.Color;

public class DependencyVisualTokensService {

    public Color resolveStrokeColor(Color baseColor, boolean critical, boolean highlighted) {
        if (highlighted) {
            return Color.web("#f59e0b");
        }
        if (critical) {
            return Color.web("#9f1239");
        }
        return baseColor != null ? baseColor : Color.web("#64748b");
    }

    public Color highlightedHaloStroke() {
        return Color.rgb(255, 247, 214, 0.98);
    }

    public double highlightedHaloWidth() {
        return 3.2;
    }

    public double highlightedColoredPathWidth() {
        return 2.25;
    }

    public Color arrowStroke(boolean highlighted) {
        return highlighted ? Color.web("#78350f") : Color.rgb(255, 255, 255, 0.82);
    }

    public double arrowStrokeWidth(boolean highlighted) {
        return highlighted ? 0.9 : 0.7;
    }

    public double badgeMinWidth(double prefWidth) {
        return Math.max(34.0, prefWidth + 8.0);
    }

    public double badgeMinHeight(double prefHeight) {
        return Math.max(18.0, prefHeight + 4.0);
    }

    public double tooltipDelayMillis() {
        return 250.0;
    }
}
