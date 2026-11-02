package io.angistrem.employeemanagement.service;

import javafx.scene.paint.Color;

public class TaskVisualTokensService {

    public Color subTaskStroke(boolean criticalPathItem, boolean selected) {
        if (criticalPathItem) {
            return Color.web("#f59e0b");
        }
        return selected ? Color.web("#0f172a") : Color.web("#0f766e");
    }

    public double subTaskStrokeWidth(boolean criticalPathItem, boolean selected) {
        if (criticalPathItem) {
            return 1.9;
        }
        return selected ? 1.35 : 0.5;
    }

    public double subTaskHoverStrokeWidth(boolean criticalPathItem, boolean selected) {
        if (criticalPathItem) {
            return 2.2;
        }
        return selected ? 1.45 : 0.9;
    }

    public double subTaskBaseOpacity(boolean criticalPathItem, boolean selected) {
        return (criticalPathItem || selected) ? 1.0 : 0.88;
    }

    public Color taskStroke(boolean criticalPathItem, boolean selected) {
        if (criticalPathItem) {
            return Color.web("#f59e0b");
        }
        return selected ? Color.web("#0f172a") : Color.web("#1d4ed8");
    }

    public double taskStrokeWidth(boolean criticalPathItem, boolean selected) {
        if (criticalPathItem) {
            return 2.15;
        }
        return selected ? 1.4 : 0.7;
    }

    public double taskHoverStrokeWidth(boolean criticalPathItem, boolean selected) {
        if (criticalPathItem) {
            return 2.45;
        }
        return selected ? 1.55 : 1.05;
    }

    public double taskBaseOpacity(boolean criticalPathItem, boolean selected) {
        return (criticalPathItem || selected) ? 1.0 : 0.95;
    }

    public Color milestoneStroke(boolean criticalPathItem, boolean selected) {
        return taskStroke(criticalPathItem, selected);
    }

    public double milestoneStrokeWidth(boolean criticalPathItem, boolean selected) {
        if (criticalPathItem) {
            return 2.1;
        }
        return selected ? 1.45 : 0.9;
    }

    public double milestoneHoverStrokeWidth(boolean criticalPathItem, boolean selected) {
        if (criticalPathItem) {
            return 2.4;
        }
        return selected ? 1.6 : 1.3;
    }
}
