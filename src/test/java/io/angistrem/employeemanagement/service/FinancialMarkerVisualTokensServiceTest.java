package io.angistrem.employeemanagement.service;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialMarkerVisualTokensServiceTest {

    private final FinancialMarkerVisualTokensService service = new FinancialMarkerVisualTokensService();

    @Test
    void geometryAndBadgeTokensShouldStayStable() {
        assertEquals(2.0, service.lineTopOffset());
        assertEquals(2.0, service.lineBottomOffset());
        assertEquals(22.0, service.hitWidth());
        assertEquals(16.0, service.badgeHeight());
        assertEquals(10.0, service.badgeArc());
        assertEquals(Color.rgb(255, 255, 255, 0.92), service.badgeStroke());
    }

    @Test
    void lineAndDotTokensShouldDependOnBlocking() {
        assertEquals(0.22, service.glowOpacity(true));
        assertEquals(0.14, service.glowOpacity(false));
        assertEquals(9.0, service.glowWidth(true));
        assertEquals(6.8, service.glowWidth(false));
        assertEquals(2.6, service.markerWidth(true));
        assertEquals(2.2, service.markerWidth(false));
        assertArrayEquals(new double[]{5.0, 3.0}, service.markerDashPattern(true), 0.0001);
        assertArrayEquals(new double[]{3.0, 3.5}, service.markerDashPattern(false), 0.0001);
        assertEquals(4.8, service.headDotRadius(true));
        assertEquals(4.0, service.headDotRadius(false));
    }
}
