package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskDependencySchedulerTest {

    @Test
    void shouldShiftTaskByFsDependencyWithLag() {
        Task predecessor = new Task("A", "C1", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5),
                TaskStatus.IN_PROGRESS, "Средний");
        predecessor.setPosition("1.");

        Task dependent = new Task("B", "C2", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6),
                TaskStatus.PLANNED, "Средний");
        dependent.setPosition("2.");
        dependent.setDependencies(List.of(new TaskDependency("1.", "FS", 2)));

        TaskDependencyScheduler.applyConstraints(List.of(predecessor, dependent));

        assertEquals(LocalDate.of(2026, 3, 8), dependent.getStart());
        assertEquals(LocalDate.of(2026, 3, 12), dependent.getEnd());
    }

    @Test
    void shouldShiftTaskBySsDependency() {
        Task predecessor = new Task("A", "C1", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5),
                TaskStatus.IN_PROGRESS, "Средний");
        predecessor.setPosition("1.");

        Task dependent = new Task("B", "C2", LocalDate.of(2026, 2, 27), LocalDate.of(2026, 3, 3),
                TaskStatus.PLANNED, "Средний");
        dependent.setPosition("2.");
        dependent.setDependencies(List.of(new TaskDependency("1.", "SS", 1)));

        TaskDependencyScheduler.applyConstraints(List.of(predecessor, dependent));

        assertEquals(LocalDate.of(2026, 3, 2), dependent.getStart());
        assertEquals(LocalDate.of(2026, 3, 6), dependent.getEnd());
    }

    @Test
    void shouldShiftTaskByAdvanceDateWhenEnabled() {
        Task task = new Task("Finance locked", "C", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5),
                TaskStatus.PLANNED, "Высокий");
        task.setRequireAdvanceBeforeStart(true);
        task.setContractAdvanceDate(LocalDate.of(2026, 3, 10));

        TaskDependencyScheduler.applyConstraints(List.of(task));

        assertEquals(LocalDate.of(2026, 3, 10), task.getStart());
        assertEquals(LocalDate.of(2026, 3, 14), task.getEnd());
    }

    @Test
    void shouldThrowWhenCycleDetected() {
        Task taskA = new Task("A", "C1", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5),
                TaskStatus.PLANNED, "Средний");
        taskA.setPosition("1.");

        Task taskB = new Task("B", "C2", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5),
                TaskStatus.PLANNED, "Средний");
        taskB.setPosition("2.");

        taskA.setDependencies(List.of(new TaskDependency("2.", "FS", 0)));
        taskB.setDependencies(List.of(new TaskDependency("1.", "FS", 0)));

        assertThrows(IllegalStateException.class, () -> TaskDependencyScheduler.applyConstraints(List.of(taskA, taskB)));
    }
}
