package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GanttDragOverlayServiceTest {

    private final GanttDragOverlayService service = new GanttDragOverlayService();

    @Test
    void badgePositionShouldClampInsideChartBounds() {
        GanttDragOverlayService.BadgePosition position = service.badgePosition(300.0, 50.0, 80.0, 20.0, 320.0);
        assertEquals(240.0, position.x());
        assertEquals(22.0, position.y());
    }

    @Test
    void markerGuideXShouldMapDateToCenterOfDayCell() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate marker = LocalDate.of(2026, 4, 4);
        assertEquals(35.0, service.markerGuideX(start, marker, 10.0));
    }

    @Test
    void itemGuideRangeShouldCalculateStartAndEndCoordinates() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate newStart = LocalDate.of(2026, 4, 3);
        LocalDate newEnd = LocalDate.of(2026, 4, 6);
        GanttDragOverlayService.GuideRange range = service.itemGuideRange(start, newStart, newEnd, 10.0);
        assertEquals(20.0, range.startX());
        assertEquals(60.0, range.endX());
    }
}
