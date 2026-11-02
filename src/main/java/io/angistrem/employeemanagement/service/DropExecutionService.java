package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;

import java.util.function.Consumer;

public class DropExecutionService {

    @FunctionalInterface
    public interface TaskGroupMove {
        void move(Task task, Task group);
    }

    @FunctionalInterface
    public interface TaskRelativeMove {
        void move(Task task, Task target, boolean insertAfter);
    }

    @FunctionalInterface
    public interface SubTaskRelativeMove {
        void move(SubTask subTask, SubTask target, boolean insertAfter);
    }

    @FunctionalInterface
    public interface SubTaskIntoTaskMove {
        void move(SubTask subTask, Task target, int index);
    }

    public boolean handleTaskDrop(
            Task draggedTask,
            Object target,
            TaskInteractionService.DropMode dropMode,
            TaskGroupMove moveTaskToGroup,
            TaskRelativeMove moveTaskRelative,
            Consumer<Task> moveTaskToRoot
    ) {
        if (draggedTask == null || dropMode == TaskInteractionService.DropMode.NONE) {
            return false;
        }
        return switch (dropMode) {
            case INTO_GROUP -> {
                if (target instanceof Task targetTask && targetTask.isGroup()) {
                    moveTaskToGroup.move(draggedTask, targetTask);
                    yield true;
                }
                yield false;
            }
            case BEFORE -> {
                if (target instanceof Task targetTask) {
                    moveTaskRelative.move(draggedTask, targetTask, false);
                    yield true;
                }
                yield false;
            }
            case AFTER -> {
                if (target instanceof Task targetTask) {
                    moveTaskRelative.move(draggedTask, targetTask, true);
                    yield true;
                }
                yield false;
            }
            case TO_ROOT -> {
                moveTaskToRoot.accept(draggedTask);
                yield true;
            }
            case NONE -> false;
            default -> false;
        };
    }

    public boolean handleSubTaskDrop(
            SubTask draggedSubTask,
            Object target,
            TaskInteractionService.DropMode dropMode,
            SubTaskRelativeMove moveSubTaskRelative,
            SubTaskIntoTaskMove moveSubTaskToTask
    ) {
        if (draggedSubTask == null || dropMode == TaskInteractionService.DropMode.NONE) {
            return false;
        }
        return switch (dropMode) {
            case BEFORE -> {
                if (target instanceof SubTask targetSubTask) {
                    moveSubTaskRelative.move(draggedSubTask, targetSubTask, false);
                    yield true;
                }
                yield false;
            }
            case AFTER -> {
                if (target instanceof SubTask targetSubTask) {
                    moveSubTaskRelative.move(draggedSubTask, targetSubTask, true);
                    yield true;
                }
                yield false;
            }
            case INTO_TASK -> {
                if (target instanceof Task targetTask) {
                    moveSubTaskToTask.move(draggedSubTask, targetTask, targetTask.getObservableSubTasks().size());
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }
}
