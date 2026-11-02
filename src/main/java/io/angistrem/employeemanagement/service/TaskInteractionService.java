package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class TaskInteractionService {

    public enum DropMode {
        NONE,
        BEFORE,
        AFTER,
        INTO_GROUP,
        TO_ROOT,
        INTO_TASK
    }

    public DropMode resolveDropMode(
            Task draggedTask,
            SubTask draggedSubTask,
            Object rowItem,
            boolean rowEmpty,
            double rowHeight,
            double pointerY,
            Predicate<Task> canMoveTaskToRoot,
            BiPredicate<Task, Task> canMoveTaskIntoGroup,
            BiPredicate<Task, Task> canReorderTaskRelative,
            BiPredicate<SubTask, Task> canMoveSubTaskToTask
    ) {
        if (draggedTask == null && draggedSubTask == null) {
            return DropMode.NONE;
        }
        if (draggedTask != null) {
            return resolveTaskDropMode(
                    draggedTask,
                    rowItem,
                    rowEmpty,
                    rowHeight,
                    pointerY,
                    canMoveTaskToRoot,
                    canMoveTaskIntoGroup,
                    canReorderTaskRelative
            );
        }
        return resolveSubTaskDropMode(
                draggedSubTask,
                rowItem,
                rowEmpty,
                rowHeight,
                pointerY,
                canMoveSubTaskToTask
        );
    }

    private DropMode resolveTaskDropMode(
            Task draggedTask,
            Object rowItem,
            boolean rowEmpty,
            double rowHeight,
            double pointerY,
            Predicate<Task> canMoveTaskToRoot,
            BiPredicate<Task, Task> canMoveTaskIntoGroup,
            BiPredicate<Task, Task> canReorderTaskRelative
    ) {
        if (rowEmpty) {
            return canMoveTaskToRoot.test(draggedTask) ? DropMode.TO_ROOT : DropMode.NONE;
        }
        if (!(rowItem instanceof Task targetTask)) {
            return DropMode.NONE;
        }

        double safeRowHeight = Math.max(rowHeight, 1.0);
        double upperZone = safeRowHeight * 0.25;
        double lowerZone = safeRowHeight * 0.75;

        if (targetTask.isGroup()
                && pointerY >= upperZone
                && pointerY <= lowerZone
                && canMoveTaskIntoGroup.test(draggedTask, targetTask)) {
            return DropMode.INTO_GROUP;
        }
        if (!canReorderTaskRelative.test(draggedTask, targetTask)) {
            return DropMode.NONE;
        }
        return pointerY < safeRowHeight / 2.0 ? DropMode.BEFORE : DropMode.AFTER;
    }

    private DropMode resolveSubTaskDropMode(
            SubTask draggedSubTask,
            Object rowItem,
            boolean rowEmpty,
            double rowHeight,
            double pointerY,
            BiPredicate<SubTask, Task> canMoveSubTaskToTask
    ) {
        if (draggedSubTask == null || rowEmpty) {
            return DropMode.NONE;
        }
        if (rowItem instanceof SubTask targetSubTask) {
            if (targetSubTask == draggedSubTask) {
                return DropMode.NONE;
            }
            return pointerY < Math.max(rowHeight, 1.0) / 2.0 ? DropMode.BEFORE : DropMode.AFTER;
        }
        if (rowItem instanceof Task targetTask && canMoveSubTaskToTask.test(draggedSubTask, targetTask)) {
            return DropMode.INTO_TASK;
        }
        return DropMode.NONE;
    }
}

