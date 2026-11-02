package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import javafx.scene.paint.Color;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GanttPresentationService {

    public Color colorByDependencyType(String type, Settings settings) {
        if (settings == null) {
            return switch (type) {
                case "SS" -> Color.web("#2563EB");
                case "FF" -> Color.web("#0F766E");
                case "SF" -> Color.web("#B45309");
                default -> Color.web("#7C848F");
            };
        }
        return switch (type) {
            case "SS" -> parseColor(settings.getDependencySsColor(), "#2563EB");
            case "FF" -> parseColor(settings.getDependencyFfColor(), "#0F766E");
            case "SF" -> parseColor(settings.getDependencySfColor(), "#B45309");
            default -> parseColor(settings.getDependencyFsColor(), "#7C848F");
        };
    }

    public Color resolveTaskColor(Task task, Settings settings) {
        if (task != null && task.isGroup()) {
            return Color.web("#334155");
        }
        if (task != null && task.isResourceOverloaded() && task.isBudgetOverrun()) {
            return Color.web("#c2410c");
        }
        if (task != null && task.isResourceOverloaded()) {
            return Color.web("#d97706");
        }
        if (task != null && task.isBudgetOverrun()) {
            return Color.web("#7c3aed");
        }
        if (task != null && task.isCriticalDependency()) {
            if (settings != null) {
                return parseColor(settings.getCriticalTaskColor(), "#dc2626");
            }
            return Color.web("#dc2626");
        }
        if (settings == null) {
            String priorityFallback = task == null || task.getPriority() == null
                    ? ""
                    : task.getPriority().toLowerCase(Locale.ROOT);
            if (priorityFallback.contains("выс")) {
                return Color.web("#dc2626");
            }
            if (priorityFallback.contains("низ")) {
                return Color.web("#0f766e");
            }
            if (priorityFallback.contains("сред")) {
                return Color.web("#2563eb");
            }
            return Color.web("#2563eb");
        }

        String priority = task == null || task.getPriority() == null ? "" : task.getPriority().toLowerCase(Locale.ROOT);
        if (priority.contains("выс")) {
            return parseColor(settings.getPriorityHighColor(), "#dc2626");
        }
        if (priority.contains("низ")) {
            return parseColor(settings.getPriorityLowColor(), "#0f766e");
        }
        if (priority.contains("сред")) {
            return parseColor(settings.getPriorityMediumColor(), "#2563eb");
        }
        return parseColor(settings.getTaskColor(), "#2563eb");
    }

    public Color resolveSubTaskColor(Settings settings) {
        if (settings == null) {
            return Color.web("#16a34a");
        }
        return parseColor(settings.getSubTaskColor(), "#16a34a");
    }

    public String buildTaskTooltip(Task task, GanttDateService ganttDateService, DateTimeFormatter formatter) {
        if (task == null || ganttDateService == null || formatter == null) {
            return "";
        }
        String period = ganttDateService.formatPeriod(task.getStart(), task.getEnd(), formatter);
        String type = task.isGroup() ? "Группа" : (task.isMilestone() ? "Веха" : "Задача");
        List<String> notes = new ArrayList<>();
        if (task.isCriticalDependency()) {
            notes.add("Риск: задача отмечена как критичная");
        }
        if (task.isResourceOverloaded() && task.getResourceLoadSummary() != null && !task.getResourceLoadSummary().isBlank()) {
            notes.add("Ресурсы: " + task.getResourceLoadSummary());
        }
        if (task.isBudgetOverrun() && task.getBudgetSummary() != null && !task.getBudgetSummary().isBlank()) {
            notes.add(task.getBudgetSummary());
        }
        return type + ": " + safeText(task.getName()) + "\n"
                + "Исполнитель: " + safeText(task.getCompany()) + "\n"
                + "Период: " + period + "\n"
                + "Статус: " + safeText(task.getStatus()) + "\n"
                + "Приоритет: " + safeText(task.getPriority()) + "\n"
                + "Прогресс: " + String.format(Locale.US, "%.1f%%", task.getPercent())
                + (task.isGroup() ? "\nВложенных задач: " + task.getObservableChildTasks().size() : "")
                + (notes.isEmpty() ? "" : "\n" + String.join("\n", notes));
    }

    public String buildSubTaskTooltip(SubTask subTask, GanttDateService ganttDateService, DateTimeFormatter formatter) {
        if (subTask == null || ganttDateService == null || formatter == null) {
            return "";
        }
        return "Подзадача: " + safeText(subTask.getName()) + "\n"
                + "Исполнитель: " + safeText(subTask.getCompany()) + "\n"
                + "Период: " + ganttDateService.formatPeriod(subTask.getStart(), subTask.getEnd(), formatter) + "\n"
                + "Статус: " + safeText(subTask.getStatus()) + "\n"
                + "Прогресс: " + String.format(Locale.US, "%.1f%%", subTask.getPercent());
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Color parseColor(String hex, String fallback) {
        try {
            return Color.web(hex);
        } catch (Exception ignored) {
            return Color.web(fallback);
        }
    }
}
