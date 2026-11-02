package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialMarkerRenderServiceTest {

    private final FinancialMarkerRenderService service = new FinancialMarkerRenderService();

    @Test
    void markerXShouldMapDateToDayCenter() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate marker = LocalDate.of(2026, 4, 4);
        assertEquals(35.0, service.markerX(start, marker, 10.0));
    }

    @Test
    void geometryShouldBuildLineAndHitArea() {
        FinancialMarkerRenderService.MarkerGeometry geometry = service.geometry(100.0, 20.0, 28.0);
        assertEquals(22.0, geometry.lineTop());
        assertEquals(46.0, geometry.lineBottom());
        assertEquals(89.0, geometry.hitX());
        assertEquals(21.0, geometry.hitY());
        assertEquals(22.0, geometry.hitWidth());
        assertEquals(26.0, geometry.hitHeight());
    }

    @Test
    void badgeMetricsShouldHaveMinimumWidthAndFixedHeight() {
        FinancialMarkerRenderService.BadgeMetrics metrics = service.badgeMetrics("A");
        assertTrue(metrics.width() >= 26.0);
        assertEquals(16.0, metrics.height());
    }

    @Test
    void tooltipTextShouldContainBlockingOrInfoMessage() {
        LocalDate date = LocalDate.of(2026, 4, 2);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String blocking = service.tooltipText("Title", date, formatter, true);
        String info = service.tooltipText("Title", date, formatter, false);
        assertTrue(blocking.contains("Title: 02.04.2026"));
        assertTrue(blocking.contains("\u0411\u043b\u043e\u043a\u0438\u0440\u0443\u0435\u0442"));
        assertTrue(info.contains("\u0418\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u043e\u043d\u043d\u0430\u044f"));
    }
}
