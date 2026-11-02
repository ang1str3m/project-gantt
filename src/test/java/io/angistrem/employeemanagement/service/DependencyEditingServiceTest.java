package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyEditingServiceTest {

    private final DependencyEditingService service = new DependencyEditingService();

    @Test
    void upsertDependencyShouldCreateAndUpdateByPredecessorKey() {
        Task source = task("1.", "Source");
        Task target = task("2.", "Target");

        boolean created = service.upsertDependency(source, target, "FS", 1, false);
        assertTrue(created);
        assertEquals(1, target.getDependencies().size());
        assertEquals("1.", target.getDependencies().get(0).getPredecessorPosition());

        boolean updated = service.upsertDependency(source, target, "SS", -2, true);
        assertTrue(updated);
        assertEquals(1, target.getDependencies().size());
        TaskDependency dependency = target.getDependencies().get(0);
        assertEquals("SS", dependency.getType());
        assertEquals(-2, dependency.getLagDays());
        assertTrue(dependency.isCritical());
    }

    @Test
    void upsertDependencyShouldWorkForSubTaskTarget() {
        Task source = task("1.", "Source");
        Task parent = task("2.", "Parent");
        SubTask targetSubTask = new SubTask(parent, "Sub", "Company", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 10, 0);
        parent.setSubTasks(List.of(targetSubTask));

        boolean created = service.upsertDependency(source, targetSubTask, "FF", 0, false);

        assertTrue(created);
        assertEquals(1, targetSubTask.getDependencies().size());
        assertEquals("1.", targetSubTask.getDependencies().get(0).getPredecessorPosition());
    }

    @Test
    void applyEditShouldMutateDependency() {
        TaskDependency dependency = new TaskDependency("1.", "FS", 0, false);

        service.applyEdit(dependency, "SF", 3, true);

        assertEquals("SF", dependency.getType());
        assertEquals(3, dependency.getLagDays());
        assertTrue(dependency.isCritical());
    }

    @Test
    void itemLabelShouldReturnFriendlyLabel() {
        Task task = task("7.", "TaskName");
        SubTask subTask = new SubTask(task, "SubName", "Company", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 1, 0);
        subTask.setSubPosition("7.1.");

        assertEquals("7. TaskName", service.itemLabel(task));
        assertEquals("7.1. SubName", service.itemLabel(subTask));
        assertEquals("элемент", service.itemLabel(new Object()));
        assertFalse(service.upsertDependency(task, task, "FS", 0, false));
    }

    private Task task(String position, String name) {
        Task task = new Task(name, "Company", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), TaskStatus.PLANNED, "Medium");
        task.setPosition(position);
        return task;
    }
}
