package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;

import java.util.List;

public class TaskFilterService {

    public boolean taskMatchesFilters(Task task, String rawSearchQuery, String statusFilter) {
        if (task == null || !taskMatchesStatus(task, statusFilter)) {
            return false;
        }

        String searchQuery = normalizeSearchQuery(rawSearchQuery);
        if (searchQuery.isBlank()) {
            return true;
        }

        if (contains(task.getName(), searchQuery)
                || contains(task.getCompany(), searchQuery)
                || contains(task.getStatus(), searchQuery)
                || contains(task.getPriority(), searchQuery)
                || contains(task.getPosition(), searchQuery)) {
            return true;
        }

        if (task.getObservableSubTasks().stream().anyMatch(subTask -> subTaskMatchesQuery(subTask, searchQuery))) {
            return true;
        }

        return task.getObservableChildTasks().stream()
                .anyMatch(childTask -> taskMatchesFilters(childTask, searchQuery, statusFilter));
    }

    public boolean taskMatchesStatus(Task task, String statusFilter) {
        if (task == null || statusFilter == null) {
            return false;
        }
        if ("ALL".equals(statusFilter)) {
            return true;
        }
        if (statusFilter.equals(task.getStatus())) {
            return true;
        }
        if (task.getObservableSubTasks().stream().anyMatch(subTask -> statusFilter.equals(subTask.getStatus()))) {
            return true;
        }
        return task.getObservableChildTasks().stream().anyMatch(childTask -> taskMatchesStatus(childTask, statusFilter));
    }

    public List<SubTask> getSubTasksForTree(Task task, String rawSearchQuery, String statusFilter) {
        if (task == null) {
            return List.of();
        }

        String searchQuery = normalizeSearchQuery(rawSearchQuery);
        if (searchQuery.isBlank() && "ALL".equals(statusFilter)) {
            return task.getObservableSubTasks();
        }

        return task.getObservableSubTasks().stream()
                .filter(subTask -> subTaskPassesFilters(subTask, searchQuery, statusFilter))
                .toList();
    }

    public List<Task> getChildTasksForTree(Task task, String rawSearchQuery, String statusFilter) {
        if (task == null) {
            return List.of();
        }

        String searchQuery = normalizeSearchQuery(rawSearchQuery);
        if (searchQuery.isBlank() && "ALL".equals(statusFilter)) {
            return task.getObservableChildTasks();
        }

        return task.getObservableChildTasks().stream()
                .filter(childTask -> taskMatchesFilters(childTask, searchQuery, statusFilter))
                .toList();
    }

    private boolean subTaskMatchesQuery(SubTask subTask, String searchQuery) {
        return contains(subTask.getName(), searchQuery)
                || contains(subTask.getCompany(), searchQuery)
                || contains(subTask.getStatus(), searchQuery)
                || contains(subTask.getSubPosition(), searchQuery)
                || contains(subTask.getUnitOfMeasurement(), searchQuery);
    }

    private boolean subTaskPassesFilters(SubTask subTask, String searchQuery, String statusFilter) {
        boolean statusOk = "ALL".equals(statusFilter) || statusFilter.equals(subTask.getStatus());
        boolean searchOk = searchQuery.isBlank() || subTaskMatchesQuery(subTask, searchQuery);
        return statusOk && searchOk;
    }

    private String normalizeSearchQuery(String searchQuery) {
        return searchQuery == null ? "" : searchQuery.trim().toLowerCase();
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
