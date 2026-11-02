package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskResourceAssignment;
import io.angistrem.employeemanagement.util.ResourceLoadAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskHealthService {

    public void syncTaskAssignmentsWithResources(Task task, List<ProjectResource> projectResources) {
        if (task == null) {
            return;
        }
        Map<String, ProjectResource> resourcesById = mapResourcesById(projectResources);
        List<TaskResourceAssignment> filteredAssignments = new ArrayList<>();
        for (TaskResourceAssignment assignment : task.getResourceAssignments()) {
            if (assignment == null) {
                continue;
            }
            ProjectResource resource = resourcesById.get(assignment.getResourceId());
            if (resource != null) {
                assignment.setResourceName(resource.getName());
                if (assignment.getRole() == null || assignment.getRole().isBlank()) {
                    assignment.setRole(resource.getRole());
                }
                filteredAssignments.add(assignment);
            }
        }
        task.setResourceAssignments(filteredAssignments);
    }

    public void recalculateTaskCosts(Task task, List<ProjectResource> projectResources) {
        if (task == null || task.isGroup()) {
            return;
        }
        Map<String, ProjectResource> resourcesById = mapResourcesById(projectResources);

        double plannedCost = 0.0;
        double actualCost = 0.0;
        for (TaskResourceAssignment assignment : task.getResourceAssignments()) {
            if (assignment == null) {
                continue;
            }
            ProjectResource resource = resourcesById.get(assignment.getResourceId());
            if (resource == null || resource.getCostPerDay() <= 0.0) {
                continue;
            }
            double hoursPerDay = Math.max(resource.getCapacityPerDay(), 1.0) * 8.0;
            plannedCost += (assignment.getPlannedHours() / hoursPerDay) * resource.getCostPerDay() * Math.max(assignment.getAllocation(), 0.0);
            actualCost += (assignment.getActualHours() / hoursPerDay) * resource.getCostPerDay() * Math.max(assignment.getAllocation(), 0.0);
        }
        task.setPlannedCost(plannedCost);
        task.setActualCost(actualCost);
    }

    public void recalculateResourceAndBudgetFlags(
            List<Task> allTasks,
            List<ProjectResource> projectResources,
            List<BudgetItem> budgetItems
    ) {
        if (allTasks == null) {
            return;
        }

        for (Task task : allTasks) {
            task.setResourceOverloaded(false);
            task.setResourceLoadSummary("");
        }

        ResourceLoadAnalyzer.Analysis analysis = ResourceLoadAnalyzer.analyze(projectResources, allTasks);
        if (analysis != null && analysis.hasData()) {
            Map<String, Task> tasksByPosition = new HashMap<>();
            for (Task task : allTasks) {
                if (task != null && task.getPosition() != null) {
                    tasksByPosition.put(task.getPosition(), task);
                }
            }

            for (ResourceLoadAnalyzer.ResourceLoadItem item : analysis.items()) {
                if (!item.hasOverload() || item.conflicts().isEmpty()) {
                    continue;
                }
                for (ResourceLoadAnalyzer.ResourceAssignmentView assignment : item.assignments()) {
                    if (assignment == null || assignment.taskPosition() == null || assignment.start() == null || assignment.end() == null) {
                        continue;
                    }
                    boolean overlapsConflict = item.conflicts().stream().anyMatch(conflict ->
                            !conflict.date().isBefore(assignment.start()) && !conflict.date().isAfter(assignment.end()));
                    if (!overlapsConflict) {
                        continue;
                    }
                    Task task = tasksByPosition.get(assignment.taskPosition());
                    if (task == null) {
                        continue;
                    }
                    task.setResourceOverloaded(true);
                    String note = item.resourceName() + ": пик " + formatLoadValue(item.peakLoad())
                            + " при мощности " + formatLoadValue(item.capacity());
                    String current = task.getResourceLoadSummary();
                    task.setResourceLoadSummary(current == null || current.isBlank() ? note : current + "; " + note);
                }
            }
        }

        recalculateBudgetFlags(allTasks, budgetItems);
    }

    private void recalculateBudgetFlags(List<Task> allTasks, List<BudgetItem> budgetItems) {
        for (Task task : allTasks) {
            task.setBudgetOverrun(false);
            task.setBudgetSummary("");
        }
        if (budgetItems == null || budgetItems.isEmpty()) {
            return;
        }

        for (Task task : allTasks) {
            if (task == null || task.getPosition() == null || task.getPosition().isBlank()) {
                continue;
            }

            double planned = 0.0;
            double actual = 0.0;
            int matchedItems = 0;
            String taskPosition = task.getPosition();

            for (BudgetItem item : budgetItems) {
                if (item == null || item.getTaskPosition() == null || item.getTaskPosition().isBlank()) {
                    continue;
                }
                boolean matches = task.isGroup()
                        ? item.getTaskPosition().startsWith(taskPosition)
                        : item.getTaskPosition().equals(taskPosition);
                if (!matches) {
                    continue;
                }
                matchedItems++;
                planned += Math.max(0.0, item.getPlannedAmount());
                actual += Math.max(0.0, item.getActualAmount());
            }

            if (matchedItems == 0) {
                continue;
            }

            double variance = actual - planned;
            task.setBudgetSummary("Бюджет: план " + formatMoneyShort(planned)
                    + ", факт " + formatMoneyShort(actual)
                    + ", отклонение " + formatSignedMoneyShort(variance)
                    + " (" + matchedItems + " стат.)");

            if (actual > planned + 0.0001 || (planned <= 0.0001 && actual > 0.0001)) {
                task.setBudgetOverrun(true);
            }
        }
    }

    private String formatLoadValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.00001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String formatMoneyShort(double value) {
        return String.format(java.util.Locale.US, "%,.0f", value).replace(',', ' ') + " ₽";
    }

    private String formatSignedMoneyShort(double value) {
        String prefix = value > 0.0001 ? "+" : "";
        return prefix + formatMoneyShort(value);
    }

    private Map<String, ProjectResource> mapResourcesById(List<ProjectResource> projectResources) {
        Map<String, ProjectResource> resourcesById = new HashMap<>();
        if (projectResources == null) {
            return resourcesById;
        }
        for (ProjectResource resource : projectResources) {
            if (resource != null && resource.getId() != null) {
                resourcesById.put(resource.getId(), resource);
            }
        }
        return resourcesById;
    }
}

