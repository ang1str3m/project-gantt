package io.angistrem.employeemanagement.service;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class FinancialMarkerInteractionService {

    @FunctionalInterface
    public interface DragPreviewHandler {
        void show(String shortLabel, LocalDate previewDate, double pointerX, double rowY);
    }

    @FunctionalInterface
    public interface DragCommitHandler<T, M> {
        void commit(T item, M markerType, LocalDate newDate, LocalDate originalDate);
    }

    public long deltaDays(double currentChartX, double pressChartX, double dayWidth) {
        return Math.round((currentChartX - pressChartX) / Math.max(dayWidth, 0.0001));
    }

    public LocalDate shiftedDate(LocalDate originalDate, long deltaDays) {
        if (originalDate == null) {
            return null;
        }
        return originalDate.plusDays(deltaDays);
    }

    public boolean hasDateChanged(LocalDate newDate, LocalDate originalDate) {
        return !Objects.equals(newDate, originalDate);
    }

    public <T, M> void installDragHandlers(T item,
                                           M markerType,
                                           LocalDate originalDate,
                                           String shortLabel,
                                           double rowY,
                                           double dayWidth,
                                           Function<MouseEvent, Double> chartXProvider,
                                           Consumer<T> selectionHandler,
                                           DragPreviewHandler previewHandler,
                                           Runnable clearDragVisuals,
                                           Runnable refreshHandler,
                                           DragCommitHandler<T, M> commitHandler,
                                           Node... nodes) {
        if (item == null || originalDate == null || chartXProvider == null || nodes == null || nodes.length == 0) {
            return;
        }

        final double[] pressChartX = {0.0};
        final boolean[] dragging = {false};

        for (Node node : nodes) {
            if (node == null) {
                continue;
            }
            node.setCursor(Cursor.H_RESIZE);

            node.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
                if (!event.isPrimaryButtonDown()) {
                    return;
                }
                pressChartX[0] = chartXProvider.apply(event);
                dragging[0] = true;
                if (selectionHandler != null) {
                    selectionHandler.accept(item);
                }
                event.consume();
            });

            node.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
                if (!dragging[0]) {
                    return;
                }
                double pointerX = chartXProvider.apply(event);
                long deltaDays = deltaDays(pointerX, pressChartX[0], dayWidth);
                LocalDate previewDate = shiftedDate(originalDate, deltaDays);
                if (previewHandler != null) {
                    previewHandler.show(shortLabel, previewDate, pointerX, rowY);
                }
                event.consume();
            });

            node.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
                if (!dragging[0]) {
                    return;
                }
                dragging[0] = false;
                double pointerX = chartXProvider.apply(event);
                long deltaDays = deltaDays(pointerX, pressChartX[0], dayWidth);
                LocalDate newDate = shiftedDate(originalDate, deltaDays);
                if (clearDragVisuals != null) {
                    clearDragVisuals.run();
                }
                if (commitHandler != null && hasDateChanged(newDate, originalDate)) {
                    commitHandler.commit(item, markerType, newDate, originalDate);
                } else if (refreshHandler != null) {
                    refreshHandler.run();
                }
                event.consume();
            });
        }
    }
}
