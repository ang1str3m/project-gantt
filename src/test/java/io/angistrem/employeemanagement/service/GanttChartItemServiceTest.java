package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttChartItemServiceTest {

    private final GanttChartItemService service = new GanttChartItemService();

    @Test
    void resolveOrderedItemsShouldReturnExplicitOrderWhenProvided() {
        Task task = buildTask("Task", "1.");
        List<Object> explicit = List.of(task);

        List<Object> actual = service.resolveOrderedItems(List.of(task), explicit, Set.of());

        assertSame(explicit, actual);
    }

    @Test
    void resolveOrderedItemsShouldIncludeExpandedGroupChildrenAndSubTasks() {
        Task group = buildTask("Group", "1.");
        group.setTaskType("GROUP");
        Task childTask = buildTask("Child", "1.1.");
        SubTask childSubTask = buildSubTask(childTask, "Sub");
        childTask.getObservableSubTasks().add(childSubTask);
        group.getObservableChildTasks().add(childTask);

        Set<Object> visible = Set.of(group + "_expanded", childTask + "_expanded");

        List<Object> actual = service.resolveOrderedItems(List.of(group), List.of(), visible);

        assertEquals(List.of(group, childTask, childSubTask), actual);
    }

    @Test
    void resolveOrderedItemsShouldSkipCollapsedChildrenWhenExpansionStateProvided() {
        Task group = buildTask("Group", "1.");
        group.setTaskType("GROUP");
        Task childTask = buildTask("Child", "1.1.");
        group.getObservableChildTasks().add(childTask);

        List<Object> actual = service.resolveOrderedItems(List.of(group), List.of(), Set.of(group));

        assertEquals(List.of(group), actual);
    }

    @Test
    void visibilityChecksShouldRespectVisibleItemsSet() {
        Task task = buildTask("Task", "1.");
        SubTask subTask = buildSubTask(task, "Sub");

        assertTrue(service.isTaskVisible(task, null));
        assertTrue(service.isSubTaskVisible(subTask, Set.of()));
        assertTrue(service.isTaskVisible(task, Set.of(task)));
        assertFalse(service.isSubTaskVisible(subTask, Set.of(task)));
    }

    private Task buildTask(String name, String position) {
        Task task = new Task(name, "Company", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), TaskStatus.PLANNED, "Medium");
        task.setPosition(position);
        return task;
    }

    private SubTask buildSubTask(Task task, String name) {
        return new SubTask(task, name, "Company", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 10, 0);
    }
}
