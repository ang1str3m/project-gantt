package io.angistrem.employeemanagement.service;

public class WindowRecoveryService {

    private static final double COLLAPSE_THRESHOLD = 0.82;

    public record TargetSize(double width, double height) {
    }

    public boolean isLayoutCollapsed(
            double sceneWidth,
            double stageWidth,
            double rootWidth,
            double topWidth,
            double workspaceWidth,
            double splitWidth
    ) {
        double targetWidth = Math.max(sceneWidth, stageWidth);
        if (targetWidth <= 0) {
            return false;
        }
        double effectiveWidth = Math.max(Math.max(rootWidth, topWidth), Math.max(workspaceWidth, splitWidth));
        return effectiveWidth > 0 && effectiveWidth < targetWidth * COLLAPSE_THRESHOLD;
    }

    public TargetSize resolveTargetSize(
            double sceneWidth,
            double sceneHeight,
            double stageWidth,
            double stageHeight,
            boolean stageMaximized,
            double screenWidth,
            double screenHeight
    ) {
        double width = Math.max(sceneWidth, stageMaximized ? screenWidth : stageWidth);
        double height = Math.max(sceneHeight, stageMaximized ? screenHeight : stageHeight);
        return new TargetSize(width, height);
    }

    public boolean shouldNudgeWindow(double width, double height, double minWidth, double minHeight) {
        return width > minWidth + 20 && height > minHeight + 20;
    }
}
