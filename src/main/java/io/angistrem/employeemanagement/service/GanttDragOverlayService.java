package io.angistrem.employeemanagement.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class GanttDragOverlayService {

    public record BadgePosition(double x, double y) {
    }

    public record GuideRange(double startX, double endX) {
    }

    public BadgePosition badgePosition(double pointerX,
                                       double anchorY,
                                       double badgeWidth,
                                       double badgeHeight,
                                       double chartRightLimit) {
        double x = Math.max(8.0, Math.min(pointerX + 14.0, chartRightLimit - badgeWidth));
        double y = Math.max(8.0, anchorY - badgeHeight - 8.0);
        return new BadgePosition(x, y);
    }

    public double markerGuideX(LocalDate startDate, LocalDate markerDate, double dayWidth) {
        if (startDate == null || markerDate == null) {
            return 0.0;
        }
        return ChronoUnit.DAYS.between(startDate, markerDate) * dayWidth + (dayWidth / 2.0);
    }

    public GuideRange itemGuideRange(LocalDate startDate, LocalDate newStart, LocalDate newEnd, double dayWidth) {
        if (startDate == null || newStart == null || newEnd == null) {
            return new GuideRange(0.0, 0.0);
        }
        double startX = ChronoUnit.DAYS.between(startDate, newStart) * dayWidth;
        double endX = ChronoUnit.DAYS.between(startDate, newEnd) * dayWidth + dayWidth;
        return new GuideRange(startX, endX);
    }
}
