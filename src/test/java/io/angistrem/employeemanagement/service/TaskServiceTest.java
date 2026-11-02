package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskServiceTest {

    private final TaskService taskService = new TaskService();

    @Test
    void collectAllTasksShouldIncludeNestedChildren() {
        Task root = buildTask("A", "1.");
        Task child = buildTask("B", "1.1.");
        root.getObservableChildTasks().add(child);
        List<Task> all = taskService.collectAllTasks(List.of(root));

        assertEquals(2, all.size());
        assertTrue(all.contains(root));
        assertTrue(all.contains(child));
    }

    @Test
    void nextTopLevelPositionShouldUseMaxNumericPrefix() {
        Task first = buildTask("A", "1.");
        Task second = buildTask("B", "8.");
        Task third = buildTask("C", "custom-x");

        String next = taskService.nextTopLevelPosition(List.of(first, second, third));

        assertEquals("9.", next);
    }

    @Test
    void normalizeTaskHierarchyShouldReindexAndRemapBudgetItems() {
        Task first = buildTask("A", "5.");
        Task second = buildTask("B", "9.");
        second.getObservableSubTasks().add(new SubTask());

        List<Task> tasks = new ArrayList<>(List.of(first, second));
        BudgetItem item = new BudgetItem();
        item.setTaskPosition("9.");
        List<BudgetItem> budgetItems = new ArrayList<>(List.of(item));

        taskService.normalizeTaskHierarchy(tasks, budgetItems);

        assertEquals("1.", first.getPosition());
        assertEquals("2.", second.getPosition());
        assertEquals("2.", item.getTaskPosition());
    }

    @Test
    void moveTaskToGroupShouldReparentTask() {
        Task group = buildTask("Group", "1.");
        group.setTaskType("GROUP");
        Task task = buildTask("Task", "2.");
        List<Task> roots = new ArrayList<>(List.of(group, task));

        boolean moved = taskService.moveTaskToGroup(roots, task, group);

        assertTrue(moved);
        assertEquals(1, roots.size());
        assertEquals(group, roots.get(0));
        assertTrue(group.getObservableChildTasks().contains(task));
    }

    @Test
    void moveTaskToRootShouldMoveChildToTopLevel() {
        Task group = buildTask("Group", "1.");
        group.setTaskType("GROUP");
        Task child = buildTask("Child", "1.1.");
        group.getObservableChildTasks().add(child);
        List<Task> roots = new ArrayList<>(List.of(group));

        boolean moved = taskService.moveTaskToRoot(roots, child);

        assertTrue(moved);
        assertEquals(2, roots.size());
        assertTrue(roots.contains(child));
        assertFalse(group.getObservableChildTasks().contains(child));
    }

    @Test
    void moveTaskRelativeShouldReorderWithinSameContainer() {
        Task first = buildTask("A", "1.");
        Task second = buildTask("B", "2.");
        Task third = buildTask("C", "3.");
        List<Task> roots = new ArrayList<>(List.of(first, second, third));

        boolean moved = taskService.moveTaskRelative(roots, first, third, true);

        assertTrue(moved);
        assertEquals(List.of(second, third, first), roots);
    }

    @Test
    void moveSubTaskToTaskShouldReparentAndPreserveOrder() {
        Task source = buildTask("Source", "1.");
        Task target = buildTask("Target", "2.");
        SubTask moved = new SubTask(source, "S1", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 10, 0);
        SubTask existing = new SubTask(target, "S2", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 10, 0);
        source.getObservableSubTasks().add(moved);
        target.getObservableSubTasks().add(existing);

        Task updatedParent = taskService.moveSubTaskToTask(moved, target, 0);

        assertEquals(target, updatedParent);
        assertFalse(source.getObservableSubTasks().contains(moved));
        assertEquals(List.of(moved, existing), target.getObservableSubTasks());
        assertEquals(target, moved.getParentTask());
    }

    @Test
    void moveSubTaskRelativeShouldInsertAfterTarget() {
        Task task = buildTask("Task", "1.");
        SubTask first = new SubTask(task, "S1", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 10, 0);
        SubTask second = new SubTask(task, "S2", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 10, 0);
        SubTask third = new SubTask(task, "S3", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "u", 10, 0);
        task.getObservableSubTasks().addAll(first, second, third);

        Task updatedParent = taskService.moveSubTaskRelative(first, second, true);

        assertEquals(task, updatedParent);
        assertEquals(List.of(second, first, third), task.getObservableSubTasks());
    }

    @Test
    void applyGanttDateChangeShouldMoveMilestoneToEndDate() {
        Task milestone = buildTask("M", "1.");
        milestone.setTaskType("MILESTONE");

        boolean changed = taskService.applyGanttDateChange(
                milestone,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 5)
        );

        assertTrue(changed);
        assertEquals(LocalDate.of(2026, 2, 5), milestone.getStart());
        assertEquals(LocalDate.of(2026, 2, 5), milestone.getEnd());
    }

    @Test
    void applyGanttDateChangeShouldShiftSubTasksWhenTaskMovedWithoutResize() {
        Task task = buildTask("Task", "1.");
        task.setStart(LocalDate.of(2026, 1, 1));
        task.setEnd(LocalDate.of(2026, 1, 10));
        SubTask subTask = new SubTask(task, "S", "C", LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 4), "u", 10, 0);
        task.getObservableSubTasks().add(subTask);

        boolean changed = taskService.applyGanttDateChange(
                task,
                LocalDate.of(2026, 1, 4),
                LocalDate.of(2026, 1, 13)
        );

        assertTrue(changed);
        assertEquals(LocalDate.of(2026, 1, 5), subTask.getStart());
        assertEquals(LocalDate.of(2026, 1, 7), subTask.getEnd());
    }

    @Test
    void applyGanttDateChangeShouldNotShiftSubTasksWhenDurationChanged() {
        Task task = buildTask("Task", "1.");
        task.setStart(LocalDate.of(2026, 1, 1));
        task.setEnd(LocalDate.of(2026, 1, 10));
        SubTask subTask = new SubTask(task, "S", "C", LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 4), "u", 10, 0);
        task.getObservableSubTasks().add(subTask);

        boolean changed = taskService.applyGanttDateChange(
                task,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 15)
        );

        assertTrue(changed);
        assertEquals(LocalDate.of(2026, 1, 2), subTask.getStart());
        assertEquals(LocalDate.of(2026, 1, 4), subTask.getEnd());
    }

    @Test
    void applyGanttDateChangeShouldHandleSubTaskAndRejectInvalidInput() {
        Task parent = buildTask("Task", "1.");
        SubTask subTask = new SubTask(parent, "S", "C", LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 4), "u", 10, 0);

        boolean changedSubTask = taskService.applyGanttDateChange(
                subTask,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 3)
        );
        boolean changedUnknown = taskService.applyGanttDateChange(new Object(), LocalDate.now(), LocalDate.now());
        boolean changedNull = taskService.applyGanttDateChange(null, LocalDate.now(), LocalDate.now());

        assertTrue(changedSubTask);
        assertEquals(LocalDate.of(2026, 3, 1), subTask.getStart());
        assertEquals(LocalDate.of(2026, 3, 3), subTask.getEnd());
        assertFalse(changedUnknown);
        assertFalse(changedNull);
    }

    private Task buildTask(String name, String position) {
        Task task = new Task(name, "Company", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), TaskStatus.PLANNED, "Medium");
        task.setPosition(position);
        return task;
    }
}
