package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.service.GanttItemDateInteractionService.DragSession;
import io.angistrem.employeemanagement.service.GanttDragService.DragMode;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttTextServiceTest {

    private final GanttTextService service = new GanttTextService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Test
    void dragActionLabelShouldMapModes() {
        DragSession move = session(DragMode.MOVE, false);
        DragSession resizeStart = session(DragMode.RESIZE_START, false);
        DragSession resizeEnd = session(DragMode.RESIZE_END, false);

        assertEquals("\u0421\u0434\u0432\u0438\u0433", service.dragActionLabel(move));
        assertEquals("\u041d\u0430\u0447\u0430\u043b\u043e", service.dragActionLabel(resizeStart));
        assertEquals("\u041e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u0435", service.dragActionLabel(resizeEnd));
    }

    @Test
    void dragBadgeTextShouldRenderRangeOrSingleDate() {
        LocalDate start = LocalDate.of(2026, 4, 2);
        LocalDate end = LocalDate.of(2026, 4, 5);

        String range = service.dragBadgeText(session(DragMode.MOVE, false), start, end, formatter);
        String single = service.dragBadgeText(session(DragMode.MOVE, true), start, end, formatter);

        assertTrue(range.contains("02.04.2026 - 05.04.2026"));
        assertTrue(single.endsWith("05.04.2026"));
    }

    @Test
    void financialMarkerDragTextShouldFormatLabelAndDate() {
        String text = service.financialMarkerDragText("\u0410\u0412", LocalDate.of(2026, 4, 10), formatter);
        assertEquals("\u0410\u0412: 10.04.2026", text);
    }

    @Test
    void milestoneTooltipShouldAppendMilestoneDate() {
        String text = service.milestoneTooltip("Task tooltip", LocalDate.of(2026, 4, 12), formatter);
        assertEquals("Task tooltip\n\u0412\u0435\u0445\u0430: 12.04.2026", text);
    }

    private DragSession session(DragMode mode, boolean milestone) {
        return new DragSession(
                new Object(),
                new Rectangle(),
                new Rectangle(0, 0, 10, 10),
                mode,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 2),
                milestone,
                0.0
        );
    }
}
