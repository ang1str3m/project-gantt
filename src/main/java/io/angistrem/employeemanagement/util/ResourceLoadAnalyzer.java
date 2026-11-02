package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskResourceAssignment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResourceLoadAnalyzer {
    private ResourceLoadAnalyzer() {
    }

    public static Analysis analyze(List<ProjectResource> resources, List<Task> tasks) {
        Map<String, ProjectResource> resourcesById = new LinkedHashMap<>();
        if (resources != null) {
            for (ProjectResource resource : resources) {
                if (resource == null || resource.getId() == null || resource.getId().isBlank()) {
                    continue;
                }
                resourcesById.put(resource.getId(), resource);
            }
        }

        List<Task> leafTasks = new ArrayList<>();
        if (tasks != null) {
            for (Task task : tasks) {
                collectLeafTasks(task, leafTasks);
            }
        }

        List<ResourceLoadItem> items = new ArrayList<>();
        for (ProjectResource resource : resourcesById.values()) {
            items.add(analyzeResource(resource, leafTasks));
        }

        items.sort(Comparator
                .comparing(ResourceLoadItem::hasOverload).reversed()
                .thenComparing(ResourceLoadItem::maxOverload).reversed()
                .thenComparing(ResourceLoadItem::resourceName, String.CASE_INSENSITIVE_ORDER));

        long overloadedResources = items.stream().filter(ResourceLoadItem::hasOverload).count();
        long assignedResources = items.stream().filter(item -> !item.assignments().isEmpty()).count();

        return new Analysis(items, overloadedResources, assignedResources);
    }

    private static void collectLeafTasks(Task task, List<Task> target) {
        if (task == null) {
            return;
        }
        if (task.isGroup()) {
            for (Task child : task.getObservableChildTasks()) {
                collectLeafTasks(child, target);
            }
            return;
        }
        target.add(task);
    }

    private static ResourceLoadItem analyzeResource(ProjectResource resource, List<Task> tasks) {
        List<ResourceAssignmentView> assignments = new ArrayList<>();
        Map<LocalDate, Double> dailyLoad = new LinkedHashMap<>();

        for (Task task : tasks) {
            if (task == null || task.getStart() == null || task.getEnd() == null) {
                continue;
            }
            for (TaskResourceAssignment assignment : task.getResourceAssignments()) {
                if (assignment == null || !resource.getId().equals(assignment.getResourceId())) {
                    continue;
                }
                double allocation = Math.max(assignment.getAllocation(), 0.0);
                assignments.add(new ResourceAssignmentView(
                        task.getPosition(),
                        task.getName(),
                        task.getCompany(),
                        task.getStart(),
                        task.getEnd(),
                        assignment.getRole(),
                        allocation
                ));
                for (LocalDate date = task.getStart(); !date.isAfter(task.getEnd()); date = date.plusDays(1)) {
                    dailyLoad.merge(date, allocation, Double::sum);
                }
            }
        }

        assignments.sort(Comparator
                .comparing(ResourceAssignmentView::start, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ResourceAssignmentView::taskName, String.CASE_INSENSITIVE_ORDER));

        double capacity = Math.max(resource.getCapacityPerDay(), 0.0001);
        double peakLoad = dailyLoad.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double maxOverload = Math.max(0.0, peakLoad - capacity);

        List<LoadConflict> conflicts = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> entry : dailyLoad.entrySet()) {
            double overload = entry.getValue() - capacity;
            if (overload > 0.0001) {
                conflicts.add(new LoadConflict(entry.getKey(), entry.getValue(), overload));
            }
        }

        return new ResourceLoadItem(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getCompany(),
                resource.getRole(),
                capacity,
                peakLoad,
                maxOverload,
                conflicts,
                assignments
        );
    }

    public record Analysis(
            List<ResourceLoadItem> items,
            long overloadedResources,
            long assignedResources
    ) {
        public boolean hasData() {
            return items != null && !items.isEmpty();
        }

        public boolean hasAssignments() {
            return items != null && items.stream().anyMatch(item -> !item.assignments().isEmpty());
        }
    }

    public record ResourceLoadItem(
            String resourceId,
            String resourceName,
            String resourceType,
            String company,
            String role,
            double capacity,
            double peakLoad,
            double maxOverload,
            List<LoadConflict> conflicts,
            List<ResourceAssignmentView> assignments
    ) {
        public boolean hasOverload() {
            return maxOverload > 0.0001;
        }
    }

    public record LoadConflict(
            LocalDate date,
            double totalLoad,
            double overload
    ) {
    }

    public record ResourceAssignmentView(
            String taskPosition,
            String taskName,
            String assignee,
            LocalDate start,
            LocalDate end,
            String role,
            double allocation
    ) {
    }
}
