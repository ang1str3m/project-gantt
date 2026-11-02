package io.angistrem.employeemanagement.service;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskVisualTokensServiceTest {

    private final TaskVisualTokensService service = new TaskVisualTokensService();

    @Test
    void subTaskTokensShouldMatchSelectionAndCriticalStates() {
        assertEquals(Color.web("#f59e0b"), service.subTaskStroke(true, false));
        assertEquals(Color.web("#0f172a"), service.subTaskStroke(false, true));
        assertEquals(Color.web("#0f766e"), service.subTaskStroke(false, false));
        assertEquals(1.9, service.subTaskStrokeWidth(true, false));
        assertEquals(1.35, service.subTaskStrokeWidth(false, true));
        assertEquals(0.5, service.subTaskStrokeWidth(false, false));
        assertEquals(0.88, service.subTaskBaseOpacity(false, false));
    }

    @Test
    void taskTokensShouldMatchSelectionAndCriticalStates() {
        assertEquals(Color.web("#f59e0b"), service.taskStroke(true, false));
        assertEquals(Color.web("#0f172a"), service.taskStroke(false, true));
        assertEquals(Color.web("#1d4ed8"), service.taskStroke(false, false));
        assertEquals(2.15, service.taskStrokeWidth(true, false));
        assertEquals(1.4, service.taskStrokeWidth(false, true));
        assertEquals(0.7, service.taskStrokeWidth(false, false));
        assertEquals(0.95, service.taskBaseOpacity(false, false));
    }

    @Test
    void milestoneTokensShouldExposeDedicatedWidths() {
        assertEquals(2.1, service.milestoneStrokeWidth(true, false));
        assertEquals(1.45, service.milestoneStrokeWidth(false, true));
        assertEquals(0.9, service.milestoneStrokeWidth(false, false));
        assertEquals(2.4, service.milestoneHoverStrokeWidth(true, false));
        assertEquals(1.6, service.milestoneHoverStrokeWidth(false, true));
        assertEquals(1.3, service.milestoneHoverStrokeWidth(false, false));
    }
}
