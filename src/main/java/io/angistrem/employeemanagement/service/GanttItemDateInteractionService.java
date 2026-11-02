package io.angistrem.employeemanagement.service;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class GanttItemDateInteractionService {

    private final GanttDragService ganttDragService = new GanttDragService();

    public record DragSession(
            Object item,
            Node node,
            Rectangle anchor,
            GanttDragService.DragMode mode,
            LocalDate originalStart,
            LocalDate originalEnd,
            boolean milestone,
            double pressChartX
    ) {
    }

    @FunctionalInterface
    public interface DragPreviewHandler {
        void preview(DragSession session, LocalDate newStart, LocalDate newEnd, double pointerX);
    }

    @FunctionalInterface
    public interface DragCommitHandler {
        void commit(DragSession session, LocalDate newStart, LocalDate newEnd);
    }

    public long deltaDays(double currentChartX, double pressChartX, double dayWidth) {
        return Math.round((currentChartX - pressChartX) / Math.max(dayWidth, 0.0001));
    }

    public boolean hasRangeChanged(LocalDate newStart, LocalDate newEnd, LocalDate originalStart, LocalDate originalEnd) {
        return !Objects.equals(newStart, originalStart) || !Objects.equals(newEnd, originalEnd);
    }

    public void installDateInteraction(Node node,
                                       Rectangle anchor,
                                       Object item,
                                       boolean milestone,
                                       boolean dependencyCreationMode,
                                       double dayWidth,
                                       Function<MouseEvent, Double> chartXProvider,
                                       Function<Object, LocalDate> itemStartProvider,
                                       Function<Object, LocalDate> itemEndProvider,
                                       Consumer<Object> selectionHandler,
                                       DragPreviewHandler previewHandler,
                                       Runnable clearPreviewHandler,
                                       DragCommitHandler commitHandler) {
        if (node == null || anchor == null || item == null || chartXProvider == null || itemStartProvider == null || itemEndProvider == null) {
            return;
        }
        if (item instanceof io.angistrem.employeemanagement.model.Task task && task.isGroup()) {
            return;
        }

        final DragSession[] activeDrag = new DragSession[1];

        node.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            if (dependencyCreationMode || activeDrag[0] != null) {
                return;
            }
            GanttDragService.DragMode hoverMode = ganttDragService.resolveDragMode(
                    chartXProvider.apply(event),
                    anchor.getX(),
                    anchor.getWidth(),
                    milestone
            );
            node.setCursor(ganttDragService.resolveCursor(hoverMode));
        });

        node.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            if (activeDrag[0] == null) {
                node.setCursor(Cursor.DEFAULT);
            }
        });

        node.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (dependencyCreationMode || !event.isPrimaryButtonDown()) {
                return;
            }
            LocalDate originalStart = itemStartProvider.apply(item);
            LocalDate originalEnd = itemEndProvider.apply(item);
            if (originalStart == null || originalEnd == null) {
                return;
            }
            GanttDragService.DragMode mode = ganttDragService.resolveDragMode(
                    chartXProvider.apply(event),
                    anchor.getX(),
                    anchor.getWidth(),
                    milestone
            );
            activeDrag[0] = new DragSession(
                    item,
                    node,
                    new Rectangle(anchor.getX(), anchor.getY(), anchor.getWidth(), anchor.getHeight()),
                    mode,
                    originalStart,
                    originalEnd,
                    milestone,
                    chartXProvider.apply(event)
            );
            if (selectionHandler != null) {
                selectionHandler.accept(item);
            }
            event.consume();
        });

        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            DragSession session = activeDrag[0];
            if (session == null) {
                return;
            }
            long deltaDays = deltaDays(chartXProvider.apply(event), session.pressChartX(), dayWidth);
            GanttDragService.DraggedRange previewRange = ganttDragService.computeDraggedRange(
                    session.originalStart(),
                    session.originalEnd(),
                    session.mode(),
                    deltaDays
            );
            if (previewHandler != null) {
                previewHandler.preview(session, previewRange.start(), previewRange.end(), chartXProvider.apply(event));
            }
            event.consume();
        });

        node.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            DragSession session = activeDrag[0];
            if (session == null) {
                return;
            }
            long deltaDays = deltaDays(chartXProvider.apply(event), session.pressChartX(), dayWidth);
            GanttDragService.DraggedRange previewRange = ganttDragService.computeDraggedRange(
                    session.originalStart(),
                    session.originalEnd(),
                    session.mode(),
                    deltaDays
            );
            activeDrag[0] = null;
            if (node instanceof Region region) {
                region.setCursor(Cursor.DEFAULT);
            } else {
                node.setCursor(Cursor.DEFAULT);
            }
            if (clearPreviewHandler != null) {
                clearPreviewHandler.run();
            }
            if (commitHandler != null) {
                commitHandler.commit(session, previewRange.start(), previewRange.end());
            }
            event.consume();
        });
    }
}
