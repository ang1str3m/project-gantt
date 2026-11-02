package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyInteractionServiceTest {

    private final DependencyInteractionService service = new DependencyInteractionService();

    @Test
    void strokeWidthShouldRespectHighlightAndHoverStates() {
        assertEquals(2.45, service.strokeWidth(true, false));
        assertEquals(2.95, service.strokeWidth(true, true));
        assertEquals(1.15, service.strokeWidth(false, false));
        assertEquals(1.8, service.strokeWidth(false, true));
    }

    @Test
    void arrowScaleShouldToggleBetweenNormalAndHover() {
        assertEquals(1.0, service.arrowScale(false));
        assertEquals(1.08, service.arrowScale(true));
    }

    @Test
    void highlightedBadgeStyleShouldContainExpectedTokens() {
        String style = service.highlightedBadgeStyle();
        assertTrue(style.contains("#92400e"));
        assertTrue(style.contains("rgba(255,251,235,0.98)"));
        assertTrue(style.contains("-fx-font-weight: bold"));
    }
}
