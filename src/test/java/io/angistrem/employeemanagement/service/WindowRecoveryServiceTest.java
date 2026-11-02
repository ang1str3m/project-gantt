package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowRecoveryServiceTest {

    private final WindowRecoveryService service = new WindowRecoveryService();

    @Test
    void isLayoutCollapsedShouldUseEffectiveWidthAgainstTarget() {
        assertTrue(service.isLayoutCollapsed(1200, 1100, 900, 0, 0, 0));
        assertFalse(service.isLayoutCollapsed(1200, 1100, 1000, 0, 0, 0));
        assertFalse(service.isLayoutCollapsed(0, 0, 500, 0, 0, 0));
    }

    @Test
    void resolveTargetSizeShouldPreferScreenWhenMaximized() {
        WindowRecoveryService.TargetSize maximized = service.resolveTargetSize(
                1000, 700, 1200, 800, true, 1920, 1080
        );
        WindowRecoveryService.TargetSize normal = service.resolveTargetSize(
                1000, 700, 1200, 800, false, 1920, 1080
        );

        assertEquals(1920, maximized.width());
        assertEquals(1080, maximized.height());
        assertEquals(1200, normal.width());
        assertEquals(800, normal.height());
    }

    @Test
    void shouldNudgeWindowShouldRequireSafeMargins() {
        assertTrue(service.shouldNudgeWindow(900, 700, 400, 300));
        assertFalse(service.shouldNudgeWindow(410, 700, 400, 300));
        assertFalse(service.shouldNudgeWindow(900, 310, 400, 300));
    }
}
