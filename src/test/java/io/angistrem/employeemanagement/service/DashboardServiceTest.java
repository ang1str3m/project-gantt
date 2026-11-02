package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.model.TaskStatus;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardServiceTest {

    private final DashboardService dashboardService = new DashboardService();

    @Test
    void buildSnapshotShouldAggregateRiskAndProgressMetrics() {
        Task task = new Task("A", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), TaskStatus.OVERDUE, "Medium");
        task.percentProperty().set(50.0);
        task.setStatus(TaskStatus.OVERDUE);
        task.setCriticalDependency(true);
        task.setResourceOverloaded(true);
        task.setBudgetOverrun(true);
        task.setDependencies(List.of(new TaskDependency("0.", "FS", 0)));
        task.getDependencies().get(0).setCritical(true);

        TreeItem<Object> root = new TreeItem<>(null);
        root.getChildren().add(new TreeItem<>(task));

        BudgetItem budgetItem = new BudgetItem();
        budgetItem.setPlannedAmount(100.0);
        budgetItem.setActualAmount(140.0);

        DashboardService.DashboardSnapshot snapshot = dashboardService.buildSnapshot(root, List.of(budgetItem));

        assertEquals(1, snapshot.taskCount());
        assertEquals(0, snapshot.subTaskCount());
        assertEquals(1, snapshot.overdueCount());
        assertEquals(50.0, snapshot.progress(), 0.0001);
        assertEquals(0.5, snapshot.normalizedProgress(), 0.0001);
        assertEquals(4, snapshot.totalRisks());
    }

    @Test
    void buildRiskTooltipTextShouldContainRiskCounters() {
        DashboardService.DashboardSnapshot snapshot = new DashboardService.DashboardSnapshot(
                1, 0, 0, 0, 10.0, 0.1, 3,
                1, 1, 0, 1, 0, 0, 1
        );

        String text = dashboardService.buildRiskTooltipText(snapshot);

        assertTrue(text.contains(": 1"));
        assertTrue(text.contains(": 0"));
    }
}
