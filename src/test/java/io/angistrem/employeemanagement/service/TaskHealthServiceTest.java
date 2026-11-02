package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskResourceAssignment;
import io.angistrem.employeemanagement.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskHealthServiceTest {

    private final TaskHealthService service = new TaskHealthService();

    @Test
    void syncTaskAssignmentsWithResourcesShouldFilterMissingAndFillMetadata() {
        ProjectResource resource = new ProjectResource();
        resource.setId("r-1");
        resource.setName("Alice");
        resource.setRole("Engineer");

        Task task = createTask("1.");
        TaskResourceAssignment valid = new TaskResourceAssignment();
        valid.setResourceId("r-1");
        valid.setRole("");
        TaskResourceAssignment missing = new TaskResourceAssignment();
        missing.setResourceId("missing");
        task.setResourceAssignments(new ArrayList<>(List.of(valid, missing)));

        service.syncTaskAssignmentsWithResources(task, List.of(resource));

        assertEquals(1, task.getResourceAssignments().size());
        TaskResourceAssignment synced = task.getResourceAssignments().get(0);
        assertEquals("Alice", synced.getResourceName());
        assertEquals("Engineer", synced.getRole());
    }

    @Test
    void recalculateTaskCostsShouldUseResourceRateAndHours() {
        ProjectResource resource = new ProjectResource();
        resource.setId("r-1");
        resource.setCapacityPerDay(1.0);
        resource.setCostPerDay(800.0);

        Task task = createTask("1.");
        TaskResourceAssignment assignment = new TaskResourceAssignment();
        assignment.setResourceId("r-1");
        assignment.setAllocation(1.0);
        assignment.setPlannedHours(8.0);
        assignment.setActualHours(4.0);
        task.setResourceAssignments(List.of(assignment));

        service.recalculateTaskCosts(task, List.of(resource));

        assertEquals(800.0, task.getPlannedCost(), 0.0001);
        assertEquals(400.0, task.getActualCost(), 0.0001);
    }

    @Test
    void recalculateResourceAndBudgetFlagsShouldMarkOverloadAndBudgetOverrun() {
        ProjectResource resource = new ProjectResource();
        resource.setId("r-1");
        resource.setName("Core");
        resource.setCapacityPerDay(1.0);

        Task task = createTask("1.");
        TaskResourceAssignment assignment = new TaskResourceAssignment();
        assignment.setResourceId("r-1");
        assignment.setAllocation(2.0);
        task.setResourceAssignments(List.of(assignment));

        BudgetItem budgetItem = new BudgetItem();
        budgetItem.setTaskPosition("1.");
        budgetItem.setPlannedAmount(100.0);
        budgetItem.setActualAmount(150.0);

        service.recalculateResourceAndBudgetFlags(
                List.of(task),
                List.of(resource),
                List.of(budgetItem)
        );

        assertTrue(task.isResourceOverloaded());
        assertNotNull(task.getResourceLoadSummary());
        assertFalse(task.getResourceLoadSummary().isBlank());
        assertTrue(task.isBudgetOverrun());
        assertTrue(task.getBudgetSummary().contains("Бюджет"));
    }

    private Task createTask(String position) {
        Task task = new Task("Task", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), TaskStatus.IN_PROGRESS, "Средний");
        task.setPosition(position);
        return task;
    }
}

