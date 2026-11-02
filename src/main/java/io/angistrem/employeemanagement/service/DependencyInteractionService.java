package io.angistrem.employeemanagement.service;

import javafx.scene.control.Label;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;

public class DependencyInteractionService {

    public String highlightedBadgeStyle() {
        return "-fx-font-size: 9px; -fx-background-color: rgba(255,251,235,0.98);"
                + "-fx-text-fill: #92400e; -fx-font-weight: bold; -fx-padding: 2 6 2 6;"
                + "-fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: rgba(245,158,11,0.95);";
    }

    public double strokeWidth(boolean highlighted, boolean hovered) {
        if (highlighted) {
            return hovered ? 2.95 : 2.45;
        }
        return hovered ? 1.8 : 1.15;
    }

    public double arrowScale(boolean hovered) {
        return hovered ? 1.08 : 1.0;
    }

    public void applyHoverState(Path coloredPath, Polygon arrowHead, Label badge, boolean highlighted, boolean hovered) {
        if (coloredPath != null) {
            coloredPath.setStrokeWidth(strokeWidth(highlighted, hovered));
        }
        if (arrowHead != null) {
            double scale = arrowScale(hovered);
            arrowHead.setScaleX(scale);
            arrowHead.setScaleY(scale);
            if (hovered) {
                arrowHead.toFront();
            }
        }
        if (badge != null && hovered) {
            badge.toFront();
        }
    }

    public void installHoverHandlers(Path interactionPath,
                                     Polygon arrowHead,
                                     Label badge,
                                     Path coloredPath,
                                     boolean highlighted) {
        Runnable hoverOn = () -> applyHoverState(coloredPath, arrowHead, badge, highlighted, true);
        Runnable hoverOff = () -> applyHoverState(coloredPath, arrowHead, badge, highlighted, false);

        if (arrowHead != null) {
            arrowHead.setOnMouseEntered(e -> hoverOn.run());
            arrowHead.setOnMouseExited(e -> hoverOff.run());
        }
        if (interactionPath != null) {
            interactionPath.setOnMouseEntered(e -> hoverOn.run());
            interactionPath.setOnMouseExited(e -> hoverOff.run());
        }
        if (badge != null) {
            badge.setOnMouseEntered(e -> hoverOn.run());
            badge.setOnMouseExited(e -> hoverOff.run());
        }
    }

    public void installEditHandlers(Path interactionPath, Label badge, Polygon arrowHead, Runnable editAction) {
        if (editAction == null) {
            return;
        }
        if (interactionPath != null) {
            interactionPath.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    editAction.run();
                    e.consume();
                }
            });
        }
        if (badge != null) {
            badge.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    editAction.run();
                    e.consume();
                }
            });
        }
        if (arrowHead != null) {
            arrowHead.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    editAction.run();
                    e.consume();
                }
            });
        }
    }
}
