package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropExecutionServiceTest {

    private final DropExecutionService service = new DropExecutionService();

    @Test
    void handleTaskDropShouldRouteToCorrectActions() {
        Task dragged = task("1.", "Dragged");
        Task group = task("2.", "Group");
        group.setTaskType("GROUP");
        Task target = task("3.", "Target");

        AtomicBoolean groupCalled = new AtomicBoolean(false);
        AtomicBoolean relativeBeforeCalled = new AtomicBoolean(false);
        AtomicBoolean rootCalled = new AtomicBoolean(false);

        boolean intoGroup = service.handleTaskDrop(
                dragged,
                group,
                TaskInteractionService.DropMode.INTO_GROUP,
                (t, g) -> groupCalled.set(true),
                (t, tar, after) -> relativeBeforeCalled.set(!after),
                t -> rootCalled.set(true)
        );
        boolean before = service.handleTaskDrop(
                dragged,
                target,
                TaskInteractionService.DropMode.BEFORE,
                (t, g) -> groupCalled.set(true),
                (t, tar, after) -> relativeBeforeCalled.set(!after),
                t -> rootCalled.set(true)
        );
        boolean toRoot = service.handleTaskDrop(
                dragged,
                target,
                TaskInteractionService.DropMode.TO_ROOT,
                (t, g) -> groupCalled.set(true),
                (t, tar, after) -> relativeBeforeCalled.set(!after),
                t -> rootCalled.set(true)
        );

        assertTrue(intoGroup);
        assertTrue(before);
        assertTrue(toRoot);
        assertTrue(groupCalled.get());
        assertTrue(relativeBeforeCalled.get());
        assertTrue(rootCalled.get());
    }

    @Test
    void handleTaskDropShouldRejectInvalidCases() {
        Task dragged = task("1.", "Dragged");
        Task nonGroup = task("2.", "Target");

        boolean none = service.handleTaskDrop(
                dragged,
                nonGroup,
                TaskInteractionService.DropMode.NONE,
                (t, g) -> { },
                (t, tar, after) -> { },
                t -> { }
        );
        boolean invalidGroupTarget = service.handleTaskDrop(
                dragged,
                nonGroup,
                TaskInteractionService.DropMode.INTO_GROUP,
                (t, g) -> { },
                (t, tar, after) -> { },
                t -> { }
        );

        assertFalse(none);
        assertFalse(invalidGroupTarget);
    }

    @Test
    void handleSubTaskDropShouldRouteToRelativeAndIntoTask() {
        Task parent = task("1.", "Parent");
        Task targetTask = task("2.", "Target");
        SubTask dragged = new SubTask(parent, "Dragged", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 1, 0);
        SubTask targetSubTask = new SubTask(parent, "Target", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 1, 0);
        targetTask.getObservableSubTasks().add(new SubTask(targetTask, "Existing", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 1, 0));

        AtomicBoolean relativeAfterCalled = new AtomicBoolean(false);
        AtomicReference<Integer> intoTaskIndex = new AtomicReference<>(-1);

        boolean after = service.handleSubTaskDrop(
                dragged,
                targetSubTask,
                TaskInteractionService.DropMode.AFTER,
                (s, t, insertAfter) -> relativeAfterCalled.set(insertAfter),
                (s, t, index) -> intoTaskIndex.set(index)
        );
        boolean intoTask = service.handleSubTaskDrop(
                dragged,
                targetTask,
                TaskInteractionService.DropMode.INTO_TASK,
                (s, t, insertAfter) -> relativeAfterCalled.set(insertAfter),
                (s, t, index) -> intoTaskIndex.set(index)
        );

        assertTrue(after);
        assertTrue(intoTask);
        assertTrue(relativeAfterCalled.get());
        assertTrue(intoTaskIndex.get() >= 1);
    }

    private Task task(String position, String name) {
        Task task = new Task(name, "Company", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), TaskStatus.PLANNED, "Medium");
        task.setPosition(position);
        return task;
    }
}
