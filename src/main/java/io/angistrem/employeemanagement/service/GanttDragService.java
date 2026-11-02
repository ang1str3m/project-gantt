package io.angistrem.employeemanagement.service;

import javafx.scene.Cursor;

import java.time.LocalDate;

public class GanttDragService {

    public enum DragMode {
        MOVE,
        RESIZE_START,
        RESIZE_END
    }

    public record DraggedRange(LocalDate start, LocalDate end) {
    }

    public DragMode resolveDragMode(double pointerX, double anchorX, double anchorWidth, boolean milestone) {
        if (milestone) {
            return DragMode.MOVE;
        }
        double edgeZone = Math.min(12.0, Math.max(5.0, anchorWidth * 0.18));
        if (pointerX <= anchorX + edgeZone) {
            return DragMode.RESIZE_START;
        }
        if (pointerX >= anchorX + anchorWidth - edgeZone) {
            return DragMode.RESIZE_END;
        }
        return DragMode.MOVE;
    }

    public Cursor resolveCursor(DragMode mode) {
        return switch (mode) {
            case RESIZE_START -> Cursor.W_RESIZE;
            case RESIZE_END -> Cursor.E_RESIZE;
            case MOVE -> Cursor.H_RESIZE;
        };
    }

    public DraggedRange computeDraggedRange(LocalDate originalStart, LocalDate originalEnd, DragMode mode, long deltaDays) {
        LocalDate newStart = originalStart;
        LocalDate newEnd = originalEnd;
        switch (mode) {
            case MOVE -> {
                newStart = originalStart.plusDays(deltaDays);
                newEnd = originalEnd.plusDays(deltaDays);
            }
            case RESIZE_START -> {
                newStart = originalStart.plusDays(deltaDays);
                if (newStart.isAfter(originalEnd)) {
                    newStart = originalEnd;
                }
            }
            case RESIZE_END -> {
                newEnd = originalEnd.plusDays(deltaDays);
                if (newEnd.isBefore(originalStart)) {
                    newEnd = originalStart;
                }
            }
        }
        return new DraggedRange(newStart, newEnd);
    }
}
