package io.angistrem.employeemanagement.service;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DependencyVisualTokensServiceTest {

    private final DependencyVisualTokensService service = new DependencyVisualTokensService();

    @Test
    void resolveStrokeColorShouldRespectPriorityOrder() {
        assertEquals(Color.web("#f59e0b"), service.resolveStrokeColor(Color.BLUE, false, true));
        assertEquals(Color.web("#9f1239"), service.resolveStrokeColor(Color.BLUE, true, false));
        assertEquals(Color.BLUE, service.resolveStrokeColor(Color.BLUE, false, false));
    }

    @Test
    void arrowAndBadgeTokensShouldMatchExpectedBounds() {
        assertEquals(Color.web("#78350f"), service.arrowStroke(true));
        assertEquals(0.9, service.arrowStrokeWidth(true));
        assertEquals(34.0, service.badgeMinWidth(10.0));
        assertEquals(18.0, service.badgeMinHeight(10.0));
    }

    @Test
    void highlightAndTooltipTokensShouldBeStable() {
        assertEquals(3.2, service.highlightedHaloWidth());
        assertEquals(2.25, service.highlightedColoredPathWidth());
        assertEquals(250.0, service.tooltipDelayMillis());
    }
}
