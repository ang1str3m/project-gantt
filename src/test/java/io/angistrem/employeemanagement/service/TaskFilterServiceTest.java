package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskFilterServiceTest {

    private final TaskFilterService service = new TaskFilterService();

    @Test
    void taskMatchesFiltersShouldMatchByTaskFieldsSubTaskAndChildren() {
        Task root = createTask("Root Task", TaskStatus.PLANNED, "1.");
        root.setPriority("High");
        SubTask subTask = new SubTask(root, "Install Cable", "Company", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "m", 10, 0);
        root.getObservableSubTasks().add(subTask);

        Task child = createTask("Child", TaskStatus.IN_PROGRESS, "1.1.");
        child.setCompany("Alpha Build");
        root.getObservableChildTasks().add(child);

        assertTrue(service.taskMatchesFilters(root, "root", "ALL"));
        assertTrue(service.taskMatchesFilters(root, "cable", "ALL"));
        assertTrue(service.taskMatchesFilters(root, "alpha", "ALL"));
        assertFalse(service.taskMatchesFilters(root, "missing", "ALL"));
    }

    @Test
    void taskMatchesStatusShouldIncludeSubTasksAndChildren() {
        Task root = createTask("Root", TaskStatus.PLANNED, "1.");
        SubTask subTask = new SubTask(root, "Sub", "C", LocalDate.now(), LocalDate.now().plusDays(1), "m", 10, 0);
        root.getObservableSubTasks().add(subTask);
        subTask.setStatus(TaskStatus.COMPLETED);

        Task child = createTask("Child", TaskStatus.PLANNED, "1.1.");
        root.getObservableChildTasks().add(child);
        SubTask childSubTask = new SubTask(child, "Child Sub", "C", LocalDate.now(), LocalDate.now().plusDays(1), "m", 10, 0);
        child.getObservableSubTasks().add(childSubTask);
        childSubTask.setStatus(TaskStatus.OVERDUE);

        assertTrue(service.taskMatchesStatus(root, "ALL"));
        assertTrue(service.taskMatchesStatus(root, TaskStatus.COMPLETED));
        assertTrue(service.taskMatchesStatus(root, TaskStatus.OVERDUE));
        assertFalse(service.taskMatchesStatus(root, TaskStatus.IN_PROGRESS));
    }

    @Test
    void getSubTasksForTreeShouldReturnFilteredSubTasks() {
        Task task = createTask("Task", TaskStatus.PLANNED, "1.");
        SubTask planned = new SubTask(task, "Plan Work", "C1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "m", 10, 0);
        planned.setStatus(TaskStatus.PLANNED);
        SubTask completed = new SubTask(task, "Finish Work", "C2", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "m", 10, 10);
        completed.setStatus(TaskStatus.COMPLETED);
        task.getObservableSubTasks().addAll(planned, completed);

        List<SubTask> statusFiltered = service.getSubTasksForTree(task, "", TaskStatus.COMPLETED);
        List<SubTask> searchFiltered = service.getSubTasksForTree(task, "plan", "ALL");

        assertEquals(List.of(completed), statusFiltered);
        assertEquals(List.of(planned), searchFiltered);
    }

    @Test
    void getChildTasksForTreeShouldReturnOnlyMatchingChildrenWhenFilterActive() {
        Task root = createTask("Root", TaskStatus.PLANNED, "1.");
        Task matching = createTask("Install Roof", TaskStatus.IN_PROGRESS, "1.1.");
        Task nonMatching = createTask("Paint Wall", TaskStatus.PLANNED, "1.2.");
        root.getObservableChildTasks().addAll(matching, nonMatching);

        List<Task> filtered = service.getChildTasksForTree(root, "roof", "ALL");
        List<Task> unfiltered = service.getChildTasksForTree(root, "", "ALL");

        assertEquals(List.of(matching), filtered);
        assertEquals(List.of(matching, nonMatching), unfiltered);
    }

    private Task createTask(String name, String status, String position) {
        Task task = new Task(name, "Company", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), status, "Medium");
        task.setPosition(position);
        task.setStatus(status);
        return task;
    }
}
