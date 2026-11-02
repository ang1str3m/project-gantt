package io.angistrem.employeemanagement.service;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;

import java.util.List;
import java.util.function.Predicate;

public class FinancialMarkerLayoutService {

    public Point2D chooseBadgePosition(double markerX,
                                       double rowY,
                                       double badgeWidth,
                                       double badgeHeight,
                                       double chartRightLimit,
                                       Predicate<BoundingBox> overlapPredicate) {
        double chartRight = chartRightLimit - 2.0;
        double rightX = Math.min(chartRight - badgeWidth, markerX + 8.0);
        double leftX = Math.max(2.0, markerX - badgeWidth - 8.0);
        double aboveY = Math.max(2.0, rowY - badgeHeight + 8.0);
        double belowY = rowY + 4.0;

        List<Point2D> candidates = List.of(
                new Point2D(rightX, aboveY),
                new Point2D(leftX, aboveY),
                new Point2D(rightX, belowY),
                new Point2D(leftX, belowY)
        );

        for (Point2D candidate : candidates) {
            BoundingBox bounds = new BoundingBox(candidate.getX(), candidate.getY(), badgeWidth, badgeHeight);
            boolean overlaps = overlapPredicate != null && overlapPredicate.test(bounds);
            if (!overlaps) {
                return candidate;
            }
        }
        return candidates.get(0);
    }
}
