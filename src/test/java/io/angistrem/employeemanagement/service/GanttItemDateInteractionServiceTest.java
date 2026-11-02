package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttItemDateInteractionServiceTest {

    private final GanttItemDateInteractionService service = new GanttItemDateInteractionService();

    @Test
    void deltaDaysShouldBeRoundedByDayWidth() {
        assertEquals(3, service.deltaDays(130.0, 100.0, 10.0));
        assertEquals(-2, service.deltaDays(81.0, 100.0, 10.0));
    }

    @Test
    void deltaDaysShouldProtectAgainstZeroDayWidth() {
        assertEquals(0, service.deltaDays(100.00002, 100.0, 0.0));
    }

    @Test
    void hasRangeChangedShouldDetectDateChanges() {
        LocalDate originalStart = LocalDate.of(2026, 4, 2);
        LocalDate originalEnd = LocalDate.of(2026, 4, 7);
        assertFalse(service.hasRangeChanged(originalStart, originalEnd, originalStart, originalEnd));
        assertTrue(service.hasRangeChanged(LocalDate.of(2026, 4, 3), originalEnd, originalStart, originalEnd));
        assertTrue(service.hasRangeChanged(originalStart, LocalDate.of(2026, 4, 8), originalStart, originalEnd));
    }
}
