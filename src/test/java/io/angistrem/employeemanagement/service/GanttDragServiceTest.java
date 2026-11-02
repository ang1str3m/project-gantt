package io.angistrem.employeemanagement.service;

import javafx.scene.Cursor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GanttDragServiceTest {

    private final GanttDragService service = new GanttDragService();

    @Test
    void resolveDragModeShouldRespectMilestoneAndEdges() {
        assertEquals(GanttDragService.DragMode.MOVE, service.resolveDragMode(100, 40, 80, true));
        assertEquals(GanttDragService.DragMode.RESIZE_START, service.resolveDragMode(41, 40, 80, false));
        assertEquals(GanttDragService.DragMode.RESIZE_END, service.resolveDragMode(119, 40, 80, false));
        assertEquals(GanttDragService.DragMode.MOVE, service.resolveDragMode(80, 40, 80, false));
    }

    @Test
    void resolveCursorShouldMapModes() {
        assertEquals(Cursor.H_RESIZE, service.resolveCursor(GanttDragService.DragMode.MOVE));
        assertEquals(Cursor.W_RESIZE, service.resolveCursor(GanttDragService.DragMode.RESIZE_START));
        assertEquals(Cursor.E_RESIZE, service.resolveCursor(GanttDragService.DragMode.RESIZE_END));
    }

    @Test
    void computeDraggedRangeShouldMoveAndClamp() {
        LocalDate start = LocalDate.of(2026, 4, 10);
        LocalDate end = LocalDate.of(2026, 4, 20);

        GanttDragService.DraggedRange moved = service.computeDraggedRange(start, end, GanttDragService.DragMode.MOVE, 2);
        assertEquals(LocalDate.of(2026, 4, 12), moved.start());
        assertEquals(LocalDate.of(2026, 4, 22), moved.end());

        GanttDragService.DraggedRange resizeStart = service.computeDraggedRange(start, end, GanttDragService.DragMode.RESIZE_START, 15);
        assertEquals(end, resizeStart.start());
        assertEquals(end, resizeStart.end());

        GanttDragService.DraggedRange resizeEnd = service.computeDraggedRange(start, end, GanttDragService.DragMode.RESIZE_END, -15);
        assertEquals(start, resizeEnd.start());
        assertEquals(start, resizeEnd.end());
    }
}
