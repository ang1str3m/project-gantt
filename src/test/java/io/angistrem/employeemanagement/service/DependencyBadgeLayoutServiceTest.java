package io.angistrem.employeemanagement.service;

import javafx.geometry.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyBadgeLayoutServiceTest {

    private final DependencyBadgeLayoutService service = new DependencyBadgeLayoutService();

    @Test
    void baseBadgeStyleShouldContainExpectedTokens() {
        String style = service.baseBadgeStyle();
        assertTrue(style.contains("rgba(255,255,255,0.93)"));
        assertTrue(style.contains("-fx-font-weight: bold"));
    }

    @Test
    void computePlacementShouldAvoidOccupiedBounds() {
        List<BoundingBox> occupied = new ArrayList<>();
        occupied.add(new BoundingBox(90, 50, 50, 20));

        DependencyBadgeLayoutService.BadgePlacement placement = service.computePlacement(
                110,
                130,
                62,
                40,
                14,
                28,
                500,
                280,
                occupied,
                bounds -> false
        );

        assertTrue(placement.y() != 50);
        assertTrue(placement.x() >= 8.0);
    }

    @Test
    void computePlacementShouldUseAlternativeWhenTaskBarOverlaps() {
        DependencyBadgeLayoutService.BadgePlacement placement = service.computePlacement(
                120,
                160,
                18,
                36,
                14,
                28,
                420,
                240,
                null,
                bounds -> true
        );

        assertEquals(12.0, placement.y(), 30.0);
        assertTrue(placement.width() >= 34.0);
    }
}
