package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TaskDependencyScheduler {

    private TaskDependencyScheduler() {
    }

    public static void applyConstraints(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        Map<String, Object> byPosition = buildScheduleMap(tasks);
        validateNoCycles(byPosition);

        boolean changed;
        int guard = 0;
        do {
            changed = false;
            guard++;
            for (Object item : byPosition.values()) {
                changed |= applyConstraint(item, byPosition);
            }
        } while (changed && guard < 32);
    }

    private static Map<String, Object> buildScheduleMap(List<Task> tasks) {
        Map<String, Object> byPosition = new HashMap<>();
        for (Task task : tasks) {
            collectScheduleItems(task, byPosition);
        }
        return byPosition;
    }

    private static void collectScheduleItems(Task task, Map<String, Object> byPosition) {
        if (task == null) {
            return;
        }

        if (!task.isGroup() && task.getPosition() != null) {
            byPosition.put(task.getPosition().trim(), task);
        }
        for (SubTask subTask : task.getObservableSubTasks()) {
            if (subTask.getSubPosition() != null) {
                byPosition.put(subTask.getSubPosition().trim(), subTask);
            }
        }
        for (Task childTask : task.getObservableChildTasks()) {
            collectScheduleItems(childTask, byPosition);
        }
    }

    private static void validateNoCycles(Map<String, Object> byPosition) {
        Map<String, List<String>> graph = new HashMap<>();
        for (String key : byPosition.keySet()) {
            graph.put(key, new ArrayList<>());
        }

        for (Map.Entry<String, Object> entry : byPosition.entrySet()) {
            for (TaskDependency dependency : getDependencies(entry.getValue())) {
                if (dependency == null || dependency.getPredecessorPosition() == null) {
                    continue;
                }
                String predecessor = dependency.getPredecessorPosition().trim();
                if (graph.containsKey(predecessor)) {
                    graph.get(predecessor).add(entry.getKey());
                }
            }
        }

        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String node : graph.keySet()) {
            if (dfsHasCycle(node, graph, visiting, visited)) {
                throw new IllegalStateException("Обнаружена циклическая зависимость в графике.");
            }
        }
    }

    private static boolean dfsHasCycle(
            String node,
            Map<String, List<String>> graph,
            Set<String> visiting,
            Set<String> visited
    ) {
        if (visited.contains(node)) {
            return false;
        }
        if (!visiting.add(node)) {
            return true;
        }

        for (String next : graph.getOrDefault(node, List.of())) {
            if (dfsHasCycle(next, graph, visiting, visited)) {
                return true;
            }
        }

        visiting.remove(node);
        visited.add(node);
        return false;
    }

    private static boolean applyConstraint(Object item, Map<String, Object> byPosition) {
        LocalDate start = getStart(item);
        LocalDate end = getEnd(item);
        if (start == null || end == null) {
            return false;
        }

        LocalDate earliestStart = null;
        long durationDays = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);

        for (TaskDependency dependency : getDependencies(item)) {
            if (dependency == null || dependency.getPredecessorPosition() == null || dependency.getPredecessorPosition().isBlank()) {
                continue;
            }

            Object predecessor = byPosition.get(dependency.getPredecessorPosition().trim());
            if (predecessor == null || getStart(predecessor) == null || getEnd(predecessor) == null) {
                continue;
            }

            int lag = dependency.getLagDays();
            String type = dependency.getType() == null ? "FS" : dependency.getType().trim().toUpperCase();

            LocalDate candidateStart;
            switch (type) {
                case "SS" -> candidateStart = getStart(predecessor).plusDays(lag);
                case "FF" -> {
                    LocalDate requiredEnd = getEnd(predecessor).plusDays(lag);
                    candidateStart = requiredEnd.minusDays(durationDays - 1);
                }
                case "SF" -> {
                    LocalDate requiredEnd = getStart(predecessor).plusDays(lag);
                    candidateStart = requiredEnd.minusDays(durationDays - 1);
                }
                default -> candidateStart = getEnd(predecessor).plusDays(1L + lag);
            }

            if (earliestStart == null || candidateStart.isAfter(earliestStart)) {
                earliestStart = candidateStart;
            }
        }

        if (item instanceof Task task) {
            if (task.isGroup()) {
                return false;
            }
            if (task.isRequireAdvanceBeforeStart() && task.getContractAdvanceDate() != null
                    && (earliestStart == null || task.getContractAdvanceDate().isAfter(earliestStart))) {
                earliestStart = task.getContractAdvanceDate();
            }

            if (task.isRequireKsBeforeStart() && task.getKsPaymentDate() != null
                    && (earliestStart == null || task.getKsPaymentDate().isAfter(earliestStart))) {
                earliestStart = task.getKsPaymentDate();
            }
        }

        if (earliestStart == null || earliestStart.isBefore(start)) {
            // Keep manual user shifts in place: rescheduling may push an item later
            // because of dependencies or finance gates, but should not snap it back
            // to an earlier date the user already overrode manually.
            earliestStart = start;
        }

        if (earliestStart.equals(start)) {
            return false;
        }

        long shiftDays = ChronoUnit.DAYS.between(start, earliestStart);
        shiftItem(item, shiftDays);
        return true;
    }

    private static List<TaskDependency> getDependencies(Object item) {
        if (item instanceof Task task) {
            return task.getDependencies();
        }
        if (item instanceof SubTask subTask) {
            return subTask.getDependencies();
        }
        return List.of();
    }

    private static LocalDate getStart(Object item) {
        if (item instanceof Task task) {
            return task.getStart();
        }
        if (item instanceof SubTask subTask) {
            return subTask.getStart();
        }
        return null;
    }

    private static LocalDate getEnd(Object item) {
        if (item instanceof Task task) {
            return task.getEnd();
        }
        if (item instanceof SubTask subTask) {
            return subTask.getEnd();
        }
        return null;
    }

    private static void shiftItem(Object item, long shiftDays) {
        if (item instanceof Task task) {
            if (task.isGroup()) {
                return;
            }
            task.setStart(task.getStart().plusDays(shiftDays));
            task.setEnd(task.getEnd().plusDays(shiftDays));

            for (SubTask subTask : task.getObservableSubTasks()) {
                if (subTask.getStart() != null) {
                    subTask.setStart(subTask.getStart().plusDays(shiftDays));
                }
                if (subTask.getEnd() != null) {
                    subTask.setEnd(subTask.getEnd().plusDays(shiftDays));
                }
            }
            return;
        }

        if (item instanceof SubTask subTask) {
            if (subTask.getStart() != null) {
                subTask.setStart(subTask.getStart().plusDays(shiftDays));
            }
            if (subTask.getEnd() != null) {
                subTask.setEnd(subTask.getEnd().plusDays(shiftDays));
            }
        }
    }
}
