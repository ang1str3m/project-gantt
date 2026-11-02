package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttDateServiceTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final GanttDateService service = new GanttDateService();

    @Test
    void weekNumberInMonthShouldBeStableFromMonday() {
        assertEquals(1, service.weekNumberInMonth(LocalDate.of(2026, 4, 1)));
        assertEquals(2, service.weekNumberInMonth(LocalDate.of(2026, 4, 7)));
    }

    @Test
    void shortDayNameShouldReturnRussianAbbreviation() {
        assertEquals("Пн", service.shortDayName(DayOfWeek.MONDAY));
        assertEquals("Вс", service.shortDayName(DayOfWeek.SUNDAY));
    }

    @Test
    void weekendChecksShouldWorkForRanges() {
        assertTrue(service.isWeekend(LocalDate.of(2026, 4, 4)));
        assertFalse(service.isWeekend(LocalDate.of(2026, 4, 6)));
        assertTrue(service.containsWeekend(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 5)));
        assertFalse(service.containsWeekend(LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 8)));
    }

    @Test
    void buildDateBlockLabelShouldAdjustByRange() {
        assertEquals("5", service.buildDateBlockLabel(LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 5)));
        assertEquals("5-8", service.buildDateBlockLabel(LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 8), 4));
        assertEquals("5.4-18.4", service.buildDateBlockLabel(LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 18), 14));
        assertEquals("29/4-3/5", service.buildDateBlockLabel(LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 3), 5));
    }

    @Test
    void formatPeriodShouldHandlePartialDates() {
        assertEquals("Не задан", service.formatPeriod(null, null, FORMATTER));
        assertEquals("до 07.04.2026", service.formatPeriod(null, LocalDate.of(2026, 4, 7), FORMATTER));
        assertEquals("с 07.04.2026", service.formatPeriod(LocalDate.of(2026, 4, 7), null, FORMATTER));
        assertEquals("07.04.2026 - 09.04.2026", service.formatPeriod(LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 9), FORMATTER));
    }
}
