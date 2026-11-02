package io.angistrem.employeemanagement.service;

import javafx.geometry.BoundingBox;

import java.util.List;
import java.util.function.Predicate;

public class DependencyBadgeLayoutService {

    public record BadgePlacement(double x, double y, double width, double height) {
    }

    public String baseBadgeStyle() {
        return "-fx-font-size: 9px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #0f172a;"
                + "-fx-background-color: rgba(255,255,255,0.93);"
                + "-fx-background-radius: 10;"
                + "-fx-border-radius: 10;"
                + "-fx-border-color: rgba(148,163,184,0.75);"
                + "-fx-border-width: 0.8;"
                + "-fx-padding: 1 6 1 6;";
    }

    public BadgePlacement computePlacement(double laneX,
                                           double targetKneeX,
                                           double laneY,
                                           double prefWidth,
                                           double prefHeight,
                                           double rowHeight,
                                           double chartRightLimit,
                                           double chartPrefHeight,
                                           List<BoundingBox> occupiedBounds,
                                           Predicate<BoundingBox> overlapsTaskBarPredicate) {
        double width = Math.max(34.0, prefWidth + 8.0);
        double height = Math.max(18.0, prefHeight + 4.0);
        double preferredX = (laneX + targetKneeX) / 2.0 - (width / 2.0);
        double maxX = Math.max(8.0, chartRightLimit - width - 8.0);
        double x = Math.max(8.0, Math.min(maxX, preferredX));

        double yBase = laneY <= 22.0 ? laneY + 8.0 : laneY - 20.0;
        List<Double> candidateY = List.of(
                yBase,
                laneY + 8.0,
                laneY - 20.0,
                laneY + 24.0,
                laneY - 36.0
        );
        double chartBottom = Math.max(rowHeight + 8.0, chartPrefHeight - height - 6.0);
        double chosenY = Math.max(4.0, Math.min(chartBottom, yBase));
        for (Double yCandidateRaw : candidateY) {
            double yCandidate = Math.max(4.0, Math.min(chartBottom, yCandidateRaw));
            BoundingBox candidateBounds = new BoundingBox(x, yCandidate, width, height);
            boolean intersectsBadge = occupiedBounds != null
                    && occupiedBounds.stream().anyMatch(candidateBounds::intersects);
            boolean overlapsTaskBar = overlapsTaskBarPredicate != null && overlapsTaskBarPredicate.test(candidateBounds);
            if (!intersectsBadge && !overlapsTaskBar) {
                chosenY = yCandidate;
                break;
            }
        }
        return new BadgePlacement(x, chosenY, width, height);
    }
}
