package io.angistrem.employeemanagement.service;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialMarkerLayoutServiceTest {

    private final FinancialMarkerLayoutService service = new FinancialMarkerLayoutService();

    @Test
    void chooseBadgePositionShouldPreferRightAboveWhenFree() {
        Point2D position = service.chooseBadgePosition(
                100.0,
                80.0,
                40.0,
                16.0,
                500.0,
                bounds -> false
        );

        assertTrue(position.getX() >= 100.0);
        assertTrue(position.getY() <= 80.0);
    }

    @Test
    void chooseBadgePositionShouldFallbackToFirstCandidateWhenAllBusy() {
        Point2D position = service.chooseBadgePosition(
                100.0,
                30.0,
                40.0,
                16.0,
                500.0,
                bounds -> true
        );

        double expectedX = Math.min(498.0 - 40.0, 108.0);
        double expectedY = Math.max(2.0, 30.0 - 16.0 + 8.0);
        assertEquals(expectedX, position.getX(), 1e-6);
        assertEquals(expectedY, position.getY(), 1e-6);
    }

    @Test
    void chooseBadgePositionShouldTryAlternativesWhenFirstOverlaps() {
        Point2D position = service.chooseBadgePosition(
                120.0,
                90.0,
                44.0,
                16.0,
                400.0,
                bounds -> bounds.intersects(new BoundingBox(128.0, 82.0, 50.0, 30.0))
        );

        assertTrue(position.getX() <= 120.0 || position.getY() > 90.0);
    }
}
