package io.angistrem.employeemanagement.service;

import javafx.scene.paint.Color;

public class FinancialMarkerVisualTokensService {

    public double lineTopOffset() {
        return 2.0;
    }

    public double lineBottomOffset() {
        return 2.0;
    }

    public double hitXOffset() {
        return 11.0;
    }

    public double hitYOffset() {
        return 1.0;
    }

    public double hitWidth() {
        return 22.0;
    }

    public double hitMinHeight() {
        return 20.0;
    }

    public double badgeMinTextWidth() {
        return 14.0;
    }

    public double badgeHorizontalPadding() {
        return 12.0;
    }

    public double badgeHeight() {
        return 16.0;
    }

    public double badgeArc() {
        return 10.0;
    }

    public Color badgeStroke() {
        return Color.rgb(255, 255, 255, 0.92);
    }

    public double badgeStrokeWidth() {
        return 1.0;
    }

    public double glowOpacity(boolean blocking) {
        return blocking ? 0.22 : 0.14;
    }

    public double glowWidth(boolean blocking) {
        return blocking ? 9.0 : 6.8;
    }

    public double markerWidth(boolean blocking) {
        return blocking ? 2.6 : 2.2;
    }

    public double[] markerDashPattern(boolean blocking) {
        return blocking ? new double[]{5.0, 3.0} : new double[]{3.0, 3.5};
    }

    public double headDotY(double rowY) {
        return rowY + 5.0;
    }

    public double headDotRadius(boolean blocking) {
        return blocking ? 4.8 : 4.0;
    }

    public Color headDotStroke() {
        return Color.rgb(255, 255, 255, 0.94);
    }

    public double headDotStrokeWidth() {
        return 1.1;
    }

    public double accentMinY(double rowY) {
        return rowY + 11.0;
    }

    public double accentBelowBadgeGap() {
        return 2.0;
    }

    public double accentXShiftFactor() {
        return 2.0;
    }

    public double hitArc() {
        return 12.0;
    }
}
