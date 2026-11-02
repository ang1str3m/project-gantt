package io.angistrem.employeemanagement.service;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GanttThemeTokensServiceTest {

    private final GanttThemeTokensService service = new GanttThemeTokensService();

    @Test
    void gridAndTodayTokensShouldMatchThemeContract() {
        assertEquals(Color.web("#e2e8f0"), service.gridLineStroke());
        assertEquals(Color.web("#94a3b8"), service.gridMajorStroke());
        assertEquals(Color.web("#64748b"), service.gridStrongStroke());
        assertEquals(Color.web("#ef4444"), service.todayLineStroke());
        assertEquals(1.8, service.todayLineWidth());
    }

    @Test
    void dragGuideAndResizeHandleTokensShouldBeStable() {
        assertEquals(1.4, service.dragGuideWidth());
        assertArrayEquals(new double[]{5.0, 4.0}, service.dragGuideDashPattern(), 0.0001);
        assertEquals(0.95, service.dragGuideOpacity());
        assertEquals(Color.rgb(255, 255, 255, 0.98), service.resizeHandleFill());
        assertEquals(Color.rgb(59, 130, 246, 0.98), service.resizeHandleStroke());
        assertEquals(1.15, service.resizeHandleStrokeWidth());
    }

    @Test
    void minorGridAndMilestoneHandleTokensShouldBeAvailable() {
        assertEquals(Color.web("#dbe4ef"), service.weekMinorStroke());
        assertEquals(Color.web("#d8e0ea"), service.quarterMinorStroke());
        assertEquals(Color.web("#e9eef7"), service.horizontalLineStroke());
        assertEquals(Color.rgb(59, 130, 246, 0.18), service.milestoneResizeHandleFill());
        assertEquals(Color.rgb(59, 130, 246, 0.9), service.milestoneResizeHandleStroke());
    }
}
