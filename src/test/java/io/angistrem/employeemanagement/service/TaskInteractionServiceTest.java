package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskInteractionServiceTest {

    private final TaskInteractionService service = new TaskInteractionService();

    @Test
    void resolveDropModeShouldReturnToRootForEmptyRowWhenTaskCanMoveToRoot() {
        Task dragged = createTask("1.");

        var mode = service.resolveDropMode(
                dragged,
                null,
                null,
                true,
                24.0,
                10.0,
                task -> true,
                (task, group) -> false,
                (task, target) -> false,
                (subTask, task) -> false
        );

        assertEquals(TaskInteractionService.DropMode.TO_ROOT, mode);
    }

    @Test
    void resolveDropModeShouldReturnIntoGroupInMiddleZone() {
        Task dragged = createTask("2.");
        Task group = createTask("1.");
        group.setTaskType("GROUP");

        var mode = service.resolveDropMode(
                dragged,
                null,
                group,
                false,
                40.0,
                20.0,
                task -> false,
                (task, targetGroup) -> true,
                (task, target) -> true,
                (subTask, task) -> false
        );

        assertEquals(TaskInteractionService.DropMode.INTO_GROUP, mode);
    }

    @Test
    void resolveDropModeShouldReturnBeforeForSubTaskWhenPointerAboveHalf() {
        SubTask draggedSubTask = new SubTask();
        SubTask targetSubTask = new SubTask();

        var mode = service.resolveDropMode(
                null,
                draggedSubTask,
                targetSubTask,
                false,
                30.0,
                5.0,
                task -> false,
                (task, group) -> false,
                (task, target) -> false,
                (subTask, task) -> false
        );

        assertEquals(TaskInteractionService.DropMode.BEFORE, mode);
    }

    @Test
    void resolveDropModeShouldReturnIntoTaskForSubTaskAndTaskTarget() {
        SubTask draggedSubTask = new SubTask();
        Task targetTask = createTask("3.");

        var mode = service.resolveDropMode(
                null,
                draggedSubTask,
                targetTask,
                false,
                30.0,
                20.0,
                task -> false,
                (task, group) -> false,
                (task, target) -> false,
                (subTask, task) -> true
        );

        assertEquals(TaskInteractionService.DropMode.INTO_TASK, mode);
    }

    private Task createTask(String position) {
        Task task = new Task("Task", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), TaskStatus.PLANNED, "Medium");
        task.setPosition(position);
        return task;
    }
}

