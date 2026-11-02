package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.model.TaskStatus;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.Locale;

public class DashboardService {

    public DashboardSnapshot buildSnapshot(TreeItem<Object> root, List<BudgetItem> budgetItems) {
        DashboardMetrics metrics = new DashboardMetrics();
        if (root != null) {
            for (TreeItem<Object> item : root.getChildren()) {
                collectDashboardMetrics(item, metrics);
            }
        }
        metrics.budgetOverrunItems = countBudgetOverrunItems(budgetItems);

        double progress = metrics.taskCount == 0 ? 0.0 : metrics.totalTaskPercent / metrics.taskCount;
        double normalizedProgress = Math.max(0.0, Math.min(1.0, progress / 100.0));

        int totalRisks = metrics.criticalTasks
                + metrics.criticalDependencies
                + metrics.financialBlockers
                + metrics.resourceOverloadedTasks
                + metrics.budgetOverrunItems;

        return new DashboardSnapshot(
                metrics.taskCount,
                metrics.subTaskCount,
                metrics.completedCount,
                metrics.overdueCount,
                progress,
                normalizedProgress,
                totalRisks,
                metrics.criticalTasks,
                metrics.criticalDependencies,
                metrics.financialBlockers,
                metrics.resourceOverloadedTasks,
                metrics.budgetOverrunTasks,
                metrics.budgetOverrunItems,
                metrics.dependencyTasks
        );
    }

    public String formatOverallProgress(double progress) {
        return String.format(Locale.US, "%.1f%%", progress);
    }

    public String buildRiskTooltipText(DashboardSnapshot snapshot) {
        return "Индекс проектного риска показывает, сколько потенциально опасных точек есть в плане.\n\n"
                + "Критические задачи: " + snapshot.criticalTasks()
                + "\nКритические зависимости: " + snapshot.criticalDependencies()
                + "\nФинансовые блокеры (АВ/КС): " + snapshot.financialBlockers()
                + "\nПерегруженные задачи по ресурсам: " + snapshot.resourceOverloadedTasks()
                + "\nЗадачи с перерасходом бюджета: " + snapshot.budgetOverrunTasks()
                + "\nСтатьи бюджета с перерасходом: " + snapshot.budgetOverrunItems()
                + "\nЗадачи с зависимостями: " + snapshot.dependencyTasks()
                + "\n\nЧем выше число, тем внимательнее стоит проверить связи, сроки и финансовые ограничения.";
    }

    private int countBudgetOverrunItems(List<BudgetItem> budgetItems) {
        if (budgetItems == null) {
            return 0;
        }
        return (int) budgetItems.stream()
                .filter(item -> item != null && item.getPlannedAmount() > 0.0 && item.getActualAmount() > item.getPlannedAmount())
                .count();
    }

    private void collectDashboardMetrics(TreeItem<Object> item, DashboardMetrics metrics) {
        if (item == null || metrics == null) {
            return;
        }

        Object value = item.getValue();
        if (value instanceof Task task) {
            if (!task.isGroup()) {
                metrics.taskCount++;
                metrics.totalTaskPercent += task.getPercent();

                if (task.isCriticalDependency()) {
                    metrics.criticalTasks++;
                }
                if (task.isResourceOverloaded()) {
                    metrics.resourceOverloadedTasks++;
                }
                if (task.isBudgetOverrun()) {
                    metrics.budgetOverrunTasks++;
                }
                if (task.getDependencies() != null && !task.getDependencies().isEmpty()) {
                    metrics.dependencyTasks++;
                }
                metrics.criticalDependencies += (int) task.getDependencies().stream()
                        .filter(TaskDependency::isCritical)
                        .count();
                boolean advanceMissing = task.isRequireAdvanceBeforeStart() && task.getContractAdvanceDate() == null;
                boolean ksMissing = task.isRequireKsBeforeStart() && task.getKsPaymentDate() == null;
                if (advanceMissing || ksMissing) {
                    metrics.financialBlockers++;
                }
                if (TaskStatus.COMPLETED.equals(task.getStatus())) {
                    metrics.completedCount++;
                }
                if (TaskStatus.OVERDUE.equals(task.getStatus())) {
                    metrics.overdueCount++;
                }
            }
        } else if (value instanceof SubTask subTask) {
            metrics.subTaskCount++;
            metrics.criticalDependencies += (int) subTask.getDependencies().stream()
                    .filter(TaskDependency::isCritical)
                    .count();
            if (TaskStatus.COMPLETED.equals(subTask.getStatus())) {
                metrics.completedCount++;
            }
            if (TaskStatus.OVERDUE.equals(subTask.getStatus())) {
                metrics.overdueCount++;
            }
        }

        for (TreeItem<Object> child : item.getChildren()) {
            collectDashboardMetrics(child, metrics);
        }
    }

    private static final class DashboardMetrics {
        private int taskCount;
        private int subTaskCount;
        private int completedCount;
        private int overdueCount;
        private int criticalTasks;
        private int criticalDependencies;
        private int dependencyTasks;
        private int financialBlockers;
        private int resourceOverloadedTasks;
        private int budgetOverrunTasks;
        private int budgetOverrunItems;
        private double totalTaskPercent;
    }

    public record DashboardSnapshot(
            int taskCount,
            int subTaskCount,
            int completedCount,
            int overdueCount,
            double progress,
            double normalizedProgress,
            int totalRisks,
            int criticalTasks,
            int criticalDependencies,
            int financialBlockers,
            int resourceOverloadedTasks,
            int budgetOverrunTasks,
            int budgetOverrunItems,
            int dependencyTasks
    ) {
    }
}

