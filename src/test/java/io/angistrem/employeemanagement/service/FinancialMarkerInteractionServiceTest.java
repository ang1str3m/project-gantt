package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialMarkerInteractionServiceTest {

    private final FinancialMarkerInteractionService service = new FinancialMarkerInteractionService();

    @Test
    void deltaDaysShouldRespectDayWidth() {
        assertEquals(3, service.deltaDays(130.0, 100.0, 10.0));
        assertEquals(-2, service.deltaDays(79.0, 100.0, 10.0));
    }

    @Test
    void deltaDaysShouldProtectAgainstZeroDayWidth() {
        assertEquals(0, service.deltaDays(100.00002, 100.0, 0.0));
    }

    @Test
    void shiftedDateShouldMoveFromOriginalDate() {
        LocalDate original = LocalDate.of(2026, 4, 2);
        assertEquals(LocalDate.of(2026, 4, 7), service.shiftedDate(original, 5));
        assertEquals(LocalDate.of(2026, 3, 31), service.shiftedDate(original, -2));
        assertNull(service.shiftedDate(null, 1));
    }

    @Test
    void hasDateChangedShouldDetectChanges() {
        LocalDate original = LocalDate.of(2026, 4, 2);
        assertTrue(service.hasDateChanged(LocalDate.of(2026, 4, 3), original));
        assertFalse(service.hasDateChanged(LocalDate.of(2026, 4, 2), original));
        assertFalse(service.hasDateChanged(null, null));
    }
}
